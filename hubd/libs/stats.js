var stats = {
    pageview: function () {
        if (!sys.debug && (sys.user.admin || sys.user.editor)) {
            return;
        }
        var i = 0;
        var device = sys.device();
        socket.send({
            action: 'stats',
            data: {
                location: document.location.toString(),
                width: $(window).width(),
                height: $(window).height(),
                device: device.device,
                os: device.os,
                ua: navigator.userAgent,
                user: sys.user.id,
                referer: ajax.referer === undefined ? document.referrer : ajax.referer,
                title: document.title
            }
        }, function (msg) {

            var goneInterval = setInterval(function () {
                socket.send({
                    action: 'stats',
                    data: {
                        id: msg.id,
                        gone: false
                    }
                });
            }, 3000);

            var gone = function (after) {
                clearInterval(goneInterval);
                socket.send({
                    action: 'stats',
                    data: {
                        id: msg.id,
                        gone: true
                    }
                }, after);
                return "";
            };
            ajax.unload(gone);
            $(window).off('beforeunload.socket').one('beforeunload', function () {
                gone(function () {
                    socket.ctx.close();
                });
            });

        });
    },
    getLive: function (where) {


        var interval = -1;
        var incrementer = function (to) {
            clearInterval(interval);
            var from = parseInt(where.text());
            if (to === from) {
                where.fadeTo(100, 1);
                return;
            }
            var delay = Math.min(1000 / Math.abs(from - to), 250);
            where.fadeTo(100, 0.7);
            interval = setInterval(function () {
                where.text(from);
                if (from > to) {
                    from--;
                } else if (from < to) {
                    from++;
                } else {
                    where.fadeTo(100, 1);
                    clearInterval(interval);
                }
            }, delay);
        };


        socket.follow('stats', function (msg) {
            incrementer(msg.live);
        }, function (msg) {
            incrementer(msg.live);
        });

    }
};