sys.coin = {
    update: function (coins) {
        $('#coins span').html(coins).pulse();
    },
    congrate: function (ele, element) {
        ele = $(ele);
        if (ele.data('lock')) {
            return;
        }
        ele.data('lock', true).css({opacity: 0.3});
        api.post('/coins', {
            action: "congrate",
            element: element
        }, function (msg) {
            ele.data('lock', false).css({opacity: ''});
            if (!msg.error) {
                ele.finish();
                ele.find('span:eq(0)').html(parseInt(msg.coins));
                ele.pulse();
            } else {
                alert(lang.get(msg.error));
            }
        });
    }
};