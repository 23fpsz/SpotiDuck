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
    let tagint = null;
    let fsUpdateTimer = null;
    let fsScrubbing = false;

    // --- Native SVG Paths (Spotifuck Assets) ---
    const NATIVE_SVGS = {
        previous: '<svg viewBox="0 0 16 16" width="32" height="32"><path d="M3.3 1a.7.7 0 0 1 .7.7v5.15l9.95-5.744a.7.7 0 0 1 1.05.606v12.575a.7.7 0 0 1-1.05.607L4 8.949V14.3a.7.7 0 0 1-.7.7H1.7a.7.7 0 0 1-.7-.7V1.7a.7.7 0 0 1 .7-.7z" fill="currentColor"/></svg>',
        next: '<svg viewBox="0 0 16 16" width="32" height="32"><path d="M12.7 1a.7.7 0 0 0-.7.7v5.15L2.05 1.107A.7.7 0 0 0 1 1.712v12.575a.7.7 0 0 0 1.05.607L12 9.149V14.3a.7.7 0 0 0 .7.7h1.6a.7.7 0 0 0 .7-.7V1.7a.7.7 0 0 0-.7-.7z" fill="currentColor"/></svg>',
        shuffle: '<svg viewBox="0 0 24 24" width="28" height="28"><path d="M18.788 3.702a1 1 0 0 1 1.414-1.414L23.914 6l-3.712 3.712a1 1 0 1 1-1.414-1.414L20.086 7h-1.518a5 5 0 0 0-3.826 1.78l-7.346 8.73a7 7 0 0 1-5.356 2.494H1v-2h1.04a5 5 0 0 0 3.826-1.781l7.345-8.73A7 7 0 0 1 18.569 5h1.518l-1.298-1.298z" fill="currentColor"/><path d="M18.788 14.289a1 1 0 0 0 0 1.414L20.086 17h-1.518a5 5 0 0 1-3.826-1.78l-1.403-1.668-1.306 1.554 1.178 1.4A7 7 0 0 0 18.568 19h1.518l-1.298 1.298a1 1 0 1 0 1.414 1.414L23.914 18l-3.712-3.713a1 1 0 0 0-1.414 0zM7.396 6.49l2.023 2.404-1.307 1.553-2.246-2.67a5 5 0 0 0-3.826-1.78H1v-2h1.04A7 7 0 0 1 7.396 6.49" fill="currentColor"/></svg>',
        smart_shuffle: '<svg viewBox="0 0 24 24" width="28" height="28"><path d="M 7.335 0.6 a 0.667 0.667 0 0 0 -1.327 0 c -0.083 0.86 -0.457 2.21 -1.309 3.386 C 3.86 5.142 2.567 6.126 0.6 6.333 a 0.667 0.667 0 0 0 0 1.327 c 1.967 0.207 3.26 1.19 4.099 2.347 c 0.851 1.176 1.227 2.527 1.307 3.386 a 0.666 0.666 0 0 0 1.329 0 c 0.08 -0.86 0.456 -2.21 1.307 -3.386 c 0.839 -1.156 2.132 -2.14 4.099 -2.348 a 0.667 0.667 0 0 0 0 -1.326 c -1.967 -0.207 -3.26 -1.191 -4.1 -2.347 C 7.792 2.81 7.417 1.459 7.336 0.6 Z m 11.979 6.186 a 1 1 0 0 1 1.415 -1.414 l 3.211 3.211 l -3.212 3.211 a 1 1 0 0 1 -1.414 -1.414 l 0.797 -0.797 h -0.862 a 4 4 0 0 0 -3.06 1.425 l -6.122 7.275 a 7.3 7.3 0 0 1 -1.383 1.279 c -0.51 0.352 -1.178 0.685 -1.905 0.685 v -2 c 0.137 0 0.4 -0.077 0.768 -0.331 c 0.35 -0.242 0.7 -0.577 0.99 -0.921 l 6.12 -7.275 a 6 6 0 0 1 4.592 -2.137 h 0.863 Z" fill="currentColor"/><path d="M19.249 19.584a6 6 0 0 1-4.591-2.137l-.771-.917-.006-.007-.003-.003-.016-.02-.06-.07a2 2 0 0 0-.118-.12l1.289-1.53a3.3 3.3 0 0 1 .42.433l.028.035.007.007.76.904a4 4 0 0 0 3.06 1.425h.84l-.798-.797a1 1 0 0 1 1.414-1.415l3.212 3.212-3.212 3.211a1 1 0 1 1-1.414-1.414l.797-.797z" fill="currentColor"/></svg>',
        repeat: '<svg viewBox="0 0 16 16" width="28" height="28"><path d="M0 4.75A3.75 3.75 0 0 1 3.75 1h8.5A3.75 3.75 0 0 1 16 4.75v5a3.75 3.75 0 0 1-3.75 3.75H9.81l1.018 1.018a.75.75 0 1 1-1.06 1.06L6.939 12.75l2.829-2.828a.75.75 0 1 1 1.06 1.06L9.811 12h2.439a2.25 2.25 0 0 0 2.25-2.25v-5a2.25 2.25 0 0 0-2.25-2.25h-8.5A2.25 2.25 0 0 0 1.5 4.75v5A2.25 2.25 0 0 0 3.75 12H5v1.5H3.75A3.75 3.75 0 0 1 0 9.75z" fill="currentColor"/></svg>',
        repeat_one: '<svg viewBox="0 0 16 16" width="28" height="28"><path d="M0 4.75A3.75 3.75 0 0 1 3.75 1h.75v1.5h-.75A2.25 2.25 0 0 0 1.5 4.75v5A2.25 2.25 0 0 0 3.75 12H5v1.5H3.75A3.75 3.75 0 0 1 0 9.75zM12.25 2.5a2.25 2.25 0 0 1 2.25 2.25v5A2.25 2.25 0 0 1 12.25 12H9.81l1.018-1.018a.75.75 0 0 0-1.06-1.06L6.939 12.75l2.829 2.828a.75.75 0 1 0 1.06-1.06L9.811 13.5h2.439A3.75 3.75 0 0 0 16 9.75v-5A3.75 3.75 0 0 0 12.25 1h-.75v1.5z" fill="currentColor"/><path d="m8 1.85.77.694H6.095V1.488q1.046-.077 1.507-.385.474-.308.583-.913h1.32V8H8z" fill="currentColor"/><path d="M8.77 2.544 8 1.85v.693z" fill="currentColor"/></svg>',
        repeat_on: '<svg viewBox="0 0 16 16" width="28" height="28"><path d="M0 4.75A3.75 3.75 0 0 1 3.75 1h8.5A3.75 3.75 0 0 1 16 4.75v5a3.75 3.75 0 0 1-3.75 3.75H9.81l1.018 1.018a.75.75 0 1 1-1.06 1.06L6.939 12.75l2.829-2.828a.75.75 0 1 1 1.06 1.06L9.811 12h2.439a2.25 2.25 0 0 0 2.25-2.25v-5a2.25 2.25 0 0 0-2.25-2.25h-8.5A2.25 2.25 0 0 0 1.5 4.75v5A2.25 2.25 0 0 0 3.75 12H5v1.5H3.75A3.75 3.75 0 0 1 0 9.75z" fill="currentColor"/><path d="M8 5.75A1.5 1.5 0 1 0 8 8.75A1.5 1.5 0 1 0 8 5.75Z" fill="currentColor"/></svg>',
        fav_on: '<svg viewBox="0 0 16 16" width="28" height="28"><path d="M0 8a8 8 0 1 1 16 0A8 8 0 0 1 0 8m11.748-1.97a.75.75 0 0 0-1.06-1.06l-4.47 4.47-1.405-1.406a.75.75 0 1 0-1.061 1.06l2.466 2.467 5.53-5.53z" fill="currentColor"/></svg>',
        fav_off: '<svg viewBox="0 0 16 16" width="28" height="28"><path d="M8 1.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13M0 8a8 8 0 1 1 16 0A8 8 0 0 1 0 8" fill="currentColor"/><path d="M11.75 8a.75.75 0 0 1-.75.75H8.75V11a.75.75 0 0 1-1.5 0V8.75H5a.75.75 0 0 1 0-1.5h2.25V5a.75.75 0 0 1 1.5 0v2.25H11a.75.75 0 0 1 .75.75" fill="currentColor"/></svg>'
    };

    // --- DOM Tagger & Interaction Logic ---
    window.tagDOM = function() {
        const bar = document.querySelector('aside[data-testid="now-playing-bar"]');
        if (bar) {
            bar.classList.add('sf-player-bar');
            const widget = bar.querySelector('[data-testid="now-playing-widget"]');
            if (widget) {
                widget.classList.add('sf-player-widget');
                const cover = widget.querySelector('[data-testid="CoverSlotCollapsed__container"]');
                if (cover && !cover.classList.contains('sf-fs-trigger')) {
                    cover.classList.add('sf-fs-trigger');
                    cover.addEventListener('click', (e) => {
                        e.preventDefault(); e.stopPropagation(); openFullscreenPlayer();
                    });
                }
                for (let i = 0; i < widget.children.length; i++) {
                    let ch = widget.children[i];
                    if (!ch.querySelector('[data-testid="CoverSlotCollapsed__container"]') && ch.querySelector('a')) {
                        if (!ch.classList.contains('sf-track-info')) {
                            ch.classList.add('sf-track-info');
                            setupSwipe(ch);
                        }
                        break;
                    }
                }
            }
        }
    };

    function openFullscreenPlayer() {
        if (document.getElementById('sf-fs-player')) return;
        const overlay = document.createElement('div');
        overlay.id = 'sf-fs-player';
        overlay.innerHTML = `
            <div class="sf-fs-top">
                <button class="sf-fs-min" aria-label="Close">
                    <svg viewBox="0 0 24 24" width="32" height="32"><path d="M7 10l5 5 5-5z" fill="currentColor"/></svg>
                </button>

                <div class="sf-fs-top-right">
                    <button class="sf-fs-connect-btn" title="Connect"><svg viewBox="0 0 16 16" width="22" height="22"><path d="M6 2.75C6 1.784 6.784 1 7.75 1h6.5c.966 0 1.75.784 1.75 1.75v10.5A1.75 1.75 0 0114.25 15h-6.5A1.75 1.75 0 016 13.25zm1.75-.25a.25.25 0 00-.25.25v10.5c0 .138.112.25.25.25h6.5a.25.25 0 00.25-.25V2.75a.25.25 0 00-.25-.25zm-6 0a.25.25 0 00-.25.25v6.5c0 .138.112.25.25.25H4V11H1.75A1.75 1.75 0 010 9.25v-6.5C0 1.784.784 1 1.75 1H4v1.5zM4 15H2v-1.5h2z" fill="currentColor"/><path d="M13 10a2 2 0 11-4 0 2 2 0 014 0m-1-5a1 1 0 11-2 0 1 1 0 012 0" fill="currentColor"/></svg></button>
                    <button class="sf-fs-queue-btn" title="Queue"><svg viewBox="0 0 16 16" width="22" height="22"><path d="M15 15H1v-1.5h14zm0-4.5H1V9h14zm-14-7A2.5 2.5 0 013.5 1h9a2.5 2.5 0 010 5h-9A2.5 2.5 0 011 3.5zm2.5-1a1 1 0 100 2h9a1 1 0 100-2z" fill="currentColor"/></svg></button>
                </div>
            </div>
            <div class="sf-fs-art-container" id="sf-fs-art-wrap">
                <img id="sf-fs-art" draggable="false">
            </div>
            <div class="sf-fs-info">
                <h2 id="sf-fs-title"></h2>
                <p id="sf-fs-artist"></p>
            </div>
            <div class="sf-fs-progress">
                <span id="sf-fs-pos">0:00</span>
                <div id="sf-fs-bar-container">
                    <div id="sf-fs-bar-rail">
                        <div id="sf-fs-bar-fill"></div>
                        <div id="sf-fs-bar-handle"></div>
                    </div>
                </div>
                <span id="sf-fs-dur">0:00</span>
            </div>
            <div class="sf-fs-controls">
                <button class="sf-fs-prev"><span id="sf-fs-prev-svg"></span></button>
                <button class="sf-fs-play"><svg id="sf-fs-play-svg"></svg></button>
                <button class="sf-fs-next"><span id="sf-fs-next-svg"></span></button>
            </div>
            <div class="sf-fs-extra-controls">
                <button class="sf-fs-shuffle-btn"><span id="sf-fs-shuffle-svg"></span></button>
                <button class="sf-fs-like-btn"><span id="sf-fs-like-svg"></span></button>
                <button class="sf-fs-repeat-btn"><span id="sf-fs-repeat-svg"></span></button>
            </div>
        `;
        document.body.appendChild(overlay);
        document.body.classList.add('sf-fs-open');

        // Robust Event Listeners
        overlay.querySelector('.sf-fs-min').onclick = (e) => { e.preventDefault(); e.stopPropagation(); closeFullscreenPlayer(); };
        overlay.querySelector('.sf-fs-connect-btn').onclick = openConnect;
        overlay.querySelector('.sf-fs-queue-btn').onclick = openQueue;
        overlay.querySelector('.sf-fs-prev').onclick = actSkipBack;
        overlay.querySelector('.sf-fs-play').onclick = (e) => actPlayPause(null, e);
        overlay.querySelector('.sf-fs-next').onclick = actSkipForward;
        overlay.querySelector('.sf-fs-shuffle-btn').onclick = actShuffle;
        overlay.querySelector('.sf-fs-like-btn').onclick = actAddToFav;
        overlay.querySelector('.sf-fs-repeat-btn').onclick = actRepeat;

        setupSwipe(document.getElementById('sf-fs-art-wrap'), true);
        const barContainer = document.getElementById('sf-fs-bar-container');
        barContainer.addEventListener('touchstart', (e) => { fsScrubbing = true; handleScrub(e); }, {passive: false});
        barContainer.addEventListener('touchmove', handleScrub, {passive: false});
        barContainer.addEventListener('touchend', () => { fsScrubbing = false; });
        updateFullscreenUI();
        fsUpdateTimer = setInterval(updateFullscreenUI, 500);
    }

    window.openConnect = function(e) { if(e) { e.preventDefault(); e.stopPropagation(); } closeFullscreenPlayer(); document.querySelector('button[aria-label="Connect to a device"]')?.click(); };
    window.openQueue = function(e) { if(e) { e.preventDefault(); e.stopPropagation(); } closeFullscreenPlayer(); document.querySelector('button[data-testid="control-button-queue"]')?.click(); };

    function handleScrub(e) {
        const rail = document.getElementById('sf-fs-bar-rail');
        const rect = rail.getBoundingClientRect();
        const x = e.touches[0].clientX - rect.left;
        const frac = Math.max(0, Math.min(1, x / rect.width));
        const pos = Math.round(frac * window.duration);
        actSeek(pos);
        updateProgressUI(pos, window.duration);
    }

    function updateFullscreenUI() {
        if (!document.getElementById('sf-fs-player')) { clearInterval(fsUpdateTimer); return; }
        document.getElementById('sf-fs-title').innerText = window.track || "No Track";
        document.getElementById('sf-fs-artist').innerText = window.artist || "No Artist";
        const art = document.getElementById('sf-fs-art');
        if (window.cover) { const bigCover = window.cover.replace('00004851', '0000b273'); if (art.src !== bigCover) art.src = bigCover; }

        document.getElementById('sf-fs-prev-svg').innerHTML = NATIVE_SVGS.previous;
        document.getElementById('sf-fs-next-svg').innerHTML = NATIVE_SVGS.next;

        const playSvg = document.getElementById('sf-fs-play-svg');
        if (playSvg) {
            playSvg.setAttribute('viewBox', '0 0 16 16');
            if (window.playing) {
                playSvg.innerHTML = '<path d="M2.7 1a.7.7 0 00-.7.7v12.6a.7.7 0 00.7.7h2.6a.7.7 0 00.7-.7V1.7a.7.7 0 00-.7-.7zm8 0a.7.7 0 00-.7.7v12.6a.7.7 0 00.7.7h2.6a.7.7 0 00.7-.7V1.7a.7.7 0 00-.7-.7z" fill="currentColor"/>';
            } else {
                playSvg.innerHTML = '<path d="M3 1.713a.7.7 0 011.05-.607l10.89 6.288a.7.7 0 010 1.212L4.05 14.894A.7.7 0 013 14.287z" fill="currentColor"/>';
            }
        }

        const shSvgWrap = document.getElementById('sf-fs-shuffle-svg');
        if (shSvgWrap) {
            if (window.shmode === 'mixed') { shSvgWrap.style.color = '#fff'; shSvgWrap.innerHTML = NATIVE_SVGS.smart_shuffle; }
            else if (window.shmode === 'true') { shSvgWrap.style.color = '#fff'; shSvgWrap.innerHTML = NATIVE_SVGS.shuffle; }
            else { shSvgWrap.style.color = '#b3b3b3'; shSvgWrap.innerHTML = NATIVE_SVGS.shuffle; }
        }

        const repSvgWrap = document.getElementById('sf-fs-repeat-svg');
        if (repSvgWrap) {
            if (window.repmode === 'true') { repSvgWrap.style.color = '#fff'; repSvgWrap.innerHTML = NATIVE_SVGS.repeat_on; }
            else if (window.repmode === 'mixed') { repSvgWrap.style.color = '#fff'; repSvgWrap.innerHTML = NATIVE_SVGS.repeat_one; }
            else { repSvgWrap.style.color = '#b3b3b3'; repSvgWrap.innerHTML = NATIVE_SVGS.repeat; }
        }

        const likeSvgWrap = document.getElementById('sf-fs-like-svg');
        if (likeSvgWrap) {
            if (window.isfav) { likeSvgWrap.style.color = '#fff'; likeSvgWrap.innerHTML = NATIVE_SVGS.fav_on; }
            else { likeSvgWrap.style.color = '#b3b3b3'; likeSvgWrap.innerHTML = NATIVE_SVGS.fav_off; }
        }

        if (!fsScrubbing) { updateProgressUI(window.position, window.duration); }
    }

    function updateProgressUI(pos, dur) {
        const fill = document.getElementById('sf-fs-bar-fill');
        const handle = document.getElementById('sf-fs-bar-handle');
        const posTxt = document.getElementById('sf-fs-pos');
        const durTxt = document.getElementById('sf-fs-dur');
        if (dur > 0) { const pct = (pos / dur) * 100; fill.style.width = `${pct}%`; handle.style.left = `${pct}%`; }
        posTxt.innerText = formatTime(pos); durTxt.innerText = formatTime(dur);
    }

    function formatTime(ms) { const totalSec = Math.floor(ms / 1000); const min = Math.floor(totalSec / 60); const sec = totalSec % 60; return `${min}:${sec.toString().padStart(2, '0')}`; }

    window.closeFullscreenPlayer = function() {
        const overlay = document.getElementById('sf-fs-player');
        if (overlay) { overlay.remove(); document.body.classList.remove('sf-fs-open'); clearInterval(fsUpdateTimer); }
    };

    function setupSwipe(el, isFs = false) {
        if (el._sfSwipeAttached) return; el._sfSwipeAttached = true;
        let startX = null, startY = null, swiping = false;
        el.addEventListener('touchstart', (e) => { if (e.touches.length !== 1) return; startX = e.touches[0].clientX; startY = e.touches[0].clientY; swiping = false; el.style.transition = 'none'; }, { passive: true });
        el.addEventListener('touchmove', (e) => { if (startX === null) return; let dx = e.touches[0].clientX - startX; let dy = e.touches[0].clientY - startY; if (!swiping && Math.abs(dx) > 10 && Math.abs(dx) > Math.abs(dy)) swiping = true; if (swiping) { el.style.transform = `translateX(${dx}px)`; el.style.opacity = Math.max(0.3, 1 - Math.abs(dx) / 250); } }, { passive: true });
        el.addEventListener('touchend', (e) => { if (startX === null) return; let dx = e.changedTouches[0].clientX - startX; startX = null; if (swiping && Math.abs(dx) > 40) { if (dx > 0) actSkipBack(); else actSkipForward(); if (isFs) setTimeout(updateFullscreenUI, 300); } el.style.transition = 'transform 0.3s ease, opacity 0.3s ease'; el.style.transform = 'translateX(0)'; el.style.opacity = '1'; });
    }

    document.addEventListener('click', function(e) {
        const row = e.target.closest('[data-testid="tracklist-row"]'); if (!row) return;
        const link = e.target.closest('a[href]'); if (link && !link.getAttribute('data-testid')?.includes('track-link')) return;
        if (e.target.closest('button')) return;
        const playBtn = row.querySelector('button[data-testid="play-button"]') || row.querySelector('button[aria-label*="Play"]');
        if (playBtn) { e.preventDefault(); e.stopPropagation(); playBtn.click(); }
    }, true);

    const canvasSelector = '#VideoPlayerNpv_ReactPortal video, .canvasVideoContainerNPV video, [data-testid="track-visual-enhancement"] ~ div video, [data-testid="canvas-video"] video, .VideoPlayer__container video';

    window.SF_UPDATE = function(config) {
        if (!config) return; window.SF_CONFIG = Object.assign(window.SF_CONFIG || {}, config);
        if (typeof config.isCanvasDisabled !== 'undefined') {
            if (config.isCanvasDisabled) { document.body.classList.add('sf-hide-canvas'); document.body.classList.remove('sf-video-bg'); }
            else { document.body.classList.remove('sf-hide-canvas'); document.body.classList.add('sf-video-bg'); }
            let cv = document.getElementById('settings.canvasVideos'); if (cv) { let shouldBeChecked = !config.isCanvasDisabled; if (cv.checked !== shouldBeChecked) cv.click(); }
            try { localStorage.setItem('canvas-videos-enabled', (!config.isCanvasDisabled).toString()); localStorage.setItem('can-play-canvas', (!config.isCanvasDisabled).toString()); } catch(e) {}
        }
        if (typeof config.isFullScreenEnabled !== 'undefined') { if (config.isFullScreenEnabled) { document.body.classList.add('sf-fullscreen-enabled'); document.body.classList.remove('sf-fullscreen-disabled'); } else { document.body.classList.remove('sf-fullscreen-enabled'); document.body.classList.add('sf-fullscreen-disabled'); } }
        if (typeof config.isAmoled !== 'undefined') { const amStyleId = 'sf-amoled-override'; let style = document.getElementById(amStyleId); if (config.isAmoled) { if (!style) { style = document.createElement('style'); style.id = amStyleId; style.textContent = '.encore-dark-theme{--background-base:#000!important;--background-highlight:#000!important;--background-elevated-base:#000!important;--background-elevated-highlight:#000!important;--background-elevated-press:#000!important;--background-tinted-base:#000!important} aside[data-testid=now-playing-bar]{background:#000!important;box-shadow:none;border-top:1px solid #666}'; document.head.appendChild(style); } } else if (style) { style.remove(); } }
    };

    window.updMedia = function(force = false) {
        const currState = (window.track || "") + '|' + (window.artist || "") + '|' + window.playing + '|' + (window.repmode || "") + '|' + (window.shmode || "") + '|' + window.isfav;
        if (force || currState !== lastState) { lastState = currState; const values = { artist: window.artist || "No Artist", track: window.track || "No Track", playing: window.playing, repeat: window.repmode || "false", shuffle: window.shmode || "false", fav: window.isfav, duration: window.duration || 0, position: window.position || 0, cover: window.cover || "" }; AndBridge.recMediaStatus(JSON.stringify(values)); lastPos = window.position; }
        else if (window.playing) { if (lastPos === null || Math.abs(window.position - lastPos) >= 1000) { AndBridge.recMediaPosition(window.position); lastPos = window.position; } }
    };

    const oriFetch = window.fetch;
    window.fetch = async function(...args) {
        const [url, opts] = args; const method = opts?.method?.toUpperCase?.() || 'GET';
        if (window.SF_CONFIG.isCanvasDisabled && (url.includes('canvas-storage') || url.includes('/v1/canvas'))) { return new Response(JSON.stringify({canvases:[]}), {status: 200}); }
        const headers = opts?.headers || {};
        if (method === 'POST' && url.includes('/track-playback/v1/devices') && opts?.body) { const body = JSON.parse(opts.body); const deviceId = body?.device?.device_id; if (deviceId && deviceId !== window.spotDevId) { window.spotDevId = deviceId; typeof checkMediaLib === 'function' && checkMediaLib(); } }
        const cliToken = headers['Client-Token'] || headers['client-token']; if (cliToken && cliToken !== window.spotCliToken) { window.spotCliToken = cliToken; typeof checkMediaLib === 'function' && checkMediaLib(); }
        const authHead = headers.Authorization || headers.authorization; if (authHead?.startsWith('Bearer ') && authHead !== window.spotAuthToken) { window.spotAuthToken = authHead; typeof checkMediaLib === 'function' && checkMediaLib(); }
        if (ffDone && url.includes('/track-playback/') && method === 'PUT') { const bodyStr = opts?.body ? (typeof opts.body === 'string' ? opts.body : '') : ''; if (bodyStr.includes('"paused":true')) manageAll(false); else if (bodyStr.includes('"paused":false')) manageAll(true); }
        try { const resp = await oriFetch(url, opts); if (resp.status === 404 && url.includes('connect-state') && url.includes('/command/from/')) { AndBridge.deferMessage('reload'); location.reload(); } return resp; } catch (err) { throw err; }
    };

    window.playFromUri = function(uri) { let type = uri.match(/^spotify:([^:]+)/)?.[1]; if (type == 'user') type = 'your_library'; oriFetch(`https://gew4-spclient.spotify.com/connect-state/v1/player/command/from/${window.spotDevId}/to/${window.spotDevId}`, { method: 'POST', headers: { 'Authorization': window.spotAuthToken, 'Client-Token': window.spotCliToken, 'Content-Type': 'application/json' }, body: JSON.stringify({ command: { context: { uri: uri, url: 'context://' + uri, metadata: {} }, play_origin: { feature_identifier: type, feature_version: featVer, referrer_identifier: 'your_library' }, options: { license: 'tft', skip_to: {}, player_options_override: {} }, endpoint: 'play' } }) }); };

    window.firstFuck = function() {
        if (pfint) clearInterval(pfint);
        pfint = setInterval(() => {
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
                pb.classList.add('fuckd'); window.pBtn = pb;
                window.pBtn.addEventListener('click', () => { if (window.pBtn.getAttribute('aria-label') !== 'Play') { reqPause = true; ulFlag = false; manageWake(false); } else if (!ulFlag) { reqPause = false; manageWake(true); ulFlag = true; setTimeout(() => { if (ulFlag && window.pBtn.getAttribute('aria-label') === 'Play') { AndBridge.deferMessage('unlock'); actSkipForward(); trigUnlock(); } else if (ulFlag) { ulFlag = false; } }, 10000); } });
                if (!ffDone) { ffDone = true; AndBridge.manageTShut(true); AndBridge.manageTSleep(false); addGlobalCleanup(); addAutoFeatures(); addCSSJSHack(); addAndAuto(); tagDOM(); if (tagint) clearInterval(tagint); tagint = setInterval(tagDOM, 2000); setTimeout(() => { manageAll(window.playing) }, 5000); }
            }
        }, 5000);
    };

    window.manageWake = function(enable) { if (enable) { if (document.visibilityState == 'hidden') AndBridge.wakeUp(); } else { let hasCanvas = !window.SF_CONFIG.isCanvasDisabled && !!document.querySelector(canvasSelector); if (!AndBridge.isWoke() && document.visibilityState == 'visible' && !hasCanvas) AndBridge.wakeOff(); } };

    window.manageAll = function(play) { window.playing = play; AndBridge.manageTShut(!play); AndBridge.manageTSleep(play); if (play) { firstFuck(); addGlobalCleanup(); addAutoFeatures(); addCSSJSHack(); addAndAuto(); } updMedia(); };

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
            let rBtn = rc.parentNode?.parentNode?.nextElementSibling?.querySelector('button');
            if (rBtn) rBtn.click();
        }
    };

    window.trigUnlock = function() { let uint = setInterval(() => { if (window.pBtn.disabled) { AndBridge.deferMessage('reload'); window.location.reload(); } else if (window.pBtn.getAttribute('aria-label') !== 'Play') { clearInterval(uint); ulFlag = false; } }, 3000); };

    window.actPlayPause = function(play, e) {
        if(e) { e.preventDefault(); e.stopPropagation(); }
        const pb = document.querySelector('button[data-testid="control-button-playpause"]') || window.pBtn;
        if (!pb) return;
        const isPaused = pb.getAttribute('aria-label') === 'Play' || pb.getAttribute('aria-label')?.includes('Play');
        if (play === null || typeof play === 'undefined') {
            pb.click();
        } else if (play === true && isPaused) {
            pb.click();
        } else if (play === false && !isPaused) {
            pb.click();
        }
    };

    window.actSkipBack = function(e) { if(e) { e.preventDefault(); e.stopPropagation(); } let bb = document.querySelector('button[data-testid=control-button-skip-back]'); if (bb) { manageWake(true); bb.click(); } };

    window.actSkipForward = function(e) { if(e) { e.preventDefault(); e.stopPropagation(); } let fb = document.querySelector('button[data-testid=control-button-skip-forward]'); if (fb) { manageWake(true); fb.click(); } };

    window.actRepeat = function(e) { if(e) { e.preventDefault(); e.stopPropagation(); } let rb = document.querySelector('button[data-testid=control-button-repeat]'); if (rb) { rb.click(); setTimeout(() => addAndAuto(true), 100); setTimeout(() => addAndAuto(true), 500); } };

    window.actShuffle = function(e) { if(e) { e.preventDefault(); e.stopPropagation(); } let sb = document.querySelector('button[data-encore-id="buttonTertiary"][aria-label*="Shuffle"]'); if (!sb) sb = document.querySelector('button[aria-label*="Shuffle"]'); if (sb) { sb.click(); setTimeout(() => addAndAuto(true), 100); setTimeout(() => addAndAuto(true), 500); } };

    window.actAddToFav = function(e) { if(e) { e.preventDefault(); e.stopPropagation(); } let fb = document.querySelector('div[data-testid=now-playing-widget]>div:last-child>button'); if (fb) { fb.click(); setTimeout(() => addAndAuto(true), 100); setTimeout(() => addAndAuto(true), 500); } };

    window.actSeek = function(pos) { let rg = document.querySelector('div[data-testid=playback-progressbar] input[type=range]'); if (rg) { rg.value = pos; rg.dispatchEvent(new Event('change', { bubbles: true })); } };

    window.syncCanvasToggle = function(el) { if (!el || el.classList.contains('fuckd-cv')) return; el.classList.add('fuckd-cv'); const updateState = () => { let shouldBeChecked = !window.SF_CONFIG.isCanvasDisabled; if (el.checked !== shouldBeChecked) { el.click(); } }; updateState(); el.addEventListener('change', () => { let isCurrentlyDisabled = !el.checked; if (isCurrentlyDisabled !== window.SF_CONFIG.isCanvasDisabled) { window.SF_CONFIG.isCanvasDisabled = isCurrentlyDisabled; AndBridge.setCanvasDisabled(isCurrentlyDisabled); if (isCurrentlyDisabled) { document.body.classList.add('sf-hide-canvas'); document.body.classList.remove('sf-video-bg'); } else { document.body.classList.remove('sf-hide-canvas'); document.body.classList.add('sf-video-bg'); } } }); };

    window.addGlobalCleanup = function() {
        let gst = document.createElement('style'); gst.id = 'global-cleanup-style'; gst.textContent = `div[data-encore-id=banner],#global-nav-bar>div:first-of-type,#global-nav-bar a[href="/download"],div.main-view-container__mh-footer-container,button[data-testid="open-in-desktop-app"],[data-testid="desktop-client-button"],a[href="/download"],button[aria-label="Open in Desktop app"],[aria-label="Download Spotify"],[data-testid="top-bar-download-button"],a[href*="desktop-download"],button[aria-label*="Download"],#Desktop_LeftSidebar_Id a[href*="/download"],#Desktop_LeftSidebar_Id button[aria-label*="Download"] { display: none !important; } aside[data-testid="now-playing-bar"],.lbtn { z-index: calc(var(--above-everything-grid-area-z-index) + 10) !important; opacity: 1 !important; visibility: visible !important; } .sf-hide-canvas .canvasVideoContainerNPV,.sf-hide-canvas #VideoPlayerNpv_ReactPortal,.sf-hide-canvas .VideoPlayer__container,.sf-hide-canvas [data-testid="canvas-video"],.sf-hide-canvas [data-testid="track-visual-enhancement"] ~ div:has(video),.sf-hide-canvas [data-testid="track-visual-enhancement"] ~ div video { display: none !important; } .sf-hide-canvas [data-testid="track-visual-enhancement"] { display: block !important; visibility: visible !important; opacity: 1 !important; pointer-events: auto !important; transform: none !important; } .sf-hide-canvas [data-testid="track-visual-enhancement"] img { display: block !important; visibility: visible !important; opacity: 1 !important; }`; if (!document.getElementById('global-cleanup-style')) document.head.appendChild(gst);
        if (typeof window.gobserver === 'undefined') { window.gobserver = new MutationObserver((mutations) => { mutations.forEach((mutation) => { mutation.addedNodes.forEach((node) => { if (node.nodeType === 1) { node.querySelectorAll("button, a, span").forEach(el => { let txt = (el.innerText || "").toLowerCase(); if (txt.includes('open in desktop app') || txt.includes('open app') || txt.includes('install app') || txt.includes('download spotify')) { el.style.setProperty('display', 'none', 'important'); if (el.closest('nav') || el.closest('#Desktop_LeftSidebar_Id')) { let p = el.parentElement; if (p && p.childNodes.length === 1) p.style.setProperty('display', 'none', 'important'); } } }); let cv = node.querySelector('#settings\\.canvasVideos') || (node.id === 'settings.canvasVideos' ? node : null); if (cv) syncCanvasToggle(cv); if (window.SF_CONFIG.isCanvasDisabled) { node.querySelectorAll("[role='menuitem'], li").forEach(el => { let txt = (el.innerText || "").toLowerCase(); if (txt.includes('canvas') && !txt.includes('artwork') && !txt.includes('album art')) { el.style.setProperty('display', 'none', 'important'); } }); } } }); }); }); window.gobserver.observe(document.body, { childList: true, subtree: true }); }
    };

    window.addAutoFeatures = function() { if (window.SF_CONFIG.autoPlayMode === "onetime") { if ('pBtn' in window && firstPlay && window.pBtn.getAttribute('aria-label') === 'Play') { window.pBtn.click(); firstPlay = false; } } if (window.SF_CONFIG.closeNowPlay || window.SF_CONFIG.takeControl || window.SF_CONFIG.autoPlayMode === "permanent" || typeof window.SF_CONFIG.isCanvasDisabled !== 'undefined') { if (afint) clearInterval(afint); afint = setInterval(() => { if (typeof window.SF_CONFIG.isCanvasDisabled !== 'undefined') { let cv = document.getElementById('settings.canvasVideos'); if (cv) syncCanvasToggle(cv); } if (window.SF_CONFIG.closeNowPlay) closeNowPlay(); if (window.SF_CONFIG.takeControl) { let ft = document.querySelector('aside div.encore-bright-accent-set button'); if (ft) { ft.click(); setTimeout(() => { let cb = document.querySelector('aside ul[role=list] li[role=listitem] div[role=button]'); if (cb) cb.click(); }, 500); } } if (window.SF_CONFIG.autoPlayMode === "permanent") { if ('pBtn' in window && !reqPause && !ulFlag && window.pBtn.getAttribute('aria-label') === 'Play') window.pBtn.click(); } }, 5000); } };

    window.addAndAuto = function(once = false) { const run = () => { let ta = document.querySelector('a[data-testid=context-item-link]'); if (ta) window.track = ta.innerText; else window.track = null; let aa = document.querySelector('a[data-testid=context-item-info-artist]') || document.querySelector('a[data-testid=context-item-info-show]'); if (aa) window.artist = aa.innerText; else window.artist = ''; let rr = document.querySelector('button[data-testid=control-button-repeat]'); if (rr) window.repmode = rr.getAttribute('aria-checked'); else window.repmode = 'false'; let sh = document.querySelector('button[aria-label*="Shuffle"]'); if (sh) { let label = (sh.getAttribute('aria-label') || "").toLowerCase(); let checked = sh.getAttribute('aria-checked'); if (label.includes('disable smart') || checked === 'mixed') window.shmode = 'mixed'; else if (label.includes('disable shuffle') || label.includes('enable smart')) window.shmode = 'true'; else window.shmode = 'false'; } else { window.shmode = 'false'; } let fb = document.querySelector('div[data-testid=now-playing-widget]>div:last-child>button'); if (fb && fb.getAttribute('aria-checked') === 'true') window.isfav = true; else window.isfav = false; let rg = document.querySelector('div[data-testid=playback-progressbar] input[type=range]'); if (rg) { window.duration = parseInt(rg.getAttribute('max')); window.position = parseInt(rg.getAttribute('value')); } else { window.duration = null; window.position = null; } let im = document.querySelector('img[data-testid=cover-art-image]'); if (im) window.cover = im.src; else window.cover = null; if (document.body.classList.contains('sf-expanded')) { let candidates = Array.from(document.querySelectorAll('div[style*="--cinema-mode-bg-color-from"]')); let cs = candidates.find(el => el.offsetParent !== null || el.getClientRects().length > 0) || candidates[candidates.length - 1]; if (cs) { ['--cinema-mode-bg-color-from', '--cinema-mode-bg-color-to', '--background-base'].forEach(v => { let val = cs.style.getPropertyValue(v).trim(); if (val) document.body.style.setProperty(v, val); }); } } updMedia(once); }; if (once) { run(); return; } if (aaint) clearInterval(aaint); aaint = setInterval(run, 250); };

    window.addCSSJSHack = function() {
        let meta = document.querySelector('meta[name="viewport"]'); if (meta) { let content = meta.getAttribute('content'); if (!content.includes('viewport-fit=cover')) { meta.setAttribute('content', content + ', viewport-fit=cover'); } } else { meta = document.createElement('meta'); meta.name = "viewport"; meta.content = "width=device-width, initial-scale=1.0, viewport-fit=cover"; document.head.appendChild(meta); }
        if (window.SF_CONFIG.guiMode === "csshack") {
            if (cssint) clearInterval(cssint);
            cssint = setInterval(function() {
                let lb = document.querySelector('#Desktop_LeftSidebar_Id header>div>div:first-child button:not(.fuckd)'); if (lb) { window.lBtn = lb; lb.classList.add('fuckd', 'lbtn'); lb.style.padding = '0px'; lb.style.height = '20px'; lb.addEventListener('click', function() { setTimeout(() => switchLs(), 0) }); switchLs(); AndBridge.cssInjected(); }
                let lbit = document.querySelector('#Desktop_LeftSidebar_Id div[role=grid]:not(.fuckd)'); if (lbit) { lbit.classList.add('fuckd'); lbit.addEventListener('click', () => { setTimeout(() => { window.lBtn.click(); closeNowPlay(); }, 0) }); }
                let hb = document.querySelector('#global-nav-bar button[data-testid=home-button]:not(.fuckd)'); if (hb) { hb.classList.add('fuckd'); hb.addEventListener('click', () => { closeNowPlay(); }); }
                let sr = document.querySelector('input[data-testid=search-input]:not(.fuckd)'); if (sr) { sr.classList.add('fuckd'); sr.addEventListener('focus', () => { let npb = document.querySelector('aside[data-testid=now-playing-bar]'); if (npb) npb.style.setProperty('display', 'none', 'important'); closeNowPlay(); AndBridge.setSearchActive(true); }); sr.addEventListener('blur', () => { let npb = document.querySelector('aside[data-testid=now-playing-bar]'); if (npb) npb.style.setProperty('display', 'flex', 'important'); AndBridge.setSearchActive(false); }); }
                let minBtn = document.querySelector('button[aria-label*="Minimize"], button[aria-label*="Back to player"]'); let isVis = !!(minBtn && (minBtn.offsetParent !== null || minBtn.offsetWidth > 0)); if (isVis !== window.sf_is_expanded) { window.sf_is_expanded = isVis; AndBridge.setExpanded(isVis); }
                if (isVis) { document.body.classList.add('sf-expanded'); } else { document.body.classList.remove('sf-expanded'); ['--background-base', '--background-highlight', '--background-press', '--cinema-mode-bg-color-from', '--cinema-mode-bg-color-to'].forEach(v => { document.body.style.removeProperty(v); }); }
                let ub = document.querySelector('button[data-testid=user-widget-link]:not(.fuckd)'); if (ub) { ub.classList.add('fuckd'); ub.addEventListener('click', () => { closeNowPlay(); }); }
            }, 1000);
            window.switchLs = function() {
                let ls = document.querySelector('#Desktop_LeftSidebar_Id'); if (ls) { let navDiv = ls.querySelector('nav>div>div:first-child'); if (!navDiv) return; let exp = navDiv.classList.length; if (exp == 2) { ls.style.setProperty('position', 'fixed', 'important'); ls.style.setProperty('width', '100%', 'important'); ls.style.setProperty('height', 'calc(100vh - 48px - env(safe-area-inset-top))', 'important'); ls.style.setProperty('top', 'calc(48px + env(safe-area-inset-top))', 'important'); ls.style.setProperty('bottom', '0px', 'important'); ls.style.setProperty('left', '0px', 'important'); ls.style.setProperty('overflow-y', 'auto', 'important'); ls.style.setProperty('z-index', 'calc(var(--above-everything-grid-area-z-index) + 5)', 'important'); let lh = ls.querySelector('header>div>div:first-child h1'); if (lh) lh.innerHTML = '✖ &nbsp; ' + (window.SF_CONFIG?.closeLibText || "Library"); } else { ls.style.setProperty('z-index', '1', 'important'); ls.style.setProperty('position', 'fixed', 'important'); ls.style.setProperty('top', 'env(safe-area-inset-top)', 'important'); ls.style.setProperty('left', '60px', 'important'); ls.style.setProperty('width', '48px', 'important'); ls.style.setProperty('height', '48px', 'important'); ls.style.removeProperty('bottom'); ls.style.removeProperty('overflow-y'); } }
            };
        }
    };
    firstFuck();
})();
