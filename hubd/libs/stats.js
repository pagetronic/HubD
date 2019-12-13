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
        var act = socket.send({action: "live"}, function (msg) {
            if (now.text() !== msg.live) {
                now.text(msg.live).pulse();
                ajax.get('/admin/stats', function (html) {
                    html = $(html);
                    $('#stats').html(html.find('#stats').html());
                    now = $('#stats #now');
                });
            }
            return true;
        });
        ajax.unload(function () {
            socket.abort(act);
        });
    }
};