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
            login.add(function (id) {
                document.location.href = '/switch/' + id;
            });
        });
    }
};
