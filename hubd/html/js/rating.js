sys.rating = {
    init: function () {
        var values = {};
        $('[rating]').each(function () {
            var rating = $(this);
            var obj = rating.attr('rating');
            var reviewCount = rating.find('.count');
            var ratingValue = rating.find('.value');
            values[obj] = -1;
            if (ratingValue.length > 0 && reviewCount.length > 0) {
                rating.attr('title', lang.get('BASED_REVIEW', reviewCount.text()));
                values[obj] = parseFloat(ratingValue.text()) - 1;
                rating.html('');
            }
            for (var i = 0; i < 5; i++) {
                rating.append($('<span/>').html('$svg.mi_star'));
            }
            var setStar = function (level) {
                rating.find('span').each(function (i) {
                    if (i <= Math.round(level)) {
                        $(this).addClass('light');
                    } else {
                        $(this).removeClass('light');
                    }
                })
            };
            setStar(values[obj]);
            rating.on('mousemove', function (e) {
                setStar($(e.target).parents('span:eq(0)').index());
            });

            rating.on('mouseleave', function (e) {
                setStar(values[obj]);
            });
            var click = function (e) {
                rating.removeAttr('title');
                var star = $(this);
                star.css({opacity: 0.6});
                var currentrate = star.index();
                setStar(currentrate);
                api.post('/rating', {
                    action: 'rate',
                    obj: obj,
                    rate: currentrate + 1
                }, function (rez) {
                    if (rez.ok) {
                        star.css({opacity: ''}).pulse();
                        values[obj] = currentrate;
                        $('.rating').trigger('rating');
                    } else {
                        setStar(values[obj]);
                    }
                    rating.find('span').one('click', click);
                });
            };
            rating.find('span').one('click', click);

            rating.on('rating', function () {
                setStar(values[obj]);
            })
        });
    }
};