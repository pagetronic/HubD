sys.menu = {
    init: function () {
        if (sys.user.admin) {
            sys.menu.admin();
        }
    },
    admin: function () {
        var menu = $('#menu ul');
        if (menu.find('li[id]').length > 1) {
            menu.sortable({
                cursor: 'move',
                items: 'li[id]',
                appendTo: document.body,
                containment: "parent",
                update: function (event, ui) {
                    var order = [];
                    menu.find('li[id]').each(function () {
                        order.push(this.id);
                    });
                    api.post('/forums', {action: 'root_sort', order: order}, function (rez) {
                        if (!rez.ok) {
                            menu.sortable('cancel');
                        }
                    }, function () {
                        menu.sortable('cancel');
                    });
                }
            });
        }
    }
};