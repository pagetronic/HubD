sys.init = function (xspeed) {

    if (sys.analytics === "" || sys.analytics === undefined || sys.analytics === null) {
        stats.pageview();
    }

    if (xspeed !== undefined && Cookies.get("debug") === 'debug') {
        var msg = 'XSpeed: ' + xspeed + 'ms';
        var head = $('head');
        var meta_robots = head.find('meta[name=robots]');
        msg += meta_robots.length > 0 ? '<br/>' + meta_robots.attr('content') : '';
        // var meta_canonical =  head.find('link[rel=canonical]'); msg += meta_canonical.length > 0 ? '<br/>' + meta_canonical.attr('href') : '';
        sys.toast(msg, 2000);
    }

    $.ajaxSetup({
        cache: true
    });

    socket.init();

    Cookies.set('tz', new Date().getTimezoneOffset(), {
        expires: 'max',
        samesite: 'none'
    });

    //sys.grip.init();
    sys.consent.message();
    sys.consent.pub();
    sys.notices.init();
    saver.init();
    time.init();
    sys.ego.init();
    sys.blobstore.popimg();
    sys.swipe.init();
    sys.video.init();
    login.autologin();
    sys.comodo();

    ajax.init();
    sys.load();

};
sys.debuggable = function (set) {
    if (set) {
        Cookies.set('debug', 'debug', {
            expires: 'max',
            samesite: 'none'
        });
    } else {
        Cookies.remove('debug');
    }
};
sys.load = function () {

    sys.svg();
    sys.rating.init();
    webpush.buttons();
    saver.init();
    ajax.it();
    $('textarea[autosize]').autosize();
};
