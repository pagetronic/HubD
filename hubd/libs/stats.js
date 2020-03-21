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
                user: sys.user.id
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
    getLive: function () {
        var now = $('#stats #now');
        var interval = -1;
        var goto = function (to) {
            clearInterval(interval);
            var from = parseInt(now.text());
            if (to === now) {
                now.fadeTo(100, 1);
                return;
            }
            now.fadeTo(100, 0.7);
            interval = setInterval(function () {
                now.text(from);
                if (from > to) {
                    from--;
                } else if (from < to) {
                    from++;
                } else {
                    now.fadeTo(100, 1);
                    clearInterval(interval);
                }
            }, 200);
        };
        var act = socket.send({action: "live"}, function (msg) {
            goto(msg.live);
            return true;
        });
        ajax.unload(function () {
            socket.abort(act);
        });
    }
};