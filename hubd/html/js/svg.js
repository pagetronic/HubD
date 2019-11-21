sys.svg = function () {
    if (window.navigator.userAgent.match(/(MSIE |Trident.*rv\:11\.)/) === null) {
        return;
    }
    var use = $('svg use').eq(0);
    if (use.length === 0) {
        return;
    }
    $.ajax({
        type: "GET",
        url: use.attr('xlink:href').replace(/#.*$/, ''),
        success: function (svg) {
            var svgobj = $(svg);
            $('svg use').each(function () {
                var ele = $(this);
                ele.parent().html(svgobj.find('#' + ele.attr('xlink:href').replace(/.*#(.*)$/, '$1')).clone());
            });
        }
    });
}