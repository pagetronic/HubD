var lang = {};
lang.get = function () {
    var lng = sys.lng;
    var lngs = sys.lng.split("_");
    var country = null;
    if (lngs.length > 1) {
        lng = lngs[0];
        country = lngs[1];
    }
    if (lang[arguments[0]] !== undefined && (lang[arguments[0]][country] !== undefined || lang[arguments[0]][lng] !== undefined)) {
        var item = lang[arguments[0]][country];
        if (item === undefined) {
            item = lang[arguments[0]][lng];
        }
        for (var i = 1; i < arguments.length; i++) {
            item = item.replaceAll('%' + i, arguments[i]);
        }
        return item;
    }
    try {
        log('Lang Js empty ' + arguments[0]);
        sysLog('Lang Js empty ' + arguments[0]);
    } catch (e) {
    }
    return '$' + arguments[0];
};
lang.exist = function () {
    return lang[arguments[0]] !== undefined && lang[arguments[0]][sys.lng] !== undefined;
};