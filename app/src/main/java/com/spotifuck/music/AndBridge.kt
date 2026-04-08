package com.spotifuck.music

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.webkit.JavascriptInterface
import com.squareup.picasso.Picasso
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class AndBridge(context: Context) {

    @JavascriptInterface
    fun deferMessage(str: String) {
        AppSingleton.activityRef?.get()?.let { mainActivity ->
            val msg = when (str) {
                "unlock" -> mainActivity.getString(R.string.txt_unlock)
                "reload" -> mainActivity.getString(R.string.txt_reload)
                "adblock" -> mainActivity.getString(R.string.txt_adblock)
                else -> str
            }
            mainActivity.runOnUiThread { MainActivity.showMessage(msg) }
        }
    }

    @JavascriptInterface
    fun setCanvasDisabled(disabled: Boolean) {
        AppSingleton.isCanvasDisabled = disabled
        AppSingleton.prefsEditor.putBoolean("DisableCanvas", disabled).apply()
    }

    @JavascriptInterface
    fun manageTShut(z2: Boolean) {
        if (!WebService.isServiceRunning || AppSingleton.autoShutMinutes <= 0 || !z2) {
            AppSingleton.shutRunnable?.let { AppSingleton.shutHandler.removeCallbacks(it) }
        } else {
            AppSingleton.shutRunnable?.let { AppSingleton.shutHandler.removeCallbacks(it) }
            AppSingleton.shutRunnable = Runnable {
                AppSingleton.activityRef?.get()?.let { mainActivity ->
                    deferMessage(mainActivity.getString(R.string.menu_autoshut))
                }
                stopServiceAutomatically()
            }
            AppSingleton.shutHandler.postDelayed(AppSingleton.shutRunnable!!, AppSingleton.autoShutMinutes * 60000L)
        }
    }

    @JavascriptInterface
    fun manageTSleep(z2: Boolean) {
        if (!WebService.isServiceRunning || AppSingleton.autoSleepMinutes <= 0 || !z2) {
            AppSingleton.sleepRunnable?.let { AppSingleton.sleepHandler.removeCallbacks(it) }
        } else {
            AppSingleton.sleepRunnable?.let { AppSingleton.sleepHandler.removeCallbacks(it) }
            AppSingleton.sleepRunnable = Runnable {
                AppSingleton.activityRef?.get()?.let { mainActivity ->
                    deferMessage(mainActivity.getString(R.string.menu_autosleep))
                }
                AppSingleton.autoSleepMinutes = 0
                AppSingleton.prefsEditor.putString("AutoSleep", "0").commit()
                stopServiceAutomatically()
            }
            AppSingleton.sleepHandler.postDelayed(AppSingleton.sleepRunnable!!, AppSingleton.autoSleepMinutes * 60000L)
        }
    }

    private fun stopServiceAutomatically() {
        AppSingleton.isServiceEnabled = false
        AppSingleton.isPlayerLoaded = false // Ensure splash shows up on next check
        AppSingleton.prefsEditor.putBoolean("ServiceOn", false).apply()
        AppSingleton.appContext.startService(Intent(AppSingleton.appContext, WebService::class.java).setAction("STOP_SERVICE"))
        AppSingleton.notifyUiUpdate()
    }

    @JavascriptInterface
    fun wakeOff() {
        Handler(Looper.getMainLooper()).post {
            AppSingleton.globalWebView?.dispatchWindowVisibilityChanged(View.GONE)
        }
    }

    @JavascriptInterface
    fun wakeUp() {
        Handler(Looper.getMainLooper()).post {
            AppSingleton.globalWebView?.dispatchWindowVisibilityChanged(View.VISIBLE)
        }
    }

    @JavascriptInterface
    fun cssInjected() {
        AppSingleton.notifyUiUpdate()
    }

    @JavascriptInterface
    fun isWoke(): Boolean {
        return AppSingleton.globalWebView?.windowVisibility == View.VISIBLE
    }

    @JavascriptInterface
    fun loginDetected() {
        AppSingleton.isLoggedIn = true
        AppSingleton.prefsEditor.putBoolean("LoggedIn", true).commit()
    }

    @JavascriptInterface
    fun playLoaded() {
        AppSingleton.isPlayerLoaded = true
        AppSingleton.notifyUiUpdate()
    }

    @JavascriptInterface
    fun recMediaPosition(j2: Long) {
        WebService.trackPosition = j2
        WebService.lastPositionUpdateTime = SystemClock.elapsedRealtime()
        WebService.updatePlaybackState()
    }

    @JavascriptInterface
    fun recMediaStatus(str: String) {
        try {
            val jSONObject = JSONObject(str)
            val objOpt = jSONObject.opt("track")
            if (objOpt != null && JSONObject.NULL != objOpt) {
                WebService.trackName = objOpt.toString()
                WebService.artistName = jSONObject.optString("artist")
                WebService.trackPosition = jSONObject.optLong("position")
                WebService.trackDuration = jSONObject.optLong("duration")
                WebService.lastPositionUpdateTime = SystemClock.elapsedRealtime()
                WebService.isPlaying = jSONObject.optBoolean("playing")
                WebService.repeatState = jSONObject.optString("repeat")
                WebService.shuffleState = jSONObject.optString("shuffle")
                WebService.isFavorite = jSONObject.optBoolean("fav")
                
                val coverUrl = jSONObject.optString("cover").replace("00004851", "0000b273")
                WebService.albumArt = null
                if (coverUrl.isNotEmpty()) {
                    try {
                        WebService.albumArt = Picasso.get().load(coverUrl).get()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                WebService.updatePlaybackState()
            }
        } catch (e2: JSONException) {
            e2.printStackTrace()
        }
    }
}
