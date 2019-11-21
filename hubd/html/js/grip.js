sys.grip = {
    init: function () {
        var menu = $('#menu');
        var cmd = $('#cmd');
        var view = $('#view');
        var center = $('#middle');
        var lat = $('#lat');
        $('#glip, #grip').css('display', 'block');
        var glip = $('<div id="glip"/>');
        var grip = $('<div id="grip"/>');
        menu.after(glip);
        lat.before(grip);
        glip.css('left', cmd.width() + 4);
        var comodo = $('#comodo');

        glip.draggable({
            axis: "x",
            addClasses: false,
            drag: function (e, ui) {
                var w = e.pageX - 2;
                if (w < 60) {
                    w = 45;
                    menu.addClass('ico');
                } else {
                    if (w > 300) {
                        w = 300;
                    }
                    menu.removeClass('ico');
                }
                cmd.css('width', w);
                view.css('left', w + 4);
                comodo.css('left', w + 4);
            },
            create: function (e, ui) {
                glip.removeClass('ui-draggable-handle');
                glip.css('cursor', 'ew-resize');
            },
            stop: function (e, ui) {
                glip.css('left', cmd.width() + 4);
                sys.prefs('glip', cmd.width());
            }
        });
        glip.on('dblclick', function () {
            if (cmd.width() === 45) {
                menu.removeClass('ico');
                cmd.css('width', '');
                view.css('left', '');
                comodo.css('left', '');
                glip.css('left', '');

            } else {
                menu.addClass('ico');
                cmd.css('width', 45);
                view.css('left', 49);
                comodo.css('left', 49);
                glip.css('left', 49);
            }
            sys.prefs('glip', cmd.width());
        });

        grip.draggable({
            axis: "x",
            addClasses: false,
            drag: function (e, ui) {
                var bw = $('body').width();
                var w = bw - e.pageX - 2;
                if (w < 20) {
                    w = 0;
                } else if (w > bw / 1.8) {
                    w = bw / 1.8;
                }
                lat.css('width', w);
                center.css('right', w);
                grip.css('right', w);
                grip.css('left', '');
            },
            create: function (e, ui) {
                grip.removeClass('ui-draggable-handle');
                grip.css('cursor', 'ew-resize');
            },
            stop: function (e, ui) {
                grip.removeClass('ui-draggable-handle');
                grip.css('left', '');
            }
        });
        grip.on('dblclick', function () {
            if (lat.width() === 0) {
                lat.css('width', '');
            } else {
                lat.css('width', 0);
            }
        });

    }
};