sys.admin = {
    ckeckReport: function (id_report) {
        api.post("/reports", {action: 'processed', id_report: id_report}, function () {
            var report = $('#' + id_report).css({opacity: 0.3});
             ajax.reload();
        });

    },
    switcher: function () {

        $('#switch .list').css({cursor: 'pointer'}).selectable({
            url: '/switch',
            select: function (id, name) {
                document.location.href = '/switch/' + id;
            }
        });
        $('#switch .add').css({cursor: 'pointer'}).on('click', function () {

            var popper = pop(false, 500);
            popper.header(lang.get('ACCOUNT_CREATE'));
            var form = $('<div class="createaccount" />');
            var name = $('<input autocomplete="off" name="title" />').attr('placeholder', lang.get('NAME'));
            form.append(name);
            var avatar = $('<button/>').html('$svg.mi_portrait ' + lang.get('AVATAR'));
            var avatar_box = $('<div class="logo flexible" />').append(avatar);
            form.append(avatar_box);
            var submit = $('<button />').text(lang.get('SAVE'));
            form.append(submit);
            popper.content(form);
            blobstore.button(avatar_box, avatar, avatar_box, 30, 30, [], true);
            submit.on('click', function () {
                var data = {action: 'create'};
                if (name.val() === '') {
                    name.addClass('error');
                    return;
                }
                data.name = name.val();
                var avainput = avatar_box.find('input');
                if (avainput.length > 0) {
                    data.avatar = avainput.val();
                }
                popper.loading(true);
                api.post('/switch', data, function (rez) {
                    popper.loading(false);
                    document.location.href = '/switch/' + rez.id;
                    popper.close();
                })
            })
        });
    }
};
