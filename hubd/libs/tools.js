var sys = (sys === undefined) ? {} : sys;
sys = $.extend({}, sys, {
        itsie: function () {
            var ua = window.navigator.userAgent;
            if (ua.indexOf('Trident/') > 0 || ua.indexOf('MSIE ') > 0) {
                return true;
            } else {
                return false;
            }

        },
        hellip: '&#8230;',
        uri: function () {
            return document.location.pathname + document.location.search + document.location.hash;
        },
        focus: function (func) {
            var input = $('<input />')
            input.css({width: 1, height: 1, opacity: 0.01});
            $(document.body).append(input);
            input.on('blur', function () {
                func();
                $(this).remove();
            }).focus();
        },
        wait: function (activate) {
            var body = $(document.body);
            if (activate) {
                sys.dynamit();
                body.stop(false).fadeTo(200, 0.5);
            } else {
                body.stop(false).fadeTo(100, 1, function () {
                    $(this).css({
                        opacity: ''
                    })
                });
                sys.dynamit(true);
            }
        },
        dynamit: function (finish) {
            if (finish) {
                var dynamit = $('#dynamit');
                dynamit.stop(false).animate({
                    width: '100%'
                }, {
                    duration: 200
                });
                dynamit.delay(100).slowRemove(200);
            } else {
                $('#dynamit').remove();
                var dynamit = $('<div id="dynamit"/>');
                $(document.body).append(dynamit);
                dynamit.animate({
                    width: '30%'
                }, {
                    duration: 500,
                    easing: 'linear',
                    complete: function () {
                        dynamit.animate({
                            width: '70%',
                        }, {
                            duration: 3000,
                            easing: 'linear',
                            complete: function () {
                                dynamit.animate({
                                    width: '80%'
                                }, {
                                    duration: 8000,
                                    easing: 'linear',
                                    complete: function () {
                                        dynamit.animate({
                                            width: '95%'
                                        }, {
                                            duration: 10000,
                                            easing: 'linear'
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }

        },
        quickmenu: function (position, actions) {
            var appener;
            if (position.pageY === undefined) {
                appener = $('#center');
            } else {
                appener = $(document.body).eq(0);
            }
            var ul = $('<ul class="quick quickmenu"/>').css({marginTop: -2, marginLeft: -2});
            for (var key in actions) {
                var li = $('<li/>');
                li.html(lang.get(key));
                li.click(function () {
                    var key = $(this).data('key');
                    ul.slowRemove(100);
                    actions[key]();
                });
                li.data('key', key);
                li.addClass(key.toLowerCase());
                ul.append(li);
            }
            if (position.pageY === undefined) {
                var pos = position.offset();
                var cor = appener.offset();
                position = {
                    top: pos.top - cor.top + appener[0].scrollTop + position.height(),
                    left: pos.left - cor.left + appener[0].scrollLeft + position.width()
                };
            } else {
                position = {top: position.pageY, left: position.pageX}
            }
            ul.css({
                top: position.top,
                left: position.left
            });
            appener.append(ul);
            ul.fadeOut(0).fadeIn(100);
            var uniq = sys.uniqueId();
            var down_mouse;
            $(document.body).on('mouseup.' + uniq + ' mousedown.' + uniq + '', function (e) {
                if (e.type === "mousedown") {
                    down_mouse = e.target;
                } else if (ul.has(down_mouse).length === 0 && ul.has(e.target).length === 0) {
                    ul.slowRemove(100);
                    $(document.body).off('mouseup.' + uniq + ' mousedown.' + uniq);
                }
            });

        },
        file_size: function (size) {
            var i = Math.floor(Math.log(size) / Math.log(1024));
            return !size && '0 Bytes' || (size / Math.pow(1024, i)).toFixed(2) + " " + ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'][i]
        },

        commands: function () {
            var center = $('#center').off('scroll.commands');
            var scrl = center.css({overflow: 'hidden'}).innerHeight() - center.css({overflow: ''}).innerHeight();
            var init_scroll = center[0].scrollTop;
            var floaters = $('#top_float, #bottom_float');
            if (floaters.length === 0) {
                return;
            }
            floaters.removeClass('fixed').removeAttr('style');

            var bottom_float = $('#bottom_float');
            var top_float = $('#top_float');
            var edit = $('#edit');
            $('.cale').remove();
            var edit_height = 0;
            if (edit.length > 0 && edit.css('display') !== 'none') {
                edit_height = edit.outerHeight() + parseInt(top_float.css('padding-bottom')) + (parseInt(edit.css('margin-top')) / 2);
            }
            var center_outer = center.outerHeight();
            var center_pos = center.position();
            var commands_height = (top_float.length === 0) ? 0 : top_float.outerHeight() - edit_height;
            var validation_height = (bottom_float.length === 0) ? 0 : bottom_float.outerHeight();


            if (floaters.length > 0 && center[0].scrollHeight > center_outer && (center_outer - commands_height - validation_height) > Math.max(commands_height, validation_height)) {

                if (top_float.length > 0) {
                    top_float.css({
                        top: center_pos.top,
                        left: center_pos.left,
                    });
                    center.prepend($('<div class="cale"/>').css({
                        height: commands_height + edit_height
                    }));
                }
                if (bottom_float.length > 0) {
                    bottom_float.css({
                        top: center_pos.top + center_outer + scrl,
                        left: center_pos.left
                    });
                    center.append($('<div class="cale"/>').css({
                        height: validation_height
                    }));
                }

            } else {
                return;
            }
            floaters.addClass('fixed');

            center.scrollTop(init_scroll);
            var last_position = init_scroll;
            if (sys.commands.to_bottom === undefined || last_position === 0) {
                sys.commands.to_bottom = false;
            }
            if (sys.commands.to_bottom) {
                top_float.css({
                    top: center_pos.top - commands_height
                });
                bottom_float.css({
                    top: center_pos.top + center_outer - validation_height
                });
            }
            center.on('scroll.commands', function () {

                if (this.scrollTop > last_position && this.scrollTop > commands_height) {
                    if (!sys.commands.to_bottom) {
                        top_float.stop().animate({
                            top: center_pos.top - commands_height
                        }, 150);
                        bottom_float.stop().animate({
                            top: center_pos.top + center_outer - validation_height
                        }, 150);
                    }
                    sys.commands.to_bottom = true;
                }
                if (this.scrollTop < last_position && this.scrollTop + center_outer < this.scrollHeight - validation_height) {
                    if (sys.commands.to_bottom) {
                        top_float.stop().animate({
                            top: center_pos.top
                        }, 150);
                        bottom_float.stop().animate({
                            top: center_pos.top + center_outer + scrl
                        }, 150);
                    }
                    sys.commands.to_bottom = false;
                }

                last_position = this.scrollTop;

            }).trigger('scroll');
        },
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
        uniqueId: function () {
            if (sys.lastId === undefined) {
                sys.lastId = 0;
            }
            sys.lastId++;
            var uniq = Math.floor(Math.random() * Math.pow(10, 15)).toString(26);
            uniq = new Date().getTime().toString(26).shuffle() + sys.lastId.toString(26) + uniq.substr(uniq.length - 8, uniq.length);
            return uniq.toUpperCase();
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
        loading: function (size, type) {

            var loading = $('<' + (type === undefined ? 'loading' : type) + ' />');
            loading.addClass('loading');
            var increment = -(size + 2);
            loading.css({
                height: size,
                width: size
            });


            var current_image = 0;
            var animate = setInterval(function () {
                if (loading === null || loading.length === 0 || loading.width() === 0 || $(document.body).has(loading).length === 0) {
                    clearInterval(animate);
                    return;
                }
                loading.css({
                    backgroundPosition: '0px ' + (current_image * increment) + 'px',
                });

                current_image++;
                if (current_image >= 7) {
                    current_image = 0;
                }

            }, 100);

            return loading;
        },
        cleanurl: function (title) {

            title = title.toLowerCase();
            var find = "/àáâãäåòóôõöøèéêëçìíîïùúûüÿñ’+".split("");
            var replace = "-aaaaaaooooooeeeeciiiiuuuuyn-'".split("");
            for (var i = 0; i < find.length; i++) {
                title = title.replaceAll(find[i], replace[i]);
            }
            return title.replaceAll("œ", "oe").replaceAll("'", "-").replaceAll('"', "-").replaceAll(/[ ]+/, "-").replaceAll(/([\-])+/, "-");
        },
        pull: function (arr, val) {
            if ( arr.indexOf ===  undefined) {
                return -1;
            }
            var index = arr.indexOf(val);
            if (index > -1) {
                arr.splice(index, 1);
            }
            return arr;
        },
        fileSize: function (bytes) {
            var thresh = 1024;
            if (Math.abs(bytes) < thresh) {
                return bytes + 'o';
            }
            var units = ['Ko', 'Mo', 'Go', 'To'];
            var u = -1;
            do {
                bytes /= thresh;
                ++u;
            } while (Math.abs(bytes) >= thresh && u < units.length - 1);
            return bytes.toFixed(1).toString().replace(/\.00$/, '') + '' + units[u];
        },
        sysId: function () {
            var sysid = function () {
                return sys.uniqueId().toUpperCase() + sys.uniqueId().toUpperCase() + sys.uniqueId().toUpperCase();
            };
            if (typeof localStorage === "object") {
                if (localStorage.getItem("sysid") === null) {
                    localStorage.setItem("sysid", sysid());
                }
                Cookies.set("sysid", localStorage.getItem("sysid"), {
                    expires: 'max',
                    samesite: 'none'
                });
                return localStorage.getItem("sysid");
            } else {
                if (Cookies.get("sysid") === null) {
                    Cookies.set("sysid", sysid(), {
                        expires: 'max',
                        samesite: 'none'
                    });
                }
                return Cookies.get("sysid");
            }

        },
        clearable: function () {
            $('[clearable]').each(function () {

                var input = $(this)
                var clearable = $('<span class="clearable" />');
                var clearer = $('<i/>').html('$svg.mi_clear');
                clearable.append(clearer);
                input.after(clearable);
                clearable.prepend(input);
                clearer.on('click', function () {
                    input.val('');
                    input.trigger('change').trigger('input');
                });
                input.on('input', function () {
                    if (this.value !== '') {
                        clearer.show();
                    } else {
                        clearer.hide();
                    }
                });
                if (this.value !== '') {
                    clearer.show();
                } else {
                    clearer.hide();
                }

            })
        },
        alert: function (msg, title, close, delay) {
            if (typeof title === 'number') {
                var delay = title;
                title = undefined;
            }
            if (msg === undefined) {
                return alert('<h3>' + lang.get('ERROR_UNKNOWN') + '</h3>', lang.get('ERROR'));
            } else if (lang.exist(msg)) {
                msg = lang.get(msg);
            }
            title = (title === undefined) ? lang.get('INFO') : title;

            msg = $('<p />').html(msg);

            var popper = pop(true, 400);
            popper.pop.addClass('alert');
            popper.header('$svg.fa_icon_exclamation_triangle' + title);
            popper.content($('<div class="selectable"/>').append(msg));
            popper.center();
            if (delay !== undefined) {
                var cd = $('<h3 />').css({textAlign: 'center'}).html(delay);
                msg.after(cd);
                setInterval(function () {
                    delay--;
                    if (delay <= 0) {
                        popper.close();
                    } else {
                        cd.html(Math.round(delay));
                    }
                }, 1000);
            }

            if (close !== undefined) {
                popper.pop.on('close', close);
            }
            return popper.pop;
        },
        confirm: function (title, msg, delay, accept, reject) {
            if (typeof title === 'function') {
                accept = title;
            } else if (accept === undefined) {
                accept = function () {
                };
            }
            if (title === null || typeof title === 'function') {
                title = lang.get("CONFIRMATION");
            }
            if (delay === undefined) {
                delay = 3;
            }
            if (reject === undefined) {
                reject = function () {
                };
            }
            if (typeof msg === 'string' && !msg.match('<')) {
                msg = '<p style="margin: 10px 0px">' + msg + '</p>';
            }
            var timer = -1;
            var chooz = $('<div/>');
            chooz.append(msg);
            var popper = pop(true);
            popper.pop.addClass('alert');
            popper.content(chooz);
            popper.header('$svg.fa_icon_question_circle_o' + title);
            popper.mask.one('close', function () {
                clearInterval(timer);
            });
            var accepter = $('<button class="flexable"/>').html('$svg.mi_done' + lang.get('ACCEPT'));
            var refuser = $('<button class="flexable"/>').html('$svg.mi_clear' + lang.get('REFUSE'));

            chooz.append($('<div class="action center flexo"/>').append(accepter).append(refuser));

            if (!sys.isMobile()) {
                var keyboard = $('<input type="text" class="focus"/>').css({
                    height: 1,
                    width: 1,
                    opacity: 0,
                    position: 'fixed',
                    top: -200
                });
                chooz.append(keyboard);
                keyboard.on('keyup keydown', function (e) {
                    if (e.keyCode === 13 || e.keyCode === 32) {
                        accepter.addClass('active').trigger('click');
                    }
                    if (e.keyCode === 27 || e.keyCode === 8 || e.keyCode === 46) {
                        refuser.addClass('active').trigger('click');
                    }
                });
                keyboard.focus();
            } else {
                accepter.focus();
            }


            accepter.one('click', function () {
                accept();
                popper.mask.trigger('close');
                clearInterval(timer);
            });
            refuser.one('click', function () {
                reject();
                popper.mask.trigger('close');
            });
            if (delay >= 0) {
                var obj_delay = $('<span/>').html(' (' + delay + ')');
                accepter.append(obj_delay);
                timer = setInterval(function () {
                    delay--;
                    obj_delay.html(' (' + delay + ')');
                    if (delay === 0) {
                        accepter.trigger('click');
                    }
                }, 1000);
            }
            popper.center();
            return popper;
        },
        isMobile: function () {
            if (navigator.userAgent.match(/Android/i)
                || navigator.userAgent.match(/webOS/i)
                || navigator.userAgent.match(/iPhone/i)
                || navigator.userAgent.match(/iPad/i)
                || navigator.userAgent.match(/iPod/i)
                || navigator.userAgent.match(/BlackBerry/i)
                || navigator.userAgent.match(/Windows Phone/i)
            ) {
                return true;
            } else {
                return false;
            }
        },
        toast: function (msg, delay) {
            if (delay === undefined) {
                delay = 1200;
            }
            var body = $(document.body);
            var toast = $('<div class="toast"/>').html(msg);
            body.append(toast);
            toast.css({
                left: (body.innerWidth() - toast.outerWidth()) / 2,
                bottom: (body.innerHeight() - toast.outerHeight()) / 4
            });
            toast.css({
                transition: 'transform ' + parseInt(delay / 8) + 'ms linear'
            });
            toast.css({transform: 'scale(1)'}).animate({
                opacity: 1
            }, parseInt(delay / 8), function () {
                setTimeout(function () {
                    toast.css({
                        transition: 'transform ' + parseInt(delay / 4) + 'ms linear'
                    });
                    toast.css({transform: 'scale(3)'}).animate({
                        opacity: 0
                    }, parseInt(delay / 4), function () {
                        toast.remove();
                    });
                }, parseInt(delay * 5 / 8));
            });
        },
        scrollto: function (ele, delay, after) {
            if (delay === undefined) {
                delay = 500;
            }
            if (after === undefined) {
                after = function () {
                    if (document.location.hash) {
                        history.pushState({}, document.location.title, document.location.href);
                    }
                };
            }
            var scroller = $('#middle');
            if (scroller.scrollParent().is(document.body)) {
                $(window).scrollTo(ele, delay, after);
            } else {
                scroller.scrollTo(ele, delay, after);
            }

        },
        scrolltop: function () {

            $(window).off('resize.gotop').on('resize.gotop', sys.scrolltop);

            $('#gotop').remove();
            var gotop = $('<a id="gotop" href="#">$svg.fa_icon_arrow_circle_up</a>');
            $(document.body).append(gotop);

            var scroller = $('#middle');
            var left = scroller.offset().left;
            if (!scroller.isScrollable()) {
                scroller = $(window);
                left = 0;
            }
            scroller.off('scroll.gotop').on('scroll.gotop', function (e) {
                if (this.scrollTop > window.innerHeight || window.scrollY > window.innerHeight) {
                    gotop.fadeIn(500);
                } else {
                    gotop.fadeOut(500);
                }
            }).trigger('scroll');

            gotop.css({
                left: left + scroller.outerWidth() - (gotop.outerWidth() + 20)
            }).off('click.gotop').on('click.gotop', function () {
                if (document.location.hash !== '') {
                    history.pushState({}, document.location.title, document.location.pathname);
                }
                scroller.scrollTo(0, 500);
                return false;
            });
        },
        nocopy: function () {

            if (sys.user.id !== null) {
                return;
            }
            var article = document.getElementById('article');
            if (article !== null) {
                article.addEventListener('copy', function (e) {
                    try {
                        var selection = window.getSelection();
                        e.clipboardData.setData('text/plain', $('<div/>').html(selection + "").text() + "\n\n" + 'Source: ' + document.location.href);
                        e.clipboardData.setData('text/html', selection + '<br /><br />Source: <a href="' + document.location.href + '">' + document.title + '</a>');
                        e.preventDefault();
                    } catch (err) {
                    }
                });
            }
        },
        device: function () {

            var device = navigator.appName;
            var ua = navigator.userAgent.toLowerCase();
            if (ua.indexOf("Edge".toLowerCase()) >= 0) {
                device = "Edge";
            } else if (ua.indexOf("Opera".toLowerCase()) >= 0) {
                device = "Opera";
            } else if (ua.indexOf("Chromium".toLowerCase()) >= 0) {
                device = "Chromium";
            } else if (ua.indexOf("Chrome".toLowerCase()) >= 0) {
                device = "Chrome";
            } else if (ua.indexOf("Safari".toLowerCase()) >= 0) {
                device = "Safari";
            } else if (ua.indexOf("Firefox".toLowerCase()) >= 0) {
                device = "Firefox";
            } else if (ua.indexOf("MSIE".toLowerCase()) >= 0) {
                device = "Internet Explorer";
            } else if (device === undefined || device === null) {
                device = "Unknown";
            }

            var os = navigator.platform;
            if (ua.indexOf("Windows Phone".toLowerCase()) >= 0) {
                os = "Windows Mobile";
            } else if (ua.indexOf("Windows".toLowerCase()) >= 0) {
                os = "Windows";
            } else if (ua.indexOf("Android".toLowerCase()) >= 0) {
                os = "Android";
            } else if (ua.indexOf("iPhone".toLowerCase()) >= 0 || ua.indexOf("iPad") >= 0) {
                os = "Apple iOs";
            } else if (ua.indexOf("MacOS".toLowerCase()) >= 0 || ua.indexOf("Mac OS") >= 0 || ua.indexOf("Macintosh") >= 0) {
                os = "Apple Macintosh";
            } else if (ua.indexOf("Ubuntu".toLowerCase()) >= 0) {
                os = "Ubuntu Linux";
            } else if (ua.indexOf("Debian".toLowerCase()) >= 0) {
                os = "Debian Linux";
            } else if (ua.indexOf("Linux".toLowerCase()) >= 0) {
                os = "Linux";
            } else if (ua.indexOf("Firefox".toLowerCase()) >= 0) {
                os = "Firefox";
            } else if (os === undefined || os === null) {
                os = "Unknown";
            }

            return {
                device: device,
                os: os
            }
        }
    }
);


window.alert = sys.alert;

window.confirm = sys.confirm;


var xhr = {
    abort: function () {
    }
};
var log = function (ele) {
    try {
        console.log(ele);
    } catch (e) {
    }
};

var sysLog = function (msg) {
    try {
        if (sys.user.admin) {
            api.post('/admin', {action: 'sysLog', sysLog: msg});
        }
    } catch (e) {
    }
};
