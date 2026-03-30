package com.spotifuck.music

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.lang.ref.WeakReference
import java.util.Locale

class MainActivity : AppCompatActivity() {

    var webView: WebView? = null

    companion object {
        @JvmField var horizontalScrollView: LockableHScrollView? = null
        @JvmField var webViewContainer: FrameLayout? = null
        @JvmField var backgroundText: TextView? = null
        @JvmField var statusText: TextView? = null
        @JvmField var messageText: TextView? = null
        @JvmField var progressBar: ProgressBar? = null
        @JvmField var serviceIntent: Intent? = null
        @JvmField var virtualWidth: Int = 0
        @JvmField var isBackButtonPressedOnce: Boolean = false
        @JvmField var isForceEnInitial: Boolean = AppSingleton.isForceEn
        @JvmField var shouldReloadWebView: Boolean = false
        @JvmField var shouldClearCookies: Boolean = false

        @JvmStatic
        fun showMessage(str: String) {
            progressBar?.visibility = View.VISIBLE
            messageText?.let {
                it.text = str
                it.visibility = View.VISIBLE
                it.bringToFront()
            }
            Handler(Looper.getMainLooper()).postDelayed({
                if (AppSingleton.isPlayerLoaded) {
                    progressBar?.visibility = View.INVISIBLE
                }
                messageText?.visibility = View.GONE
            }, 2500L)
        }

        @JvmStatic
        fun updateLoadingVisibility() {
            progressBar?.let {
                if (AppSingleton.isServiceEnabled && !AppSingleton.isPlayerLoaded) {
                    it.visibility = View.VISIBLE
                } else if (AppSingleton.isPlayerLoaded) {
                    it.visibility = View.INVISIBLE
                }
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        if (AppSingleton.isForceEn) {
            Locale.setDefault(Locale("en"))
        }
        AppSingleton.activityRef = WeakReference(this)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.main)?.let { mainView ->
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
                v.setPadding(0, top, 0, 0)
                insets
            }
        }
        
        serviceIntent = Intent(AppSingleton.appContext, WebService::class.java)
        
        horizontalScrollView = findViewById(R.id.hScrollView)
        webViewContainer = findViewById(R.id.webViewContainer)
        backgroundText = findViewById(R.id.frameBgText)
        statusText = findViewById(R.id.frameStatusText)
        messageText = findViewById(R.id.txtMessage)
        
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
        
        // Ensure service is running if app is opened
        if (!WebService.isServiceRunning) {
            AppSingleton.isServiceEnabled = true
            AppSingleton.prefsEditor.putBoolean("ServiceOn", true).apply()
        }

        if (AppSingleton.isServiceEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            setupWebView()
        }
        updateLoadingVisibility()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!WebService.isServiceRunning) {
                    finish()
                } else {
                    webView?.let { wv ->
                        val url = wv.url
                        if (url != null && url != "https://open.spotify.com/" && !url.matches("https://open\\.spotify\\.com/intl-[a-zA-Z]{2}/".toRegex()) && wv.canGoBack()) {
                            wv.goBack()
                            showMessage(getString(R.string.txt_loadprevious))
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

    fun setupWebView() {
        webView = AppSingleton.getWebView()
        
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
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.layoutParams = FrameLayout.LayoutParams(virtualWidth, -1)
            webViewContainer?.addView(wv)
        }
        
        backgroundText?.text = ""
        updateLoadingVisibility()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView?.let { webViewContainer?.removeView(it) }
        AppSingleton.activityRef = null
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = if (AppSingleton.isForcePortrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        // Re-enable service if it was auto-shutdown
        if (!WebService.isServiceRunning) {
            AppSingleton.isServiceEnabled = true
            AppSingleton.prefsEditor.putBoolean("ServiceOn", true).apply()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            setupWebView()
        }

        if (WebService.isServiceRunning && AppSingleton.globalWebView != null && AppSingleton.globalWebView?.isAttachedToWindow == false) {
            setupWebView()
        }
        if (AppSingleton.isForceEn) {
            Locale.setDefault(Locale("en"))
        }
        if (isForceEnInitial != AppSingleton.isForceEn) {
            shouldReloadWebView = true
        }
        isForceEnInitial = AppSingleton.isForceEn
        if (shouldReloadWebView) {
            shouldReloadWebView = false
            setupWebView()
            webView?.let { wv ->
                try {
                    AppSingleton.isPlayerLoaded = false
                    updateLoadingVisibility()
                    wv.clearCache(true)
                    wv.clearHistory()
                    wv.clearFormData()
                    WebStorage.getInstance().deleteAllData()
                    if (AppSingleton.isServiceEnabled) {
                        wv.reload()
                        showMessage(getString(R.string.txt_reload))
                    }
                } catch (e: Exception) {
                    AppSingleton.globalWebView = null
                    setupWebView()
                }
            }
        }
        if (shouldClearCookies) {
            shouldClearCookies = false
            if (WebService.isServiceRunning) {
                backgroundText?.setText(R.string.txt_servicenr)
                webView?.let { webViewContainer?.removeView(it) }
                startService(Intent(AppSingleton.appContext, WebService::class.java).setAction("STOP_SERVICE"))
            }
            val webView2 = WebView(AppSingleton.appContext)
            webView2.clearCache(true)
            webView2.clearHistory()
            webView2.clearFormData()
            WebStorage.getInstance().deleteAllData()
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            AppSingleton.isLoggedIn = false
            AppSingleton.globalWebView = null
            setupWebView()
        }
    }
}
