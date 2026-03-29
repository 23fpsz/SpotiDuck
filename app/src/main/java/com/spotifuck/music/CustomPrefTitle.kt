package com.spotifuck.music

import android.content.Context
import android.content.pm.PackageManager
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class CustomPrefTitle(context: Context, attributeSet: AttributeSet?) : Preference(context, attributeSet) {
    init {
        layoutResource = R.layout.custom_pref_title
        
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            var version = pInfo.versionName
            
            if (version != null && version.contains("-")) {
                version = version.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            }

            summary = "v$version by OVERLORD"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.isClickable = false
    }
}
