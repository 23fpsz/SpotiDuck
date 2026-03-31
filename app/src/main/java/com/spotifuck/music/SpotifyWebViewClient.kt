package com.spotifuck.music

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.HashMap

class SpotifyWebViewClient : WebViewClient() {

    override fun onPageStarted(webView: WebView, url: String, favicon: Bitmap?) {
        webView.evaluateJavascript(AppSingleton.getAssetFile("desktop_spoof.js"), null)
        super.onPageStarted(webView, url, favicon)
    }

    override fun onPageFinished(webView: WebView, url: String) {
        super.onPageFinished(webView, url)

        if (url.startsWith("https://www.facebook.com/privacy/consent/gdp/")) {
            webView.evaluateJavascript(AppSingleton.getAssetFile("facebook_consent.js"), null)
        } else if (url.endsWith("/login")) {
            webView.evaluateJavascript(AppSingleton.getAssetFile("classic_login.js"), null)
        }

        if (!AppSingleton.isLoggedIn) {
            webView.evaluateJavascript("(function() {let l=document.querySelector('button[data-testid=web-player-link]');if(l) { AndBridge.loginDetected(); l.click(); }})();", null)
            return
        }

        val config = String.format(
            "window.SF_CONFIG = { isAndAutoEnabled: %b, guiMode: '%s', isCanvasDisabled: %b, autoPlayMode: '%s', closeNowPlay: %b, takeControl: %b, closeLibText: '%s' };",
            AppSingleton.isAndAutoEnabled,
            AppSingleton.guiMode,
            AppSingleton.isCanvasDisabled,
            AppSingleton.autoPlayMode,
            AppSingleton.closeNowPlay,
            AppSingleton.takeControl,
            AppSingleton.appContext.getString(R.string.txt_closelib)
        )
        
        webView.evaluateJavascript(config + AppSingleton.getAssetFile("spotify_bridge.js"), null)

        if (AppSingleton.guiMode == "csshack") {
            val css = AppSingleton.getAssetFile("css_hacks.css")
            val amoledCss = if (AppSingleton.isAmoled) ".encore-dark-theme{--background-base:#000;--background-highlight:#000;--background-elevated-base:#000;--background-elevated-highlight:#000;--background-elevated-press:#000;--background-tinted-base:#000} aside[data-testid=now-playing-bar]{background:#000!important;box-shadow:none;border-top:1px solid #666}" else ""
            
            val classLogic = StringBuilder("document.body.classList.remove('sf-canvas-blur', 'sf-video-bg', 'sf-fullscreen-enabled', 'sf-fullscreen-disabled', 'sf-hide-canvas');")
            if (AppSingleton.isCanvasDisabled) {
                classLogic.append("document.body.classList.add('sf-canvas-blur', 'sf-hide-canvas');")
            } else {
                classLogic.append("document.body.classList.add('sf-video-bg');")
            }
            if (AppSingleton.isFullScreenEnabled) {
                classLogic.append("document.body.classList.add('sf-fullscreen-enabled');")
            } else {
                classLogic.append("document.body.classList.add('sf-fullscreen-disabled');")
            }

            val injectCssJs = String.format(
                "(function() {" +
                "  let style = document.getElementById('sf-custom-style');" +
                "  if(!style) { style = document.createElement('style'); style.id = 'sf-custom-style'; document.head.appendChild(style); }" +
                "  style.textContent = `%s` + `%s`;" +
                "  %s" +
                "})();",
                css, amoledCss, classLogic.toString()
            )
            webView.evaluateJavascript(injectCssJs, null)
        }
    }

    override fun shouldInterceptRequest(webView: WebView, webResourceRequest: WebResourceRequest): WebResourceResponse? {
        val url = webResourceRequest.url.toString()
        val requestHeaders = webResourceRequest.requestHeaders

        // 1. Instant Analytics Block (Master list)
        if (url.contains("doubleclick.net") || url.contains("googlesyndication.com") || 
            url.contains("fastly-insights.com") || url.contains("sentry.io")) {
            val headers = HashMap<String, String>()
            headers["Access-Control-Allow-Origin"] = "*"
            return WebResourceResponse("text/plain", "utf-8", 200, "OK", headers, ByteArrayInputStream(ByteArray(0)))
        }

        // 2. Extension Skip (Matches 1.0.9 logic)
        val path = webResourceRequest.url.path
        if (path != null) {
            val lowerPath = path.lowercase()
            if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".png") || 
                lowerPath.endsWith(".js") || lowerPath.endsWith(".css") || lowerPath.endsWith(".svg") || 
                lowerPath.endsWith(".json") || lowerPath.endsWith(".woff2") || lowerPath.endsWith(".woff")) {
                return null
            }
        }

        // 3. Canvas Block (Matches v1.0.9 logic)
        if (AppSingleton.isCanvasDisabled && (url.endsWith(".mp4") || url.endsWith(".webm") || url.contains("/canvaz/"))) {
            return WebResourceResponse("text/plain", "utf-8", 200, "OK", null, ByteArrayInputStream(ByteArray(0)))
        }

        // 4. Ad Block Implementation from 1.0.9 (Simple and stable)
        if (url.contains(".net/audio/") || url.contains(".co/audio/") || 
            url.contains("/mp3-ad/") || url.contains("amillionads.com") || 
            url.contains("2mdn.net") || url.contains("adxcel.com") || 
            url.contains("akamaized.net/audio/") || url.contains("scdn.co/audio/") || 
            url.contains("scdn.co/mp3-ad/") || url.contains("spotifycdn.com/audio/") || 
            url.contains("adstudio-assets.scdn.co")) {

            var connection: HttpURLConnection? = null
            try {
                connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = webResourceRequest.method
                for ((key, value) in requestHeaders) {
                    connection.setRequestProperty(key, value)
                }
                connection.connectTimeout = 2000
                connection.readTimeout = 2000
                connection.connect()

                val contentType = connection.contentType
                if (contentType != null && contentType.startsWith("audio/mpeg") && 
                    !url.contains("podz-content") && !url.contains("gew4-spclient")) {
                    
                    AndBridge(webView.context).deferMessage("adblock")
                    val res = WebResourceResponse("audio/mpeg", null, webView.context.assets.open("silent.mp3"))
                    connection.disconnect()
                    return res
                }
            } catch (e: Exception) {
                Log.e("Spotifuck", "Adblock Error: " + e.message)
            } finally {
                connection?.disconnect()
            }
        }

        return null
    }
}
