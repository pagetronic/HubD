sys.prefs = function (key, value) {
    if (sys.user.id === null) {
        alert(lang.get('PLEASE_LOGIN'));
        return;
    }
    if (typeof value !== "string") {
        value = JSON.stringify(value);
    }
    var data = {
        action: "prefs",
        data: {}
    };
    data.data[key] = eval(value);
    socket.send(data);
}