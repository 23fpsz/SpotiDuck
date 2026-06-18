# SpotiDuck Changelog

## v1.1.5
- **Performance & WebView Optimizations**:
    - **WebView Background Pre-warming**: Implemented asynchronous preloading of the WebView instance in the `Application` startup sequence (`AppSingleton.onCreate()`), starting DNS resolution and page loading 1-2 seconds earlier.
    - **Optimized Ad-Blocker Interceptions**: Moved known ad networks and trackers (`amillionads.com`, `2mdn.net`, `adxcel.com`, `adstudio-assets.scdn.co`, `scdn.co/mp3-ad/`) directly to the instant in-memory block list, completely eliminating thread-blocking synchronous HTTP network requests in `shouldInterceptRequest`. Serves a custom partial-content responder (`HTTP 206`) for the local `silent.mp3` file to respect browser Range headers and prevent audio decoder freezes.
    - **Offline Splash State Safeguard**: Updated `onReceivedError` and `onReceivedHttpError` to write failures directly to the global `AppSingleton` error states, ensuring the app displays the retry screen immediately rather than freezing on the splash screen during offline startup.
- **Playback & Ad-Blocking Fixes**:
    - **Ad-Blocking Mode Selector**: Introduced a dropdown selector in Settings to switch between the default "Legacy (Connection-Verified)" ad blocker and the "Instant (In-Memory)" ad blocker. This resolves song skipping issues on various devices by defaulting to the connection-verified legacy blocker while keeping the instant-blocking method as an optional configuration.
    - **CDN Token Bypass**: Implemented a fast-path bypass for track stream URLs containing `__token__` in the ad-blocking network interception layer to prevent single-use token invalidation and resolve 10-30 second audio playback stops.
    - **Simplified Background Keep-Awake**: Simplified background keep-awake checks to rely solely on the active `playing` state, resolving background audio freezing when Canvas rendering is disabled.
    - **Clean Telemetry Logs**: Removed verbose matched URL logs from the network interception client to optimize Logcat output.
- **UI & Layout Optimizations**:
    - **Portrait-Only Stacking Overrides**: Wrapped custom mini-player vertical stacking overrides inside a portrait-only media query in `css_hacks.css`. This allows landscape mode to fall back naturally to Spotify's default web desktop horizontal layout, preventing the player from taking up half the screen.
    - **Search Suggestions & Auto-Dismiss Fix**: Completely hide search history/suggestions dropdown overlays to prevent UI freezing. Registered touch and mouse click listeners to auto-blur the search input when tapping outside or playing a song to prevent screen transition layout bugs.
- **Expanded Player & Gesture Controls**:
    - **SpotiCap-Style Fullscreen Player & Dynamic Art Shadow**: Introduced an immersive, custom full-screen player overlay (`#sf-fs-player`) with deep background gradients, progress bar control, styled controls, and a dynamic album art shadow glow matching the extracted artwork colors.
    - **Gesture Support**: Enabled swipe-left and swipe-right gestures on the now playing bar track info and fullscreen album art to skip tracks.
    - **Tap-to-Play & DOM Tagger**: Added automated DOM tagging (`sf-player-bar`, `sf-player-widget`, `sf-track-info`) and tap-to-play overrides on track rows to trigger playback instantly.
    - **Instant Transition Colors**: Optimized color extraction to run instantly when the player expands, removing the 250ms delay and preventing visual layout flashes.
    - **Library Expansion Fix**: Restrained player state detection to ignore minimize/collapse events from the library panel, resolving system status bar visibility bugs.
    - **System Back Button Support**: Hooked the Android system back button callback in `MainActivity` to execute dynamic DOM queries, allowing it to collapse the expanded player (`sf-expanded`), close the custom fullscreen overlay (`#sf-fs-player`), and dismiss side panels instead of exiting the application.
- **Media Compatibility Restored**:
    - **MediaSessionCompat Rollback**: Reverted from Media3 back to legacy `MediaSessionCompat` to fix the 3-button limitation on Android 13+ and ensure a full 5-button lock screen and notification media layout.
