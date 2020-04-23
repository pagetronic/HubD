var pop = function (doOverlay, width, height) {

    var overlay = (doOverlay === undefined) ? false : doOverlay;

    var body = $(document.body);
    var win = $(window);
    var previous_size_maximize = null;
    var previous_size_dock = null;
    var pulse_timer = -1;
    var header = $('<h2 />').css({cursor: 'move'});

    var maximize = $('<span />').html('$svg.fa_icon_window_maximize');
    var dock = $('<span />').html('$svg.fa_icon_window_minimize');
    var closer = $('<span />').html('$svg.fa_icon_close');

    var mask = $('<div class="mask"/>');

    mask.on('mousedown', function (e) {
        if (e.currentTarget !== e.target) {
            return true;
        }
        closer.trigger('click');
    });
    var popin = $('<div class="pop box" />').css({zIndex: 1001 + $('.pop').length + 1});


    popin.data('bottom', false);
    popin.fadeOut(0);
    var mano = $('<span class="mano" />');
    var addmano = $('<span class="addmano" />');
    closer.on('click', function () {
        close();
        return false;
    });
    popin.on('close', function () {
        closer.trigger('click');
    });
    var close = function (delay) {
        if (delay === undefined) {
            delay = 100;
        }
        popin.slowRemove(delay, function () {
            mask.trigger('close');
        });
    };

    mano.append(addmano);
    if (!overlay) {
        maximize.on('click', function (e) {
            if (previous_size_maximize !== null) {
                popin.removeClass("maximized");
                popin.resizable("enable");
                maximize.html('$svg.fa_icon_window_maximize');
                popin.animate(previous_size_maximize, {
                    duration: 100
                });
                previous_size_maximize = null;
                return;
            }
            popin.addClass("maximized");
            maximize.html('$svg.fa_icon_window_restore');
            previous_size_maximize = popin.position();
            previous_size_maximize.width = popin.width();
            previous_size_maximize.height = popin.height();

            popin.animate({
                top: 15,
                bottom: 15,
                left: 15,
                right: 15,
                width: '95%',
                height: '95%'
            }, 100, function () {
                popin.css({
                    width: 'auto',
                    height: 'auto'
                });

            });
            popin.resizable("disable");
        });
        dock.on('click', function () {
            if (previous_size_dock !== null) {
                if (previous_size_maximize !== null) {
                    popin.resizable("disable");
                    popin.addClass("maximized");
                } else {
                    popin.resizable("enable");
                }
                popin.animate(previous_size_dock, {
                    duration: 100,
                    complete: function () {
                        popin.css({maxHeight: '', maxWidth: ''});
                    }
                });
                previous_size_dock = null;
                dock.html('$svg.fa_icon_window_minimize');
                addmano.show();
                maximize.show();
                popin.data('bottom', false);
                clearInterval(pulse_timer);
                popin.trigger("undock");
                return;
            }
            popin.removeClass("maximized");
            dock.html('$svg.fa_icon_window_restore');
            addmano.hide();
            popin.resizable("enable");
            popin.trigger("dock");
            popin.data('bottom', true);
            previous_size_dock = popin.position();
            previous_size_dock.width = popin.width();
            previous_size_dock.height = popin.height();
            var space = 75;
            var left = 0;
            $('.pop').each(function () {
                if ($(this).data('bottom')) {
                    left += space;
                }
            });
            if (left > body.width() - space) {
                left -= body.width() - space + (space / 2);
            }
            popin.animate({
                width: 250,
                height: 23,
                maxHeight: 23,
                left: left,
                top: win.height() - 23
            }, {
                duration: 100
            }).resizable("disable");
            maximize.hide();
        });
        header.on('dblclick', function () {
            if (popin.data('bottom')) {
                dock.trigger('click');
            } else {
                maximize.trigger('click');
            }
        });
        mano.append(dock).append(maximize);
    }

    mano.append(closer);

    var title = $('<span class="title"/>');

    popin.append(header.append(title).append(mano));


    popin.on('mousedown', function () {
        var popins = $('.pop');
        var popinss = {};
        popins.each(function () {
            var ele = $(this);
            popinss[ele.css('z-index')] = ele;
        });
        popinss = Object.keys(popinss).sort().reduce(function (result, key) {
            result[key] = popinss[key];
            return result;
        }, {});
        var index;
        popins.css({zIndex: ''});
        var zi = parseInt(popins.eq(0).css('z-index'));
        for (var index in popinss) {
            popinss[index].css({zIndex: zi++});
        }
        popin.css({zIndex: zi++});
    }).trigger('mousedown');

    popin.css({position: 'fixed'}).draggable({
        handle: header,
        drag: function (event, ui) {
            if (previous_size_maximize !== null && previous_size_dock === null) {
                previous_size_maximize.top = event.pageY + 20;
                previous_size_maximize.left = event.pageX - previous_size_maximize.width / 2;
                maximize.trigger('click');
                return false;
            } else {
                ui.position.top = Math.max(Math.min(body.height() - 29, ui.position.top), 0);
                popin.data('bottom', false);
                return true;
            }
        }
    });

    header.css('cursor', 'move');
    var content = $('<div class="content" />');
    var footer = $('<div class="footer" />').hide();
    footer.visible = false;
    popin.resizable({
        minWidth: 50,
        minHeight: 50,
        resize: function (event, ui) {
            if (popin.innerHeight() < (header.outerHeight() + content.outerHeight())) {
                popin.height(header.outerHeight() + content.outerHeight());
            }
            previous_size_maximize = null;
        }
    });

    if (overlay) {

        mask.on('close', function () {
            mask.slowRemove(220);
        });
        body.append(mask.append(popin.fadeIn(100)));
        mask.fadeOut(0).fadeIn(100);
    } else {
        body.append(popin.fadeIn(100));
    }
    if (width !== undefined) {
        popin.css({width: width, maxWidth: '80%'});
        popin.css({width: popin.width(), maxWidth: ''});
    }
    if (height !== undefined) {
        popin.css({height: height, maxHeight: '80%'});
        popin.css({height: popin.height(), maxHeight: ''});
    }

    popin.append(content);
    popin.append(footer);


    popin.on('center', function () {

        if (popin.height() > win.height()) {
            popin.css('height', win.height() * 0.95);
        }
        if (popin.width() > win.width()) {
            popin.css('width', win.width() * 0.95);
        }
        var dec = 30;
        popin.css({
            top: ((win.height() - popin.height()) / 4) + (Math.random() * dec * 2 - dec),
            left: ((win.width() - popin.width()) / 2) + (Math.random() * dec * 2 - dec)
        });

    }).trigger('center');
    popin.on('open', function () {
        popin.trigger('mousedown');
        if (previous_size_dock !== null) {
            dock.trigger('click');
        }
        popin.trigger('center');
        popin.css({
            borderColor: '#ffefa2',
            boxShadow: popin.css('box-shadow') + ', 0px 0px 10px 5px rgba(255, 251, 232, 0.8)'
        });
        setTimeout(function () {
            popin.css({
                borderColor: '',
                boxShadow: ''
            });
        }, 100);
    });
    popin.on('pulse', function () {
        popin.trigger('mousedown');
        var left = parseInt(popin.css('left'));
        var top = parseInt(popin.css('top'));
        popin.animate({
            zoom: 1.2,
            left: left * 0.8,
            top: top * 0.8
        }, 200, function () {
            popin.animate({
                zoom: 1,
                left: left,
                top: top
            }, 150);
        });
        if (previous_size_dock !== null) {
            dock.trigger('click');
        }
    });


    return {
        pop: popin,
        mask: mask,
        pulse: function () {
            popin.trigger('pulse');
        },
        addMano: function (item) {
            addmano.append(item);
        },
        helps: function (helps) {
            var help = $('<span class="help" />').html('$svg.mi_help').on('click', function () {
                sys.help.view(helps);
            });
            addmano.prepend(help);
        },
        header: function (cnt) {
            if (cnt !== undefined) {
                title.html(cnt);
            }
            return title;
        },
        content: function (cnt) {
            if (cnt !== undefined) {
                content.html(cnt);
            }
            return content;

        },
        footer: function (cnt) {
            footer.show();
            footer.visible = true;
            if (cnt !== undefined) {
                footer.html(cnt);
            }
            return footer;
        },
        append: function (cnt) {
            content.append(cnt);
            return content;
        },
        loading: function (load) {
            if (load) {
                if (footer.visible) {
                    footer.hide();
                }
                popin.css({minHeight: 100});
                content.hide();
                content.after(sys.loading(60, 'div'));
            } else {
                if (footer.visible) {
                    footer.show();
                }
                popin.css({minHeight: ''});
                content.show();
                popin.find('loading, .loading').remove();
            }
        },
        width: function (width, doAfter) {
            var init = popin.width();
            var size = {
                width: width,
                maxWidth: '90%'
            };
            popin.css(size);
            size.left = popin.position().left - (popin.width() / 4);
            delete size.maxWidth;
            popin.width(init).animate(size, 150, function () {
                if(doAfter!==undefined) {
                    doAfter();
                }
                popin.css({maxWidth: ''});
            });
        },
        height: function (height, doAfter) { var init = popin.height();
            var size = {
                height: height,
                maxHeight: '90%'
            };
            popin.css(size);
            size.top = popin.position().top - (popin.height() / 4);
            delete size.maxHeight;
            popin.width(init).animate(size, 150, function () {
                if(doAfter!==undefined) {
                    doAfter();
                }
                popin.css({maxHeight: ''});
            });
        },
        close: close,
        center: function () {
            popin.trigger('center');

        },
        isOpen: function () {
            return popin.data('bottom');
        }
    }


};

pop.init = function () {
    var lockcenter = -1;
    var win = $(window);
    win.on('resize', function () {
        $('.pop').each(function () {
            var body = $('body');
            var popin = $(this);
            var header = popin.find('h2:eq(0)');
            if (popin.height() > win.height()) {
                popin.css('height', win.height() * 0.95);
            }
            if (popin.width() > win.width()) {
                popin.css('width', win.width() * 0.95);
            }
            var pos = header.position();
            if (pos.top < 0) {
                popin.css('top', 10);
            }
            if (pos.left < 0) {
                popin.css('left', 10);
            }
            if (pos.left > win.width()) {
                popin.css('left', win.width() - 30);
            }
        });
    });
};

