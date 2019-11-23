sys.init = function (xspeed) {

    if (sys.analytics === "" || sys.analytics === undefined || sys.analytics === null) {
        stats.pageview();
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
    if (constants.ajax) {
        ajax.init();
    }
    sys.load(xspeed);

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
sys.load = function (xspeed) {

    if (xspeed !== undefined && Cookies.get("debug") === 'debug') {
        var meta_robots = $('meta[name=robots]');
        var meta = meta_robots.length > 0 ? '<br/>' + meta_robots.attr('content') : '';
        sys.toast('XSpeed: ' + xspeed + 'ms' + meta, 2000);
    }

    sys.svg();
    sys.rating.init();
    webpush.buttons();
    saver.init();

    if (constants.ajax) {
        ajax.it();
    }
    $('textarea[autosize]').autosize();
};
