package com.spotifuck.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class WebService : MediaBrowserServiceCompat() {

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
                if (service.mMediaSession != null) {
                    val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                    
                    val stateBuilder = PlaybackStateCompat.Builder()
                        .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                                PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_STOP or
                                PlaybackStateCompat.ACTION_PLAY_PAUSE)
                    
                    stateBuilder.setState(state, trackPosition, 1.0f, SystemClock.elapsedRealtime())
                    
                    val favIcon = if (isFavorite) R.drawable.favon else R.drawable.favoff
                    stateBuilder.addCustomAction(PlaybackStateCompat.CustomAction.Builder("ADDTOFAV_ACTION", "Favorite", favIcon).build())
                    
                    repeatIconResId = when (repeatState) {
                        "true" -> R.drawable.repon
                        "mixed" -> R.drawable.repone
                        else -> R.drawable.repoff
                    }
                    stateBuilder.addCustomAction(PlaybackStateCompat.CustomAction.Builder("REPEAT_ACTION", "Repeat", repeatIconResId).build())

                    shuffleIconResId = when (shuffleState) {
                        "true" -> R.drawable.shuffle
                        "mixed" -> R.drawable.ic_smart_shuffle
                        else -> R.drawable.repoff
                    }
                    stateBuilder.addCustomAction(PlaybackStateCompat.CustomAction.Builder("SHUFFLE_ACTION", "Shuffle", shuffleIconResId).build())
                    
                    service.mMediaSession?.setPlaybackState(stateBuilder.build())

                    if (!onlyWidgets) {
                        val metadata = MediaMetadataCompat.Builder()
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, trackName)
                                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artistName)
                                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, trackDuration)
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                                .build()
                        service.mMediaSession?.setMetadata(metadata)

                        val manager = service.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                        manager?.notify(1337, service.createNotification())
                    }
                    
                    service.updateWidgets()
                    
                    if (isPlaying && !service.mWakeLock.isHeld) service.mWakeLock.acquire(10 * 60 * 1000L)
                    else if (!isPlaying && service.mWakeLock.isHeld) service.mWakeLock.release()
                }
            }
        }
    }

    private var mMediaSession: MediaSessionCompat? = null
    private lateinit var mWakeLock: PowerManager.WakeLock
    private var mAudioManager: AudioManager? = null
    private var mAudioDeviceCallback: AudioDeviceCallback? = null

    class MediaActionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            if (isServiceRunning && AppSingleton.isPlayerLoaded && sInstance != null && sInstance?.mMediaSession != null) {
                val action = intent.action ?: return
                val controls = sInstance?.mMediaSession?.controller?.transportControls ?: return
                
                when (action) {
                    "android.intent.action.HEADSET_PLUG" -> {
                        val state = intent.getIntExtra("state", -1)
                        if (AppSingleton.headsetPlay && state == 1) controls.play()
                        else if (AppSingleton.headsetPause && state == 0) controls.pause()
                    }
                    "REPEAT_ACTION", "ADDTOFAV_ACTION", "SHUFFLE_ACTION" -> {
                        controls.sendCustomAction(action, null)
                    }
                    "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED" -> {
                        val btState = intent.getIntExtra(android.bluetooth.BluetoothProfile.EXTRA_STATE, 0)
                        if (AppSingleton.btPlay && btState == 2) controls.play()
                        else if (AppSingleton.btPause && btState == 0) controls.pause()
                    }
                    "WIDGET_PLAYPAUSE" -> {
                        if (isPlaying) controls.pause()
                        else controls.play()
                    }
                    "WIDGET_NEXT" -> controls.skipToNext()
                    "WIDGET_PREV" -> controls.skipToPrevious()
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
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("SpotifuckChannel", "Spotifuck Background Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }

        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioDeviceCallback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                    if (isServiceRunning && AppSingleton.isPlayerLoaded && mMediaSession != null) {
                        for (device in addedDevices) {
                            val type = device.type
                            if (AppSingleton.headsetPlay && (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES)) {
                                mMediaSession?.controller?.transportControls?.play()
                            } else if (AppSingleton.btPlay && (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO)) {
                                mMediaSession?.controller?.transportControls?.play()
                            }
                        }
                    }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                    if (isServiceRunning && AppSingleton.isPlayerLoaded && mMediaSession != null) {
                        for (device in removedDevices) {
                            val type = device.type
                            if (AppSingleton.headsetPause && (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES)) {
                                mMediaSession?.controller?.transportControls?.pause()
                            } else if (AppSingleton.btPause && (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO)) {
                                mMediaSession?.controller?.transportControls?.pause()
                            }
                        }
                    }
                }
            }
            mAudioManager?.registerAudioDeviceCallback(mAudioDeviceCallback, Handler(Looper.getMainLooper()))
        } else {
            val filter = IntentFilter().apply {
                addAction("android.intent.action.HEADSET_PLUG")
                addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
            }
            registerReceiver(MediaActionReceiver(), filter)
        }

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Spotifuck:KeepAlive")
        mWakeLock.setReferenceCounted(false)
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
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (AppSingleton.stopOnSwipe) {
            stopService()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun setupMediaSession() {
        mMediaSession = MediaSessionCompat(this, "Spotifuck").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(MediaSessionCallback())
            isActive = true
        }
        sessionToken = mMediaSession?.sessionToken
        updatePlaybackState()
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

        if (AppSingleton.isAndAutoEnabled) {
            builder.addAction(NotificationCompat.Action.Builder(R.drawable.previous, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)).build())
                    .addAction(NotificationCompat.Action.Builder(if (isPlaying) R.drawable.pause else R.drawable.play, "Play/Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)).build())
                    .addAction(NotificationCompat.Action.Builder(R.drawable.next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)).build())
                    .addAction(NotificationCompat.Action.Builder(if (isFavorite) R.drawable.favon else R.drawable.favoff, "Favorite", PendingIntent.getBroadcast(this, 1, Intent(this, MediaActionReceiver::class.java).setAction("ADDTOFAV_ACTION"), flags)).build())
                    .addAction(NotificationCompat.Action.Builder(repeatIconResId, "Repeat", PendingIntent.getBroadcast(this, 1, Intent(this, MediaActionReceiver::class.java).setAction("REPEAT_ACTION"), flags)).build())
                    .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mMediaSession?.sessionToken)
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
        mMediaSession?.apply {
            setCallback(null)
            isActive = false
            release()
        }
        mMediaSession = null
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
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return BrowserRoot("sfroot", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        if ("sfroot" == parentId) {
            val items = mutableListOf<MediaBrowserCompat.MediaItem>()
            items.add(createBrowsableItem("playlists", "\uD83C\uDFB5 " + getString(R.string.aa_playlists)))
            items.add(createBrowsableItem("albums", "\uD83D\uDCBF " + getString(R.string.aa_albums)))
            items.add(createBrowsableItem("artists", "\uD83D\uDC64 " + getString(R.string.aa_artists)))
            items.add(createBrowsableItem("podcasts", "\uD83C\uDF99 " + getString(R.string.aa_podcasts)))
            result.sendResult(items)
        } else {
            result.detach()
            AppSingleton.globalWebView?.post {
                val js = "JSON.stringify(window.mediaLib?.['$parentId'])"
                AppSingleton.globalWebView?.evaluateJavascript(js) { value ->
                    val items = mutableListOf<MediaBrowserCompat.MediaItem>()
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
                                
                                val desc = MediaDescriptionCompat.Builder()
                                        .setMediaId(id)
                                        .setTitle(name)
                                        .setSubtitle(sub)
                                        .setIconUri(if (image != null) Uri.parse(image) else null)
                                        .build()
                                items.add(MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
                            }
                        } catch (e: JSONException) {
                            Log.e("Spotifuck", "JSON error", e)
                        }
                    }
                    result.sendResult(items)
                }
            } ?: result.sendResult(emptyList())
        }
    }

    private fun createBrowsableItem(id: String, title: String): MediaBrowserCompat.MediaItem {
        val desc = MediaDescriptionCompat.Builder()
                .setMediaId(id)
                .setTitle(title)
                .build()
        return MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            AppSingleton.globalWebView?.post { AppSingleton.globalWebView?.evaluateJavascript("actPlayPause(true);", null) }
        }

        override fun onPause() {
            AppSingleton.globalWebView?.post { AppSingleton.globalWebView?.evaluateJavascript("actPlayPause(false);", null) }
        }

        override fun onSkipToNext() {
            AppSingleton.globalWebView?.post { AppSingleton.globalWebView?.evaluateJavascript("actSkipForward();", null) }
        }

        override fun onSkipToPrevious() {
            AppSingleton.globalWebView?.post { AppSingleton.globalWebView?.evaluateJavascript("actSkipBack();", null) }
        }

        override fun onSeekTo(pos: Long) {
            AppSingleton.globalWebView?.post { AppSingleton.globalWebView?.evaluateJavascript("actSeek($pos);", null) }
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            AppSingleton.globalWebView?.post {
                when (action) {
                    "REPEAT_ACTION" -> AppSingleton.globalWebView?.evaluateJavascript("actRepeat();", null)
                    "ADDTOFAV_ACTION" -> AppSingleton.globalWebView?.evaluateJavascript("actAddToFav();", null)
                    "SHUFFLE_ACTION" -> AppSingleton.globalWebView?.evaluateJavascript("actShuffle();", null)
                }
            }
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            AppSingleton.globalWebView?.post { AppSingleton.globalWebView?.evaluateJavascript("playFromUri('$mediaId');", null) }
        }
    }
}
