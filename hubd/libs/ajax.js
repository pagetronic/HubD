var ajax = {
    /**
     * Init Ajax watch back button
     */
    init: function () {

        ajax.previous = document.location.pathname + document.location.search;
        if (!settings.get('noajax')) {
            history.replaceState({}, document.title);
        }
        $(window).on('popstate.ajax', function (e) {
            if (settings.get('noajax')) {
                return true;
            }

            history.replaceState({scroll: sys.scroller()[0].scrollTop}, document.title);

            var after = function () {
                ajax.previous = document.location.pathname + document.location.search;
                sys.wait(false);
                var hash = document.location.hash !== '' ? document.location.hash.substr(1) : null;
                if (hash !== null) {
                    sys.jumpto(hash);
                } else {
                    sys.scrollto(e.originalEvent.state.scroll === undefined ? 0 : e.originalEvent.state.scroll, 500);
                }
            };
            if (ajax.previous !== document.location.pathname + document.location.search) {
                sys.wait(true);
                ajax.load(sys.uri(), true, undefined, after);
            } else {
                after();
            }

        });

    },

    /**
     * Make links ajax clickable if possible
     */
    it: function (where) {
        if (where === undefined) {
            where = $('#menu, header, #center');
        }
        where.find("a[href][noajax]:not([onclick])").off('click.noajax').on('click.noajax', function (e) {
            if ($(this).attr('target') !== '_blank' && !$(this).attr('href').startsWith("/")) {
                sys.wait(true);
            }
            return true;
        });

        where.find("a[href]:not([noajax]):not([onclick]):not([href^=\\/files\\/]):not([href^=http])").off('click.ajax').on('click.ajax', function (e) {
            if (e.altKey || e.ctrlKey || e.shiftKey || settings.get('noajax')) {
                return true;
            }
            e.preventDefault();
            e.stopPropagation();
            if ($(this).attr('noajax')) {
                return false;
            }

            var href = $(this).attr('href');
            if (href.startsWith('#')) {
                history.pushState({}, document.title, href);
                sys.jumpto(href.substr(1));
            } else {
                history.replaceState({scroll: sys.scroller()[0].scrollTop}, document.title);
                ajax.load(href);
            }
            return false;
        });
    }
    ,
    /**
     * Get url
     *
     * @param url request to get
     * @param func function to execute on success
     * @param error function to execute on error
     */
    get: function (url, func, error) {

        var headers = {
            'X-Requested-With': 'XMLHttpRequest'
        };
        $.ajax({
            url: url,
            headers: headers,
            success: func,
            error: error
        });
    }
    ,
    /**
     * Functions to execute on new ajax request, like "on quit"
     */
    unload: function (func) {
        if (ajax.unload_funcs === undefined) {
            ajax.unload_funcs = [];
        }
        if (func === undefined) {
            $.each(ajax.unload_funcs, function () {
                this();
            });
            ajax.unload_funcs = [];
        } else {
            ajax.unload_funcs.push(func);
        }
    }
    ,
    /**
     * Reload current page by Ajax
     *
     * @param silent mode if true
     * @param after function to execute
     */
    reload: function (silent, after) {
        ajax.load(sys.uri(), (silent !== undefined && silent), undefined, function () {
            if (typeof after === 'function') {
                after();
            }
        });
    }
    ,
    /**
     * Reload page by Ajax
     *
     * @param url of the page
     * @param silent mode if true
     * @param success function to execute on success
     * @param after function to execute on finish
     */
    load: function (url, silent, success, after) {
        ajax.referer = url;
        silent = silent === undefined ? false : silent;
        if (!silent) {
            ajax.previous = url;
        }
        if (success === undefined || success === null) {
            success = function (html) {

                if (xhr.getResponseHeader('X-Title')) {
                    document.title = $('<span />').html(decodeURIComponent(xhr.getResponseHeader('X-Title')).replace(/\+/g, ' ')).text();
                }
                ajax.unload();
                if (!silent) {
                    try {
                        var canonical = xhr.getAllResponseHeaders().match(/Link: ?<([^>]+)>; ?rel="?canonical"?/mi);
                        if (canonical !== null && canonical.length > 0) {
                            var hash = '';
                            if (url.indexOf('#') >= 0) {
                                hash = '#' + url.split('#')[1];
                            }
                            url = canonical[0].replace(/Link: ?<([^>]+)>; ?rel="?canonical"?/i, '$1') + hash;
                        }
                    } catch (e) {
                        log(e);
                    }
                    history.replaceState({}, document.title, url);
                }

                var body = $(html);

                $('#center').html(body.find('#center').html());

                var header = $('header');
                if (header.length > 0) {
                    header.find('h1').html(body.find('header h1').html());
                    header.attr('style', body.find('header').attr('style'));
                }
                if (xhr.getResponseHeader('X-Speed')) {
                    sys.load(xhr.getResponseHeader('X-Speed').replace('ms', ''));
                } else {
                    sys.load(-1);
                }

                if (!silent) {
                    if (url.indexOf('#') >= 0) {
                        var jump = url.split('#')[1];
                        sys.jumpto(jump, 0);
                    } else {
                        sys.scrollto(0, 0);
                    }
                }

                sys.wait(false);

                if (after !== undefined) {
                    after();
                }

            }
        }

        xhr.abort();
        if (!silent) {
            sys.wait(true);
            history.pushState({}, document.title, document.location.href);
        }
        $('#menu').trigger('ajax');
        xhr = $.ajax({
            url: url,
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            },
            success: success,
            error: function (e) {
                if (e.status === 404) {
                    success(e.responseText);
                } else {
                    document.location.href = url;
                }
            }
        });
    }
};
