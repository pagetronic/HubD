var stats = {
    pageview: function () {
        if (!sys.debug && (sys.user.admin || sys.user.editor)) {
            return;
        }
        socket.send({
            action: 'stats',
            data: {
                sysid: sys.sysId(),
                location: document.location.toString(),
                width: $(window).width(),
                height: $(window).height(),
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
            }, 1000);

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
    }
};