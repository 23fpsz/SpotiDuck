package com.spotifuck.music

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LinkHandler : AppCompatActivity() {
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        val data = intent.data
        if (data == null) {
            finish()
            return
        }

        var spotifyUri: String? = null
        val scheme = data.scheme
        if ("spotify" == scheme) {
            spotifyUri = data.toString()
        } else if ("https" == scheme && "open.spotify.com" == data.host) {
            var pathSegments = data.pathSegments
            if (pathSegments.isNotEmpty() && pathSegments[0].startsWith("intl-")) {
                pathSegments = pathSegments.subList(1, pathSegments.size)
            }
            if (pathSegments.size >= 2) {
                val sb = StringBuilder("spotify")
                for (str in pathSegments) {
                    sb.append(":")
                    sb.append(str)
                }
                spotifyUri = sb.toString()
            }
        }

        if (WebService.isServiceRunning && AppSingleton.isPlayerLoaded) {
            val uriToPlay = spotifyUri
            AppSingleton.globalWebView?.post { AppSingleton.globalWebView?.evaluateJavascript("playFromUri('$uriToPlay');", null) }
        } else {
            Toast.makeText(this, getString(R.string.txt_notloaded), Toast.LENGTH_SHORT).show()
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
        overridePendingTransition(0, 0)
    }
}
