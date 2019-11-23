sys.reload = function () {
    ajax.get(sys.uri(), function (html) {
        var body = $("<div/>").html(html);
        $('#center').html(body.find('#center').html());
    });
};