- **Settings & UI Enhancements**:
    - **Settings Menu Icons**: Added Bluetooth, headphones, developer, and fullscreen vector icons to category menus.
    - **Robust Progress Bar Calculations**: Added safe boundary and type checks to the fullscreen progress bar calculations in `spotify_bridge.js` to prevent NaN or division-by-zero layout errors.
    - **Prioritize Local Assets Toggle**: Introduced a developer option in Settings to force loading local APK assets instead of hotfixes during development.
- **System & Hotfix Automation**:
    - **Firebase Remote Config Hotfixes**: Implemented automatic loading and injection of CSS and JS hotfixes fetched directly from Firebase Remote Config.
    - **Hotfix Publishing CLI**: Created a `publish-hotfix.js` Node script to automate diff checking and publishing local asset updates to Firebase.
    - **CI/CD Pipeline**: Configured GitHub Actions to automatically run release checks and builds.
    - **Spotify Structural Monitor**: Added a Playwright-based Python monitoring script (`scripts/monitor_spotify.py`) running on a cron schedule (`.github/workflows/monitor_spotify.yml`) to query Spotify's public Web Player DOM structure, verify active player CSS/DOM selectors, and report breaks automatically to Firebase.
- **Technical Documentation & Repository Cleanup**:
    - **Granular Technical Documentation**: Completely rewrote and expanded the codebase developer guide `DOCS.md` to map the fully refactored Kotlin architecture, setting preferences, ad-blocking stream interventions, system services (MediaSession, Auto catalog), and WebView visibility behaviors.
    - **Standardized Project README**: Created a fresh, clean user-facing `README.md` documenting installation details, features, configuration keys, and project architecture.
    - **Clean Repository Footprint**: Removed stale, redundant markdown files (`AGENTS.md`, `OPTIMIZATIONS.md`, `Research.md`) and gitignored agent workspace records to clean up the repository.


## v1.1.4
- **Media3 Migration**:
    - **Modern Core**: Fully migrated from legacy `MediaBrowserServiceCompat` to `MediaLibraryService`, improving background playback stability and future-proofing the app.
    - **Reliable Metadata**: Fixed a persistent issue where song information and album art would stop refreshing when skipping tracks. Implemented dynamic `MediaId` generation and thread-safe state invalidation.
    - **Playback Control**: Introduced `WebViewPlayer` (extending `SimpleBasePlayer`) for more robust communication between the native Android system and the Spotify WebView.
- **Enhanced Notification & UI**:
    - **Restored Controls**: Fixed the "3-button" limitation on Android 13+. Restored a full 5-button layout (**Favorite**, **Previous**, **Play/Pause**, **Next**, **Repeat**) in the expanded notification and lock screen.
    - **Clean UI**: Automatically hides the Settings button when the search bar is active to prevent UI overlap.
- **Widget & Stability**:
    - **Crash Fix**: Resolved a critical crash occurring when interacting with the Repeat toggle on the home screen widget.
    - **Icon Synchronization**: Restored the logic to dynamically update Repeat and Shuffle icons in the notification and home screen widget.
- **System & Analytics**:
    - **Firebase Integration**: Successfully integrated Firebase Analytics, Crashlytics, and Performance Monitoring for better app health tracking.
    - **Utility Fixes**: Improved Autofill support and resolved several splash screen rendering issues for a smoother startup experience.

## v1.1.3
- **Tablet & High-DPI Optimization**:
    - **Adaptive Layout**: Fixed a critical issue where navigation icons (Home, Search, Library) would appear "stacked" or misaligned on high-DPI devices (702+).
    - **Smart Viewport**: Implemented `useWideViewPort = false` and transitioned to an iPad (Tablet) User Agent to stabilize the mobile-style navigation bar on large screens without breaking authentication.
    - **Rotation Stability**: Overrode `onConfigurationChanged` and migrated to `MATCH_PARENT` layout logic to eliminate "half-empty" screens and rendering freezes when rotating between portrait and landscape modes.
    - **CSS Refinement**: Target-locked the navigation flex-containers to prevent horizontal icon drift in landscape while excluding centered elements like the search bar.
    - **Search Bar Alignment**: Fixed an overflow issue where the search bar would extend beyond the top navigation bar on smaller tablets.
    - **Clean UI**: Added case-insensitive CSS rules to hide "Explore Premium" and "Upgrade" buttons in the top navigation bar.
    - **Stability**: Fixed a rendering flicker when interacting with the webpage via a mouse or cursor by enabling explicit WebView hardware acceleration layering and hiding redundant tooltips while preserving essential dropdown menus.
    - **TV Optimization**: Automatically hides OneTrust cookie banners and privacy modals to prevent focus traps and "frozen" screens on Android TV devices.
