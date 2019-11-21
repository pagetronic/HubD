sys.ego = {
    init: function () {
        if (sys.user.id !== null) {
            socket.follow('user', function (msg) {
                switch (msg.action) {
                    case 'coins':
                        sys.coin.update(msg.coins);
                        break;
                    case 'notices':
                        $('#bell .info').html(msg.notices);
                        if (msg.notices > 0) {
                            $('#bell').addClass('unread').pulse();
                        } else {
                            $('#bell').removeClass('unread');
                        }
                        break;
                    case 'settings':
                        break;
                    default:
                        break;
                }
            });
        }
    }
};