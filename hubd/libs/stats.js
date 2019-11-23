/*
 * Copyright (c) 2019. PAGE and Sons
 */

var stats = {
    pageview: function () {
        if (sys.user.admin || sys.user.editor) {
            return;
        }
        socket.send({
            action: 'stats',
            data: {
                id: sys.sysId(),
                location: document.location.toString(),
                width: $(window).width(),
                height: $(window).height(),
                ua: navigator.userAgent,
                user: sys.user.id
            }
        });
    }
};