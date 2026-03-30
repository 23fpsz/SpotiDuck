# Section 7: Security Best Practices - ProGuard/R8 Rules

# Preserve JavascriptInterface methods so they aren't stripped or renamed
-keepattributes JavascriptInterface
-keepattributes *Annotation*

# Keep the bridge class and all its public methods
-keep class com.spotifuck.music.AndBridge {
    public *;
}

# Keep the JavaScript interface methods specifically
-keepclassmembers class com.spotifuck.music.AndBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Picasso for album art loading
-keep class com.squareup.picasso.** { *; }
-dontwarn com.squareup.picasso.**

# OkHttp3 rules (brought in by Picasso)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Keep MediaSession and related support classes
-keep class android.support.v4.media.** { *; }
-keep class androidx.media.** { *; }

# General WebView protection
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, android.webkit.RenderProcessGoneDetail);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.PermissionRequest);
}