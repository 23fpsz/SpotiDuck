package com.spotifuck.music

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.lang.ref.WeakReference
import java.util.Locale

class MainActivity : AppCompatActivity() {

    var webView: WebView? = null
    private var splashOverlay: ConstraintLayout? = null
    private var imgSplashLogo: ImageView? = null
    private var imgSplashError: ImageView? = null
    private var txtSplashError: TextView? = null
    private var btnSettings: ImageButton? = null

    private var horizontalScrollView: LockableHScrollView? = null
    private var webViewContainer: FrameLayout? = null
    private var backgroundText: TextView? = null
    private var statusText: TextView? = null
    private var messageText: TextView? = null
    private var progressBar: ProgressBar? = null
    private var serviceIntent: Intent? = null
    private var pendingSpotifyUri: String? = null

    companion object {
        @JvmField var virtualWidth: Int = 0
        @JvmField var isBackButtonPressedOnce: Boolean = false
        @JvmField var isForceEnInitial: Boolean = AppSingleton.isForceEn
        @JvmField var shouldReloadWebView: Boolean = false
        @JvmField var shouldClearCookies: Boolean = false

        @JvmStatic
        fun showMessage(str: String) {
            AppSingleton.activityRef?.get()?.let { activity ->
                activity.runOnUiThread {
                    activity.showInstanceMessage(str)
                }
            }
        }

        @JvmStatic
        fun updateLoadingVisibility() {
            AppSingleton.activityRef?.get()?.let { mainActivity ->
                mainActivity.runOnUiThread {
                    mainActivity.syncUiState()
                }
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val hideMessageRunnable = Runnable {
        if (AppSingleton.isPlayerLoaded) {
            progressBar?.visibility = View.INVISIBLE
        }
        messageText?.visibility = View.GONE
    }

    fun showInstanceMessage(str: String) {
        progressBar?.visibility = View.VISIBLE
        messageText?.let {
            it.text = str
            it.visibility = View.VISIBLE
            it.bringToFront()
        }
        mainHandler.removeCallbacks(hideMessageRunnable)
        mainHandler.postDelayed(hideMessageRunnable, 2500L)
    }

    override fun onCreate(bundle: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        
        super.onCreate(bundle)
        
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        
        if (AppSingleton.isForceEn) {
            Locale.setDefault(Locale("en"))
        }
        AppSingleton.activityRef = WeakReference(this)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.main)?.let { mainView ->
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                // Remove padding from the main view to allow content to go edge-to-edge
                v.setPadding(0, 0, 0, 0)
                
                // Keep Settings button below status bar
                findViewById<View>(R.id.btnSettings)?.let { btn ->
                    val lp = btn.layoutParams as ConstraintLayout.LayoutParams
                    lp.topMargin = systemBars.top + (5 * resources.displayMetrics.density).toInt()
                    btn.layoutParams = lp
                }

                // Keep ProgressBar below status bar
                findViewById<View>(R.id.progressBar)?.let { pb ->
                    val lp = pb.layoutParams as ConstraintLayout.LayoutParams
                    lp.topMargin = systemBars.top
                    pb.layoutParams = lp
                }

                insets
            }
        }
        
        serviceIntent = Intent(AppSingleton.appContext, WebService::class.java)
        
        horizontalScrollView = findViewById(R.id.hScrollView)
        webViewContainer = findViewById(R.id.webViewContainer)
        backgroundText = findViewById(R.id.frameBgText)
        statusText = findViewById(R.id.frameStatusText)
        messageText = findViewById(R.id.txtMessage)
        btnSettings = findViewById(R.id.btnSettings)
        
        splashOverlay = findViewById(R.id.splashOverlay)
        imgSplashLogo = findViewById(R.id.imgSplashLogo)
        imgSplashError = findViewById(R.id.imgSplashError)
        txtSplashError = findViewById(R.id.txtSplashError)

        splashOverlay?.setOnClickListener {
            if (AppSingleton.isErrorShowing) {
                retryAction()
            }
        }

        val userAgentString = WebView(AppSingleton.appContext).settings.userAgentString
        val iIndexOf = userAgentString.indexOf("Chrome/")
        if (iIndexOf != -1) {
            var iIndexOf2 = userAgentString.indexOf(" ", iIndexOf)
            if (iIndexOf2 == -1) {
                iIndexOf2 = userAgentString.length
            }
            statusText?.text = "WebView v." + userAgentString.substring(iIndexOf + 7, iIndexOf2)
        }
        progressBar = findViewById(R.id.progressBar)
        
        findViewById<View>(R.id.btnSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        syncUiState()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (AppSingleton.isLoggedIn && WebService.isServiceRunning) {
                    webView?.evaluateJavascript("(function() { if(window.closeFullscreenPlayer) { let open = !!document.getElementById('sf-fs-player'); if(open) { window.closeFullscreenPlayer(); return true; } } return false; })();") { value ->
                        if (value == "true") {
                            // Back handled by closing fullscreen player
                        } else {
                            handleRegularBack()
                        }
                    }
                } else {
                    handleRegularBack()
                }
            }

            private fun handleRegularBack() {
                if (!WebService.isServiceRunning) {
                    finish()
                } else {
                    webView?.let { wv ->
                        val url = wv.url
                        if (url != null && url != "https://open.spotify.com/" && !url.matches("https://open\\.spotify\\.com/intl-[a-zA-Z]{2}/".toRegex()) && wv.canGoBack()) {
                            wv.goBack()
                            showInstanceMessage(getString(R.string.txt_loadprevious))
                        } else if (!isBackButtonPressedOnce) {
                            isBackButtonPressedOnce = true
                            Toast.makeText(AppSingleton.appContext, getString(R.string.txt_pressagain), Toast.LENGTH_SHORT).show()
                            mainHandler.postDelayed({ isBackButtonPressedOnce = false }, 2000L)
                        } else {
                            moveTaskToBack(true)
                        }
                    } ?: finish()
                }
            }
        })
    }

    private fun retryAction() {
        if (!AppSingleton.isNetworkAvailable()) {
            Toast.makeText(this, "Network Error\nTap to retry", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!AppSingleton.isServiceEnabled) {
            AppSingleton.isServiceEnabled = true
            AppSingleton.isPlayerLoaded = false
            AppSingleton.prefsEditor.putBoolean("ServiceOn", true).apply()
            hideErrorState {
                restartService()
            }
        } else {
            hideErrorState {
                restartService()
            }
        }
    }

    private fun restartService() {
        Log.d("Spotifuck", "Starting manual service restart...")
        AppSingleton.isPlayerLoaded = false
        
        // Clear WebView data if requested
        if (shouldClearCookies) {
            Log.d("Spotifuck", "Performing deep clear of cookies and storage...")
            val cm = CookieManager.getInstance()
            cm.setAcceptCookie(true) // Ensure manager is active
            cm.removeSessionCookies(null)
            cm.removeAllCookies { success ->
                Log.d("Spotifuck", "Cookies removed: $success")
                cm.flush()
            }
            WebStorage.getInstance().deleteAllData()
            android.webkit.WebViewDatabase.getInstance(this).clearFormData()
            android.webkit.WebViewDatabase.getInstance(this).clearHttpAuthUsernamePassword()
        }

        // 1. Destroy existing WebView immediately
        webView?.let { wv ->
            Log.d("Spotifuck", "Removing and destroying old WebView")
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.stopLoading()
            wv.loadUrl("about:blank")
            wv.destroy()
        }
        webView = null
        AppSingleton.globalWebView = null

        // 2. Stop the Service explicitly
        val stopIntent = Intent(this, WebService::class.java).apply {
            action = "STOP_SERVICE"
        }
        startService(stopIntent)
        
        // 3. Re-initialize after a clean slate
        mainHandler.postDelayed({
            if (!AppSingleton.isNetworkAvailable()) {
                Log.d("Spotifuck", "No network. Showing error state.")
                showErrorState(true)
                return@postDelayed
            }

            Log.d("Spotifuck", "Cleanup delay over. Starting service fresh.")
            val startIntent = Intent(this, WebService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startIntent)
            } else {
                startService(startIntent)
            }
            
            // setupWebView calls getWebView() which creates the new WebView
            setupWebView()
            syncUiState()
            
            // Only reset the flag AFTER setupWebView has been called
            shouldClearCookies = false
            
            Log.d("Spotifuck", "Service restart command sent. UI syncing.")
        }, 1200) // Increased delay to ensure service stop is processed
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val uri = intent?.getStringExtra("spotify_uri")
        if (uri != null) {
            pendingSpotifyUri = uri
            processPendingUri()
        }
    }

    private fun processPendingUri() {
        val uri = pendingSpotifyUri ?: return
        if (WebService.isServiceRunning && AppSingleton.isPlayerLoaded) {
            AppSingleton.globalWebView?.post {
                AppSingleton.globalWebView?.evaluateJavascript("playFromUri('$uri');", null)
            }
            pendingSpotifyUri = null
        }
    }

    fun syncUiState() {
        updateBackgroundColors()
        
        btnSettings?.visibility = if (AppSingleton.isSearchActive) View.GONE else View.VISIBLE

        val currentUrl = webView?.url ?: ""
        val isAuthPage = currentUrl.contains("/login") || currentUrl.contains("/auth") || currentUrl.contains("accounts.spotify.com")

        if (!AppSingleton.isServiceEnabled) {
            progressBar?.visibility = View.INVISIBLE
            showErrorState(false)
        } else if (AppSingleton.isErrorShowing) {
            showErrorState(AppSingleton.currentErrorType == 1)
        } else if (!AppSingleton.isPlayerLoaded && !isAuthPage && AppSingleton.isLoggedIn) {
            progressBar?.visibility = View.VISIBLE
            showSplash()
        } else {
            progressBar?.visibility = View.INVISIBLE
            hideSplash(isAuthPage || !AppSingleton.isLoggedIn) // Force hide if on auth page or not logged in
            processPendingUri() // Handle any URI that was waiting for the player to load
        }
    }

    private fun updateBackgroundColors() {
        val color = if (AppSingleton.isAmoled) Color.BLACK else Color.parseColor("#121212")
        splashOverlay?.setBackgroundColor(color)
        webViewContainer?.setBackgroundColor(color)
        findViewById<View>(R.id.main)?.setBackgroundColor(color)
    }

    fun showSplash() {
        if (AppSingleton.isErrorShowing) {
            hideErrorState { showSplash() }
            return
        }
        runOnUiThread {
            splashOverlay?.animate()?.cancel()
            splashOverlay?.visibility = View.VISIBLE
            splashOverlay?.alpha = 1f
            webViewContainer?.visibility = View.INVISIBLE
            
            imgSplashLogo?.animate()?.cancel()
            imgSplashLogo?.alpha = 1f
            
            imgSplashError?.animate()?.cancel()
            imgSplashError?.visibility = View.GONE
            
            txtSplashError?.animate()?.cancel()
            txtSplashError?.visibility = View.GONE
        }
    }

    fun showErrorState(isNetworkError: Boolean) {
        val errorType = if (isNetworkError) 1 else 0
        if (AppSingleton.isErrorShowing && AppSingleton.currentErrorType == errorType) {
            // Ensure UI matches the state if it's already "showing" logically but maybe not visually
            if (splashOverlay?.visibility != View.VISIBLE) {
                // Force show without animation if it's out of sync
                splashOverlay?.visibility = View.VISIBLE
                splashOverlay?.alpha = 1f
                webViewContainer?.visibility = View.INVISIBLE
                imgSplashLogo?.alpha = 0f
                imgSplashError?.visibility = View.VISIBLE
                imgSplashError?.alpha = 1f
                val moveUpPx = -50 * (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
                imgSplashError?.translationY = moveUpPx
                txtSplashError?.visibility = View.VISIBLE
                txtSplashError?.alpha = 1f
            }
            return
        }
        
        AppSingleton.isErrorShowing = true
        AppSingleton.currentErrorType = errorType

        runOnUiThread {
            splashOverlay?.animate()?.cancel()
            splashOverlay?.visibility = View.VISIBLE
            splashOverlay?.alpha = 1f
            webViewContainer?.visibility = View.INVISIBLE
            
            txtSplashError?.text = if (isNetworkError) "Network Error\nTap to retry" else "Service Disabled\nTap to start"
            
            imgSplashLogo?.animate()?.cancel()
            imgSplashLogo?.animate()?.alpha(0f)?.setDuration(300)?.start()
            
            val moveUpPx = -50 * (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
            
            imgSplashError?.animate()?.cancel()
            imgSplashError?.apply {
                visibility = View.VISIBLE
                alpha = 0f
                translationY = 0f
                animate()
                    .alpha(1f)
                    .translationY(moveUpPx)
                    .setDuration(500)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            if (AppSingleton.isErrorShowing) {
                                txtSplashError?.animate()?.cancel()
                                txtSplashError?.apply {
                                    visibility = View.VISIBLE
                                    alpha = 0f
                                    animate().alpha(1f).setDuration(300).start()
                                }
                            }
                        }
                    })
                    .start()
            }
        }
    }

    private fun hideErrorState(onComplete: () -> Unit) {
        if (!AppSingleton.isErrorShowing) {
            onComplete()
            return
        }
        AppSingleton.isErrorShowing = false
        AppSingleton.currentErrorType = -1

        txtSplashError?.animate()?.cancel()
        txtSplashError?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
            txtSplashError?.visibility = View.GONE
        }?.start()

        imgSplashError?.animate()?.cancel()
        imgSplashError?.animate()
            ?.alpha(0f)
            ?.translationY(0f)
            ?.setDuration(400)
            ?.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!AppSingleton.isErrorShowing) {
                        imgSplashError?.visibility = View.GONE
                        imgSplashLogo?.animate()?.cancel()
                        imgSplashLogo?.animate()?.alpha(1f)?.setDuration(300)?.withEndAction {
                            onComplete()
                        }?.start()
                    }
                }
            })
            ?.start()
    }

    fun hideSplash(force: Boolean = false) {
        if (AppSingleton.isErrorShowing) return
        runOnUiThread {
            splashOverlay?.animate()?.cancel()
            splashOverlay?.animate()
                ?.alpha(0f)
                ?.setDuration(500)
                ?.setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!AppSingleton.isErrorShowing && (AppSingleton.isPlayerLoaded || force)) {
                            splashOverlay?.visibility = View.GONE
                            webViewContainer?.visibility = View.VISIBLE
                        }
                    }
                })
                ?.start()
        }
    }

    fun setExpandedMode(expanded: Boolean) {
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        if (expanded) {
            windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    fun setupWebView() {
        webView = AppSingleton.getWebView(this)
        
        if (AppSingleton.guiMode == "bigwindow") {
            horizontalScrollView?.isScrollingEnabled = true
            virtualWidth = 1600
        } else {
            horizontalScrollView?.apply {
                isScrollingEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
            }

            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            virtualWidth = displayMetrics.widthPixels
        }

        webView?.let { wv ->
            if (wv.parent != webViewContainer) {
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.layoutParams = FrameLayout.LayoutParams(virtualWidth, -1)
                webViewContainer?.addView(wv)
            } else {
                val lp = wv.layoutParams
                if (lp.width != virtualWidth) {
                    lp.width = virtualWidth
                    wv.layoutParams = lp
                }
            }
            wv.requestLayout()
        }
        
        backgroundText?.text = ""
        syncUiState()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(hideMessageRunnable)
        webView?.let { webViewContainer?.removeView(it) }
        AppSingleton.activityRef = null
    }

    override fun onResume() {
        super.onResume()
        FirebaseHotfixManager.initialize(this)
        requestedOrientation = if (AppSingleton.isForcePortrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        // Centralized Launch/Recovery Logic
        if (!WebService.isServiceRunning) {
            AppSingleton.isServiceEnabled = true
            AppSingleton.prefsEditor.putBoolean("ServiceOn", true).apply()
            
            // Prioritize logout if requested
            if (shouldClearCookies) {
                AppSingleton.isLoggedIn = false
                AppSingleton.prefsEditor.putBoolean("LoggedIn", false).apply()
            }

            if (AppSingleton.isErrorShowing) {
                hideErrorState { restartService() }
            } else {
                restartService()
            }
        } else if (shouldClearCookies) {
            // Service is running but we need to logout/clear
            AppSingleton.isLoggedIn = false
            AppSingleton.prefsEditor.putBoolean("LoggedIn", false).apply()
            restartService()
        } else {
            // If running but UI is detached (e.g., after a crash or process swap), re-attach
            if (AppSingleton.globalWebView == null || AppSingleton.globalWebView?.isAttachedToWindow == false) {
                setupWebView()
            }
        }

        if (AppSingleton.isForceEn) {
            Locale.setDefault(Locale("en"))
        }

        if (WebService.isServiceRunning && AppSingleton.isPlayerLoaded) {
            hideSplash()
        }

        intent?.getStringExtra("spotify_uri")?.let {
            pendingSpotifyUri = it
            processPendingUri()
        }

        if (shouldReloadWebView || isForceEnInitial != AppSingleton.isForceEn) {
            shouldReloadWebView = false
            isForceEnInitial = AppSingleton.isForceEn
            restartService()
        }

        syncUiState()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        setupWebView()
    }
}
