sys.setting = {
    set: function (data) {
        if (typeof data === "string") {
            var where = data;
            data = {};
            data[where] = sys.sets[where];
        }
        if (sys.user.id) {
            socket.send({
                action: "settings",
                data: data
            }, function (rez) {
                sys.sets = rez.settings;
            });
        }
    },
    get: function (key) {
        if (sys.sets[key] === undefined) {
            return null;
        } else {
            return sys.sets[key];
        }
    },
    unpush: function (where, value) {
        if (sys.sets[where] === undefined || !settings.exist(where, value)) {
            return;
        }
        sys.sets[where].splice(sys.sets[where].indexOf(value), 1);
        settings.set(where);
    },
    push: function (where, value) {
        if (sys.sets[where] === undefined) {
            sys.sets[where] = [];
        }
        if (!settings.exist(where, value)) {
            sys.sets[where].push(value);
            settings.set(where);
        }
    },
    exist: function (where, value) {
        return (sys.sets !== undefined && sys.sets[where] !== undefined && sys.sets[where].indexOf(value) >= 0);
    }

};