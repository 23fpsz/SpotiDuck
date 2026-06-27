# SpotiDuck Technical Documentation

This document provides a comprehensive, exhaustive breakdown of the SpotiDuck codebase, architecture, settings, JavaScript injection modules, ad-blocking system, media sessions, and system integrations.

---

## 📂 Obfuscation Analysis & History

The original application, packaged under `it.deviato.spotifuck`, utilized a moderate level of R8 obfuscation:
- Packages were renamed to random identifiers like `p000A`, `p001B`, etc.
- Core classes and interfaces were scrambled to single letters or sequential names (e.g., `AbstractC0000a`, `C0363e`, `C0364f`).
- Fields and methods were shortened (e.g., `f3444F`, `m7d`, `m9f`).

The SpotiDuck codebase has been fully decompiled, analyzed, refactored, and rewritten in clean, non-obfuscated Kotlin under the package `com.spotifuck.music`. All obfuscated helper logic, nested wrapper classes, and scrambled API interfaces have been replaced with standard Android SDK components and clean asynchronous helper routines.

---

## 📂 Architecture Overview

SpotiDuck is structured as a single-module Android application written in Kotlin. It targets the Spotify Web Player via a custom, preloaded WebView container and integrates native Android system services to provide background audio playback, lock screen media controls, widgets, and Android Auto interface integration.

---

## 📄 Files Analyzed

### 1. MainActivity
```
FILE: app/src/main/java/com/spotifuck/music/MainActivity.kt
├── LOCATION: app/src/main/java/com/spotifuck/music/MainActivity.kt
├── TYPE: Kotlin Class (AppCompatActivity)
├── PURPOSE: Main UI container of the application, managing layouts, splash screens, permissions, and WebView attachment.
│
├── KEY FINDINGS:
│   ├── line 98-215: onCreate() sets up window parameters (transparency, edge-to-edge), registers settings button click, and binds back press callback.
│   ├── line 237-298: restartService() destroys the running WebView, stops the service, and recreates the setup after a short delay.
│   ├── line 320-341: syncUiState() synchronizes UI splash screens, error displays, and the progress bar state.
│   ├── line 495-530: setupWebView() retrieves/attaches the background WebView and configures its display width.
│   └── line 539-596: onResume() handles orientation, deep-link triggers, and re-attaches the WebView context.
│
├── METHODS/FUNCTIONS:
│   ├── onCreate(): Binds layout views, handles edge-to-edge system insets, and registers callback for system back button navigation.
│   ├── onResume(): Re-attaches WebView, checks if WebService is running, and processes pending URIs.
│   ├── setupWebView(): Retrieves the singleton WebView instance and attaches it to the layout container.
│   ├── restartService(): Completely resets the WebView state and restarts the background service.
│   ├── syncUiState(): Syncs loading progress bar and splash overlay views based on login and playback states.
│   └── showInstanceMessage(): Flashes brief status notification messages on the UI.
│
├── DEPENDENCIES:
│   ├── AppSingleton: Retrieves the global WebView instance and settings configuration.
│   ├── WebService: Controls foreground media service states.
│   └── LockableHScrollView: A custom scroll view wrapper for layout modes.
│
└── NOTES:
    ├── Leverages MutableContextWrapper to enable WebView preloading before UI attachment.
    ├── Supports responsive scroll layouts and edge-to-edge immersive viewing mode.
    └── Manages dynamic splash overlay and error display transitions.
```

### 2. AppSingleton
```
FILE: app/src/main/java/com/spotifuck/music/AppSingleton.kt
├── LOCATION: app/src/main/java/com/spotifuck/music/AppSingleton.kt
├── TYPE: Kotlin Class (Application)
├── PURPOSE: Holds global application state, initializes preferences, and hosts the pre-warmed WebView factory.
│
├── KEY FINDINGS:
│   ├── line 147-233: getWebView() is the singleton factory that instantiates the WebView using a MutableContextWrapper.
│   ├── line 185-202: Configures WebSettings (JavaScript, DOM storage, custom desktop User-Agent, autofill support).
│   ├── line 236-276: onCreate() initializes context, parses SharedPreferences, and schedules the background preloading task.
│   └── line 273-276: Enqueues an asynchronous pre-warming task on the main thread after a 150ms delay.
│
├── METHODS/FUNCTIONS:
│   ├── onCreate(): Loads initial shared preferences, initializes the hotfix manager, and schedules pre-warming.
│   ├── getWebView(): Lazy-creates the WebView or updates its contextWrapper baseContext to the active Activity context.
│   ├── getAssetFile(): Loads asset contents (JS/CSS) from local storage or downloads hotfixes dynamically.
│   └── isNetworkAvailable(): Utility method checking standard internet network capabilities.
│
├── DEPENDENCIES:
│   ├── PreferenceManager: For persistent user settings toggles.
│   ├── MutableContextWrapper: Allows hot-swapping WebView contexts between Application and Activity.
│   └── FirebaseHotfixManager: Checks for online script updates.
│
└── NOTES:
    ├── Pre-warms the WebView on startup to fetch Spotify's pages before the UI is drawn.
    ├── Spoofs user agent as a Windows Desktop browser to force Spotify to render the full Web Player UI.
    └── Stores an in-memory cache for injected JS/CSS assets to prevent disk read bottlenecks.
```

