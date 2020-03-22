sys.init = function (xspeed) {


    $.ajaxSetup({
        cache: true
    });

    socket.init();

    Cookies.set('tz', new Date().getTimezoneOffset(), {
        expires: 'max'
    });

    sys.consent.message();
    sys.consent.pub();
    sys.notices.init();
    sys.ego.init();
    login.autologin();
    sys.comodo.init();
    if (constants.ajax) {
        ajax.init();
    }
    sys.load(xspeed);

    stats.getLive($('#livestats span'));

};
sys.debuggable = function (set) {
    if (set) {
        Cookies.set('debug', 'debug', {
            expires: 'max'
        });
    } else {
        Cookies.remove('debug');
    }
};
sys.load = function (xspeed) {


    if (sys.analytics === "" || sys.analytics === undefined || sys.analytics === null) {
        stats.pageview();
    }

    if (xspeed !== undefined && Cookies.get("debug") === 'debug') {
        var meta_robots = $('meta[name=robots]');
        var meta = meta_robots.length > 0 ? '<br/>' + meta_robots.attr('content') : '';
        sys.toast('XSpeed: ' + xspeed + 'ms' + meta, 2000);
    }

    sys.comodo.show();

    saver.init();
    time.init();


    sys.swipe.make();
    blobstore.popimg();
    sys.video.init();
    sys.svg();
    sys.rating.init();
    webpush.buttons();
    saver.init();

    if (constants.ajax) {
        ajax.it();
    }
    $('textarea[autosize]').autosize();
};
