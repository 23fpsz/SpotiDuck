(function() {
    // Global flags injected by Android:
    // window.SF_CONFIG = { ... };

    let reqPause = false;
    let firstPlay = true;
    let ulFlag = false;
    let ffDone = false;
    let featVer = `web-player_${new Date().toISOString().split('T')[0]}_${Date.now()}_${Math.floor(Math.random()*0xFFFFFFF).toString(16).padStart(7,'0')}`;
    let lastState = null;
    let lastPos = null;
    window.playing = false;
    let pfint = null;
    let afint = null;
    let cssint = null;
    let aaint = null;

    // Robust Selector: Avoids temp minified classes
    const canvasSelector = '#VideoPlayerNpv_ReactPortal video, .canvasVideoContainerNPV video, [data-testid="track-visual-enhancement"] ~ div video, [data-testid="canvas-video"] video, .VideoPlayer__container video';

    // Live Settings Update Function
    window.SF_UPDATE = function(config) {
        if (!config) return;
        window.SF_CONFIG = Object.assign(window.SF_CONFIG || {}, config);

        // Handle Canvas Toggle
        if (typeof config.isCanvasDisabled !== 'undefined') {
            if (config.isCanvasDisabled) {
                document.body.classList.add('sf-hide-canvas');
                document.body.classList.remove('sf-video-bg');
            } else {
                document.body.classList.remove('sf-hide-canvas');
                document.body.classList.add('sf-video-bg');
            }

            // Sync with Spotify internal settings
            // 1. Immediate attempt if button exists
            let cv = document.getElementById('settings.canvasVideos');
            if (cv) {
                let shouldBeChecked = !config.isCanvasDisabled;
                if (cv.checked !== shouldBeChecked) cv.click();
            }

            // 2. Direct API Sync (This works even when the Settings page is NOT open)
            // We spoof the persistent storage that Spotify uses for these toggles
            try {
                localStorage.setItem('canvas-videos-enabled', (!config.isCanvasDisabled).toString());
                localStorage.setItem('can-play-canvas', (!config.isCanvasDisabled).toString());
            } catch(e) {}
        }

        // Handle Fullscreen Toggle
        if (typeof config.isFullScreenEnabled !== 'undefined') {
            if (config.isFullScreenEnabled) {
                document.body.classList.add('sf-fullscreen-enabled');
                document.body.classList.remove('sf-fullscreen-disabled');
            } else {
                document.body.classList.remove('sf-fullscreen-enabled');
                document.body.classList.add('sf-fullscreen-disabled');
            }
        }

        // Handle Amoled Toggle
        if (typeof config.isAmoled !== 'undefined') {
            const amStyleId = 'sf-amoled-override';
            let style = document.getElementById(amStyleId);
            if (config.isAmoled) {
                if (!style) {
                    style = document.createElement('style');
                    style.id = amStyleId;
                    style.textContent = '.encore-dark-theme{--background-base:#000!important;--background-highlight:#000!important;--background-elevated-base:#000!important;--background-elevated-highlight:#000!important;--background-elevated-press:#000!important;--background-tinted-base:#000!important} aside[data-testid=now-playing-bar]{background:#000!important;box-shadow:none;border-top:1px solid #666}';
                    document.head.appendChild(style);
                }
            } else if (style) {
                style.remove();
            }
        }
    };

    window.updMedia = function(force = false) {
        const currState = (window.track || "") + '|' + (window.artist || "") + '|' + window.playing + '|' + (window.repmode || "") + '|' + (window.shmode || "") + '|' + window.isfav;
        if (force || currState !== lastState) {
            lastState = currState;
            const values = {
                artist: window.artist || "No Artist",
                track: window.track || "No Track",
                playing: window.playing,
                repeat: window.repmode || "false",
                shuffle: window.shmode || "false",
                fav: window.isfav,
                duration: window.duration || 0,
                position: window.position || 0,
                cover: window.cover || ""
            };
            AndBridge.recMediaStatus(JSON.stringify(values));
            lastPos = window.position;
        } else if (window.playing) {
            // Update position anchor every 1000ms to keep interpolation accurate
            if (lastPos === null || Math.abs(window.position - lastPos) >= 1000) {
                AndBridge.recMediaPosition(window.position);
                lastPos = window.position;
            }
        }
    };

    const oriFetch = window.fetch;
    window.fetch = async function(...args) {
        const [url, opts] = args;
        const method = opts?.method?.toUpperCase?.() || 'GET';

        // INTERCEPT Canvas Requests if disabled
        if (window.SF_CONFIG.isCanvasDisabled && (url.includes('canvas-storage') || url.includes('/v1/canvas'))) {
             return new Response(JSON.stringify({canvases:[]}), {status: 200});
        }

        const headers = opts?.headers || {};
        if (method === 'POST' && url.includes('/track-playback/v1/devices') && opts?.body) {
            const body = JSON.parse(opts.body);
            const deviceId = body?.device?.device_id;
            if (deviceId && deviceId !== window.spotDevId) {
                window.spotDevId = deviceId;
                typeof checkMediaLib === 'function' && checkMediaLib();
            }
        }
        const cliToken = headers['Client-Token'] || headers['client-token'];
        if (cliToken && cliToken !== window.spotCliToken) {
            window.spotCliToken = cliToken;
            typeof checkMediaLib === 'function' && checkMediaLib();
        }
        const authHead = headers.Authorization || headers.authorization;
        if (authHead?.startsWith('Bearer ') && authHead !== window.spotAuthToken) {
            window.spotAuthToken = authHead;
            typeof checkMediaLib === 'function' && checkMediaLib();
        }
        if (ffDone && url.includes('/track-playback/') && method === 'PUT') {
            const bodyStr = opts?.body ? (typeof opts.body === 'string' ? opts.body : '') : '';
            if (bodyStr.includes('"paused":true')) manageAll(false);
            else if (bodyStr.includes('"paused":false')) manageAll(true);
        }
        try {
            const resp = await oriFetch(url, opts);
            if (resp.status === 404 && url.includes('connect-state') && url.includes('/command/from/')) {
                AndBridge.deferMessage('reload');
                location.reload();
            }
            return resp;
        } catch (err) {
            throw err;
        }
    };

    window.playFromUri = function(uri) {
        let type = uri.match(/^spotify:([^:]+)/)?.[1];
        if (type == 'user') type = 'your_library';
        oriFetch(`https://gew4-spclient.spotify.com/connect-state/v1/player/command/from/${window.spotDevId}/to/${window.spotDevId}`, {
            method: 'POST',
            headers: { 'Authorization': window.spotAuthToken, 'Client-Token': window.spotCliToken, 'Content-Type': 'application/json' },
            body: JSON.stringify({
                command: {
                    context: { uri: uri, url: 'context://' + uri, metadata: {} },
                    play_origin: { feature_identifier: type, feature_version: featVer, referrer_identifier: 'your_library' },
                    options: { license: 'tft', skip_to: {}, player_options_override: {} },
                    endpoint: 'play'
                }
            })
        });
    };

    window.firstFuck = function() {
        if (pfint) clearInterval(pfint);
        pfint = setInterval(() => {
            // Only wake if playing, hidden, and Canvas is ALLOWED and PRESENT
            let hasCanvas = !window.SF_CONFIG.isCanvasDisabled && !!document.querySelector(canvasSelector);
            if (window.playing && document.visibilityState == 'hidden' && hasCanvas) AndBridge.wakeUp();
            else if (!AndBridge.isWoke() && document.visibilityState == 'visible' && !hasCanvas) AndBridge.wakeOff();

            if (window.SF_CONFIG.guiMode === "csshack") {
                if (typeof window.npBtn == 'undefined') {
                    let lyBtn = document.querySelector('button[data-testid=lyrics-button]:not(.fuckd)');
                    if (lyBtn) {
                        lyBtn.classList.add('fuckd');
                        window.npBtn = document.createElement('button');
                        window.npBtn.className = 'npbtn';
                        window.npBtn.onclick = clickNP;
                        window.npBtn.innerHTML = `<svg viewBox="0 0 16 17"><rect x="1" y="0.75" width="14" height="15.5" rx="2" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M 6 5 L 6 5.9160156 L 9.6933594 8.5 L 6 11.080078 L 6 12 L 11 8.5 L 6 5 z" stroke="currentColor" stroke-width="1.2"/></svg>`;
                        lyBtn.parentNode.insertBefore(window.npBtn, lyBtn);
                        closeNowPlay();
                    }
                }
            }

            let pb = document.querySelector('aside button[data-testid=control-button-playpause]:not(.fuckd)');
            if (pb) {
                AndBridge.playLoaded();
                pb.classList.add('fuckd');
                window.pBtn = pb;
                window.pBtn.addEventListener('click', () => {
                    if (window.pBtn.getAttribute('aria-label') !== 'Play') {
                        reqPause = true;
                        ulFlag = false;
                        manageWake(false);
                    } else if (!ulFlag) {
                        reqPause = false;
                        manageWake(true);
                        ulFlag = true;
                        setTimeout(() => {
                            if (ulFlag && window.pBtn.getAttribute('aria-label') === 'Play') {
                                AndBridge.deferMessage('unlock');
                                actSkipForward();
                                trigUnlock();
                            } else if (ulFlag) {
                                ulFlag = false;
                            }
                        }, 10000);
                    }
                });
                if (!ffDone) {
                    ffDone = true;
                    AndBridge.manageTShut(true);
                    AndBridge.manageTSleep(false);
                    addGlobalCleanup();
                    addAutoFeatures();
                    addCSSJSHack();
                    addAndAuto();
                    setTimeout(() => { manageAll(window.playing) }, 5000);
                }
            }
        }, 5000);
    };

    window.manageWake = function(enable) {
        if (enable) {
            if (document.visibilityState == 'hidden') AndBridge.wakeUp();
        } else {
            let hasCanvas = !window.SF_CONFIG.isCanvasDisabled && !!document.querySelector(canvasSelector);
            if (!AndBridge.isWoke() && document.visibilityState == 'visible' && !hasCanvas) AndBridge.wakeOff();
        }
    };

    window.manageAll = function(play) {
        window.playing = play;
        AndBridge.manageTShut(!play);
        AndBridge.manageTSleep(play);
        if (play) {
            firstFuck();
            addGlobalCleanup();
            addAutoFeatures();
            addCSSJSHack();
            addAndAuto();
        }
        updMedia();
    };

    window.clickNP = function() {
        let rBtn = document.querySelector('#Desktop_PanelContainer_Id')?.parentNode?.parentNode?.nextElementSibling?.querySelector('button');
        if (rBtn) {
            let npHid = document.querySelector('#Desktop_PanelContainer_Id').parentNode.parentNode.ariaHidden;
            if (typeof window.npBtn !== 'undefined') {
                if (npHid && npHid == 'true') window.npBtn.classList.add('active');
                else window.npBtn.classList.remove('active');
            }
            rBtn.click();
        }
    };

    window.closeNowPlay = function() {
        let rc = document.querySelector('#Desktop_PanelContainer_Id');
        if (rc && rc.parentNode.parentNode.ariaHidden == 'false') {
            clickNP();
        }
    };

    window.trigUnlock = function() {
        let uint = setInterval(() => {
            if (window.pBtn.disabled) {
                AndBridge.deferMessage('reload');
                window.location.reload();
            } else if (window.pBtn.getAttribute('aria-label') !== 'Play') {
                clearInterval(uint);
                ulFlag = false;
            }
        }, 3000);
    };

    window.actPlayPause = function(play) {
        if ('pBtn' in window) {
            let isPaused = window.pBtn.getAttribute('aria-label') === 'Play';
            if (play && isPaused) window.pBtn.click();
            else if (!play && !isPaused) window.pBtn.click();
        }
    };

    window.actSkipBack = function() {
        let bb = document.querySelector('button[data-testid=control-button-skip-back]');
        if (bb) { manageWake(true); bb.click(); }
    };

    window.actSkipForward = function() {
        let fb = document.querySelector('button[data-testid=control-button-skip-forward]');
        if (fb) { manageWake(true); fb.click(); }
    };

    window.actRepeat = function() {
        let rb = document.querySelector('button[data-testid=control-button-repeat]');
        if (rb) {
            rb.click();
            setTimeout(() => addAndAuto(true), 100);
            setTimeout(() => addAndAuto(true), 500);
        }
    };

    window.actShuffle = function() {
        let sb = document.querySelector('button[data-encore-id="buttonTertiary"][aria-label*="Shuffle"]');
        if (!sb) sb = document.querySelector('button[aria-label*="Shuffle"]');
        if (sb) {
            sb.click();
            setTimeout(() => addAndAuto(true), 100);
            setTimeout(() => addAndAuto(true), 500);
        }
    };

    window.actAddToFav = function() {
        let fb = document.querySelector('div[data-testid=now-playing-widget]>div:last-child>button');
        if (fb) {
            fb.click();
            setTimeout(() => addAndAuto(true), 100);
            setTimeout(() => addAndAuto(true), 500);
        }
    };

    window.actSeek = function(pos) {
        let rg = document.querySelector('div[data-testid=playback-progressbar] input[type=range]');
        if (rg) {
            rg.value = pos;
            rg.dispatchEvent(new Event('change', { bubbles: true }));
        }
    };

    window.syncCanvasToggle = function(el) {
        if (!el || el.classList.contains('fuckd-cv')) return;
        el.classList.add('fuckd-cv');

        const updateState = () => {
            let shouldBeChecked = !window.SF_CONFIG.isCanvasDisabled;
            if (el.checked !== shouldBeChecked) {
                el.click();
            }
        };

        updateState();
        el.addEventListener('change', () => {
            let isCurrentlyDisabled = !el.checked;
            if (isCurrentlyDisabled !== window.SF_CONFIG.isCanvasDisabled) {
                window.SF_CONFIG.isCanvasDisabled = isCurrentlyDisabled;
                AndBridge.setCanvasDisabled(isCurrentlyDisabled);
                if (isCurrentlyDisabled) {
                    document.body.classList.add('sf-hide-canvas');
                    document.body.classList.remove('sf-video-bg');
                } else {
                    document.body.classList.remove('sf-hide-canvas');
                    document.body.classList.add('sf-video-bg');
                }
            }
        });
    };

    window.addGlobalCleanup = function() {
        let gst = document.createElement('style');
        gst.id = 'global-cleanup-style';
        gst.textContent = `
            div[data-encore-id=banner],
            #global-nav-bar>div:first-of-type,
            #global-nav-bar a[href="/download"],
            div.main-view-container__mh-footer-container,
            button[data-testid="open-in-desktop-app"],
            [data-testid="desktop-client-button"],
            a[href="/download"],
            button[aria-label="Open in Desktop app"],
            [aria-label="Download Spotify"],
            [data-testid="top-bar-download-button"],
            a[href*="desktop-download"],
            button[aria-label*="Download"],
            #Desktop_LeftSidebar_Id a[href*="/download"],
            #Desktop_LeftSidebar_Id button[aria-label*="Download"] {
                display: none !important;
            }

            /* Fix for player visibility in expanded view and library overlay */
            aside[data-testid="now-playing-bar"],
            .lbtn {
                z-index: calc(var(--above-everything-grid-area-z-index) + 10) !important;
                opacity: 1 !important;
                visibility: visible !important;
            }

            /* Force Album Art over Canvas when disabled */
            .sf-hide-canvas .canvasVideoContainerNPV,
            .sf-hide-canvas #VideoPlayerNpv_ReactPortal,
            .sf-hide-canvas .VideoPlayer__container,
            .sf-hide-canvas [data-testid="canvas-video"],
            .sf-hide-canvas [data-testid="track-visual-enhancement"] ~ div:has(video),
            .sf-hide-canvas [data-testid="track-visual-enhancement"] ~ div video {
                display: none !important;
            }

            .sf-hide-canvas [data-testid="track-visual-enhancement"] {
                display: block !important;
                visibility: visible !important;
                opacity: 1 !important;
                pointer-events: auto !important;
                transform: none !important;
            }

            .sf-hide-canvas [data-testid="track-visual-enhancement"] img {
                display: block !important;
                visibility: visible !important;
                opacity: 1 !important;
            }
        `;
        if (!document.getElementById('global-cleanup-style')) document.head.appendChild(gst);
        if (typeof window.gobserver === 'undefined') {
            window.gobserver = new MutationObserver((mutations) => {
                mutations.forEach((mutation) => {
                    mutation.addedNodes.forEach((node) => {
                        if (node.nodeType === 1) {
                            node.querySelectorAll("button, a, span").forEach(el => {
                                let txt = (el.innerText || "").toLowerCase();
                                if (txt.includes('open in desktop app') || txt.includes('open app') || txt.includes('install app') || txt.includes('download spotify')) {
                                    el.style.setProperty('display', 'none', 'important');
                                    // Also try to hide parent if it's a sidebar item
                                    if (el.closest('nav') || el.closest('#Desktop_LeftSidebar_Id')) {
                                        let p = el.parentElement;
                                        if (p && p.childNodes.length === 1) p.style.setProperty('display', 'none', 'important');
                                    }
                                }
                            });

                            let cv = node.querySelector('#settings\\.canvasVideos') || (node.id === 'settings.canvasVideos' ? node : null);
                            if (cv) syncCanvasToggle(cv);

                            if (window.SF_CONFIG.isCanvasDisabled) {
                                node.querySelectorAll("[role='menuitem'], li").forEach(el => {
                                    let txt = (el.innerText || "").toLowerCase();
                                    if (txt.includes('canvas') && !txt.includes('artwork') && !txt.includes('album art')) {
                                        el.style.setProperty('display', 'none', 'important');
                                    }
                                });
                            }
                        }
                    });
                });
            });
            window.gobserver.observe(document.body, { childList: true, subtree: true });
        }
    };

    window.addAutoFeatures = function() {
        if (window.SF_CONFIG.autoPlayMode === "onetime") {
            if ('pBtn' in window && firstPlay && window.pBtn.getAttribute('aria-label') === 'Play') {
                window.pBtn.click();
                firstPlay = false;
            }
        }
        if (window.SF_CONFIG.closeNowPlay || window.SF_CONFIG.takeControl || window.SF_CONFIG.autoPlayMode === "permanent" || typeof window.SF_CONFIG.isCanvasDisabled !== 'undefined') {
            if (afint) clearInterval(afint);
            afint = setInterval(() => {
                if (typeof window.SF_CONFIG.isCanvasDisabled !== 'undefined') {
                    let cv = document.getElementById('settings.canvasVideos');
                    if (cv) syncCanvasToggle(cv);
                }

                if (window.SF_CONFIG.closeNowPlay) closeNowPlay();
                if (window.SF_CONFIG.takeControl) {
                    let ft = document.querySelector('aside div.encore-bright-accent-set button');
                    if (ft) {
                        ft.click();
                        setTimeout(() => {
                            let cb = document.querySelector('aside ul[role=list] li[role=listitem] div[role=button]');
                            if (cb) cb.click();
                        }, 500);
                    }
                }
                if (window.SF_CONFIG.autoPlayMode === "permanent") {
                    if ('pBtn' in window && !reqPause && !ulFlag && window.pBtn.getAttribute('aria-label') === 'Play') window.pBtn.click();
                }
            }, 5000);
        }
    };

    window.addAndAuto = function(once = false) {
        const run = () => {
            // Track context info
            let ta = document.querySelector('a[data-testid=context-item-link]');
            if (ta) window.track = ta.innerText;
            else window.track = null;
            let aa = document.querySelector('a[data-testid=context-item-info-artist]') || document.querySelector('a[data-testid=context-item-info-show]');
            if (aa) window.artist = aa.innerText;
            else window.artist = '';
            let rr = document.querySelector('button[data-testid=control-button-repeat]');
            if (rr) window.repmode = rr.getAttribute('aria-checked');
            else window.repmode = 'false';

            let sh = document.querySelector('button[aria-label*="Shuffle"]');
            if (sh) {
                let label = (sh.getAttribute('aria-label') || "").toLowerCase();
                let checked = sh.getAttribute('aria-checked');
                if (label.includes('disable smart') || checked === 'mixed') window.shmode = 'mixed';
                else if (label.includes('disable shuffle') || label.includes('enable smart')) window.shmode = 'true';
                else window.shmode = 'false';
            } else {
                window.shmode = 'false';
            }

            let fb = document.querySelector('div[data-testid=now-playing-widget]>div:last-child>button');
            if (fb && fb.getAttribute('aria-checked') === 'true') window.isfav = true;
            else window.isfav = false;
            let rg = document.querySelector('div[data-testid=playback-progressbar] input[type=range]');
            if (rg) {
                window.duration = parseInt(rg.getAttribute('max'));
                window.position = parseInt(rg.getAttribute('value'));
            } else {
                window.duration = null;
                window.position = null;
            }
            let im = document.querySelector('img[data-testid=cover-art-image]');
            if (im) window.cover = im.src;
            else window.cover = null;

            // Robust Color Extraction
            if (document.body.classList.contains('sf-expanded')) {

                //let cs = Array.from(document.querySelectorAll('div[style*="--cinema-mode-bg-color-from"]'))
                //              .find(el => el.offsetParent !== null || el.getClientRects().length > 0);

                let candidates = Array.from(document.querySelectorAll('div[style*="--cinema-mode-bg-color-from"]'));
                let cs = candidates.find(el => el.offsetParent !== null || el.getClientRects().length > 0) || candidates[candidates.length - 1];

                if (cs) {
                    ['--cinema-mode-bg-color-from', '--cinema-mode-bg-color-to', '--background-base'].forEach(v => {
                        let val = cs.style.getPropertyValue(v).trim();
                        if (val) document.body.style.setProperty(v, val);
                    });
                }
            }

            updMedia(once);
        };

        if (once) { run(); return; }
        if (aaint) clearInterval(aaint);
        aaint = setInterval(run, 250);
    };

    window.addCSSJSHack = function() {
        if (window.SF_CONFIG.guiMode === "csshack") {
            if (cssint) clearInterval(cssint);
            cssint = setInterval(function() {
                let lb = document.querySelector('#Desktop_LeftSidebar_Id header>div>div:first-child button:not(.fuckd)');
                if (lb) {
                    window.lBtn = lb;
                    lb.classList.add('fuckd', 'lbtn');
                    lb.style.padding = '0px';
                    lb.style.height = '20px';
                    lb.addEventListener('click', function() { setTimeout(() => switchLs(), 0) });
                    switchLs();
                    AndBridge.cssInjected();
                }
                let lbit = document.querySelector('#Desktop_LeftSidebar_Id div[role=grid]:not(.fuckd)');
                if (lbit) {
                    lbit.classList.add('fuckd');
                    lbit.addEventListener('click', () => {
                        setTimeout(() => {
                            window.lBtn.click();
                            closeNowPlay();
                        }, 0)
                    });
                }
                let hb = document.querySelector('#global-nav-bar button[data-testid=home-button]:not(.fuckd)');
                if (hb) {
                    hb.classList.add('fuckd');
                    hb.addEventListener('click', () => { closeNowPlay(); });
                }
                let sr = document.querySelector('input[data-testid=search-input]:not(.fuckd)');
                if (sr) {
                    sr.classList.add('fuckd');
                    sr.addEventListener('focus', () => {
                        let npb = document.querySelector('aside[data-testid=now-playing-bar]');
                        if (npb) npb.style.setProperty('display', 'none', 'important');
                        closeNowPlay();
                    });
                    sr.addEventListener('blur', () => {
                        let npb = document.querySelector('aside[data-testid=now-playing-bar]');
                        if (npb) npb.style.setProperty('display', 'flex', 'important');
                    });
                }
                let minBtn = document.querySelector('button[aria-label*="Minimize"], button[aria-label*="Back to player"]');
                let isVis = minBtn && (minBtn.offsetParent !== null || minBtn.offsetWidth > 0);
                if (isVis) {
                    document.body.classList.add('sf-expanded');
                } else {
                    document.body.classList.remove('sf-expanded');
                    ['--background-base', '--background-highlight', '--background-press', '--cinema-mode-bg-color-from', '--cinema-mode-bg-color-to'].forEach(v => {
                        document.body.style.removeProperty(v);
                    });
                }

                let ub = document.querySelector('button[data-testid=user-widget-link]:not(.fuckd)');
                if (ub) {
                    ub.classList.add('fuckd');
                    ub.addEventListener('click', () => { closeNowPlay(); });
                }
            }, 1000);

            window.switchLs = function() {
                let ls = document.querySelector('#Desktop_LeftSidebar_Id');
                if (ls) {
                    let navDiv = ls.querySelector('nav>div>div:first-child');
                    if (!navDiv) return;
                    let exp = navDiv.classList.length;
                    if (exp == 2) {
                        ls.style.setProperty('position', 'fixed', 'important');
                        ls.style.setProperty('width', '100%', 'important');
                        ls.style.setProperty('height', 'calc(100vh - 48px)', 'important');
                        ls.style.setProperty('top', '48px', 'important');
                        ls.style.setProperty('bottom', '0px', 'important');
                        ls.style.setProperty('left', '0px', 'important');
                        ls.style.setProperty('overflow-y', 'auto', 'important');
                        ls.style.setProperty('z-index', 'calc(var(--above-everything-grid-area-z-index) + 5)', 'important');
                        let lh = ls.querySelector('header>div>div:first-child h1');
                        if (lh) lh.innerHTML = '✖ &nbsp; ' + (window.SF_CONFIG?.closeLibText || "Library");
                    } else {
                        ls.style.setProperty('z-index', '1', 'important');
                        ls.style.setProperty('position', 'fixed', 'important');
                        ls.style.setProperty('top', '0px', 'important');
                        ls.style.setProperty('left', '60px', 'important');
                        ls.style.setProperty('width', '48px', 'important');
                        ls.style.setProperty('height', '48px', 'important');
                        ls.style.removeProperty('bottom');
                        ls.style.removeProperty('overflow-y');
                    }
                }
            };
        }
    };

    firstFuck();
})();