### 3. WebService
```
FILE: app/src/main/java/com/spotifuck/music/WebService.kt
├── LOCATION: app/src/main/java/com/spotifuck/music/WebService.kt
├── TYPE: Kotlin Class (MediaBrowserServiceCompat)
├── PURPOSE: Foreground playback service managing the MediaSession, system notifications, widget updates, and Android Auto interface.
│
├── KEY FINDINGS:
│   ├── line 54-105: updatePlaybackState() updates the native MediaSession playback metadata and refreshes notification elements.
│   ├── line 113-152: MediaActionReceiver class processes media button broadcast intents (headset plugged/unplugged, Bluetooth profile transitions, widgets).
│   ├── line 220-228: setupMediaSession() initializes MediaSessionCompat, registers callbacks, and sets the active token.
│   ├── line 230-237: startForegroundService() starts the service in the foreground with a mediaPlayback service type.
│   └── line 315-362: onLoadChildren() handles Android Auto library catalog queries asynchronously using JavaScript evaluation.
│
├── METHODS/FUNCTIONS:
│   ├── setupMediaSession(): Creates the media session and sets transport button handles.
│   ├── updatePlaybackState(): Builds and updates PlaybackStateCompat with active position, speed, and custom actions.
│   ├── createNotification(): Instantiates the player notification containing album art and action buttons.
│   ├── stopService(): Releases media session, unregisters callbacks, and destroys the background WebView.
│   └── onLoadChildren(): Evaluates JS against the WebView to map Spotify library nodes for Android Auto.
│
├── DEPENDENCIES:
│   ├── MediaSessionCompat: Relays playback state and metadata to the OS.
│   ├── Picasso: Asynchronously downloads album art bitmaps.
│   └── HomeWidget: Synchronizes status updates with home widgets.
│
└── NOTES:
    ├── Operates as a foreground service with PARTIAL_WAKE_LOCK to prevent system standby during background playback.
    ├── Exposes a standard MediaBrowserService structure allowing seamless Android Auto browsing.
    └── Registers custom action listeners for Repeat, Favorite, and Shuffle states.
```

### 4. SpotifyWebViewClient
```
FILE: app/src/main/java/com/spotifuck/music/SpotifyWebViewClient.kt
├── LOCATION: app/src/main/java/com/spotifuck/music/SpotifyWebViewClient.kt
├── TYPE: Kotlin Class (WebViewClient)
├── PURPOSE: Intercepts page load actions, handles ad-blocking logic, injects custom script overrides, and registers page errors.
│
├── KEY FINDINGS:
│   ├── line 17-20: onPageStarted() injects desktop spoofing script immediately.
│   ├── line 22-91: onPageFinished() injects custom CSS hacks, theme overrides, and the main bridge script.
│   ├── line 92-139: onReceivedError() and onReceivedHttpError() update error states in AppSingleton to prevent splash screen freezes.
│   ├── line 141-198: getSilentMediaResponse() parses Range headers to serve silent.mp3 with HTTP 206 Partial Content support.
│   └── line 200-283: shouldInterceptRequest() matches ad/analytics domains instantly in-memory, replacing media ads with the silent MP3 and bypassing tracks via __token__ checks.
│
├── METHODS/FUNCTIONS:
│   ├── shouldInterceptRequest(): Central filter node intercepting and blocking/replacing specific network requests.
│   ├── getSilentMediaResponse(): Custom responder that serves sliced bytes of silent.mp3 for partial range requests to prevent decoder stalls.
│   ├── onPageFinished(): Configures the web player window, injects CSS layout hacks, and initializes the native-JS bridge.
│   └── onReceivedError(): Catch-all for main-frame loading errors, triggering the UI error screen state.
│
├── DEPENDENCIES:
│   ├── AppSingleton: Accesses player configuration parameters (e.g., canvas disabled, amoled enabled).
│   └── AndBridge: Handles login and page load success signals.
│
└── NOTES:
    ├── Employs instant in-memory blocking for ad/analytics hosts to completely save connection thread-blocking overhead.
    ├── Utilizes silent.mp3 injection supporting HTTP 206 Range headers to cleanly skip audio ads in the web player.
    └── Integrates edge-to-edge and system status bar configuration rules.
```

