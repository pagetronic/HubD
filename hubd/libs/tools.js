var sys = (sys === undefined) ? {} : sys;
sys = $.extend({}, sys, {
        hellip: '&#8230;',

        /**
         * Jump to an anchor
         * @param anchor
         */
        jumpto: function (anchor, delay) {
            if (delay === undefined) {
                delay = 500;
            }
            var ele = $('#' + $.escapeSelector(anchor) + ', [name=' + $.escapeSelector(anchor) + ']').eq(0);
            sys.scrollto(ele, delay, function () {
                var dest = ele.attr('id') === anchor ? ele : ele.parent();
                dest.animate({
                    backgroundColor: 'rgba(169, 222, 166, 0.3)',
                    borderRadius: 10
                }, 400, function () {
                    dest.animate({
                        backgroundColor: 'transparent',
                        borderRadius: 0
                    }, 200);
                });
            });
        },
        /**
         * Execute a function to execute after client do other action
         * @param func to execute on blur
         */
        focus: function (func) {
            var input = $('<input />');
            input.css({width: 1, height: 1, opacity: 0.01});
            $(document.body).append(input);
            input.on('blur', function () {
                func();
                $(this).remove();
            }).focus();
        },
        uri: function () {
            return document.location.pathname + document.location.search + document.location.hash;
        },

        /**
         * pull in an array
         * @param arr array
         * @param val value to pull
         */
        pull: function (arr, val) {
            if (arr.indexOf === undefined) {
                return -1;
            }
            var index = arr.indexOf(val);
            if (index > -1) {
                arr.splice(index, 1);
            }
            return arr;
        },

        /**
         * File size of a bytes
         */
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
        /**
         * Used for make clearable an object
         */
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
        /**
         * Popup an alert box
         * @param title of the box
         * @param msg of the box
         * @param delay of scroll time //TODO pixels per millisecond
         * @param close function to execute on close.
         * @param delay if needed before autoclose
         */
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
        /**
         * Popup a confirmation box
         * @param title of the box
         * @param msg of the box
         * @param delay of scroll time //TODO pixels per millisecond
         * @param accept function to execute on accept.
         * @param reject function to execute on reject.
         */
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
        /**
         * Protect for content copiers, insert a link into clipBoard copy
         */
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
        /**
         * Try to generate an unique Id
         */
        uniqueId: function () {
            if (sys.lastId === undefined) {
                sys.lastId = 0;
            }
            sys.lastId++;
            var uniq = Math.floor(Math.random() * Math.pow(10, 15)).toString(26);
            uniq = new Date().getTime().toString(26).shuffle() + sys.lastId.toString(26) + uniq.substr(uniq.length - 8, uniq.length);
            return uniq.toUpperCase();
        },
        /**
         * Unique identification of the client
         */
        sysId: function () {
            var sysid = function () {
                return sys.uniqueId().toUpperCase() + sys.uniqueId().toUpperCase() + sys.uniqueId().toUpperCase();
            };
            if (typeof localStorage === "object") {
                if (localStorage.getItem("sysid") === null) {
                    localStorage.setItem("sysid", sysid());
                }
                Cookies.set("sysid", localStorage.getItem("sysid"), {
                    expires: 'max'
                });
                return localStorage.getItem("sysid");
            } else {
                if (Cookies.get("sysid") === null) {
                    Cookies.set("sysid", sysid(), {
                        expires: 'max'
                    });
                }
                return Cookies.get("sysid");
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
