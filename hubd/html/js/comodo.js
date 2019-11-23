sys.comodo = {
    init: function () {

        var lasttop = $(window).scrollTop();
        var comodo = $('#comodo, #menu, #lateral');
        $(window).on('scroll', function () {
            if (comodo.hasClass('open')) {
                return;
            }
            var st = $(this).scrollTop();
            if (st < (lasttop - 10) || st > (lasttop + 10)) {
                if (st > 40 && st > lasttop) {
                    comodo.addClass('hider');
                } else {
                    comodo.removeClass('hider');
                }
            }
            lasttop = st;
        });

    },
    hide: function () {
        $('#comodo, #menu, #lateral').addClass('hider');
    },
    show: function () {
        $('#comodo, #menu, #lateral').removeClass('hider');
    }
};