### 5. AndBridge
```
FILE: app/src/main/java/com/spotifuck/music/AndBridge.kt
├── LOCATION: app/src/main/java/com/spotifuck/music/AndBridge.kt
├── TYPE: Kotlin Class (JavascriptInterface)
├── PURPOSE: Acts as the Javascript-to-Kotlin bridge, parsing player states, metadata, and sleep triggers from the webpage DOM.
│
├── KEY FINDINGS:
│   ├── line 130-134: recMediaPosition() updates playback seek positions.
│   ├── line 137-166: recMediaStatus() parses metadata JSON (song name, artist, duration, play state, repeat, shuffle) and fetches album art.
│   ├── line 37-50: manageTShut() handles automatic shutdown timers.
│   └── line 79-91: wakeOff() and wakeUp() toggle WebView visibility states.
│
├── METHODS/FUNCTIONS:
│   ├── recMediaStatus(): Receives and parses Spotify playback metadata, updating global service states.
│   ├── loginDetected(): Flags the session as authenticated and updates preferences.
│   ├── playLoaded(): Informs the system that the player UI is fully initialized, dismissing the splash screen.
│   └── setExpanded(): Requests the parent activity to toggle immersive fullscreen mode.
│
├── DEPENDENCIES:
│   ├── WebService: Receives updated positions and metadata.
│   └── Picasso: Downloads and parses album art URLs.
│
└── NOTES:
    ├── Functions as the core communications bridge between Javascript injection and Android native code.
    ├── All Javascript interface methods must be marked with the @JavascriptInterface annotation.
    └── Exposes sleep and auto-shutdown timers to the player settings UI.
```

### 6. LinkHandler
```
FILE: app/src/main/java/com/spotifuck/music/LinkHandler.kt
├── LOCATION: app/src/main/java/com/spotifuck/music/LinkHandler.kt
├── TYPE: Kotlin Class (AppCompatActivity)
├── PURPOSE: Resolves shared and incoming Spotify link intents and redirects playback commands to the active WebView.
│
├── KEY FINDINGS:
│   ├── line 15-28: Checks intent types, extracting plain text URLs (from shares) or direct deep-link URIs.
│   ├── line 36-41: If the service is running, it evaluates JavaScript to play the track immediately in the active WebView.
│   └── line 59-86: convertToSpotifyUri() parses incoming Spotify HTTP URLs and converts them to native Spotify URI strings.
│
├── METHODS/FUNCTIONS:
│   ├── onCreate(): Resolves incoming intents, converts track links, and delegates tasks to MainActivity.
│   └── convertToSpotifyUri(): Parses domain and path segments to build standard URI strings (e.g. `spotify:track:id`).
│
├── DEPENDENCIES:
│   ├── WebService: Checks service active states.
│   └── MainActivity: Launches when the player needs to be initialized.
│
└── NOTES:
    ├── Allows other applications to share music directly to SpotiDuck.
    └── Operates as a transparent transit activity, calling finish() immediately.
```

### 7. HomeWidget
```
FILE: app/src/main/java/com/spotifuck/music/HomeWidget.kt
├── LOCATION: app/src/main/java/com/spotifuck/music/HomeWidget.kt
├── TYPE: Kotlin Class (AppWidgetProvider)
├── PURPOSE: Controls the responsive home screen widget, handling layout changes, click broadcasts, and updating widget displays with track metadata.
│
├── KEY FINDINGS:
│   ├── line 32-42: onUpdate() retrieves the widget manager and updates all instances.
│   ├── line 44-78: updateWidget() builds RemoteViews, reads cached metadata, and binds PendingIntents.
│   └── line 80-104: getPlaybackIntent() registers intent triggers for widget media buttons.
│
├── METHODS/FUNCTIONS:
│   ├── onUpdate(): Executed on widget refresh ticks.
│   ├── updateWidget(): Binds layout properties and refreshes cover art bitmaps asynchronously.
│   └── getPlaybackIntent(): Forms PendingIntents mapping to MediaActionReceiver events.
│
├── DEPENDENCIES:
│   ├── RemoteViews: Layout inflation interface.
│   └── WebService: Reads playback state context cache.
│
└── NOTES:
    └── Synchronized by WebService whenever active track metadata transitions.
```

### 8. SettingsActivity
```
FILE: app/src/main/java/com/spotifuck/music/SettingsActivity.kt
├── LOCATION: app/src/main/java/com/spotifuck/music/SettingsActivity.kt
├── TYPE: Kotlin Class (PreferenceActivity)
├── PURPOSE: Implements the preferences UI, exposing settings controls for Amoled mode, canvas, auto-play, timers, and language.
│
├── KEY FINDINGS:
│   ├── line 25-50: PreferenceFragmentCompat configuration class loading XML items.
│   └── line 60-110: onSharedPreferenceChanged() listener syncing edited keys directly back to AppSingleton values.
│
├── METHODS/FUNCTIONS:
│   ├── onCreate(): Binds preference fragments and configures support bars.
│   └── onSharedPreferenceChanged(): Toggles static configurations instantly in memory.
│
└── NOTES:
    └── Syncs with SharedPreferences and notifies the singleton manager on value modifications.
```

