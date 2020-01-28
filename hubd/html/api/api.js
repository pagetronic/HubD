sys.api = {
    id: function (id) {

        if (data.get('speedcopy', true)) {
            var temp = $('<input />').css({position: 'fixed'});
            $(document.body).append(temp);
            temp.val(id);
            temp[0].focus();
            temp[0].select();
            if (document.execCommand('copy') === true) {
                sys.toast(id + '<br/>&laquo; ' + lang.get('RAPID_COPY') + ' &raquo;', 700);
                temp.remove();
                return;
            }
            temp.remove();
        }

        var popin = pop(true);
        var input = $('<input type="text" />').val(id).css({width: 300, textAlign: 'center'})
            .on('click', function () {
                this.select();
            }).on('change', function () {
                this.value = id;
            });


        input.attr('autocomplete', 'off').attr('autocorrect', 'off').attr('autocapitalize', 'off').attr('spellcheck', false);

        popin.header('$svg.mi_settings_input_hdmi API ID');
        var speedcopy = $('<button />').html('$svg.mi_content_copy' + lang.get('RAPID_COPY'));
        speedcopy.on('click', function () {
            input.select();
            if (document.execCommand('copy') === true) {
                data.set('speedcopy', true, true);
                popin.close();
                sys.toast(id + '<br/>&laquo; ' + lang.get('RAPID_COPY') + ' &raquo;', 700);
            } else {
                alert(lang.get('ERROR_UNKNOWN'));
            }
        });
        popin.content($('<div class="selectable flexo"/>').append(input).append(speedcopy));
        input.select();

    },
    set_scopes: function () {
        $('.apps').each(function () {
            var apps = $(this);
            var scopes = apps.find('.scopes input[name=scope]');
            scopes.on('change', function () {
                var checkbox = $(this);
                var scopesValues = [];
                scopes.each(function () {
                    if ($(this).is(':checked')) {
                        scopesValues.push($(this).val());
                    }
                });
                api.post('/api/apps', {
                    action: 'scopes',
                    id: apps.attr('id'),
                    scopes: scopesValues
                }, function (rez) {
                    if (rez.ok) {
                        checkbox.pulse();
                    } else {
                        checkbox.prop("checked", !checkbox.is(':checked'));
                    }

                }, function () {
                    checkbox.prop("checked", !checkbox.is(':checked'));
                });
            });

        });
    },
    redirect_uri: function () {

        $('.apps').each(function () {
            var apps = $(this);
            var list = apps.find('.redirect_uri');
            var empty = list.find('li.empty');
            var add = $('<icon/>').append('$svg.mi_add');
            var add_li = $('<li class="add" />').append(add).append(lang.get('ADD'));
            list.prepend(add_li);
            var isEmpty = function () {
                if (list.find('li.selectable').length === 0) {
                    empty.removeClass('none');
                } else {
                    empty.addClass('none');
                }
            };
            var removable = function (li) {
                li.prepend($('<icon />').html('$svg.mi_clear').on('click', function () {
                    li.fadeOut(300);
                    api.post('/api/apps', {
                        action: 'redirect_uris',
                        type: 'remove',
                        id: apps.attr('id'),
                        redirect_uri: li.attr('value')
                    }, function (rez) {
                        if (rez.ok) {
                            li.stop(true).slowRemove(300, function () {
                                isEmpty();
                            });
                        } else {
                            li.stop(true).fadeIn(0);
                        }

                    }, function () {
                        li.stop(true).fadeIn(0);
                    });
                }));
            };
            list.find('li:not(.empty):not(.add)').each(function () {
                removable($(this));
            });
            add.on('click', function () {
                var empty = apps.find('.redirect_uri .empty');
                empty.hide();
                add_li.hide();
                var input = $('<input type="text" />').attr('placeholder', lang.get('URL').ucfirst());
                input.on('blur', function () {
                    input.remove();
                    add_li.show();
                    isEmpty();
                });
                input.on('keypress', function (e) {
                    if (e.which === 13) {
                        var li = $('<li/>').attr('value', input.val()).addClass('selectable').html(input.val());
                        list.append(li);
                        api.post('/api/apps', {
                            action: 'redirect_uris',
                            type: 'add',
                            id: apps.attr('id'),
                            redirect_uri: input.val()
                        }, function (rez) {
                            if (rez.ok) {
                                removable(li);
                                empty.addClass('none');
                            } else {
                                li.remove();
                            }

                        }, function () {
                            li.remove();
                        });
                        input.trigger('blur');
                    }
                });
                add_li.after(input);
                input.focus();
            });

        });
    },
    create_apps: function () {
        $('#add_apps').on('click', function () {
            sys.wait(true);
            api.post('/api/apps', {
                action: 'create'
            }, function (res) {
                ajax.reload(true, function () {
                    $('#center .bx').first().pulse();
                    sys.wait(false);
                });
            }, function () {
                sys.wait(false);
            });
        });
    },
    get_access: function () {
        $('.apps .access').off('click').one('click', function () {
            var ele = $(this);
            var parent = ele.parents('.apps:eq(0)');
            parent.fadeTo(300, 0.5);
            var id = parent.attr('id');
            parent.find('.access_token, .refresh_token').remove();
            api.post('/api/apps', {
                action: 'get_access',
                id: id
            }, function (res) {
                if (res.ok) {
                    ajax.load('/api/access', false, null, function () {
                        $('#' + res.id).pulse();
                    });

                }
            }, function () {
                parent.stop(true).fadeTo(0, 1);
                sys.api.get_access();
            });
        });
    },
    change_secret: function () {
        $('.apps .change_secret').off('click').one('click', function () {
            var ele = $(this);
            sys.confirm(null, null, -1, function () {
                var client_secret = ele.parents('.apps:eq(0)').find('.client_secret');
                client_secret.fadeTo(200, 0.5);
                var id = ele.parents('.apps:eq(0)').attr('id');
                api.post('/api/apps', {
                    action: 'change_secret',
                    id: id
                }, function (res) {
                    client_secret.stop(true).fadeTo(0, 1);
                    if (res.ok) {
                        client_secret.html(res.client_secret);
                        client_secret.fadeOut(100).fadeIn(300);
                    }
                    sys.api.change_secret();
                }, function () {
                    client_secret.stop(true).fadeTo(0, 1);
                    sys.api.change_secret();
                });
            }, function () {
                sys.api.change_secret();
            });
        });
    },
    app_delete: function () {
        $('.apps .delete').off('click').one('click', function () {
            var ele = $(this);
            var parent = ele.parents('.apps:eq(0)');
            var id = parent.attr('id');
            var app = $('#' + id);
            sys.confirm(null, null, -1, function () {

                parent.fadeTo(500, 0.5);
                api.post('/api/apps', {
                    action: 'delete_apps',
                    id: id
                }, function (rez) {
                    if (rez.ok) {
                        app.slowRemove();
                    } else {
                        app.fadeIn(0);
                    }
                }, function () {
                    app.fadeIn(0);
                });

            });
        });
    },
    change_name: function () {
        $('.apps h3 span').attr('title', lang.get('DBCLICK_EDIT')).off('dblclick').on('dblclick', function () {
            var ele = $(this);
            ele.off('dblclick');
            var initial = ele.html();
            var input = $('<input type="text"/>');
            ele.html(input);
            input.val(initial);
            input.focus();
            input.on('blur', function () {
                ele.html(initial);
                sys.api.change_name();
            });
            input.on('keydown', function (event) {
                if (event.keyCode === 13) {
                    api.post('/api/apps', {
                        action: 'rename_apps',
                        id: ele.parents('.apps:eq(0)').attr('id'),
                        name: input.val()
                    }, function (res) {
                        if (res.ok) {
                            ele.html(res.name);
                        } else {
                            input.trigger('blur');
                        }
                        sys.api.change_name();
                    }, function () {
                        input.trigger('blur');
                    });
                }
            });
        });
    },
    remove_access: function () {
        $('.accs .remove_access').off('click').one('click', function () {
            var ele = $(this);

            sys.confirm(null, null, -1, function () {
                var parent = ele.parents('.accs:eq(0)');
                parent.fadeTo(300, 0.5);
                var id = parent.attr('id');
                api.post('/api/access', {
                    action: 'remove_access',
                    id: id
                }, function (res) {
                    if (res.ok) {
                        $('#' + id).slowRemove();
                    }
                }, function () {
                    parent.stop(true).fadeTo(0, 1);
                });

            });
        });
    },
    refresh_access: function () {
        $('.accs .refresh_access').off('click').one('click', function () {
            var ele = $(this);

            sys.confirm(null, null, -1, function () {
                var parent = ele.parents('.accs:eq(0)');
                parent.fadeTo(300, 0.5);
                var id = parent.attr('id');
                api.post('/api/access', {
                    action: 'refresh_access',
                    id: id
                }, function (res) {
                    if (res.ok) {
                        ajax.reload(true, function () {
                            $('#' + id).pulse();
                        });
                    }
                });

            }, function () {
                sys.api.remove_access();
            });
        });
    }
};