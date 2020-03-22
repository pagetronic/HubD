/**
 * Class used for WebSocket functions
 */
var socket = {
    /**
     * Object contains functions to execute on send event
     */
    events: {},
    /**
     * Object contains functions to execute on follow event
     */
    fevents: {},
    /**
     * Object contains functions waiting connection open
     */
    funcs: [],
    /**
     * Unique WebSocket connection
     */
    ctx: null,
    /**
     * Initialization, connect the WebSocket
     */
    init: function () {

        if (WebSocket === undefined) {
            return;
        }
        socket.ctx = new WebSocket(constants.apiurl.replace(/^http/, 'ws') + '/socket', sys.lng);

        /**
         * Disconnect on 60 minutes of inactivity
         */
        var socketTimeout = -1;
        var inactiveEvents = 'mousedown mousemove keypress click scroll touchstart focus';
        $(window).on(inactiveEvents, function () {
            clearTimeout(socketTimeout);
            socketTimeout = setTimeout(function () {
                log('Socket closed for inactivity');
                socket.lock = true;
                socket.ctx.close();
                $(window).off('resize scroll').on(inactiveEvents, function () {
                    document.location.reload();
                });
                $(document.body).html('').css('background', '#EEE url(' + constants.logo + '@256x256' + ') 50% 20% no-repeat');
            }, 60 * 60 * 1000);
        });

        socket.ctx.onopen = function () {
            //Execute functions stored who waiting connection
            var funcs = socket.funcs;
            socket.funcs = [];
            socket.funcs.push = function (msg) {
                socket.ctx.send(msg);
            };
            $.each(funcs, function (key) {
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

            //Is there a follow ?
            if (socket.fevents[data.channel] !== undefined) {
                try {
                    socket.fevents[data.channel](data.message);
                } catch (e) {
                }
            }
            //Is there a function to execute on reply ?
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

        //Try to reconnect 5 seconds after close
        socket.ctx.onclose = function () {
            if (socket.lock !== true) {
                socket.funcs.push = Array.prototype.push;
                setTimeout(function () {
                    socket.init();
                }, 5000);
            }

        };
    },
    /**
     * Send request to WebSocket
     *
     * @param message object to send
     * @param func function to execute on reply
     * @returns act identifier
     */
    send: function (message, func) {
        // generate an unique identifier for store function to execute on reply
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
    /**
     * Abort a action
     *
     * @param act action to abort
     */
    abort: function (act) {
        socket.funcs.push(JSON.stringify({
            action: 'abort',
            act: act
        }));
        delete socket.events[act];
    },
    /**
     * Follow a channel and get updates
     *
     * @param channel to follow
     * @param func function to execute on event transmit by webSocket
     * @param submit function to execute on submit to webSocket
     */
    follow: function (channel, func, submit) {
        var data = {
            action: "follow",
            channel: channel
        };
        if (submit !== undefined) {
            data.act = sys.uniqueId();
            socket.events[data.act] = submit;
        }
        if (socket.fevents[channel] === undefined) {
            socket.fevents[channel] = func;
            socket.send(data);
        } else {
            socket.fevents[channel] = func;
        }
    },
    /**
     * Unfollow a channel
     * @param channel to unfollow
     */
    unfollow: function (channel) {
        delete socket.fevents[channel];
        socket.send({
            action: "unfollow",
            channel: channel
        });
    }
};
/**
 * Close websocket on client quit
 */
$(window).on('beforeunload.socket', function () {
    try {
        socket.ctx.close();
    } catch (e) {
    }
});