### 9. SpotifyWebChromeClient
```
FILE: app/src/main/java/com/spotifuck/music/SpotifyWebChromeClient.kt
├── LOCATION: app/src/main/java/com/spotifuck/music/SpotifyWebChromeClient.kt
├── TYPE: Kotlin Class (WebChromeClient)
├── PURPOSE: Intercepts console messages, file upload handlers, and grants DRM permissions automatically.
│
├── KEY FINDINGS:
│   └── line 10-18: onPermissionRequest() intercepts requests and automatically grants Widevine DRM permissions.
│
└── NOTES:
    └── Direct permission grant guarantees uninterrupted media decoding under WebView.
```

### 10. FirebaseHotfixManager
```
FILE: app/src/main/java/com/spotifuck/music/FirebaseHotfixManager.kt
├── LOCATION: app/src/main/java/com/spotifuck/music/FirebaseHotfixManager.kt
├── TYPE: Kotlin Class (Object)
├── PURPOSE: Checks Firebase Remote Config parameters to download and save dynamic CSS/JS hotfix scripts.
│
├── KEY FINDINGS:
│   ├── line 40-75: initialize() sets up default configurations and triggers fetch requests.
│   └── line 80-112: updateHotfixes() retrieves, validates, and stores fetched files in internal directories.
│
└── NOTES:
    └── Dynamically bypasses layout updates from Spotify without rebuilding the Android package.
```

### 11. CustomPrefTitle
```
FILE: app/src/main/java/com/spotifuck/music/CustomPrefTitle.kt
├── LOCATION: app/src/main/java/com/spotifuck/music/CustomPrefTitle.kt
├── TYPE: Kotlin Class (Preference)
├── PURPOSE: Custom Preference widget displaying the application name and version details on top of the Settings screen.
│
├── KEY FINDINGS:
│   ├── line 13-24: Extracts package name and VersionName dynamically from PackageManager.
│   └── line 27-30: Overrides onBindViewHolder to disable clicks.
│
└── NOTES:
    └── Strips hyphen metadata tags to display clean release version numbers.
```

### 12. LockableHScrollView
```
FILE: app/src/main/java/com/spotifuck/music/LockableHScrollView.kt
├── LOCATION: app/src/main/java/com/spotifuck/music/LockableHScrollView.kt
├── TYPE: Kotlin Class (HorizontalScrollView)
├── PURPOSE: Custom scroll container that can lock scrolling interactions depending on active layout modes.
│
├── KEY FINDINGS:
│   └── line 10-16: scrollingEnabled flag sets interaction lock and forces scroll positions to 0.
│
└── NOTES:
    └── Crucial in csshack mode to lock scrolling and prevent layout overflow.
```

---

## 📂 App Startup Flow

```
1. AppSingleton.onCreate() loads all configuration preferences from SharedPreferences.
2. AppSingleton enqueues a delayed (150ms) task to pre-warm getWebView().
3. If isLoggedIn is true and network is available, WebView loads open.spotify.com in the background.
4. MainActivity.onCreate() inflates the layout and starts WebService if isServiceEnabled is true.
5. setupWebView() attaches the pre-warmed WebView instance to the layout container.
6. WebView spoofs desktop Win32 Chrome User-Agent to retrieve the desktop Web Player layout.
7. JavaScript bridge (AndBridge) connects the web player DOM with native system features.
```

---

## 📂 Email + Password Classic Login Flow

Spotify's default web authentication screen encourages social media sign-in (Google/Facebook/Apple) and obscures standard credentials inputs on mobile layouts. 

```
1. WebView navigates to accounts.spotify.com/login.
2. SpotifyWebViewClient.onPageFinished() detects that the URL matches the login page.
3. Injects "classic_login.js" script into the page DOM.
4. The script locates the first social login option, creates a new anchor element styled as a blue button, and inserts it before the social providers.
5. The button href is assigned as "?allow_password=1".
6. When clicked, the page reloads, and Spotify renders the standard email and password input fields.
7. Upon successful login, the script detects the "open web player" link, triggers AndBridge.loginDetected() to toggle the state in SharedPreferences, and enters the player automatically.
```

---

## 📂 App Settings and Preferences

