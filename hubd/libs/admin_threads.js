sys.admin_threads = {
    init: function (id) {

        sys.admin_threads.pages($('#pages'), id);
        sys.admin_threads.relocate($('#relocater'), id);
        sys.admin_threads.parents(id);
    },
    pages: function (ele, id) {

        var filter = [];
        ele.find('li[id]').each(function () {
            filter.push(this.id);
        });
        var btn = $('<span/>').addClass('add').html('$svg.mi_playlist_add').attr('title', lang.get('ADD'));
        ele.find('.hr').append(btn);
        btn.selectable({
            url: '/edit', filter: filter, select: function (page_id, name) {
                api.post("/threads", {
                    action: 'pages_add',
                    id: id,
                    page_id: page_id
                }, function (rez) {
                    if (rez.ok) {
                         ajax.reload();
                    } else {
                        alert(lang.get("EXISTS"));
                    }
                })
            }
        });

        ele.find('li[id]').each(function () {
            $(this).prepend($('<span/>').addClass('remove').attr('title', lang.get('DELETE')).html('$svg.fa_icon_times_circle').on('click', function () {
                    var li = $(this).parent();
                    confirm(lang.get("DELETE"), lang.get("DELETE_CONFIRM") + ' ?', 3, function () {
                        api.post("/threads", {
                            action: 'pages_remove',
                            id: id,
                            page_id: li.attr('id')
                        }, function (rez) {
                            if (!rez.ok) {
                                alert(lang.get("NOT_EXISTS"));
                            } else {
                                li.slowRemove();
                                 ajax.reload();
                            }
                        });
                    });
                })
            );
        });

        ele.sortable({
            cursor: 'move',
            items: 'li[id]',
            appendTo: document.body,
            containment: "parent",
            update: function () {
                var pages = [];
                ele.find('li[id]').each(function () {
                    pages.push(this.id);
                });
                api.post('/threads', {
                    id: id,
                    action: 'pages_sort',
                    pages: pages
                });

            }
        });
    },
    parents: function (id) {
        var ele = $('#parents');
        var filter = [];
        ele.find('li[id]').each(function () {
            filter.push(this.id);
        });

        var btn = $('<span/>').addClass('add').html('$svg.mi_playlist_add').attr('title', lang.get('ADD'));
        ele.find('.hr').append(btn);
        btn.selectable({
            url: '/forums', filter: filter, select: function (forum_id, name) {

                api.post("/threads", {
                    action: 'parents_add',
                    id: id,
                    parent: 'Forums(' + forum_id + ')'
                }, function (rez) {
                    if (rez.ok) {
                         ajax.reload();
                    } else {
                        alert(lang.get("EXISTS"));
                    }
                })
            }
        });

        ele.find('li[id]').each(function () {
            $(this).prepend($('<span/>').addClass('remove').attr('title', lang.get('DELETE')).html('$svg.fa_icon_times_circle').on('click', function () {
                var li = $(this).parent();
                confirm(lang.get("DELETE"), lang.get("DELETE_CONFIRM") + ' ?', 3, function () {
                    api.post("/threads", {
                        action: 'parents_remove',
                        id: id,
                        parent: 'Forums(' + li.attr('id') + ')'
                    }, function (rez) {
                        if (rez.ok) {
                            li.slowRemove();
                             ajax.reload();
                        } else {
                            alert(lang.get(rez.error));
                        }
                    });
                });
            }));

        });

        ele.sortable({
            cursor: 'move',
            items: 'li[id]',
            appendTo: document.body,
            containment: "parent",
            update: function (event, ui) {
                var parents = [];
                ele.find('li[id]').each(function () {
                    parents.push(this.id);
                });
                api.post('/threads', {action: 'parents_sort', id: id, parents: parents});

            }
        });
    },
    relocate: function (ele, id) {
        ele.selectable({
            url: '/threads', filter: [id], select: function (thread_id, name) {
                api.post('/threads', {
                    action: 'relocate',
                    id: id,
                    to: thread_id
                }, function (msg) {
                    if (msg.url) {
                        document.location.href = msg.url;
                    }
                });
            }
        });
    }
};
