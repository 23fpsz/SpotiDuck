# Spotifuck Changelog

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