```
SETTING: ServiceOn
├── VARIABLE: AppSingleton.isServiceEnabled (boolean)
├── DEFAULT: true
├── STORAGE KEY: "ServiceOn"
└── PURPOSE: Auto-starts the background foreground media playback service when the application starts.
```
```
SETTING: LoggedIn
├── VARIABLE: AppSingleton.isLoggedIn (boolean)
├── DEFAULT: false
├── STORAGE KEY: "LoggedIn"
└── PURPOSE: Stores the authentication state to load open.spotify.com directly instead of accounts.spotify.com/login.
```
```
SETTING: APlayMode
├── VARIABLE: AppSingleton.autoPlayMode (string)
├── DEFAULT: "disabled"
├── STORAGE KEY: "APlayMode"
└── PURPOSE: Controls play button actions on load: "disabled", "onetime" (plays once on load), or "permanent" (checks play status at intervals and forces play).
```
```
SETTING: CloseNowPlay
├── VARIABLE: AppSingleton.closeNowPlay (boolean)
├── DEFAULT: false
├── STORAGE KEY: "CloseNowPlay"
└── PURPOSE: Forces the expanded track detail sidebar or dialog panel to automatically collapse.
```
```
SETTING: TakeControl
├── VARIABLE: AppSingleton.takeControl (boolean)
├── DEFAULT: true
├── STORAGE KEY: "TakeControl"
└── PURPOSE: Auto-clicks the "Take Control" confirmation overlay if playback is running on another device.
```
```
SETTING: GuiMode
├── VARIABLE: AppSingleton.guiMode (string)
├── DEFAULT: "csshack"
├── STORAGE KEY: "GuiMode"
└── PURPOSE: Controls layout width and CSS overlays: "csshack" (fits layouts to mobile screens) or "bigwindow" (desktop view that locks screen to landscape, disables horizontal scrolling, and scales viewport to fit display dimensions).
```
```
SETTING: Amoled
├── VARIABLE: AppSingleton.isAmoled (boolean)
├── DEFAULT: false
├── STORAGE KEY: "Amoled"
└── PURPOSE: Overrides webpage backgrounds with pure #000000 colors inside csshack mode.
```
```
SETTING: AndAuto
├── VARIABLE: AppSingleton.isAndAutoEnabled (boolean)
├── DEFAULT: true
├── STORAGE KEY: "AndAuto"
└── PURPOSE: Enables metadata updates, position queries, repeat/shuffle status syncing, and Android Auto library catalog generation.
```
```
SETTING: SwipeStop
├── VARIABLE: AppSingleton.stopOnSwipe (boolean)
├── DEFAULT: true
├── STORAGE KEY: "SwipeStop"
└── PURPOSE: Determines if swiping away the active media playback notification stops the service.
```
```
SETTING: AutoShut
├── VARIABLE: AppSingleton.autoShutMinutes (int)
├── DEFAULT: 0
├── STORAGE KEY: "AutoShut"
└── PURPOSE: Stops the service and closes the application after playback remains paused for a specified duration.
```
```
SETTING: AutoSleep
├── VARIABLE: AppSingleton.autoSleepMinutes (int)
├── DEFAULT: 0
├── STORAGE KEY: "AutoSleep"
└── PURPOSE: Automatically pauses active music playback after a specified duration.
```
```
SETTING: ForcePortrait
├── VARIABLE: AppSingleton.isForcePortrait (boolean)
├── DEFAULT: false
├── STORAGE KEY: "ForcePortrait"
└── PURPOSE: Restricts screen orientations to Portrait (mode 1) instead of Sensor (mode 13).
```
```
SETTING: ForceEn
├── VARIABLE: AppSingleton.isForceEn (boolean)
├── DEFAULT: false
├── STORAGE KEY: "ForceEn"
└── PURPOSE: Configures WebView layout locales to English to guarantee selector matching in layout sheets.
```
```
SETTING: HPAP (Headphone Auto Play)
├── VARIABLE: AppSingleton.headsetPlay (boolean)
├── DEFAULT: false
├── STORAGE KEY: "HPAP"
└── PURPOSE: Resumes music playback immediately when a wired headset connection is detected.
```
```
SETTING: HPAS (Headphone Auto Stop)
├── VARIABLE: AppSingleton.headsetPause (boolean)
├── DEFAULT: false
├── STORAGE KEY: "HPAS"
└── PURPOSE: Pauses music playback immediately when a wired headset is disconnected.
```
```
SETTING: BTAP (Bluetooth Auto Play)
├── VARIABLE: AppSingleton.btPlay (boolean)
├── DEFAULT: false
├── STORAGE KEY: "BTAP"
└── PURPOSE: Resumes music playback when a Bluetooth audio sink profile connects.
```
```
SETTING: BTAS (Bluetooth Auto Stop)
├── VARIABLE: AppSingleton.btPause (boolean)
├── DEFAULT: false
├── STORAGE KEY: "BTAS"
└── PURPOSE: Pauses music playback when a Bluetooth audio profile disconnects.
```
```
SETTING: DisableCanvas
├── VARIABLE: AppSingleton.isCanvasDisabled (boolean)
├── DEFAULT: true
├── STORAGE KEY: "DisableCanvas"
└── PURPOSE: Instructs the injection engine to block video canvas background assets to conserve bandwidth.
```
```
SETTING: ExpandedFullScreen
├── VARIABLE: AppSingleton.isFullScreenEnabled (boolean)
├── DEFAULT: true
├── STORAGE KEY: "ExpandedFullScreen"
└── PURPOSE: Expands the WebView to utilize all available screen space, hiding status bars.
```
```
SETTING: PrioritizeLocalAssets
├── VARIABLE: AppSingleton.prioritizeLocalAssets (boolean)
├── DEFAULT: true
├── STORAGE KEY: "PrioritizeLocalAssets"
└── PURPOSE: Forces loading local assets from Android resources instead of remote config in debug environments.
```

