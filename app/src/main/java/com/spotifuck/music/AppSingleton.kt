package com.spotifuck.music

import android.app.Application
import android.content.Context
import android.content.MutableContextWrapper
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
import java.io.File
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
        private var contextWrapper: MutableContextWrapper? = null

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
        @JvmField var isSearchActive: Boolean = false
        @JvmField var prioritizeLocalAssets: Boolean = true
        @JvmField var adBlockMode: String = "legacy"
        @JvmField val adBlockHosts = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        @JvmField var adBlockListUrl: String = "https://raw.githubusercontent.com/Isaaker/Spotify-AdsList/main/Lists/standard_list.txt"

        @JvmStatic
        fun loadAdBlockHosts() {
            try {
                adBlockHosts.clear()
                val cachedFile = File(appContext.filesDir, "adblock_hosts.txt")
                val reader = if (cachedFile.exists()) {
                    BufferedReader(InputStreamReader(cachedFile.inputStream()))
                } else {
                    BufferedReader(InputStreamReader(appContext.assets.open("adblock_hosts.txt")))
                }
                
                reader.use { r ->
                    var line: String? = r.readLine()
                    while (line != null) {
                        val cleaned = line.trim()
                        if (cleaned.isNotEmpty() && !cleaned.startsWith("#") && !cleaned.startsWith("!")) {
                            var domain = cleaned
                            if (domain.startsWith("||")) {
                                domain = domain.substring(2)
                            }
                            if (domain.endsWith("^")) {
                                domain = domain.substring(0, domain.length - 1)
                            }
                            
                            if (domain.startsWith("0.0.0.0")) {
                                domain = domain.substring(7).trim()
                            } else if (domain.startsWith("127.0.0.1")) {
                                domain = domain.substring(9).trim()
                            }
                            
                            val hashIndex = domain.indexOf('#')
                            if (hashIndex != -1) {
                                domain = domain.substring(0, hashIndex).trim()
                            }
                            
                            if (domain.isNotEmpty()) {
                                adBlockHosts.add(domain.lowercase())
                            }
                        }
                        line = r.readLine()
                    }
                }
                android.util.Log.d("AppSingleton", "loadAdBlockHosts: loaded ${adBlockHosts.size} domains")
            } catch (e: Exception) {
                android.util.Log.e("AppSingleton", "loadAdBlockHosts: error", e)
            }
        }

        @JvmStatic
        fun fetchAdBlockHostsAsync(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
            Thread {
                var success = false
                try {
                    android.util.Log.d("AppSingleton", "fetchAdBlockHostsAsync: downloading list from $adBlockListUrl")
                    val url = java.net.URL(adBlockListUrl)
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.connect()
                    
                    if (conn.responseCode == 200) {
                        val tempFile = File(context.filesDir, "adblock_hosts.txt.tmp")
                        conn.inputStream.use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        val destFile = File(context.filesDir, "adblock_hosts.txt")
                        if (tempFile.renameTo(destFile) || (destFile.delete() && tempFile.renameTo(destFile))) {
                            success = true
                            loadAdBlockHosts()
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    android.util.Log.e("AppSingleton", "fetchAdBlockHostsAsync: download failed", e)
                }
                onComplete?.invoke(success)
            }.start()
        }

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
            if (cached != null) {
                android.util.Log.d("AppSingleton", "getAssetFile: $fileName loaded from memory cache")
                return cached
            }

            // Check if hotfix file should be ignored based on debug builds and toggle
            val isDebug = (appContext.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val shouldIgnoreHotfix = isDebug && prioritizeLocalAssets
            val hotfixFile = File(appContext.filesDir, "hotfixes/$fileName")
            android.util.Log.d("AppSingleton", "getAssetFile: checking hotfix path: ${hotfixFile.absolutePath} (exists: ${hotfixFile.exists()}, isDebug: $isDebug, shouldIgnoreHotfix: $shouldIgnoreHotfix)")
            if (!shouldIgnoreHotfix && hotfixFile.exists()) {
                try {
                    val content = hotfixFile.readText()
                    android.util.Log.d("AppSingleton", "getAssetFile: loaded $fileName from hotfix storage (length: ${content.length})")
                    assetCache[fileName] = content
                    return content
                } catch (e: Exception) {
                    android.util.Log.e("AppSingleton", "getAssetFile: failed to read hotfix for $fileName", e)
                }
            }

            android.util.Log.d("AppSingleton", "getAssetFile: falling back to built-in asset for $fileName")
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
        fun clearAssetCache(fileName: String) {
            assetCache.remove(fileName)
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
                contextWrapper = MutableContextWrapper(context)
                globalWebView = object : WebView(contextWrapper!!) {
                    override fun onWindowVisibilityChanged(visibility: Int) {
                        if (getContext() != null && (visibility == View.GONE || visibility == View.INVISIBLE)) {
                            evaluateJavascript("typeof playing!=='undefined'&&playing;") { value ->
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
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    overScrollMode = View.OVER_SCROLL_NEVER
                    scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY

                    isLongClickable = false
                    setOnLongClickListener { true }

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_YES
                    }

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
                        
                        // Enable Autofill relevant settings
                        savePassword = true
                        @Suppress("DEPRECATION")
                        saveFormData = true
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
            } else {
                contextWrapper?.baseContext = context
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
        prioritizeLocalAssets = prefs.getBoolean("PrioritizeLocalAssets", true)
        adBlockMode = prefs.getString("AdBlockMode", "legacy") ?: "legacy"
        adBlockListUrl = prefs.getString("AdBlockListUrl", "https://raw.githubusercontent.com/Isaaker/Spotify-AdsList/main/Lists/standard_list.txt") ?: "https://raw.githubusercontent.com/Isaaker/Spotify-AdsList/main/Lists/standard_list.txt"

        // Load cached or fallback blocklist hosts
        loadAdBlockHosts()

        // Fetch latest blocklist updates asynchronously
        if (isNetworkAvailable()) {
            fetchAdBlockHostsAsync(this)
        }

        // Initialize dynamic hotfix updates
        FirebaseHotfixManager.initialize(this)

        // Asynchronously pre-warm the WebView on app startup to speed up launch times
        Handler(Looper.getMainLooper()).postDelayed({
            if (isNetworkAvailable() && isLoggedIn) {
                android.util.Log.d("AppSingleton", "Pre-warming WebView...")
                getWebView()
            }
        }, 150)
    }
}
