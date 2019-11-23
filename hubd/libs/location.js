var sys = (sys === undefined) ? {} : sys;
sys = $.extend({}, sys, {
    replaceState: function (url, isAjax) {
        var state = {
            ajax: isAjax === undefined ? history.state !== undefined && history.state !== null && history.state.ajax !== undefined ? history.state.ajax : false : isAjax
        };
        var center = $('#center');
        if (center.length > 0) {
            state.scroll = center.length > 0 ? center[0].scrollTop / center[0].scrollHeight : 0;
        }
        history.replaceState(state, document.title, url);
    },
    pushState: function (url, isAjax) {
        history.pushState({
            ajax: isAjax === undefined ? true : isAjax
        }, document.title, url);
    },

    correctParameter: function (param, uri, replace) {
        var parameters = sys.getAllParameters(uri);
        var query = [];
        var did = false;
        for (var key in parameters) {
            if ((param + '&').startsWith(key)) {
                query.push(param);
                did = true;
            } else {
                query.push(key + '=' + parameters[key][0]);
            }
        }
        if (!did) {
            query.push(param);
        }
        sys.replaceState('?' + query.join('&'));

    },
    getParameter: function (key, uri) {
        var regex = new RegExp("[\\?&]" + key + "=([^&#]*)");
        var results = regex.exec(uri === undefined ? document.location.search : uri);
        return results === null ? null : decodeURIComponent(results[1]);
    },
    getAllParameters: function (uri) {

        var vars = (uri === undefined ? document.location.search : uri).replace(/.*?\?(.*)/, "$1").split(/&(amp;)?/);

        var rez = {};
        $.each(vars, function (i, variable) {
            if (variable === undefined || variable === "") {
                return;
            }
            var ele = variable.split("=");
            if (rez[ele[0]] === undefined) {
                rez[ele[0]] = [];
            }
            rez[ele[0]].push(ele[1]);
        });
        return rez;
    },

    cleanurl: function (title) {

        title = title.toLowerCase();
        var find = "/àáâãäåòóôõöøèéêëçìíîïùúûüÿñ’+".split("");
        var replace = "-aaaaaaooooooeeeeciiiiuuuuyn-'".split("");
        for (var i = 0; i < find.length; i++) {
            title = title.replaceAll(find[i], replace[i]);
        }
        return title.replaceAll("œ", "oe").replaceAll("'", "-").replaceAll('"', "-").replaceAll(/[ ]+/, "-").replaceAll(/([\-])+/, "-");
    }

});