---

## 📂 JavaScript Injection System

The injection system loads custom scripts on different execution scopes to hook DOM nodes, modify CSS selectors, intercept APIs, and communicate with the native app via Android JavaScript interfaces.

### ⚙️ Injection Phases

#### Phase 1: `desktop_spoof.js` (onPageStarted)
Spoofs the browser's screen metrics and window attributes immediately as pages begin to load:
```javascript
window.screen.__defineGetter__('width', function() { return 1920; });
window.screen.__defineGetter__('height', function() { return 1080; });
window.navigator.__defineGetter__('vendor', function() { return 'Google Inc.'; });
window.navigator.__defineGetter__('platform', function() { return 'Win32'; });
```
This forces Spotify to treat the WebView as a desktop client rather than a mobile browser.

#### Phase 2: GDPR & Login Override (onPageFinished)
Loads context-specific files based on URLs:
- `facebook_consent.js`: Auto-clicks Facebook cookie consent prompts.
- `classic_login.js`: Adds the "Email + Password Classic Login" button.
- Login Detection: Detects login success and navigates forward.

#### Phase 3: Core Control Wrapper (`spotify_bridge.js`)
If the user is logged in, the application injects the bridge wrapper script.

---

### 📂 Breakdown of `spotify_bridge.js` Modules

#### Module 1: Fetch Interception
Intercepts `window.fetch` to parse authentication headers, client tokens, and device IDs:
```javascript
const oriFetch = window.fetch;
window.fetch = async function(...args) {
    const [url, opts] = args;
    const headers = opts?.headers || {};
    
    const cliToken = headers['Client-Token'] || headers['client-token'];
    if (cliToken && cliToken !== window.spotCliToken) {
        window.spotCliToken = cliToken;
        typeof checkMediaLib === 'function' && checkMediaLib();
    }
    // Captures Bearer Auth tokens for Pathfinder library queries
    const auth = headers.Authorization || headers.authorization;
    if (auth?.startsWith('Bearer ') && auth !== window.spotAuthToken) {
        window.spotAuthToken = auth;
        typeof checkMediaLib === 'function' && checkMediaLib();
    }
    return resp;
}
```

#### Module 2: Pathfinder Library Fetching
Utilizes captured tokens to run paginated Pathfinder requests to retrieve user playlists, albums, artists, and show subscriptions:
```javascript
window.fetchAllLibrary = async function() {
    // Queries Pathfinder endpoint in pages of 50 items
    // JSON-parses responses to extract metadata properties
}
```

#### Module 3: Media State Synchronization
Checks DOM playback widgets every 2 seconds to scrape position, cover art, play status, and track/artist labels:
```javascript
let currState = track + '|' + artist + '|' + playing + '|' + repmode + '|' + isfav;
if (currState !== lastState) {
    lastState = currState;
    AndBridge.recMediaStatus(JSON.stringify(values));
}
```

#### Module 4: Playback Commands
Translates native events into JavaScript clicks and API posts:
- `playFromUri(uri)`: Invokes spclient Connect APIs to trigger media changes.
- `actPlayPause()`: Clicks the play/pause button.
- `actSkipForward()` / `actSkipBack()`: Clicks control buttons.
- `actSeek(pos)`: Dispatches progress bar range inputs.

#### Module 5: Immersive Fullscreen UI
If the user clicks the track album art, `spotify_bridge.js` generates a fullscreen overlay (`sf-fs-player`) containing:
- **High-Resolution Cover Art**: Replaces default `00004851` low-res thumbnails with `0000b273` high-res image variants.
- **Dynamic HSL Color Extraction**: Extracts primary colors from the album art using an HTML `<canvas>` pixel-sampling routine. Converts RGB pixels to HSL, enforces vibrant/neon pastel constraints (clamping saturation and lightness), and sets CSS variables (`--sf-art-color-from` and `--sf-art-color-to`) on `document.body` to render the immersive gradient backgrounds and glowing art shadows.
- **Gesture Controls**: Employs swipe-left and swipe-right event listeners on both artwork and title areas to skip tracks, and vertical drag actions to collapse the overlay.
- **Seek Scrub Bars**: Custom progress bar controls mapping touch and drag inputs to native seek callbacks.

---

### ⚙️ Script Injection Flow

```
   LOAD START (WebView)
           │
           ├──> onPageStarted: Injects desktop_spoof.js
           │
   PAGE LOAD COMPLETED
           │
           ├──> Facebook consent page? ──> Injects facebook_consent.js
           │
           ├──> Spotify account login? ──> Injects classic_login.js
           │
           └──> Logged in? ──> Injects spotify_bridge.js
                      │
                      ├──> Intercepts fetch calls & extracts authorization tokens
                      ├──> Scrapes track metadata & syncs to AndBridge
                      ├──> Renders custom overlay on album cover click
                      └──> Listens for controls sent from native services
```

