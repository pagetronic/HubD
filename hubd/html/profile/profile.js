sys.profile = {
    webpushRemove: function (id) {
        webpush.remove(id);
        $('#' + id).slowRemove(500, function () {
            var pushs = $('#pushs');
            if (pushs.find('ul li').length === 0) {
                pushs.addClass('none');
            }
        });

    },
    webpush: function () {
        var parsePush = function (rez) {
            var pushs = $('#pushs');
            if (rez.result.length > 0) {
                pushs.removeClass('none');
            }
            $.each(rez.result, function () {
                var result = this;
                var push = pushs.find('#' + result.id);
                if (push.length === 0) {
                    push = $('<li class="option"/>');
                    push.attr('id', result.id);
                    pushs.find('ul').append(push);
                }
                push.html($('<span class="delete" />').html('$svg.mi_clear').attr('title', lang.get('DELETE')).one('click', function () {
                    sys.profile.webpushRemove(result.id);
                }));

                var user = $('<strong class="user"/>');
                push.append(user);
                user.append($('<img height="50" width="50" />').attr('src', result.user.avatar + '@50x50'));
                if (result.user.name !== undefined) {
                    user.append(result.user.name);
                } else {
                    user.append(lang.get('ANONYMOUS'));
                }

                var date = $('<span class="date"/>');
                push.append(date);
                date.append(lang.get('DATE').ucfirst() + ': ').append(time.formatDate(result.date)).append(' (').append(time.since(result.date)).append(')');
                if (result.date !== result.update) {
                    date.append(' ' + lang.get('UPDATE').toLowerCase() + ': ').append(time.formatDate(result.update)).append(' (').append(time.since(result.update)).append(')<br/>');
                }
                if (result.device !== undefined) {
                    var device = $('<span class="device"/>');
                    push.append(device);
                    if (result.device.device !== undefined) {
                        device.append(result.device.device + ' ');
                    }
                    if (result.device.os !== undefined) {
                        device.append(result.device.os);
                    }
                }

                var objs = $('<em class="objs none"/>');
                push.append(objs);
                $.each(result.obj, function (i) {
                    if (i > 0) {
                        if (i + 1 === result.obj.length) {
                            objs.append(' ' + lang.get('AND') + ' ');
                        } else {
                            objs.append(', ');
                        }
                    }
                    objs.append(this);
                });
                push.pulse();
            });
            if (rez.paging.next !== undefined) {
                webpush.get(parsePush, rez.paging.next);
            }
        };

        webpush.get(parsePush);
    },
    avatar: function (where) {
        if (sys.user === null) {
            return;
        }
        var change = $('<span class="change">$svg.mi_add_a_photo</span>');
        var img = where.find('img').eq(0);
        var init_src = img.attr('src');
        where.append(change);
        change.on('click', function () {
            blobstore.image(img, function (id) {
                api.post("/profile", {
                    action: 'avatar',
                    avatar: id
                }, function (rez) {
                    if (!rez.ok) {
                        img.attr('src', init_src);
                    } else {
                        img.pulse();
                    }
                });
            });
            return false;
        });
    }
};