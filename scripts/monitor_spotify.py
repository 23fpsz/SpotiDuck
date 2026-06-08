import asyncio
import json
import os
import sys
from datetime import datetime
from playwright.async_api import async_playwright
import firebase_admin
from firebase_admin import credentials, db

# Configuration: Updated for Logged-Out / Public View
# Spotify hides Shuffle, Favorites, and Lyrics unless logged in.
# In "Preview Mode" (public), they also hide the cover art and track info.
SELECTORS = {
    "play_pause": "button[data-testid='control-button-playpause']",
    "skip_back": "button[data-testid='control-button-skip-back']",
    "skip_forward": "button[data-testid='control-button-skip-forward']",
    "repeat": "button[data-testid='control-button-repeat']",
    "progress_bar": "div[data-testid='playback-progressbar'] input[type=range]",
    # These are only visible if logged in
    "shuffle": "button[aria-label*='Shuffle']",
    "favorite": "div[data-testid='now-playing-widget']>div:last-child>button",
    "lyrics": "button[data-testid='lyrics-button']",
    "cover_art": "img[data-testid='cover-art-image'], div[data-testid='cover-art-image'] img, .now-playing-widget img, [data-testid='now-playing-widget'] img"
}

# Selectors that are expected to be missing in public "Preview" mode
OPTIONAL_SELECTORS = ["shuffle", "favorite", "lyrics", "cover_art"]

SPOTIFY_URL = "https://open.spotify.com/"

async def monitor():
    print(f"[{datetime.now()}] Starting Spotify Structural Monitor...")

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        context = await browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            viewport={'width': 1280, 'height': 800}
        )
        page = await context.new_page()

        try:
            print("Navigating to Spotify...")
            await page.goto(SPOTIFY_URL, wait_until="domcontentloaded", timeout=60000)

            print("Waiting for player UI to load...")
            try:
                await page.wait_for_selector('aside[data-testid="now-playing-bar"]', timeout=30000)
                print("Player UI detected.")
            except Exception:
                print("Warning: Timed out waiting for Now Playing Bar.")

            # 1. Check Specific Selectors (Functional Health)
            selector_results = {}
            broken_selectors = []
            for name, selector in SELECTORS.items():
                try:
                    element = await page.query_selector(selector)
                    selector_results[name] = "OK" if element else "MISSING"

                    if not element and name not in OPTIONAL_SELECTORS:
                        broken_selectors.append(name)
                except Exception:
                    selector_results[name] = "ERROR"
                    if name not in OPTIONAL_SELECTORS:
                        broken_selectors.append(name)

            # Detect specific UI states
            is_logged_in = selector_results.get("shuffle") == "OK"

            # Check for "Preview Mode" signup bar
            is_preview_mode = await page.evaluate("() => !!document.querySelector('[data-testid=\"signup-bar\"]')")

            status = "HEALTHY" if not broken_selectors else "BROKEN"

            if is_preview_mode and status == "HEALTHY":
                status = "HEALTHY_PREVIEW"
            elif not is_logged_in and status == "HEALTHY":
                status = "HEALTHY_PUBLIC"

            print(f"Status: {status} (Logged In: {is_logged_in}, Preview: {is_preview_mode})")

            # 2. Extract Structural Map (Exhaustive Deep-Tracking)
            structural_map = await page.evaluate("""() => {
                const getFullDecomposition = (rootSelector) => {
                    const root = document.querySelector(rootSelector);
                    if (!root) return { status: "MISSING" };

                    const elements = root.querySelectorAll('*');
                    const decomposition = {
                        tag: root.tagName.toLowerCase(),
                        ids: root.id ? [root.id] : [],
                        classes: Array.from(root.classList),
                        data_testids: {},
                        child_structure: []
                    };

                    elements.forEach(el => {
                        const testid = el.getAttribute('data-testid');
                        if (testid) {
                            decomposition.data_testids[testid] = {
                                tag: el.tagName.toLowerCase(),
                                classes: Array.from(el.classList),
                                text_sample: el.innerText ? el.innerText.substring(0, 20) : ""
                            };
                        }
                    });

                    return decomposition;
                };

                return {
                    now_playing_bar: getFullDecomposition('aside[data-testid="now-playing-bar"]'),
                    main_view: getFullDecomposition('main[data-testid="main-view"]'),
                    left_sidebar: getFullDecomposition('#Desktop_LeftSidebar_Id'),
                    global_nav: getFullDecomposition('#global-nav-bar'),
                    player_controls: getFullDecomposition('div[data-testid="player-controls"]')
                };
            }""")

            payload = {
                "last_check": datetime.utcnow().isoformat(),
                "status": status,
                "is_logged_in": is_logged_in,
                "is_preview_mode": is_preview_mode,
                "functional_checks": selector_results,
                "structural_map": structural_map,
                "broken_selectors": broken_selectors
            }

            await update_firebase(payload)

        except Exception as e:
            print(f"Critical Monitor Error: {e}")
            await update_firebase({
                "last_check": datetime.utcnow().isoformat(),
                "status": "CRITICAL_ERROR",
                "error": str(e)
            })
        finally:
            await browser.close()

async def update_firebase(data):
    fb_creds_json = os.environ.get("FIREBASE_SERVICE_ACCOUNT")
    if not fb_creds_json:
        print("Firebase credentials not found.")
        return

    try:
        creds_dict = json.loads(fb_creds_json)
        cred = credentials.Certificate(creds_dict)

        if not firebase_admin._apps:
            db_url = os.environ.get("FIREBASE_DATABASE_URL")
            if not db_url:
                db_url = f'https://{creds_dict["project_id"]}-default-rtdb.firebaseio.com/'

            print(f"Connecting to Firebase: {db_url}")
            firebase_admin.initialize_app(cred, { 'databaseURL': db_url })

        db.reference('/monitor/current').set(data)
        db.reference('/monitor/history').push(data)
        print("Firebase updated successfully.")
    except Exception as e:
        print(f"Failed to update Firebase: {e}")

if __name__ == "__main__":
    asyncio.run(monitor())
