package com.spotifuck.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media.session.MediaButtonReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.*
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

@UnstableApi
class WebService : MediaLibraryService() {

    companion object {
        @JvmField var isServiceRunning = false
        @JvmField var isPlaying = false
        @JvmField var isFavorite = true
        @JvmField var artistName = "No Artist"
        @JvmField var trackName = "No Track"
        @JvmField var repeatState = "false"
        @JvmField var shuffleState = "false"
        @JvmField var trackDuration: Long = 0
        @JvmField var trackPosition: Long = 0
        @JvmField var lastPositionUpdateTime: Long = 0
        @JvmField var albumArt: Bitmap? = null
        @JvmField var lastCoverUrl: String = ""
        @JvmField var repeatIconResId: Int = R.drawable.repoff
        @JvmField var shuffleIconResId: Int = R.drawable.repoff

        private var sInstance: WebService? = null

        @JvmStatic
        fun updatePlaybackState(onlyWidgets: Boolean = false) {
            sInstance?.let { service ->
                repeatIconResId = when (repeatState) {
                    "true" -> R.drawable.repon
                    "mixed" -> R.drawable.repone
                    else -> R.drawable.repoff
                }
                shuffleIconResId = when (shuffleState) {
                    "true" -> R.drawable.shuffle
                    "mixed" -> R.drawable.smart_shuffle
                    else -> R.drawable.repoff
                }

                service.mPlayer?.notifyStateChanged()

                val buttons = service.createMediaButtonPreferences()
                service.mMediaSession?.setMediaButtonPreferences(buttons)
                service.mMediaSession?.setCustomLayout(buttons)
                
                // Update platform session for Android 13+ and legacy controllers
                service.updatePlatformSession()

                service.updateWidgets()
                
                if (isPlaying && !service.mWakeLock.isHeld) service.mWakeLock.acquire(10 * 60 * 1000L)
                else if (!isPlaying && service.mWakeLock.isHeld) service.mWakeLock.release()
            }
        }
    }

    private var mMediaSession: MediaLibrarySession? = null
    private var mPlayer: WebViewPlayer? = null
    private lateinit var mWakeLock: PowerManager.WakeLock
    private var mAudioManager: AudioManager? = null
    private var mAudioDeviceCallback: AudioDeviceCallback? = null

    class MediaActionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            if (isServiceRunning && AppSingleton.isPlayerLoaded && sInstance != null && sInstance?.mPlayer != null) {
                val action = intent.action ?: return
                val player = sInstance?.mPlayer ?: return
                
