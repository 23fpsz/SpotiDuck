# SpotiDuck 🦆

SpotiDuck is a high-performance, web-wrapped Spotify client for Android. By wrapping the Spotify Web Player in a highly optimized Android WebView, SpotiDuck combines the full feature set of Spotify's web browser experience with native Android integrations—such as background service control, hardware ad-blocking, lock screen media sessions, widgets, and Android Auto.

---

## 🚀 Key Features

*   **⚡ WebView Pre-warming (Preloading)**: Instantiates the WebView and starts DNS resolution and webpage loading immediately during the `Application.onCreate()` phase, cutting startup wait times drastically.
*   **🔇 Hardware-Level Ad Blocking**: Intercepts ad and tracking requests (`shouldInterceptRequest`) and blocks them. Supports three ad-blocking modes configured via a clean, nested settings dialog:
    *   **Legacy (Connection-Verified)**: Verifies media streams via background HTTP connections to prevent legitimate song skipping.
    *   **Instant (In-Memory)**: Blocks known ad/analytics domains instantly in-memory, bypassing network connection checks.
    *   **Dynamic Blocklist (In-Memory)**: Uses a custom-parsed domain blocklist that can be configured and updated dynamically from a remote URL directly within the settings dialog.
*   **🎵 MediaSession & Lock Screen Controls**: Binds WebView playback states to Android's `MediaSessionCompat` API. Supports full metadata syncing (song title, artist, duration, dynamic album art fetched via Picasso) and lock screen buttons (**Favorite**, **Repeat**, **Shuffle**, **Skip**, **Play/Pause**, and **Seek** progress).
*   **📱 Immersive Fullscreen Player & Gestures**: A custom SpotiCap-style fullscreen layout featuring a dynamic background gradient and a pulsing album art shadow glow matching the colors of the current track's artwork. Includes swipe gestures on artwork/info to skip tracks and tap-to-play overrides.
*   **🚗 Android Auto Support**: Implements `MediaBrowserServiceCompat` to expose your playlists, albums, artists, and podcasts directly to Android Auto or automotive media centers.
*   **🔋 Background Playback**: Employs partial CPU WakeLocks and foreground media playback services (`WebService`) to prevent Android from suspending audio execution when the app is placed in the background.
*   **🎨 Custom UI & Layout Hacks**:
    *   **Amoled Mode**: Inject custom CSS overrides for true pitch-black background layouts.
    *   **Landscape Auto-Layout**: Responsive CSS constraints that adapt WebView layouts automatically when rotating between Portrait and Landscape orientations.
    *   **Search suggestions auto-dismiss**: Registered listeners to blur and hide overlays to prevent UI overlap.
*   **📱 Interactive Widgets**: A responsive home screen widget that automatically adapts its layout from a compact 4x1 player to an expanded 4x2 view containing progress indicators.

---

## 📂 Project Structure

The project is modularly structured in Kotlin under the main package `com.spotifuck.music`:

*   **[MainActivity.kt](app/src/main/java/com/spotifuck/music/MainActivity.kt)**: Manages layout container attachments, splash overlay transitions, immersive fullscreen logic, and activity lifecycle hooks.
*   **[AppSingleton.kt](app/src/main/java/com/spotifuck/music/AppSingleton.kt)**: The main `Application` class. Initiates background WebView preloading and handles global shared preferences.
*   **[WebService.kt](app/src/main/java/com/spotifuck/music/WebService.kt)**: The foreground service that handles MediaSession states, notifies the Android system of playback changes, updates widgets, and provides the Android Auto browser catalog.
*   **[SpotifyWebViewClient.kt](app/src/main/java/com/spotifuck/music/SpotifyWebViewClient.kt)**: The custom `WebViewClient` that manages ad-blocking, blocks tracking scripts, intercepts page error events, and injects CSS/JS hacks.
*   **[AndBridge.kt](app/src/main/java/com/spotifuck/music/AndBridge.kt)**: The Javacript interface exposing Android APIs (like player states, volume adjust, and auto-sleep timers) to the Web Player's DOM.
*   **[LinkHandler.kt](app/src/main/java/com/spotifuck/music/LinkHandler.kt)**: Resolves shared and incoming Spotify link intents and redirects playback commands to the active WebView.

---

## 🛠️ Building & Installation

### Prerequisites
*   **JDK 17** (Temurin distribution recommended)
*   **Android SDK** (Target API Level 34 / Compile API Level 36)
*   **Android Studio** or **Gradle 9.x+**

### Local Build Commands
To compile the project and generate a debug build:
```bash
# Set your Java Home path if it is not in your environment variable
$env:JAVA_HOME="C:\Path\To\JDK"

# Run Gradle assemble
.\gradlew assembleDebug
```
The compiled APK will be output to `app/build/outputs/apk/debug/app-debug.apk`.
