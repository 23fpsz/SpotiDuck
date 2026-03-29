I have built a Spotify Web Wrapper app in Android Studio and need fine refinements.
Do deep research on the following areas and provide actionable improvements:

## 1. WebView Optimization for Music Streaming
- Best practices for configuring WebView/WebViewClient/WebChromeClient
  for audio-heavy web apps
- Hardware acceleration settings specific to media playback
- Memory management and preventing WebView memory leaks
- Cache strategies (Application Cache, DOM Storage, Database storage)
  for Spotify's web player
- Proper handling of WebView process crashes and recovery
- Multi-process WebView architecture pros/cons for streaming apps

## 2. Spotify Web Player Specific Handling
- How to handle Spotify's DRM (Widevine) content within Android WebView
- Proper User-Agent string to ensure Spotify serves the full web player
  (not a mobile redirect or "download the app" page)
- JavaScript injection techniques to enhance/modify the Spotify web UI
  for a native-like feel
- Handling Spotify's OAuth login flow inside WebView securely
- Cookie persistence and session management so users stay logged in
- Handling Spotify Connect and device picker within WebView
- Deep link handling (spotify: URIs and open.spotify.com links)

## 3. Media & Audio Integration
- MediaSession API integration so the app works with:
    - Lock screen controls
    - Bluetooth metadata (track name, artist, album art)
    - Android Auto compatibility
    - Wear OS media controls
- Extracting Now Playing info from WebView (via JavaScript bridge)
  to populate MediaSession metadata
- Audio focus handling (duck, pause on calls, respond to other apps)
- Preventing audio interruption when WebView goes to background

## 4. Background Playback
- Foreground Service implementation for uninterrupted background audio
- Proper notification with media controls (play/pause/skip)
- Battery optimization whitelisting and Doze mode handling
- Wakelock strategies without excessive battery drain
- Handling task removal and swipe-to-kill scenarios

## 5. Native Android Feel & UI Refinements
- Edge-to-edge display and status bar/navigation bar theming
  to match Spotify's dark UI
- Splash screen API implementation (Android 12+)
- Pull-to-refresh implementation
- Handling back navigation properly within WebView
  (history stack management)
- Immersive mode considerations
- Dynamic color/Material You theming that complements Spotify's brand
- Custom error pages when offline or page fails to load
- Loading indicators/progress bars that feel native
- Swipe gestures for navigation

## 6. Network & Offline Handling
- Graceful offline detection and user-friendly error screens
- Network change listeners (WiFi to mobile data seamless transition)
- Bandwidth detection for adaptive streaming quality
- Prefetching and Service Worker support in WebView
- Intercepting network requests via WebViewClient.shouldInterceptRequest()

## 7. Security Best Practices
- SSL pinning for Spotify domains
- Preventing WebView vulnerabilities (XSS, URL scheme attacks)
- Safe handling of file uploads/downloads within WebView
- ProGuard/R8 rules for WebView-related classes
- Preventing screen recording/screenshots of DRM content
  (FLAG_SECURE considerations)

## 8. Performance Profiling
- WebView rendering performance (GPU profiling)
- JavaScript performance monitoring
- ANR prevention in WebView-heavy apps
- Startup time optimization (preloading WebView, process pre-warming)
- Reducing APK size while keeping WebView features

## 9. Distribution & Compliance
- Google Play Store policies regarding web wrapper apps
- Spotify's Terms of Service for third-party clients/wrappers
- Trademark/branding guidelines to avoid takedowns
- Privacy policy requirements for apps handling Spotify credentials

## 10. Advanced Features to Consider
- Picture-in-Picture mode for video podcasts/music videos
- Download manager integration if Spotify web supports downloads
- Share intent handling (receiving shared Spotify links)
- Widget support showing current track
- Shortcuts API (long-press app icon for quick actions)
- Tasker/automation app integration

For each area, provide:
1. Specific code snippets or configuration changes
2. AndroidManifest.xml entries needed
3. Gradle dependencies if applicable
4. Known pitfalls and how to avoid them
5. Testing strategies