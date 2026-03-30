package com.spotifuck.music

import android.os.Handler
import android.os.Looper
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient

class SpotifyWebChromeClient : WebChromeClient() {
    override fun onPermissionRequest(request: PermissionRequest) {
        Handler(Looper.getMainLooper()).post {
            if (request.resources.contains(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)) {
                request.grant(request.resources)
            } else {
                request.deny()
            }
        }
    }
}
