package com.spotifuck.music

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
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
            "AdBlockMode" -> {
                AppSingleton.adBlockMode = sharedPreferences.getString(key, "legacy") ?: "legacy"
                MainActivity.shouldReloadWebView = true
            }
            "AdBlockListUrl" -> {
                AppSingleton.adBlockListUrl = sharedPreferences.getString(key, "https://raw.githubusercontent.com/Isaaker/Spotify-AdsList/main/Lists/standard_list.txt") ?: "https://raw.githubusercontent.com/Isaaker/Spotify-AdsList/main/Lists/standard_list.txt"
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

            val adBlockModePref = findPreference<Preference>("AdBlockMode")

            fun updateModeSummary(mode: String?) {
                val entries = resources.getStringArray(R.array.adblock_entries)
                val values = resources.getStringArray(R.array.adblock_values)
                val index = values.indexOf(mode)
                if (index >= 0 && index < entries.size) {
                    adBlockModePref?.summary = entries[index]
                } else {
                    adBlockModePref?.summary = mode ?: "Legacy (Connection-Verified)"
                }
            }

            val initialMode = AppSingleton.prefs.getString("AdBlockMode", "legacy")
            updateModeSummary(initialMode)

            adBlockModePref?.setOnPreferenceClickListener {
                val context = requireContext()
                val inflater = LayoutInflater.from(context)
                val dialogView = inflater.inflate(R.layout.dialog_adblock_config, null)

                val spinner = dialogView.findViewById<Spinner>(R.id.dialog_adblock_spinner)
                val dynamicLayout = dialogView.findViewById<View>(R.id.dialog_dynamic_config_layout)
                val urlEdit = dialogView.findViewById<EditText>(R.id.dialog_adblock_url_edit)
                val updateBtn = dialogView.findViewById<Button>(R.id.dialog_adblock_update_btn)

                // Set up spinner adapter
                val adapter = ArrayAdapter(
                    context,
                    android.R.layout.simple_spinner_item,
                    resources.getStringArray(R.array.adblock_entries)
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter

                // Pre-select current mode
                val values = resources.getStringArray(R.array.adblock_values)
                val currentMode = AppSingleton.prefs.getString("AdBlockMode", "legacy")
                val selectedIndex = values.indexOf(currentMode).let { if (it >= 0) it else 0 }
                spinner.setSelection(selectedIndex)

                // Pre-fill current URL
                val currentUrl = AppSingleton.prefs.getString("AdBlockListUrl", "https://raw.githubusercontent.com/Isaaker/Spotify-AdsList/main/Lists/standard_list.txt")
                urlEdit.setText(currentUrl)

                // Toggle visibility of dynamic settings based on spinner selection
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val isDynamic = values.getOrNull(position) == "dynamic"
                        dynamicLayout.visibility = if (isDynamic) View.VISIBLE else View.GONE
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                // Handle manual update button click inside dialog
                updateBtn.setOnClickListener {
                    val newUrl = urlEdit.text.toString().trim()
                    if (newUrl.isEmpty()) {
                        android.widget.Toast.makeText(context, "URL cannot be empty", android.widget.Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    
                    // Temporarily save the URL in preferences so the update task uses it
                    AppSingleton.prefsEditor.putString("AdBlockListUrl", newUrl).apply()
                    AppSingleton.adBlockListUrl = newUrl

                    android.widget.Toast.makeText(context, "Updating blocklist...", android.widget.Toast.LENGTH_SHORT).show()
                    AppSingleton.fetchAdBlockHostsAsync(context) { success ->
                        activity?.runOnUiThread {
                            if (success) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Blocklist updated! ${AppSingleton.adBlockHosts.size} domains loaded",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } else {
                                android.widget.Toast.makeText(context, "Failed to update blocklist.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                AlertDialog.Builder(context)
                    .setTitle("Ad Blocker Settings")
                    .setView(dialogView)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Save") { _, _ ->
                        val selectedPos = spinner.selectedItemPosition
                        val newMode = values.getOrNull(selectedPos) ?: "legacy"
                        val newUrl = urlEdit.text.toString().trim()

                        AppSingleton.prefsEditor.putString("AdBlockMode", newMode).apply()
                        AppSingleton.adBlockMode = newMode
                        
                        if (newMode == "dynamic") {
                            AppSingleton.prefsEditor.putString("AdBlockListUrl", newUrl).apply()
                            AppSingleton.adBlockListUrl = newUrl
                        }

                        updateModeSummary(newMode)
                        MainActivity.shouldReloadWebView = true
                    }
                    .show()

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
