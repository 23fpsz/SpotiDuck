# Project Spotifuck: Logic & Optimization Audit

This document tracks identified issues in the codebase, their impact, and proposed solutions for future implementation.

---

## ✅ Fixed / Addressed

### 1. Widget Battery Drain (The 500ms Loop)
*   **Status:** Fixed
*   **Implementation:** 
    *   Switched to `RemoteViews.setChronometer()` for smooth time display ticking (handled by System UI).
    *   Increased the manual update interval from 500ms to 2000ms (only for progress bar movement).
    *   Added logic to toggle between static text (paused) and Chronometer (playing) to maximize efficiency.

### 2. Synchronous Ad-Blocking Probe (Optimized)
*   **Status:** Refined & Optimized
*   **Implementation:** 
    *   Implemented an instant blocklist for known tracking and non-audio ad domains.
    *   Optimized the audio ad probe by using `HEAD` requests instead of full `GET` requests, significantly reducing latency.
    *   Added strict timeouts (1.5s) to prevent the player from hanging on slow connections.

### 3. Redundant Asset Disk I/O
*   **Status:** Fixed
*   **Implementation:** 
    *   Added a `ConcurrentHashMap` cache in `AppSingleton`.
    *   `getAssetFile` now returns from memory after the first read, drastically reducing disk hits.

---

## 🛠️ High Priority (Performance & Reliability)

### 4. Brittle DOM Selectors
*   **Problem:** The Javascript bridge relies on `data-testid` and `aria-label` which Spotify changes frequently.
*   **Impact:** High probability of the bridge "breaking" silently after a Spotify update.
*   **Proposed Solution:** 
    *   *Pending Re-implementation*: Use a safer "Internal State Interception" model or monitor the `<video>` element with robust fallbacks. (Previous attempt was too aggressive and caused issues).

### 5. Android Auto / Media Browser Latency
*   **Problem:** `WebService.onLoadChildren` relies on `evaluateJavascript` to fetch playlist data from the WebView on-demand.
*   **Impact:** High risk of timeouts (showing "No items") when the app is in the background or the JS engine is throttled.
*   **Proposed Solution:** 
    *   Implement a "Push Model": The Javascript Bridge should send library updates to Kotlin whenever they change.
    *   Store this data in a local memory cache or Room database.
    *   `onLoadChildren` should serve data from the local cache instantly.

---

## 🛡️ Medium Priority (Stability & Maintainability)

### 6. Threading Inefficiency in Bridge
*   **Problem:** `AndBridge.recMediaStatus` spawns a new `Thread` for every track update to fetch album art.
*   **Impact:** Excessive thread creation/destruction overhead.
*   **Proposed Solution:** 
    *   Use a dedicated `CoroutineScope` with `Dispatchers.IO`.
    *   Utilize Picasso's asynchronous `.into(Target)` to handle threading and caching automatically.

### 7. Unsecured Broadcast Receivers
*   **Problem:** `MediaActionReceiver` in `WebService` listens for playback commands (Next, Play/Pause) without permission checks or being non-exported.
*   **Impact:** Other malicious apps on the device can send these Intents to hijack your music player.
*   **Proposed Solution:** 
    *   Set `android:exported="false"` in `AndroidManifest.xml` for the receiver.
    *   If export is required for Widgets, use a `signature` level permission.

---

## 🧹 Low Priority (Code Quality)

### 8. WebView Visibility Lifecycle Hack
*   **Problem:** `onWindowVisibilityChanged` is overridden to force `VISIBLE` state via JS evaluation.
*   **Impact:** Potential ANRs (App Not Responding) and fragile playback continuity.
*   **Proposed Solution:** 
    *   Investigate the `WebView.onPause()` and `onResume()` lifecycle properly.
    *   Use the `MediaSession` and `PartialWakeLock` more effectively to keep the process alive without tricking the View hierarchy.

### 9. Global Static State & Memory Leaks
*   **Problem:** Extensive use of `companion object` to hold `WebView`, `Activity`, and `Service` instances.
*   **Impact:** Susceptibility to memory leaks and inconsistent behavior after Android process reclamation.
*   **Proposed Solution:** 
    *   Migrate to a cleaner Dependency Injection pattern or an Application-level state holder.
    *   Use `WeakReference` consistently for Activity/View contexts.
