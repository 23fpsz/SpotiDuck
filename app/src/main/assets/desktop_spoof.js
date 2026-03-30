(function() {
    window.screen.__defineGetter__('width', function() { return 1920; });
    window.screen.__defineGetter__('height', function() { return 1080; });
    window.screen.__defineGetter__('availWidth', function() { return 1920; });
    window.screen.__defineGetter__('availHeight', function() { return 1040; });
    window.__defineGetter__('innerWidth', function() { return 1920; });
    window.__defineGetter__('innerHeight', function() { return 978; });
    window.navigator.__defineGetter__('vendor', function() { return 'Google Inc.'; });
    window.navigator.__defineGetter__('productSub', function() { return '20030107'; });
    window.navigator.__defineGetter__('platform', function() { return 'Win32'; });
    window.navigator.__defineGetter__('oscpu', function() { return 'null'; });
})();