---

## 📂 Ad Blocking Mechanism

The ad blocker operates in one of three modes, configurable in the Player Settings:

### 1. Legacy Ad Blocker (Default)
To ensure maximum compatibility and prevent legitimate songs from being skipped on some devices, the app defaults to the **Legacy Ad Blocker**.
- **Connection-Verified Blocking**: When a URL matching the ad pattern is loaded, the app opens a quick synchronous connection using `HttpURLConnection` to check the response's `contentType`.
- **Audio Ad Interception**: If the content type is indeed `audio/mpeg` (and does not contain exclude tags like `podz-content` or `gew4-spclient`), it intercepts the request and replaces it with a local silent audio stream (`silent.mp3`) from the assets folder.
- **Analytics Block**: Known analytics and tracking hosts (like `doubleclick.net`, `sentry.io`, `googlesyndication.com`, `fastly-insights.com`) are instantly blocked with a `200 OK` empty response to avoid network bottlenecks.
- **CDN Token Bypass**: Fast-path bypass for legitimate media track requests containing `__token__` URL parameters, preventing single-use token invalidation and avoiding audio playback freezes (which caused 10-30 second halts).

### 2. Instant Ad Blocker (Optional mode: "instant")
Blocks known ad and tracking networks instantly in-memory:
- **In-Memory Pattern Matching**: All ad detection for doubleclick, googlesyndication, sentry, etc., is handled instantly in-memory without blocking threads or initiating connections.
- **No CDN Interception**: Legacy CDN checks are bypassed, completely preventing music tracks from skipping or stalling.
- **Range Support**: Serves local `silent.mp3` with full HTTP 206 Range support to prevent browser audio decoder hangs.

### 3. Dynamic Blocklist Ad Blocker (Optional mode: "dynamic")
Utilizes a dynamically updated domain blocklist fetched from a user-configured URL:
- **Automatic Caching**: Falls back to a default offline list (`assets/adblock_hosts.txt`) and caches updates in the app's internal files directory.
- **Domain Suffix Matching**: Performs in-memory checks on request hosts against thousands of blacklisted domains (using clean, optimized loop-matching for speed).
- **Inline Settings & Updater**: Fully configured and updated in-place via the nested Settings dialog. Bypasses risky CDN interceptions to ensure song playback is 100% reliable.

---

## 📂 WebService & Media Session Integration

`WebService` is a foreground service implementing `MediaBrowserServiceCompat`. It manages system media controls and relays states to lock screens and notifications.

- **Media Session Configuration**: Initializes `MediaSessionCompat` and updates it with transport states (Seek, Play, Pause, Next, Previous, Repeat, Shuffle).
- **Custom Playback Options**: Syncs repeat and favorite toggles as custom session actions.
- **Audio Focus Management**: Registers `AudioManager.OnAudioFocusChangeListener` to pause playback on focus losses (e.g., incoming telephone calls) and lower volumes on notifications (ducking).
- **Hardware Profile Receivers**:
  - `HEADSET_PLUG`: Pauses or resumes playback based on headphone connections.
  - `ACTION_CONNECTION_STATE_CHANGED` (Bluetooth): Stops playback if Bluetooth receivers are disconnected.
- **Partial Wake Lock**: Acquires `PARTIAL_WAKE_LOCK` to keep the CPU active and prevent background audio freezes when the screen turns off.

---

## 📂 Home Widget Integration

`HomeWidget` is an `AppWidgetProvider` that displays currently playing tracks and lets users trigger controls from their home screens.

```
+--------------------------------------------------------+
|  [Art]   Track Name - Artist                           |
|          [Shuffle] [Prev] [Play/Pause] [Next] [Fav]    |
+--------------------------------------------------------+
```

1. Whenever `WebService` receives a state update from `AndBridge`, it saves the track title, artist name, playing state, repeat, favorite status, and album art bitmap in a local cache.
2. `WebService` calls `HomeWidget.updateWidget()` to rebuild layout components.
3. `HomeWidget` populates the widget `RemoteViews` with cached metadata and downloads cover art.
4. Binds custom actions as PendingIntents targeting `MediaActionReceiver`.
5. Updates all active widgets on the screen.

---

## 📂 Link Handler

`LinkHandler` resolves Spotify links shared from other applications or clicked in browser windows:
```
https://open.spotify.com/track/4PTG3Z6ehGkBF3zI7YkUL3
                       │
                       ▼ (LinkHandler parsing)
             spotify:track:4PTG3Z6ehGkBF3zI7YkUL3
                       │
                       ▼ (JavaScript evaluate)
            window.playFromUri("spotify:track:...")
```

- Checks intent types and extracts text URLs or Spotify URI schemes.
- Converts standard URL structures into native Spotify URIs.
- Evaluates `playFromUri` against the WebView container to start playback instantly.

---

## 📂 Notification System

