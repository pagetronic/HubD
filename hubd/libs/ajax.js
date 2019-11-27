var ajax = {
    init: function () {
        //TODO save page in history, is not necessary to reload or just for crontrol
        $(window).on('popstate.ajax', function (event) {
            event = event.originalEvent;
            if (!$(document.body).hasClass('hide_loading')) {
                if (event !== undefined && event.state !== undefined && event.state !== null && event.state.ajax === true) {
                    sys.wait(true);
                    ajax.load(sys.uri(), true, undefined, function () {
                        sys.wait(false);
                    }, event.state.scroll);
                    return;
                }
                if (document.location.hash.match("#.*")) {
                    var center = $('#center');
                    center.animate({
                        scrollTop: $(document.location.hash).position().top - center.height() / 2
                    }, 300);
                    return;
                }

            }
            document.location.href = sys.uri();

        });
        if (!settings.get('noajax')) {
            sys.replaceState(document.location.href, true);
        }

    },
    hide: function () {
        var center = $('#center');
        history.pushState({
            ajax: false
        }, document.title, document.location.href);
        $(document.body).addClass('hide_loading').html(sys.loading(50).css({marginTop: 200}));

    },
    it: function (where) {
        if (where === undefined) {
            where = $('#menu, header, #center');
        }
        where.find("a[href][noajax]:not([onclick])").off('click.noajax').on('click.noajax', function (e) {
            if ($(this).attr('target') !== '_blank' && !$(this).attr('href').startsWith("/")) {
                ajax.hide();
            }
            return true;
        });

        where.find("a[href]:not([noajax]):not([onclick]):not([href^=\\/files\\/]):not([href^=http])").off('click.ajax').on('click.ajax', function (e) {
            if (e.ctrlKey || e.shiftKey || settings.get('noajax')) {
                return true;
            }
            e.preventDefault();
            e.stopPropagation();
            if ($(this).attr('noajax')) {
                return false;
            }

            var href = $(this).attr('href');
            if (href.startsWith('#')) {
                var dest = $(href);
                sys.scrollto(dest, 300, function () {
                    dest.pulse();
                });
            } else {
                ajax.load(href);
            }
            return false;
        });
    },
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
    },
    unload_funcs: [],
    unload: function (func) {
        if (func === undefined) {
            $.each(ajax.unload_funcs, function () {
                this();
            });
            ajax.unload_funcs = [];

        } else {
            ajax.unload_funcs.push(func);
        }
    },
    reload: function (silent, after) {
        ajax.load(sys.uri(), (silent !== undefined && silent), undefined, function () {
            if (typeof after === 'function') {
                after();
            }
        });
    },
    load: function (url, silent, success, after, scroll) {
        silent = silent === undefined ? false : silent;
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
                            url = canonical[0].replace(/Link: ?<([^>]+)>; ?rel="?canonical"?/i, '$1');
                        }
                    } catch (e) {
                        log(e);
                    }
                    sys.pushState(url);
                }

                var body = $(html);
                var center = $('#center');
                $('#menu').html(body.find('#menu').html());
                center.html(body.find('#center').html());
                $('header h1').html(body.find('header h1').html());
                $('header').attr('style', body.find('header').attr('style'));
                sys.load(xhr.getResponseHeader('X-Speed').replace('ms', ''));

                sys.wait(false);

                if (scroll !== undefined) {
                    sys.scrollto(scroll * center[0].scrollHeight, 300);
                } else if (!silent) {
                    sys.scrollto(0, 100);
                }

                if (after !== undefined) {
                    after();
                }

            }


        }

        xhr.abort();
        if (!silent) {
            sys.wait(true);
            sys.replaceState(document.location.href);
        }
        $('#menu').trigger('ajax');
        xhr = $.ajax({
            url: url,
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            },
            success: success,
            error: function () {
                document.location.href = url;
            }
        });
    }
};