- **Immersive System UI**:
    - **Transparent System Bars**: Updated the main activity to use fully transparent status and navigation bars, providing a seamless edge-to-edge visual experience.
    - **Dynamic Status Bar**: Implemented automated logic to hide the status bar when the player is expanded, maximizing screen real estate for "Cinema Mode."
- **Adaptive UI**:
    - **Dynamic Navigation Padding**: Migrated the player bar and connect bar to use `env(safe-area-inset-bottom)`, allowing the UI to automatically adjust its height for both Gesture Navigation and 3-Button Navigation modes.
    - **Viewport Optimization**: Added automated injection of `viewport-fit=cover` to the Spotify web player via the JavaScript bridge, enabling correct safe-area inset reporting in the WebView.

## v1.1.2
- **Album Art Synchronization**:
    - Fixed a critical race condition where fast track skipping caused the notification and lock screen album art to mismatch the currently playing song.
    - Implemented synchronous image loading on the Javascript bridge thread to ensure metadata updates occur in the correct sequence.
    - Added an explicit reset to clear the album art bitmap at the start of every track update, preventing "ghosting" of previous artwork.
- **Enhanced Integration**:
    - **Link Sharing**: Added the ability to share Spotify links directly to SpotiDuck from other apps.
    - **Website Preloading**: Implemented preloading for the web player to significantly reduce initial startup time.
    - **Two-Way Canvas Bridge**: Developed a bidirectional bridge for Canvas control, allowing the app to both read and set the Spotify Canvas state.
- **UI & UX Improvements**:
    - **Android Automotive**: Added support for Android Automotive systems.
    - **Animation Overhaul**: Improved UI animations for smoother transitions between player states.
    - **Playlist Navigation**: Fixed horizontal scrolling issues inside playlists.
    - **Visual Polish**: Updated icons, refined z-index layering, and fixed library section layout bugs.
- **System & Automation**:
    - **CI/CD Pipeline**: Fully automated the release process with GitHub Actions, including signed APK generation and automated GitHub Releases.
    - **App Branding**: Transitioned internal naming and resources to "SpotiDuck".

## v1.1.1
- **Now Playing & Canvas Improvements**:
    - **Dynamic Gradient Sync**: Fixed an issue where the player bar gradient would only update for the first song; it now correctly synchronizes with the album art colors on every track change.
    - **Deep Glass Effect**: Implemented a high-end "frosted glass" effect for the expanded player controls with 40px blur and 170% saturation, matching the premium look of the native full-screen view.
    - **Weighted Gradient**: Refined the control bar gradient to flow into a deep, weighted black at the bottom for better visual grounding and depth.
    - **Robust Color Extraction**: Improved the Javascript bridge to specifically target visible UI elements, preventing "black player" glitches during song transitions.
    - **Canvas Reliability**: Fixed several issues where Canvas video backgrounds wouldn't correctly hide or show when toggling settings.
- **Performance & Battery Optimizations**:
    - **Widget Efficiency**: Migrated the home screen widget timer to `RemoteViews.setChronometer()`, offloading 1-second ticking to the System UI.
    - **Reduced CPU Usage**: Decreased widget background update frequency from 500ms to 2000ms, significantly extending battery life during playback.
    - **Asset Caching**: Implemented an in-memory cache for Javascript and CSS assets, eliminating redundant disk I/O during page loads.

## v1.1.0
- **Unified Responsive Widget**:
    - **Dynamic Resizing**: Merged Slim and Expanded widgets into a single responsive player.
    - **Adaptive Layout**: Automatically transforms from a compact 4x1 bar to a full-featured 4x2 player when resized vertically.
    - **Real-time Sync**: Implemented a high-frequency ticker for the expanded view to ensure smooth progress bar movement and timestamp accuracy.
    - **UI Polish**:
        - Added 15dp rounded corner mask for album art in expanded view.
        - Reorganized layout for better grid alignment (4x2 and 5x2).
        - Improved text truncation logic to prevent long song titles from overlapping controls.
