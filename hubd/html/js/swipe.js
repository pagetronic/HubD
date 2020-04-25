sys.swipe = {
    /**
     * Make lateral menus swipable and openable like Android Ui
     */
    make: function () {
        var menu = $('#menu');
        var menu_btn = $('.opener[to=menu]');
        var lateral = $('#lateral');
        var lateral_btn = $('.opener[to=lateral]');
        var win = $('body, html, #middle').add($(window)).css({overflow: ''});
        var box = $(window).css({overflow: ''});
        var tolerance = 8;

        var getPosX = function (e) {
            try {
                return Math.min(box.width(), Math.max(0, e.originalEvent.changedTouches[0].pageX));
            } catch (e) {
                return 0;
            }
        };

        var left = -1;
        var right = -1;

        box.off('touchstart.swipe').off("touchend.swipe touchleave.swipe touchcancel.swipe").off("touchmove.swipe");

        box.on('touchstart.swipe', function (e) {

            var posX = getPosX(e);
            if (lateral.length > 0 && lateral.hasClass('open') && posX <= tolerance) {
                right = box.width() - posX;
                return true;
            }
            if (posX <= tolerance) {
                left = posX;
            }
            if (left >= 0) {
                sys.comodo.show();
                menu.css({zIndex: 10001, left: Math.min(0, Math.max(40, left) - menu.width())});
                lateral.css({zIndex: 10000});
                return false;
            }
            return true;
        });
        box.on("touchend.swipe touchleave.swipe touchcancel.swipe", function (e) {
            var posX = getPosX(e);
            if (left < 0) {
                return;
            }
            if (posX < menu.width() * 3 / 4 && menu.hasClass('open')) {
                //   sys.comodo.show();
                menu.animate({left: -menu.width()}, 200, function () {
                    menu.removeClass('open');
                    menu.css({left: ''});
                });
            } else if (posX > (menu.width() / 4)) {
                menu.animate({left: 0}, 200, function () {
                    // sys.comodo.hide();
                    menu.addClass('open');
                    menu.css({left: ''});
                });
            } else {
                //sys.comodo.show();
                menu.animate({left: -menu.width()}, 200, function () {
                    menu.css({left: ''});
                });
            }

            left = -1;
        });
        box.on("touchmove.swipe", function (e) {
            var posX = getPosX(e);
            if (left < 0) {
                return;
            }
            left = posX;
            menu.css({left: Math.min(0, left - menu.width())});

        });

        ////


        box.on("touchstart.swipe", function (e) {
            var posX = getPosX(e);
            var x = box.width() - posX;
            if (x <= tolerance && menu.hasClass('open')) {
                left = posX;
                return true;
            }
            if (lateral.length === 0) {
                return true;
            }
            if (x <= tolerance) {
                right = x;
            }
            if (right >= 0) {
                sys.comodo.show();
                lateral.css({position: 'fixed', zIndex: 10001, right: Math.min(0, Math.max(40, right) - lateral.width())});
                menu.css({zIndex: 10000});
                return false;
            }
            return true;
        });
        box.on("touchend.swipe touchleave.swipe touchcancel.swipe", function (e) {
            var posX = getPosX(e);
            if (lateral.length === 0) {
                return;
            }
            var x = box.width() - posX;
            if (right < 0) {
                return;
            }

            if (Math.abs(x) < lateral.width() * 3 / 4 && lateral.hasClass('open')) {
                // sys.comodo.show();
                lateral.animate({position: '', right: -lateral.width()}, 200, function () {
                    lateral.removeClass('open');
                    lateral.css({right: '', position: ''});
                });
            } else if (x > (lateral.width() / 4)) {
                lateral.animate({right: 0}, 200, function () {
                    //    sys.comodo.hide();
                    lateral.addClass('open');
                    lateral.css({right: ''});
                });
            } else {
                //   sys.comodo.show();
                lateral.animate({right: -lateral.width()}, 200, function () {
                    lateral.removeClass('open').css({right: '', position: ''});
                });
            }

            right = -1;
        });
        box.on("touchmove.swipe", function (e) {
            var posX = getPosX(e);
            if (right < 0) {
                return;
            }
            right = box.width() - posX;
            lateral.css({right: Math.min(0, right - lateral.width())});
        });


        ////
        menu_btn.off('click.swipe')
            .on('click.swipe', function () {
                lateral.removeClass('open');
                sys.comodo.show();
                if (menu.hasClass('open')) {
                    menu.removeClass('open');
                    win.css({overflow: ''});
                } else {
                    menu.addClass('open');
                    win.css({overflow: 'hidden'});
                }
            });

        if (lateral.length === 0) {
            lateral_btn.removeClass('showable');
        } else {
            lateral_btn.addClass('showable');
            var timer_lateral = -1;
            lateral_btn.on('click', function () {
                clearTimeout(timer_lateral);
                menu.removeClass('open');
                sys.comodo.show();
                if (lateral.hasClass('open')) {
                    lateral.removeClass('open');
                    timer_lateral = setTimeout(function () {
                        lateral.css({position: ''});
                    }, 600);
                    win.css({overflow: ''});
                } else {
                    lateral.addClass('open');
                    lateral.css({position: 'fixed'});
                    win.css({overflow: 'hidden'});
                }
            });
        }
    }
};