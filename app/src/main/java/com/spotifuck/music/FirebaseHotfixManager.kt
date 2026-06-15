package com.spotifuck.music

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import java.io.File

object FirebaseHotfixManager {
    private const val TAG = "FirebaseHotfixManager"
    
    private val HOTFIX_FILES = listOf(
        "spotify_bridge.js",
        "css_hacks.css",
        "classic_login.js",
        "desktop_spoof.js",
        "facebook_consent.js"
    )

    private var isListenerRegistered = false

    fun initialize(context: Context) {
        try {
            val remoteConfig = Firebase.remoteConfig
            
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 0L
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Firebase Remote Config fetch and activate succeeded")
                        applyHotfixes(context)
                    } else {
                        Log.e(TAG, "Firebase Remote Config fetch failed", task.exception)
                    }
                }

            // Real-time updates registration
            if (!isListenerRegistered) {
                remoteConfig.addOnConfigUpdateListener(object : com.google.firebase.remoteconfig.ConfigUpdateListener {
                    override fun onUpdate(configUpdate: com.google.firebase.remoteconfig.ConfigUpdate) {
                        Log.d(TAG, "Real-time config update detected: ${configUpdate.updatedKeys}")
                        remoteConfig.activate().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d(TAG, "Real-time config activated successfully")
                                applyHotfixes(context)
                            } else {
                                Log.e(TAG, "Failed to activate config after real-time update")
                            }
                        }
                    }

                    override fun onError(error: com.google.firebase.remoteconfig.FirebaseRemoteConfigException) {
                        Log.e(TAG, "Real-time config update error", error)
                    }
                })
                isListenerRegistered = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase Remote Config", e)
        }
    }

    private fun applyHotfixes(context: Context) {
        val hotfixDir = File(context.filesDir, "hotfixes")
        if (!hotfixDir.exists()) {
            hotfixDir.mkdirs()
        }

        val remoteConfig = Firebase.remoteConfig
        var cssUpdated = false
        var jsUpdated = false

        for (fileName in HOTFIX_FILES) {
            val configKey = "hotfix_" + fileName.substringBeforeLast(".")
            val remoteContent = remoteConfig.getString(configKey)

            val localFile = File(hotfixDir, fileName)
            val currentLocalContent = if (localFile.exists()) localFile.readText() else ""

            if (remoteContent.isNotEmpty()) {
                if (remoteContent != currentLocalContent) {
                    Log.d(TAG, "Updating hotfix for $fileName (length: ${remoteContent.length})")
                    try {
                        localFile.writeText(remoteContent)
                        AppSingleton.clearAssetCache(fileName)
                        if (fileName.endsWith(".css")) {
                            cssUpdated = true
                        } else if (fileName.endsWith(".js")) {
                            jsUpdated = true
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to write hotfix for $fileName", e)
                    }
                }
            } else {
                if (localFile.exists()) {
                    Log.d(TAG, "Removing hotfix for $fileName (falling back to built-in asset)")
                    localFile.delete()
                    AppSingleton.clearAssetCache(fileName)
                    if (fileName.endsWith(".css")) {
                        cssUpdated = true
                    } else if (fileName.endsWith(".js")) {
                        jsUpdated = true
                    }
                }
            }
        }

        // Live Apply Updates to WebView
        if (cssUpdated || jsUpdated) {
            AppSingleton.globalWebView?.post {
                val webView = AppSingleton.globalWebView ?: return@post
                
                // 1. Live-inject CSS changes without interrupting user playback
                if (cssUpdated && AppSingleton.guiMode == "csshack") {
                    val css = AppSingleton.getAssetFile("css_hacks.css")
                    val amoledCss = if (AppSingleton.isAmoled) {
                        ".encore-dark-theme{--background-base:#000;--background-highlight:#000;--background-elevated-base:#000;--background-elevated-highlight:#000;--background-elevated-press:#000;--background-tinted-base:#000} aside[data-testid=now-playing-bar]{background:#000!important;box-shadow:none;border-top:1px solid #666}"
                    } else {
                        ""
                    }
                    
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
                        "  style.textContent = %s + %s;" +
                        "  %s" +
                        "})();",
                        org.json.JSONObject.quote(css),
                        org.json.JSONObject.quote(amoledCss),
                        classLogic.toString()
                    )
                    webView.evaluateJavascript(injectCssJs, null)
                    Log.d(TAG, "Live CSS hotfix injected successfully")
                    MainActivity.showMessage("Styles updated!")
                }

                // 2. Apply JS changes: reload if not playing, otherwise wait for next launch/reload
                if (jsUpdated) {
                    if (!WebService.isPlaying) {
                        Log.d(TAG, "JS hotfix updated. Music not playing, reloading WebView...")
                        MainActivity.showMessage("Updating web player...")
                        webView.reload()
                    } else {
                        Log.d(TAG, "JS hotfix updated but music is playing. Updates will apply on next launch/reload.")
                        MainActivity.showMessage("Updates will apply on next launch.")
                    }
                }
            }
        }
    }
}
