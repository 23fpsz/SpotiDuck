package com.spotifuck.music

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LinkHandler : AppCompatActivity() {
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        var spotifyUri: String? = null

        if (Intent.ACTION_SEND == intent.action && intent.type == "text/plain") {
            // Handle shared text (e.g., sharing from the Spotify app)
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            val url = extractUrl(sharedText)
            if (url != null) {
                spotifyUri = convertToSpotifyUri(Uri.parse(url))
            }
        } else {
            // Handle direct clicks on links
            val data = intent.data
            if (data != null) {
                spotifyUri = convertToSpotifyUri(data)
            }
        }

        if (spotifyUri == null) {
            finish()
            return
        }

        // Play immediately if service is ready
        if (WebService.isServiceRunning && AppSingleton.isPlayerLoaded) {
            val uriToPlay = spotifyUri
            AppSingleton.globalWebView?.post { 
                AppSingleton.globalWebView?.evaluateJavascript("playFromUri('$uriToPlay');", null) 
            }
        }

        // Pass to MainActivity to handle playback or wait for load
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("spotify_uri", spotifyUri)
        }
        startActivity(intent)
        finish()
        overridePendingTransition(0, 0)
    }

    private fun extractUrl(text: String?): String? {
        if (text == null) return null
        val regex = "(https?://[\\w-]+(\\.[\\w-]+)+(/\\S*)?)".toRegex()
        return regex.find(text)?.value
    }

    private fun convertToSpotifyUri(data: Uri): String? {
        val scheme = data.scheme
        if ("spotify" == scheme) {
            return data.toString()
        } else if (("https" == scheme || "http" == scheme) && 
                   (data.host == "open.spotify.com" || data.host == "spotify.link")) {
            
            // For spotify.link, we can't easily resolve the ID without a network request,
            // but for open.spotify.com we can convert it directly.
            if (data.host == "spotify.link") {
                return data.toString() // Pass the URL as is, the player might handle it
            }

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
                return sb.toString()
            }
        }
        return null
    }
}