                when (action) {
                    "android.intent.action.HEADSET_PLUG" -> {
                        val state = intent.getIntExtra("state", -1)
                        if (AppSingleton.headsetPlay && state == 1) player.play()
                        else if (AppSingleton.headsetPause && state == 0) player.pause()
                    }
                    "REPEAT_ACTION" -> {
                        AppSingleton.globalWebView?.post { AppSingleton.globalWebView?.evaluateJavascript("actRepeat();", null) }
                    }
                    "ADDTOFAV_ACTION" -> {
                        AppSingleton.globalWebView?.post { AppSingleton.globalWebView?.evaluateJavascript("actAddToFav();", null) }
                    }
                    "SHUFFLE_ACTION" -> {
                        AppSingleton.globalWebView?.post { AppSingleton.globalWebView?.evaluateJavascript("actShuffle();", null) }
                    }
                    "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED" -> {
                        val btState = intent.getIntExtra(android.bluetooth.BluetoothProfile.EXTRA_STATE, 0)
                        if (AppSingleton.btPlay && btState == 2) player.play()
                        else if (AppSingleton.btPause && btState == 0) player.pause()
                    }
                    "WIDGET_PLAYPAUSE" -> {
                        if (player.isPlaying) player.pause()
                        else player.play()
                    }
                    "WIDGET_NEXT" -> player.seekToNext()
                    "WIDGET_PREV" -> player.seekToPrevious()
                    "WIDGET_VOL_UP" -> {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                        updatePlaybackState(true)
                    }
                    "WIDGET_VOL_DOWN" -> {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                        updatePlaybackState(true)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sInstance = this
        
        setMediaNotificationProvider(CustomNotificationProvider())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("SpotifuckChannel", getString(R.string.app_name) + " Background Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }

        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioDeviceCallback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                    if (isServiceRunning && AppSingleton.isPlayerLoaded && mPlayer != null) {
                        for (device in addedDevices) {
                            val type = device.type
                            if (AppSingleton.headsetPlay && (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES)) {
                                mPlayer?.play()
                            } else if (AppSingleton.btPlay && (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO)) {
                                mPlayer?.play()
                            }
                        }
                    }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                    if (isServiceRunning && AppSingleton.isPlayerLoaded && mPlayer != null) {
                        for (device in removedDevices) {
                            val type = device.type
                            if (AppSingleton.headsetPause && (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES)) {
                                mPlayer?.pause()
                            } else if (AppSingleton.btPause && (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO)) {
                                mPlayer?.pause()
                            }
                        }
                    }
                }
            }
            mAudioManager?.registerAudioDeviceCallback(mAudioDeviceCallback, Handler(Looper.getMainLooper()))
        }

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getString(R.string.app_name) + ":KeepAlive")
        mWakeLock.setReferenceCounted(false)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mMediaSession
    }

    private fun updatePlatformSession() {
        val session = mMediaSession ?: return
        val sessionCompat = session.sessionCompatToken as? MediaSessionCompat.Token ?: return
        
        // This is a bit tricky with Media3 as it manages the platform session.
        // But we can try to influence it by updating metadata and playback state if needed.
        // Actually, Media3 should sync automatically, but let's ensure custom layout is set.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isServiceRunning) {
            isServiceRunning = true
            setupMediaSession()
            startForegroundService()
        } else if (intent?.action == "STOP_SERVICE") {
            stopService()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (AppSingleton.stopOnSwipe) {
            stopService()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun setupMediaSession() {
        val basePlayer = WebViewPlayer(Looper.getMainLooper())
        mPlayer = basePlayer
        
        // Wrap the player to force "Next" and "Previous" commands as available
        // This is often required on Android 13+ to ensure the system UI shows all slots
        val forwardingPlayer = object : ForwardingPlayer(basePlayer) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SET_REPEAT_MODE)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT, 
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SET_REPEAT_MODE,
                    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }
            }
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        mMediaSession = MediaLibrarySession.Builder(this, forwardingPlayer, LibraryCallback())
            .setSessionActivity(pendingIntent)
            .build()
            
        updatePlaybackState()
    }

    private inner class CustomNotificationProvider : MediaNotification.Provider {
        @OptIn(UnstableApi::class)
        override fun createNotification(
            session: MediaSession,
            customLayout: ImmutableList<CommandButton>,
            actionFactory: MediaNotification.ActionFactory,
            onNotificationChangedCallback: MediaNotification.Provider.Callback
        ): MediaNotification {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val builder = NotificationCompat.Builder(this@WebService, "SpotifuckChannel")
                .setSmallIcon(R.drawable.sfnotif)
                .setContentTitle(trackName)
                .setContentText(artistName)
                .setLargeIcon(albumArt)
                .setContentIntent(PendingIntent.getActivity(this@WebService, 0, Intent(this@WebService, MainActivity::class.java), flags))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)

            // Add all 5 actions to the builder in the exact order
            // 0: Favorite, 1: Prev, 2: Play/Pause, 3: Next, 4: Repeat
            val buttons = createMediaButtonPreferences()
            buttons.forEach { button ->
                val action = if (button.sessionCommand != null) {
                    actionFactory.createCustomActionFromCustomCommandButton(session, button)
                } else {
                    actionFactory.createMediaAction(session, IconCompat.createWithResource(this@WebService, button.iconResId), button.displayName, button.playerCommand)
                }
                builder.addAction(action)
            }

            val style = androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(session.sessionCompatToken as MediaSessionCompat.Token)
                .setShowActionsInCompactView(1, 2, 3) // Previous, Play/Pause, Next

            builder.setStyle(style)
            return MediaNotification(1337, builder.build())
        }

        override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean {
            return false
        }
    }

    private fun createMediaButtonPreferences(): List<CommandButton> {
        val buttons = mutableListOf<CommandButton>()
        
        // Slot 0: Favorite
        buttons.add(CommandButton.Builder()
            .setDisplayName("Favorite")
            .setSessionCommand(SessionCommand("ADDTOFAV_ACTION", Bundle.EMPTY))
            .setIconResId(if (isFavorite) R.drawable.favon else R.drawable.favoff)
            .build())

        // Slot 1: Previous
        buttons.add(CommandButton.Builder()
            .setDisplayName("Previous")
            .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS)
            .setIconResId(R.drawable.previous)
            .build())

        // Slot 2: Play/Pause
        buttons.add(CommandButton.Builder()
            .setDisplayName(if (isPlaying) "Pause" else "Play")
            .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
            .setIconResId(if (isPlaying) R.drawable.pause else R.drawable.play)
            .build())

        // Slot 3: Next
        buttons.add(CommandButton.Builder()
            .setDisplayName("Next")
            .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT)
            .setIconResId(R.drawable.next)
            .build())

        // Slot 4: Repeat
        buttons.add(CommandButton.Builder()
            .setDisplayName("Repeat")
            .setSessionCommand(SessionCommand("REPEAT_ACTION", Bundle.EMPTY))
            .setIconResId(repeatIconResId)
            .build())
            
        return buttons
    }

    private fun startForegroundService() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1337, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1337, notification)
        }
    }

    private fun createNotification(): Notification {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val builder = NotificationCompat.Builder(this, "SpotifuckChannel")
                .setSmallIcon(R.drawable.sfnotif)
                .setContentTitle(if (AppSingleton.isAndAutoEnabled) trackName else getString(R.string.srv_title))
                .setContentText(if (AppSingleton.isAndAutoEnabled) artistName else getString(R.string.srv_notif))
                .setLargeIcon(albumArt)
                .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), flags))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)

        // Add actions for immediate feedback and system media control consistency
        builder.addAction(R.drawable.previous, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
        builder.addAction(if (isPlaying) R.drawable.pause else R.drawable.play, if (isPlaying) "Pause" else "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE))
        builder.addAction(R.drawable.next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))

        mMediaSession?.let { session ->
            builder.setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(session.sessionCompatToken as MediaSessionCompat.Token)
                    .setShowActionsInCompactView(0, 1, 2))
        }

        return builder.build()
    }

    private fun updateWidgets() {
        HomeWidget.updateAllWidgets(this)
    }

    private fun stopService() {
        isServiceRunning = false
        isPlaying = false
        if (mWakeLock.isHeld) mWakeLock.release()
        
        mMediaSession?.run {
            player.release()
            release()
        }
        mMediaSession = null
        mPlayer = null
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mAudioManager != null && mAudioDeviceCallback != null) {
            mAudioManager?.unregisterAudioDeviceCallback(mAudioDeviceCallback!!)
        }
        
        stopForeground(true)
        stopSelf()
        
        Handler(Looper.getMainLooper()).post {
            updateWidgets()
            AppSingleton.globalWebView?.apply {
                loadUrl("about:blank")
                stopLoading()
                destroy()
            }
            AppSingleton.globalWebView = null
        }
        AppSingleton.isPlayerLoaded = false
        AppSingleton.cleanup()
        sInstance = null
        albumArt = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId("sfroot")
                .setMediaMetadata(MediaMetadata.Builder().setIsBrowsable(true).build())
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetChildren(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, parentId: String, page: Int, pageSize: Int, params: LibraryParams?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            if ("sfroot" == parentId) {
                val items = mutableListOf<MediaItem>()
                items.add(createBrowsableItem("playlists", "\uD83C\uDFB5 " + getString(R.string.aa_playlists)))
                items.add(createBrowsableItem("albums", "\uD83D\uDCBF " + getString(R.string.aa_albums)))
                items.add(createBrowsableItem("artists", "\uD83D\uDC64 " + getString(R.string.aa_artists)))
                items.add(createBrowsableItem("podcasts", "\uD83C\uDF99 " + getString(R.string.aa_podcasts)))
                return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
            } else {
                val resultFuture = SettableFuture<LibraryResult<ImmutableList<MediaItem>>>()
                AppSingleton.globalWebView?.post {
                    val js = "JSON.stringify(window.mediaLib?.['$parentId'])"
                    AppSingleton.globalWebView?.evaluateJavascript(js) { value ->
                        val items = mutableListOf<MediaItem>()
                        if (value != null && value != "null" && value != "\"null\"") {
                            try {
                                val unquoted = value.replace("^\"|\"$".toRegex(), "").replace("\\\"", "\"")
                                val jsonArray = JSONArray(unquoted)
                                for (i in 0 until jsonArray.length()) {
                                    val jSONObject = jsonArray.getJSONObject(i)
                                    val id = jSONObject.optString("id")
                                    val name = jSONObject.optString("name", "Unknown")
                                    val image = jSONObject.optString("image")
                                    val artists = jSONObject.optJSONArray("artists")
                                    var sub = ""
                                    if (artists != null && artists.length() > 0) {
                                        sub = getString(R.string.aa_by) + " " + (0 until artists.length()).map { artists.getString(it) }.joinToString(", ")
                                    } else if ("podcasts" == parentId) {
                                        sub = "Podcast"
                                    }
                                    
                                    val metadata = MediaMetadata.Builder()
                                            .setTitle(name)
                                            .setSubtitle(sub)
                                            .setArtworkUri(if (image != null) Uri.parse(image) else null)
                                            .setIsPlayable(true)
                                            .build()
                                    items.add(MediaItem.Builder().setMediaId(id).setMediaMetadata(metadata).build())
                                }
                            } catch (e: JSONException) {
                                Log.e("Spotifuck", "JSON error", e)
                            }
                        }
                        resultFuture.set(LibraryResult.ofItemList(items, params))
                    }
                } ?: resultFuture.set(LibraryResult.ofItemList(emptyList(), params))
                return resultFuture
            }
        }

        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            if (sInstance != null) {
                session.setCustomLayout(controller, sInstance!!.createMediaButtonPreferences())
            }
        }

        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(SessionCommand("ADDTOFAV_ACTION", Bundle.EMPTY))
                .add(SessionCommand("REPEAT_ACTION", Bundle.EMPTY))
                .add(SessionCommand("SHUFFLE_ACTION", Bundle.EMPTY))
                .build()
            
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SET_REPEAT_MODE)
                .build()

            val buttons = sInstance?.createMediaButtonPreferences() ?: emptyList()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .setMediaButtonPreferences(ImmutableList.copyOf(buttons))
                .setCustomLayout(ImmutableList.copyOf(buttons))
                .build()
        }

        override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
            AppSingleton.globalWebView?.post {
                when (customCommand.customAction) {
                    "REPEAT_ACTION" -> AppSingleton.globalWebView?.evaluateJavascript("actRepeat();", null)
                    "ADDTOFAV_ACTION" -> AppSingleton.globalWebView?.evaluateJavascript("actAddToFav();", null)
                    "SHUFFLE_ACTION" -> AppSingleton.globalWebView?.evaluateJavascript("actShuffle();", null)
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun createBrowsableItem(id: String, title: String): MediaItem {
        return MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setIsBrowsable(true).build())
                .build()
    }

    // A simple SettableFuture implementation since we don't have Guava's full implementation easily or we can use another way.
    // Actually, Media3 might have something similar or we can use a custom one.
    private class SettableFuture<T> : ListenableFuture<T> {
        private val listeners = mutableListOf<Pair<Runnable, java.util.concurrent.Executor>>()
        private var result: T? = null
        private var exception: Throwable? = null
        private var isDone = false

        fun set(value: T) {
            synchronized(this) {
                if (isDone) return
                result = value
                isDone = true
            }
            fireListeners()
        }

        fun setException(t: Throwable) {
            synchronized(this) {
                if (isDone) return
                exception = t
                isDone = true
            }
            fireListeners()
        }

        private fun fireListeners() {
            listeners.forEach { (runnable, executor) -> executor.execute(runnable) }
        }

        override fun addListener(listener: Runnable, executor: java.util.concurrent.Executor) {
            synchronized(this) {
                if (isDone) {
                    executor.execute(listener)
                } else {
                    listeners.add(listener to executor)
                }
            }
        }

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
        override fun isCancelled(): Boolean = false
        override fun isDone(): Boolean = isDone
        override fun get(): T {
            if (exception != null) throw java.util.concurrent.ExecutionException(exception)
            return result!!
        }
        override fun get(timeout: Long, unit: java.util.concurrent.TimeUnit): T = get()
    }
}