- **Service Reliability**:
    - Fixed a issue where the background service would stay disabled after an auto-shutdown; the service now automatically re-activates whenever the main app is opened.
- **Localization**:
    - Significantly expanded global support by migrating comprehensive translations for French (FR), German (DE), Portuguese (PT), Russian (RU), Japanese (JA), and Simplified Chinese (ZH-CN) alongside Spanish (ES).
- **Cleanup**:
    - Removed redundant Expanded Widget provider and metadata to streamline the app.
    - Optimized background service widget update logic.

## v1.0.9
- **UI & Performance**:
    - **Jitter Fix**: Disabled horizontal scrolling in the main player view for non-tablet modes to prevent UI jittering during navigation.
    - **Full-Screen Refinement**: Enhanced the immersive full-screen CSS to ensure smoother transitions and better element layering.
    - **Stability**: Improved WebView client logic for more reliable ad-blocking and canvas management.

## v1.0.8
- **UI & Iconography**:
    - **Canvas Icon Redesign**: Redesigned the "Disable Canvas" icon to perfectly match the size, stroke, and style of the "Take Player Control" icon for a unified settings UI.
    - New Settings button design: White circular background with zoomed black waves logo.
    - Standardized Settings button size to 40dp with a 5dp top margin for better positioning.
- **Full-Screen Logic Improvements**:
    - **Intelligent Expansion**: Immersive full-screen styles are now strictly applied only when "Immersive Full-screen" is enabled in settings.
    - **Now Playing Integration**: Full-screen logic is now correctly synchronized with the expansion and minimization of the Spotify "Now Playing" menu.
- **Cleanup & Optimization**:
    - **Removed Auto-Hide**: Removed the experimental "Auto-hide player" feature to simplify the UI and improve compatibility with native Spotify player behaviors.
    - **Streamlined Settings**: Removed outdated warnings about manual service restarts, as most changes are now handled automatically via background updates or reloads.
    - **Automation**: The version in the settings menu now automatically updates from the `build.gradle` file. Removed hardcoded version strings.
    - Removed experimental "Car Mode" and "Keep Screen On" features to improve stability and focus on core player functionality.

## v1.0.7
- **Settings Update**:
    - Added "Disable Canvas" toggle in settings to control video background playback.
    - Improved internal WebView scaling and user agent handling.
    - New "Canvas" icon: Transparent portrait rectangle with a play symbol, matching modern UI standards.

## v1.0.6
- **Home Widget**: 
    - Fixed widget functionality: buttons now correctly control playback.
    - Added `previewImage` to the widget info so it appears as a player in the widget menu.
    - Standardized widget icon colors (Black) for better contrast on the red background.
    - Fixed background color to match the deep red of the source app.

## v1.0.5
- **Canvas Fixes**: 
    - Fixed an issue where the Canvas video would not show even when enabled.
    - Fixed the "thin line" bug by forcing the Canvas to display full-screen behind the controls.
    - Added logic to hide static cover art when a Canvas video is playing.
- **UI Clean-up**: 
    - Improved removal of "Open in Desktop app" and "Download Spotify" prompts.
    - Refined prompt removal logic to ensure the **3-dot context menu** remains visible and functional.
- **Settings**: Updated version display in the settings menu.

## v1.0.4
- **Background Service**: Support for playing music in a background service.
- **AutoPlay Modes**: Options for "One time at start" and "Permanent" playback.
- **Appearance**: 
    - Added Amoled theme support.
    - Improved Mobile CSS/JS hack for a cleaner interface.
- **Android Auto**: Integrated controls and basic Android Auto support.
- **Triggered Actions**: Added options to Play/Stop when Headphones or Bluetooth are connected/disconnected.
- **System Utilities**: Added "Clear Cache" and "Clear Data" (Log out) options.
- **Navigation**: Added "Swipe to stop service" and "Force Portrait" orientation options.

## v1.0.0 - v1.0.3
- Initial releases and basic web player wrapper implementation.
- Basic CSS injection for mobile optimization.
- Javascript bridge for media session control.
