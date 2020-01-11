sys.notices = {
    init: function () {
    },
    box: {
        view: function () {
            var body = $(document.body);
            var bell = $('#bell');
            var notices = $('<div id="notices" />');
            var notices_cmd = $('<div id="notices_cmd"/>');
            var notices_list = $('<ul/>');
            var readall = $('<span class="readall">$svg.mi_sort</span>').on('click', function () {
                api.post('/notices', {
                    action: 'read',
                    readall: true
                });
                notices.find('a').removeClass('unread');
                bell.removeClass('unread').find('.info').html(0);
            });
            var close = $('<span class="close"/>').html('$svg.mi_close');

            notices_cmd.append(readall).append(close);
            notices.append(notices_cmd).append(notices_list);

            body.prepend(notices);
            notices.append($('<style type="text/css" />').text('#notices:before{left:' + Math.round((bell.outerWidth() / 2) + bell.position().left - notices.position().left) + 'px}'));

            var xhr;
            var next = null;
            var get = function (paging) {
                var pager = {};
                if (paging !== null) {
                    pager.paging = paging;
                }
                xhr = api.get('/notices', pager, function (rez) {

                    if (rez.result.length === 0) {
                        notices_list.append($('<li/>').html(lang.get('EMPTY')));
                        return;
                    }
                    if (rez.paging !== undefined) {
                        next = rez.paging.next;
                    }
                    $(rez.result).each(function (i, item) {
                        var li = $('<li/>');
                        var a = $('<a/>').attr('id', item.id);
                        var del = $('<span />').html('x').addClass('remove').on('click', function () {
                            api.post('/notices', {action: 'remove', id: item.id});
                            li.slowRemove(300);
                            return false;
                        });
                        a.append($('<em />').append(time.since(item.date, 3)).append(del));
                        var title = item.title;
                        if (item.count > 1) {
                            title += ' (' + item.count + ')';
                        }
                        a.append($('<span class="title brown" />').append(title));
                        a.append(item.message);
                        a.attr('href', item.url);
                        a.on('click', function () {
                            a.removeClass('unread');
                            if (sys.isMobile()) {
                                notices.slowRemove(300);
                            }
                        });
                        if (item.read === undefined) {
                            a.addClass('unread');
                        }
                        notices_list.append(li.append(a));
                        ajax.it(li);

                    });
                });
            };
            get(null);
            var abort = function () {
                body.off('mouseup.notice');
                try {
                    xhr.abort();
                } catch (e) {

                }
                notices.slowRemove(300);
            };
            close.on('click', abort);
            body.on('mouseup.notice', function (e) {
                if (notices.has(e.target).length === 0) {
                    abort();
                }
            });
            notices_list.on('scroll.notice', function () {
                var height = notices_list.innerHeight();
                if (next != null && (notices_list.scrollTop() + height) > notices_list[0].scrollHeight - height) {
                    get(next);
                    next = null;
                }
            }).trigger('scroll.notice');
        }
    },

    view: function () {
        sys.notices.box.view();
    }

};