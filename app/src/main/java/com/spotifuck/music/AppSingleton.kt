package com.spotifuck.music

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.preference.PreferenceManager
import android.webkit.WebSettings
import android.webkit.WebView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

class AppSingleton : Application() {

    companion object {
        @JvmField var autoSleepMinutes: Int = 0
        @JvmField var isPlayerLoaded: Boolean = false
        @JvmField var isErrorShowing: Boolean = false
        @JvmField var currentErrorType: Int = -1 // -1: none, 0: service disabled, 1: network error
        @JvmField val shutHandler: Handler = Handler(Looper.getMainLooper())
        @JvmField val sleepHandler: Handler = Handler(Looper.getMainLooper())
        @JvmField var shutRunnable: Runnable? = null
        @JvmField var sleepRunnable: Runnable? = null

        lateinit var appContext: Context
        @JvmField var activityRef: WeakReference<MainActivity>? = null
        lateinit var prefs: SharedPreferences
        lateinit var prefsEditor: SharedPreferences.Editor
        @JvmField var globalWebView: WebView? = null

        @JvmField var autoPlayMode: String? = null
        @JvmField var guiMode: String? = null
        @JvmField var isServiceEnabled: Boolean = false
        @JvmField var isLoggedIn: Boolean = false
        @JvmField var takeControl: Boolean = false
        @JvmField var closeNowPlay: Boolean = false
        @JvmField var isAmoled: Boolean = false
        @JvmField var isAndAutoEnabled: Boolean = false
        @JvmField var stopOnSwipe: Boolean = false
        @JvmField var isForcePortrait: Boolean = false
        @JvmField var isForceEn: Boolean = false
        @JvmField var headsetPlay: Boolean = false
        @JvmField var headsetPause: Boolean = false
        @JvmField var btPlay: Boolean = false
        @JvmField var btPause: Boolean = false
        @JvmField var autoShutMinutes: Int = 0
        @JvmField var isCanvasDisabled: Boolean = false
        @JvmField var isFullScreenEnabled: Boolean = false

        private val assetCache = ConcurrentHashMap<String, String>()

        @JvmStatic
        fun cleanup() {
            shutRunnable?.let { shutHandler.removeCallbacks(it) }
            sleepRunnable?.let { sleepHandler.removeCallbacks(it) }
            shutRunnable = null
            sleepRunnable = null
            isPlayerLoaded = false
        }

        @JvmStatic
        fun notifyUiUpdate() {
            activityRef?.get()?.let { mainActivity ->
                mainActivity.runOnUiThread {
                    MainActivity.updateLoadingVisibility()
                }
            }
        }

        @JvmStatic
        fun pushLiveUpdate() {
            val json = String.format(
                "{ isCanvasDisabled: %b, isFullScreenEnabled: %b, isAmoled: %b, closeNowPlay: %b, takeControl: %b, autoPlayMode: '%s' }",
                isCanvasDisabled, isFullScreenEnabled, isAmoled, closeNowPlay, takeControl, autoPlayMode
            )
            globalWebView?.post {
                globalWebView?.evaluateJavascript("if(window.SF_UPDATE) window.SF_UPDATE($json);", null)
            }
        }

        @JvmStatic
        fun getAssetFile(fileName: String): String {
            val cached = assetCache[fileName]
            if (cached != null) return cached

            return try {
                appContext.assets.open(fileName).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val content = reader.lines().collect(Collectors.joining("\n"))
                        assetCache[fileName] = content
                        content
                    }
                }
            } catch (e: Exception) {
                ""
            }
        }

        @JvmStatic
        fun isNetworkAvailable(): Boolean {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val activeNetwork = cm.getNetworkCapabilities(network) ?: return false
            return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        @JvmStatic
        @JvmOverloads
        fun getWebView(context: Context = appContext): WebView? {
            if (globalWebView == null) {
                globalWebView = object : WebView(appContext) {
                    override fun onWindowVisibilityChanged(visibility: Int) {
                        if (getContext() != null && (visibility == View.GONE || visibility == View.INVISIBLE)) {
                            evaluateJavascript("typeof playing!=='undefined'&&playing&&!!document.querySelector('.VideoPlayer__container video');") { value ->
                                if ("true" == value) {
                                    super.onWindowVisibilityChanged(View.VISIBLE)
                                } else {
                                    super.onWindowVisibilityChanged(visibility)
                                }
                            }
                        } else {
                            super.onWindowVisibilityChanged(visibility)
                        }
                    }
                }

                globalWebView?.apply {
                    overScrollMode = View.OVER_SCROLL_NEVER
                    scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY

                    isLongClickable = false
                    setOnLongClickListener { true }

                    if (MainActivity.shouldClearCookies) {
                        clearCache(true)
                        clearFormData()
                        clearHistory()
                        android.webkit.WebStorage.getInstance().deleteAllData()
                    }

                    settings.apply {
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"
                        javaScriptEnabled = true
                        setSupportMultipleWindows(true)
                        javaScriptCanOpenWindowsAutomatically = true
                        domStorageEnabled = true
                        @Suppress("DEPRECATION")
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    setInitialScale(100)
                    
                    if (isAmoled) {
                        setBackgroundColor(Color.BLACK)
                    } else {
                        setBackgroundColor(Color.parseColor("#121212"))
                    }
                    
                    addJavascriptInterface(AndBridge(appContext), "AndBridge")

                    webChromeClient = SpotifyWebChromeClient()
                    webViewClient = SpotifyWebViewClient()

                    if (MainActivity.shouldClearCookies) {
                        clearCache(true)
                    }

                    if (!isPlayerLoaded) {
                        if (isLoggedIn && !MainActivity.shouldClearCookies) {
                            loadUrl("https://open.spotify.com/")
                        } else {
                            loadUrl("https://accounts.spotify.com/login")
                        }
                    }
                }
            }
            return globalWebView
        }
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefsEditor = prefs.edit()

        isServiceEnabled = prefs.getBoolean("ServiceOn", true)
        isLoggedIn = prefs.getBoolean("LoggedIn", false)
        autoPlayMode = prefs.getString("APlayMode", "disabled")
        closeNowPlay = prefs.getBoolean("CloseNowPlay", false)
        takeControl = prefs.getBoolean("TakeControl", true)
        guiMode = prefs.getString("GuiMode", "csshack")
        isAmoled = prefs.getBoolean("Amoled", false)
        isAndAutoEnabled = prefs.getBoolean("AndAuto", true)
        stopOnSwipe = prefs.getBoolean("SwipeStop", true)
        isFullScreenEnabled = prefs.getBoolean("ExpandedFullScreen", true)

        autoShutMinutes = try {
            prefs.getString("AutoShut", "0")?.toInt() ?: 0
        } catch (e: NumberFormatException) {
            0
        }

        autoSleepMinutes = try {
            prefs.getString("AutoSleep", "0")?.toInt() ?: 0
        } catch (e: NumberFormatException) {
            0
        }

        isForcePortrait = prefs.getBoolean("ForcePortrait", false)
        isForceEn = prefs.getBoolean("ForceEn", false)
        headsetPlay = prefs.getBoolean("HPAP", false)
        headsetPause = prefs.getBoolean("HPAS", false)
        btPlay = prefs.getBoolean("BTAP", false)
        btPause = prefs.getBoolean("BTAS", false)
        isCanvasDisabled = prefs.getBoolean("DisableCanvas", true)
    }
}
