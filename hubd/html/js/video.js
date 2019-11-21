sys.video = {
    init: function () {
        $(window).on('resize', function () {
            $('iframe.video').each(function () {
                var video = $(this);
                video.css({width: '100%', maxWidth: 1200});
                video.css({height: video.outerWidth() * (parseInt(video.attr('height')) / parseInt(video.attr('width')))});
            });
        }).trigger('resize');
    }
};