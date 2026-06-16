package com.spotifuck.music

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(bundle: Bundle?) {
        if (AppSingleton.isAmoled) {
            setTheme(R.style.Theme_Spotifuck_Settings_Amoled)
        }
        
        super.onCreate(bundle)
        setContentView(R.layout.settings_activity)

        if (bundle == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings, SettingsFragment())
                    .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key == null) return
        
        when (key) {
            "ServiceOn" -> {
                AppSingleton.isServiceEnabled = sharedPreferences.getBoolean(key, true)
                val intent = Intent(AppSingleton.appContext, WebService::class.java)
                if (AppSingleton.isServiceEnabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    AppSingleton.activityRef?.get()?.let { main ->
                        main.runOnUiThread { main.setupWebView() }
                    }
                } else {
                    startService(intent.setAction("STOP_SERVICE"))
                }
                AppSingleton.notifyUiUpdate()
            }
            "Amoled" -> {
                AppSingleton.isAmoled = sharedPreferences.getBoolean(key, false)
                AppSingleton.pushLiveUpdate()
                recreate()
            }
            "SwipeStop" -> {
                AppSingleton.stopOnSwipe = sharedPreferences.getBoolean(key, true)
            }
            "AutoSleep" -> {
                AppSingleton.autoSleepMinutes = sharedPreferences.getString(key, "0")?.toInt() ?: 0
            }
            "TakeControl" -> {
                AppSingleton.takeControl = sharedPreferences.getBoolean(key, true)
                AppSingleton.pushLiveUpdate()
            }
            "BTAP" -> {
                AppSingleton.btPlay = sharedPreferences.getBoolean(key, false)
            }
            "BTAS" -> {
                AppSingleton.btPause = sharedPreferences.getBoolean(key, false)
            }
            "HPAP" -> {
                AppSingleton.headsetPlay = sharedPreferences.getBoolean(key, false)
            }
            "HPAS" -> {
                AppSingleton.headsetPause = sharedPreferences.getBoolean(key, false)
            }
            "ForcePortrait" -> {
                AppSingleton.isForcePortrait = sharedPreferences.getBoolean(key, false)
            }
            "APlayMode" -> {
                AppSingleton.autoPlayMode = sharedPreferences.getString(key, "disabled")
                AppSingleton.pushLiveUpdate()
            }
            "AndAuto" -> {
                AppSingleton.isAndAutoEnabled = sharedPreferences.getBoolean(key, true)
            }
            "CloseNowPlay" -> {
                AppSingleton.closeNowPlay = sharedPreferences.getBoolean(key, false)
                AppSingleton.pushLiveUpdate()
            }
            "ExpandedFullScreen" -> {
                AppSingleton.isFullScreenEnabled = sharedPreferences.getBoolean(key, true)
                AppSingleton.pushLiveUpdate()
            }
            "ForceEn" -> {
                AppSingleton.isForceEn = sharedPreferences.getBoolean(key, false)
                MainActivity.shouldReloadWebView = true
            }
            "AutoShut" -> {
                AppSingleton.autoShutMinutes = sharedPreferences.getString(key, "0")?.toInt() ?: 0
            }
            "GuiMode" -> {
                AppSingleton.guiMode = sharedPreferences.getString(key, "csshack")
                MainActivity.shouldReloadWebView = true
            }
            "DisableCanvas" -> {
                AppSingleton.isCanvasDisabled = sharedPreferences.getBoolean(key, true)
                AppSingleton.pushLiveUpdate()
            }
            "PrioritizeLocalAssets" -> {
                AppSingleton.prioritizeLocalAssets = sharedPreferences.getBoolean(key, true)
                MainActivity.shouldReloadWebView = true
            }
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val isDebug = (requireContext().applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (!isDebug) {
                findPreference<Preference>("PrioritizeLocalAssets")?.isVisible = false
            }

            findPreference<Preference>("ClearCache")?.setOnPreferenceClickListener {
                MainActivity.shouldReloadWebView = true
                activity?.finish()
                true
            }

            findPreference<Preference>("ClearData")?.setOnPreferenceClickListener {
                AlertDialog.Builder(requireContext())
                        .setTitle(R.string.dlg_title)
                        .setMessage(R.string.dlg_text)
                        .setNegativeButton(R.string.dlg_no, null)
                        .setPositiveButton(R.string.dlg_yes) { _, _ ->
                            MainActivity.shouldClearCookies = true
                            AppSingleton.isLoggedIn = false
                            AppSingleton.prefsEditor.putBoolean("LoggedIn", false).apply()
                            activity?.finish()
                        }
                        .show()
                true
            }
        }
    }
}
