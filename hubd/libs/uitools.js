var sys = (sys === undefined) ? {} : sys;
sys = $.extend({}, sys, {
        /**
         * Wait effect for ui
         */
        wait: function (activate) {
            var window = $('#window');
            if (activate) {
                sys.dynamit();
                window.stop(false).fadeTo(200, 0.5);
            } else {
                window.stop(false).fadeTo(100, 1, function () {
                    $(this).css({
                        opacity: ''
                    })
                });
                sys.dynamit(true);
            }
        },

        /**
         * Loading effect for ajax loading
         */
        dynamit: function (finish) {
            var dynamit = $('#dynamit');
            if (finish) {
                dynamit.stop(false).animate({
                    width: '100%'
                }, {
                    duration: 200
                });
                dynamit.delay(100).slowRemove(200);
                return;
            }
            dynamit.remove();
            dynamit = $('<div id="dynamit"/>');
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
        /**
         * Button for jumping to top
         */
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

        /**
         * Toast effect like Android UI
         * @param msg Text/Html to display in toast view
         * @param delay to display in milliseconds
         */
        toast: function (msg, delay) {
            if (delay === undefined) {
                delay = 1200;
            }
            var body = $(document.body);
            var box = $('<div />').css({
                position: 'fixed',
                zIndex: 999999,
                left: 0,
                right: 0,
                textAlign: 'center'
            })
            var toast = $('<div class="toast"/>').html(msg);
            body.append(box.append(toast));
            box.css({
                bottom: ($(window).outerHeight() - toast.outerHeight()) / 6
            });
            toast.css({
                transition: 'transform ' + Math.round(delay / 8) + 'ms linear',
                transform: 'scale(1)'
            }).animate({
                opacity: 1
            }, Math.round(delay / 8), function () {
                setTimeout(function () {
                    toast.css({
                        transition: 'transform ' + Math.round(delay / 4) + 'ms linear'
                    });
                    toast.css({transform: 'scale(3)'}).animate({
                        opacity: 0
                    }, Math.round(delay / 4), function () {
                        toast.remove();
                    });
                }, Math.round(delay * 5 / 8));
            });
        },
        /**
         * Get the scrolling div
         */
        scroller: function () {
            var scroller = $('#middle');
            if (scroller.scrollParent().is(document.body)) {
                return $(window);
            }
            return scroller;
        },
        /**
         * Used for scroll principal content
         * @param ele jQuery or pixels from top
         * @param delay Delay of scroll time //TODO pixels per millisecond
         * @param after execute the function scroll completed
         */
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
            var scroller = sys.scroller();
            scroller.scrollTo(ele, delay, function () {
                after();
                scroller.trigger('scroll');
            });
            scroller.trigger('scroll');
        }
    }
);

