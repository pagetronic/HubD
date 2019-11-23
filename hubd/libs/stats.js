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
            var gone = function () {
                socket.send({
                    action: 'stats',
                    data: {
                        id: msg.id,
                        gone: true
                    }
                });
            };
            ajax.unload(gone);
            $(window).off('beforeunload.socket').on('beforeunload', function () {
                gone();
                socket.ctx.close();
            });

        });
    }
};