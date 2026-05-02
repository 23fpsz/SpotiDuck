package com.spotifuck.music

import android.os.Handler
import android.os.Looper
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class WebViewPlayer(private val playerLooper: Looper) : SimpleBasePlayer(playerLooper) {

    override fun getState(): State {
        val metadata = MediaMetadata.Builder()
            .setTitle(WebService.trackName)
            .setArtist(WebService.artistName)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(WebService.trackName + WebService.artistName)
            .setMediaMetadata(metadata)
            .build()

        val itemData = MediaItemData.Builder(WebService.trackName + WebService.artistName)
            .setMediaItem(mediaItem)
            .setDurationUs(WebService.trackDuration * 1000)
            .build()

        val playbackState = if (WebService.trackName == "No Track") Player.STATE_IDLE else Player.STATE_READY
        
        return State.Builder()
            .setAvailableCommands(
                Player.Commands.Builder()
                    .addAll(
                        Player.COMMAND_PLAY_PAUSE,
                        Player.COMMAND_STOP,
                        Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                        Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                        Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_GET_METADATA,
                        Player.COMMAND_SET_REPEAT_MODE,
                        Player.COMMAND_SET_SHUFFLE_MODE,
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_BACK,
                        Player.COMMAND_SEEK_FORWARD,
                        Player.COMMAND_GET_AUDIO_ATTRIBUTES,
                        Player.COMMAND_GET_VOLUME,
                        Player.COMMAND_SET_VOLUME,
                    )
                    .build()
            )
            .setPlayWhenReady(WebService.isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(playbackState)
            .setPlaylist(listOf(itemData))
            .setCurrentMediaItemIndex(0)
            .setContentPositionMs { WebService.trackPosition }
            .setRepeatMode(
                when (WebService.repeatState) {
                    "true" -> Player.REPEAT_MODE_ALL
                    "mixed" -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
            )
            .setShuffleModeEnabled(WebService.shuffleState == "true")
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        AppSingleton.globalWebView?.post {
            AppSingleton.globalWebView?.evaluateJavascript("actPlayPause($playWhenReady);", null)
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, @Player.Command seekCommand: Int): ListenableFuture<*> {
        if (seekCommand == Player.COMMAND_SEEK_TO_NEXT) {
            AppSingleton.globalWebView?.post {
                AppSingleton.globalWebView?.evaluateJavascript("actSkipForward();", null)
            }
        } else if (seekCommand == Player.COMMAND_SEEK_TO_PREVIOUS) {
            AppSingleton.globalWebView?.post {
                AppSingleton.globalWebView?.evaluateJavascript("actSkipBack();", null)
            }
        } else if (positionMs != C.TIME_UNSET) {
            AppSingleton.globalWebView?.post {
                AppSingleton.globalWebView?.evaluateJavascript("actSeek($positionMs);", null)
            }
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        AppSingleton.globalWebView?.post {
            AppSingleton.globalWebView?.evaluateJavascript("actRepeat();", null)
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
        AppSingleton.globalWebView?.post {
            AppSingleton.globalWebView?.evaluateJavascript("actShuffle();", null)
        }
        return Futures.immediateVoidFuture()
    }

    fun notifyStateChanged() {
        if (Looper.myLooper() == playerLooper) {
            invalidateState()
        } else {
            Handler(playerLooper).post { invalidateState() }
        }
    }
}
