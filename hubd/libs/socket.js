/*
 *
 *
 *	socket.follow(channel, function(msg) {});
 *	socket.send({command : "",	data : data	}, function(msg) {});
 *
 *
 */

var socket = {
    events: {},
    fevents: {},
    funcs: [],
    ctx: null,

    init: function () {

        if (WebSocket === undefined) {
            return;
        }
        socket.ctx = new WebSocket(constants.apiurl.replace(/^http/, 'ws') + '/socket', sys.lng);

        socket.ctx.onopen = function (event) {
            var funcs = socket.funcs;
            socket.funcs = [];
            socket.funcs.push = function (msg) {
                socket.ctx.send(msg);
            };
            $.each(funcs, function(key) {
                if (key !== "push") {
                    socket.ctx.send(this);
                }
            });
        };

        socket.ctx.onmessage = function (event) {
            var data = {};
            try {
                data = eval("(" + event.data + ")");
            } catch (e) {
                return;
            }

            if (data.channel === undefined) {
                return;
            }
            if (socket.fevents[data.channel] !== undefined) {
                try {
                    socket.fevents[data.channel](data.message);
                } catch (e) {
                }
            }
            if (socket.events[data.channel] !== undefined) {
                try {
                    //return true for multiple needed
                    if (socket.events[data.channel](data.message) !== true) {
                        delete socket.events[data.channel];
                    }
                } catch (e) {
                    try {
                        delete socket.events[data.channel];
                    } catch (e) {

                    }
                }
            }
        };

        socket.ctx.onclose = function () {
            socket.funcs.push = Array.prototype.push;
            setTimeout(function () {
                socket.init();
            }, 3000);

        };
        socket.ctx.onerror = function (event) {
            $('#bell').removeClass('waiting connected');
        };
    },
    send: function (message, func) {
        var act = sys.uniqueId();
        if (func !== undefined && typeof message === "object") {
            message.act = act;
            socket.events[message.act] = func;
        }
        if (typeof message !== "string") {
            message = JSON.stringify(message);
        }
        socket.funcs.push(message);
        return act;
    },
    abort: function (act) {
        socket.funcs.push(JSON.stringify({
            action: 'abort',
            act: act
        }));
        delete socket.events[act];
    },
    follow: function (channel, func) {
        if (socket.fevents[channel] === undefined) {
            socket.fevents[channel] = func;
            var data = {
                action: "follow",
                channel: channel
            };
            socket.send(data);
        } else {
            socket.fevents[channel] = func;
        }
    },
    unfollow: function (channel) {
        delete socket.fevents[channel];
        socket.send({
            action: "unfollow",
            channel: channel
        });
    }
};
$(window).on('beforeunload.socket', function () {
    try {
        socket.ctx.close();
    } catch (e) {
    }
});
