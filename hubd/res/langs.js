var lang = {};
lang.get = function () {
    if (lang[arguments[0]] !== undefined && lang[arguments[0]][sys.lng] !== undefined) {
        var item = lang[arguments[0]][sys.lng];
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