`WebService` creates and maintains a custom foreground media-style notification:
- Extends standard controls to display Repeat and Favorite buttons in expanded views.
- Reads album art cover bitmaps to color-match notification background accents.
- Utilizes a designated Notification Channel (`SpotiDuckChannel`) targeting Android 8.0+.
- Uses `SwipeStop` settings to determine if the foreground service stops when the notification is swiped away.

---

## 📂 Android Auto Integration

`WebService` implements `MediaBrowserServiceCompat` to display library contents on Android Auto dashboards:
1. When Android Auto connects, it queries `onGetRoot()`.
2. The service returns a parent media root container ID.
3. Upon browsing child lists, `onLoadChildren()` uses JavaScript calls to query library items from the WebView.
4. Translates JSON responses into `MediaBrowserCompat.MediaItem` lists containing tracks, albums, and playlists.
5. Employs `playFromUri` to support voice commands (e.g., "Play Liked Songs on SpotiDuck").

---

## 📂 Custom WebView Client Behaviors

### 📺 Background Video playback
By default, WebViews pause video rendering when hidden. To maintain background playback for video contents, the WebView container overrides `onWindowVisibilityChanged`:
```kotlin
override fun onWindowVisibilityChanged(visibility: Int) {
    if (visibility == View.GONE || visibility == View.INVISIBLE) {
        evaluateJavascript("typeof playing!=='undefined'&&playing;") { value ->
            if ("true" == value) {
                // Forces WebView to retain VISIBLE layout properties
                super.onWindowVisibilityChanged(View.VISIBLE)
            } else {
                super.onWindowVisibilityChanged(visibility)
            }
        }
    } else {
        super.onWindowVisibilityChanged(visibility)
    }
}
```

### 🔒 DRM Verification
`SpotifyWebChromeClient` overrides `onPermissionRequest` to automatically grant `RESOURCE_PROTECTED_MEDIA_ID` permissions, enabling Widevine DRM decryption and secure audio streaming.

---

### 🔙 System Back Button Interception
To prevent the application from exiting when overlays are visible (such as the expanded now playing view, the fullscreen overlay, or library side panels), the system back button is intercepted inside `MainActivity` via `OnBackPressedCallback`:
```kotlin
onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
        if (AppSingleton.isLoggedIn && WebService.isServiceRunning) {
            webView?.evaluateJavascript("(function() { 
                if(window.closeFullscreenPlayer) { 
                    let open = !!document.getElementById('sf-fs-player'); 
                    if(open) { window.closeFullscreenPlayer(); return true; } 
                } 
                if(document.body.classList.contains('sf-expanded')) { 
                    let minBtn = document.querySelector('button[aria-label*=\"Minimize\"]:not(#Desktop_LeftSidebar_Id *):not(.YourLibraryX *):not([aria-label*=\"Library\"]), button[aria-label*=\"Back to player\"]'); 
                    if(minBtn) { minBtn.click(); return true; } 
                } 
                if(document.body.classList.contains('sf-show-nowplaying')) { 
                    if(window.triggerCloseNowPlay) { window.triggerCloseNowPlay(); } 
                    else { document.body.classList.remove('sf-show-nowplaying'); } 
                    return true; 
                } 
                if(document.body.classList.contains('sf-show-library')) { 
                    if(window.triggerCloseLib) { window.triggerCloseLib(); } 
                    else { document.body.classList.remove('sf-show-library'); } 
                    return true; 
                } 
                return false; 
            })();") { value ->
                if (value == "true") {
                    // Back was handled inside the web player
                } else {
                    handleRegularBack()
                }
            }
        } else {
            handleRegularBack()
        }
    }
})
```
If a customized overlay is active, the JavaScript handles the collapse/close trigger and returns `true`, preventing the app from exiting. If no overlays are active, it falls back to standard page back navigation or double-tap to background.

---

## 📂 Structural Monitoring & Hotfix Automation

To ensure layout modifications from Spotify do not break the client and to automate updates, the repository includes structural monitoring and hotfix publishing scripts:

### 1. Spotify Structural Monitor (`scripts/monitor_spotify.py`)
- **Type**: Playwright-based Python automation script.
- **Purpose**: Runs periodically to load Spotify Web Player and verify active play/pause, skip, repeat, shuffle, and progress bar selectors in the DOM.
- **Reporting**: Compares active DOM components, extracts a structural hash map, and publishes health status/errors to Firebase Realtime Database.
- **Automation**: Configured in `.github/workflows/monitor_spotify.yml` to run on a cron schedule, notifying developers immediately when Spotify changes layout classes.

### 2. Hotfix Publishing CLI (`scripts/publish-hotfix.js`)
- **Type**: Node.js script.
- **Purpose**: Automates dynamic file publishes. Compares local assets (`classic_login.js`, `spotify_bridge.js`, `css_hacks.css`) with remote Firebase versions.
- **Action**: Uploads updated asset text contents directly to Firebase Remote Config, triggering immediate in-app hotfix updates for users without requiring APK updates.