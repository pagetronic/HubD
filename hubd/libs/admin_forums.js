sys.admin_forums = {
    init: function (forum_id) {
        if (forum_id !== undefined) {
            sys.admin_forums.childrens($('#childrens'), forum_id);
            sys.admin_forums.parents($('#parents'), forum_id);
            sys.admin_forums.pages($('#pages'), forum_id);

        }
        sys.admin_forums.sisters($('#sisters'));
        sys.admin_forums.threads();
    },
    split: function (post_id, thread_id) {
        var posts = $('#split_posts');
        var postbox = $('#' + post_id);
        if (posts.length > 0) {
            if (postbox.hasClass('splitable')) {
                postbox.removeClass('splitable');
                posts.val(posts.val().replace(new RegExp('\\|?' + post_id), ''));
                if (posts.val() === '') {
                    posts.parents('.pop:eq(0)').trigger('close')
                }
            } else {
                postbox.addClass('splitable');
                posts.val(posts.val() + '|' + post_id);
            }
            return;
        }
        postbox.addClass('splitable');
        var form = $('<div class="split" />');
        var title = $('<input autocomplete="off" name="title" />').attr('placeholder', lang.get('TITLE'));

        var parent = $('<select name="parent" url="/forums" />').attr('placeholder', lang.get('PARENT'));

        posts = $('<input type="hidden" id="split_posts" name="posts" />').val(post_id);
        var question = $('<input type="hidden" name="thread" />').val(thread_id);

        var submit = $('<button />').text(lang.get('SPLIT'));


        form.append(title).append(parent).append(posts).append(question).append(submit);

        var popper = pop(false, 500);
        popper.content(form);
        popper.header(lang.get("SPLIT"));
        popper.mask.on('close', function () {
            $('.splitable').removeClass('splitable');
        });
        parent.selectable();

        var save = function () {
            if (title.val() === '') {
                return;
            }

            popper.loading(true);
            api.post("/threads", {
                action: 'split',
                title: title.val(),
                parent: parent.val(),
                posts: posts.val().split('|'),
                question: question.val()
            }, function (rez) {
                $('.splitable').slowRemove();
                document.location.href = rez.url;
            });
        };
        submit.on('click', save);


    },
    create: function (parent_ele, after) {
        var form = $('<div class="category" />');
        var title = $('<input autocomplete="off" name="title" />').attr('placeholder', lang.get('TITLE'));
        var meta_title = $('<input autocomplete="off" name="meta_title" />').attr('placeholder', lang.get('BIG_TITLE'));

        var forum_id = $('<input type="hidden" name="forum_id" />');

        var url = $('<input autocomplete="off" name="url" />').attr('placeholder', 'Url');

        title.on('input.url', function () {
            url.val(sys.cleanurl(this.value));
        });
        title.on('input', function () {
            title.next('.error').remove();
        });
        url.on('input', function () {
            title.off('input.url');
            url.next('.error').remove();
        });

        var parents = $('<select multiple name="parent" url="/forums" />').attr('placeholder', lang.get('PARENT'));


        if (parent_ele !== undefined) {
            parents.append('<option value="' + parent_ele.id + '">' + parent_ele.text + '</option>').val(parent_ele.id);
        }
        var text = $('<textarea name="text" />').attr('placeholder', lang.get('DESCRIPTION'));
        var submit = $('<button class="flexable" />').text(lang.get('SAVE'));


        form.append(meta_title).append(title).append(url).append(parents).append(text).append(forum_id);

        var popper = pop(false, 500);
        popper.content(form);
        popper.header('$svg.mi_format_align_right' + lang.get("CREATE_CATEGORY"));
        popper.footer($('<div class="flexible flexo submit"/>').append(submit));
        parents.selectable();

        var save = function () {
            popper.loading(true);
            popper.pop.find('span.error').remove();
            submit.off('click');
            var forum = {
                title: title.val(),
                meta_title: meta_title.val(),
                url: url.val(),
                text: text.val(),
                parents: parents.val(),
                domain: document.location.hostname

            };
            if (forum_id.val() !== "") {
                forum.forum_id = forum_id.val();
            }
            api.post("/forums", {
                action: 'create',
                forum: forum
            }, function (rez) {
                popper.loading(false);
                if (rez.errors !== undefined) {
                    submit.on('click', save);
                    for (var i in rez.errors) {
                        for (var item in rez.errors[i]) {
                            var error = $('<span/>').addClass('error').text(lang.get(rez.errors[i][item]));
                            popper.pop.find('[name=' + item + ']').after(error);
                        }
                    }
                } else if (rez.error !== undefined) {
                    alert(rez.error);
                    submit.on('click', save);
                } else {
                    if (after !== undefined) {
                        after(rez);
                    }
                    popper.close();

                     ajax.reload();
                }
            })
        };
        submit.on('click', save);
        text.autosize();
        return {
            popper: popper,
            title: title,
            meta_title: meta_title,
            url: url,
            parent: parents,
            text: text,
            forum: forum_id
        }
    },
    edit: function (id) {
        var data = sys.admin_forums.create(undefined, function () {
             ajax.reload();
        });
        data.popper.header('$svg.mi_format_align_right ' + lang.get('FORUM_EDIT'));
        data.popper.loading(true);
        data.forum.val(id);
        data.parent.attr('filter', id);
        data.title.off('input');
        api.post("/forums", {action: 'edit', id: id}, function (rez) {
            data.title.val(rez.title);
            data.meta_title.val(rez.meta_title);

            data.url.val(rez.url);
            data.text.val(rez.text);

            var parents_ = [];
            $.each(rez.parents, function () {
                data.parent.append('<option value="' + this.id + '" selected="selected">' + this.title + '</option>')
                parents_.push(this.id);
            });
            data.parent.val(parents_).trigger('change');
            data.popper.loading(false);
            data.popper.pulse();
        }, function () {
            sounds.play('failure');
            data.popper.loading(false);
        });
    },
    order: function () {

        var box_ul = $('<ul/>');
        var box = $('<div class="arbo" />').append(box_ul);
        box.attr('item', 'ROOT');
        var popper = pop(true, 500);
        popper.mask.on('close', function () {
             ajax.reload();
        });
        popper.content(box);
        popper.header(lang.get("ORDER_CATEGORIES").ucfirst());

        var xhr;
        var update = function (data, success) {
            try {
                xhr.abort();
            } catch (e) {
            }
            xhr = api.post("/forums", {action: 'order', data: data}, success);
        };

        var createli = function (item) {
            var li = $('<li/>').attr('item', item.id);
            if (item.childrens === undefined || item.childrens === 0) {
                li.addClass('nochilds');
            }
            var deploy = $('<span/>').addClass('deploy');
            deploy.on('click', function () {
                var deploy = $(this);
                var ele = deploy.parent();
                if (ele.hasClass('deployed')) {
                    ele.removeClass('deployed');
                    ele.find('ul').slowRemove();
                } else {
                    load(ele);
                    ele.addClass('deployed')
                }
            });
            li.append(deploy);

            li.append(item.title);

            var remove = $('<span/>').html('$svg.fa_icon_times_circle').addClass('remove').attr('title', lang.get('REMOVE').toLowerCase());
            remove.on('click', function () {
                var li = $(this).parents('[item]:eq(0)');
                var parent = li.parents('[item]:eq(0)');

                li.css({opacity: 0.4});
                api.post("/forums", {
                    action: 'parents_remove',
                    id: li.attr('item'),
                    parent: parent.attr('item')
                }, function (rez) {
                    if (rez.error === undefined) {
                        arrange(undefined, parent);
                        li.remove();
                    } else {
                        li.css({opacity: ''});
                    }
                });
            });
            li.append(remove);

            var add = $('<span/>').html('$svg.mi_add_circle_outline').addClass('add').attr('title', lang.get('ADD').toLowerCase());
            add.on('click', function () {
                var item = $(this).parents('[item]:eq(0)');
                var text = item.clone();
                text.find("> *").remove();
                text = text.text();
                sys.admin_forums.create({text: text, id: item.attr('item')}, function (rez) {

                    item.addClass('deployed');
                    $('[item=' + rez.parents.join('], [item=') + ']').each(function () {
                        load($(this).removeClass('nochilds'));
                    });


                });
            });
            li.append(add);

            var duplicate = $('<span/>').html('$svg.mi_control_point_duplicate').addClass('duplicate').attr('title', lang.get('DUPLICATE').toLowerCase());
            duplicate.on('click', function () {
                var li = $(this).parents('[item]:eq(0)');
                var parent = li.parents('[item]:eq(0)');
                var new_li = li.clone(true);
                li.parent().append(new_li.pulse());

                arrange(li);

            });
            li.append(duplicate);
            return li;
        };
        var load = function (where) {

            if (where.hasClass('nochilds')) {
                return;
            }
            var loading = $('<li/>').text(lang.get('LOADING') + '…').css({fontSize: '12px', color: '#CCC'});

            var list = where.find('ul');
            if (list.length === 0) {
                list = $('<ul/>');
            }
            where.append(list);
            list.append(loading);
            api.post("/forums", {action: 'childrens', parent: where.attr('item')}, function (rez) {
                loading.remove();

                for (var i in rez.result) {
                    var item = rez.result[i];
                    if (list.find('li[item=' + item.id + ']').length === 0) {
                        list.append(createli(item));
                    }

                }

            }, function () {
                loading.remove();
                alert();
            });
            return list;
        };


        var box_ul = load(box);
        var previous_parent = null;
        var previous_item = null;

        var arrange = function (li, parent) {
            if (parent !== undefined) {
                previous_parent = parent;
            }
            if (previous_parent !== null) {
                $('[item=' + previous_parent.attr('item') + ']').not(previous_parent).each(function () {
                    $(this).find('[item]').eq(li.index()).remove();
                });
                previous_parent = null;
            }

            if (li !== undefined) {
                var parent = li.parents('[item]').eq(0);
                $('[item=' + parent.attr('item') + ']').not(parent).each(function () {
                    var cln = parent.clone(true);
                    if ($(this).hasClass('deployed')) {
                        cln.addClass('deployed');
                    } else {
                        cln.removeClass('deployed');
                    }
                    $(this).replaceWith(cln);
                });
            }
            box_ul.find('ul').each(function () {
                var ul = $(this);
                if (ul.find('[item]').length === 0) {
                    ul.parents('[item]').eq(0).removeClass('deployed').addClass('nochilds');
                    ul.remove();
                }
            });

            var arbo = [];
            var did = [];
            box.parent().find('[item]').each(function () {
                var ele = $(this);
                var item = ele.attr('item');
                if (did.indexOf(item) === -1) {
                    did.push(item);
                    var data = {id: item, parents: [], order: []};
                    box_ul.find('[item=' + item + ']').each(function () {
                        var parent = $(this).parents('[item]:eq(0)').attr('item');
                        if (parent) {
                            data.parents.push(parent);
                        }
                    });
                    ele.find('> ul > [item]').each(function () {
                        data.order.push($(this).attr('item'));
                    });
                    arbo.push(data);
                }
            });

            update({action: 'order', arbo: arbo}, function () {

            });
        };
        box_ul.nestedSortable({
            cursor: 'move',
            appendTo: document.body,
            delay: 100,
            items: 'li',
            connectWith: '.arbo > ul ul',
            isTree: true,
            listType: "ul",
            maxLevels: 20000,
            startCollapsed: true,
            expandedClass: "deployed",
            expand: function (li) {
                if (li.hasClass('nochilds')) {
                    li.removeClass('nochilds');
                }
                load(li);
            },
            collapse: function (li) {
                if (li.find('li[item]').length === 0) {
                    li.addClass('nochilds');
                }
            },
            start: function (event, ui) {
                previous_item = $(ui.item);
                previous_item.attr('key', sys.uniqueId());
                previous_parent = previous_item.parents('[item]').eq(0);

            },
            stop: function (event, ui) {
                if ($('[key=' + previous_item.attr('key') + ']').length > 0) {
                    previous_item.removeAttr('key');
                    arrange($(ui.item));
                } else {
                    box_ul.sortable('cancel');
                }
            }

        });

    },
    childrens: function (ele, forum_id) {

        var filter = [];
        ele.find('li[id]').each(function () {
            filter.push(this.id);
        });

        var btn = $('<span/>').addClass('add').html('$svg.mi_playlist_add').attr('title', lang.get('ADD'));
        ele.find('.hr').append(btn);
        btn.selectable({
            url: '/forums', filter: filter, select: function (id, name) {
                api.post("/forums", {action: 'children_add', id: id, children: forum_id}, function (rez) {
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
                    api.post("/forums", {
                        action: 'parents_remove',
                        id: li.attr('id'),
                        parent: forum_id
                    }, function (rez) {
                        if (rez.ok) {
                             ajax.reload();
                        }
                        if (rez.error) {
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
                var order = [];
                ele.find('li[id]').each(function () {
                    order.push(this.id);
                });
                api.post('/forums', {action: 'childrens_sort', id: forum_id, order: order});

            }
        });
    },
    parents: function (ele, forum_id) {
        var filter = [];
        ele.find('li[id]').each(function () {
            filter.push(this.id);
        });

        var btn = $('<span/>').addClass('add').html('$svg.mi_playlist_add').attr('title', lang.get('ADD'));
        ele.find('.hr').append(btn);
        ele.find('li[id]').each(function () {
            $(this).prepend($('<span/>').addClass('remove').attr('title', lang.get('DELETE')).html('$svg.fa_icon_times_circle').on('click', function () {
                var li = $(this).parent();
                confirm(lang.get("DELETE"), lang.get("DELETE_CONFIRM") + ' ?', 3, function () {
                    api.post("/forums", {
                        action: 'parents_remove',
                        id: forum_id,
                        parent: li.attr('id')
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
                api.post('/forums', {action: 'parents_sort', id: forum_id, parents: parents});

            }
        });
        btn.selectable({
            url: '/forums', filter: filter, select: function (id, name) {

                api.post("/forums", {action: 'parents_add', id: forum_id, parent: id}, function (rez) {
                    if (rez.ok) {
                         ajax.reload();
                    } else {
                        alert(lang.get("EXISTS"));
                    }
                })
            }
        });

    },
    sisters: function (ele) {
        if (ele.find('li[id]').length > 1) {
            ele.sortable({
                cursor: 'move',
                items: 'li[id]',
                appendTo: document.body,
                containment: "parent",
                update: function (event, ui) {
                    var order = [];
                    ele.find('li[id]').each(function () {
                        order.push(this.id);
                    });
                    api.post('/forums', {action: 'root_sort', order: order}, function (rez) {
                        if (!rez.ok) {
                            ele.sortable('cancel');
                        }
                    }, function () {
                        ele.sortable('cancel');
                    });
                }
            });
        }
    },
    pages: function (ele, forum_id) {

        var filter = [];
        ele.find('li[id]').each(function () {
            filter.push(this.id);
        });
        var btn = $('<span/>').addClass('add').html('$svg.mi_playlist_add').attr('title', lang.get('ADD'));
        ele.find('.hr').append(btn);
        btn.selectable({
            url: '/edit', filter: filter, select: function (page_id, name) {
                api.post("/edit", {action: 'forums_add', id: page_id, forum_id: forum_id}, function (rez) {
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
                        api.post("/edit", {
                            action: 'forums_remove',
                            id: forum_id,
                            page_id: li.attr('id')
                        }, function (rez) {
                            if (!rez.ok) {
                                alert(lang.get("NOT_EXISTS"));
                            } else {
                                li.slowRemove();
                                var scrolltop = [$('#middle')[0].scrollTop, $('#lateral')[0].scrollTop];
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
            update: function (event, ui) {
                var pages = [];
                ele.find('li[id]').each(function () {
                    pages.push(this.id);
                });
                api.post('/forums', {action: 'pages_sort', id: forum_id, pages: pages});

            }
        });
    },
    threads: function () {

        var posts_list = $('#posts_list');
        posts_list.find('li[id]').each(function () {
            var li = $(this);

            var admin = $('<span class="admin"/>');
            li.prepend(admin);
            var remover = $('<span class="remover"/>');
            admin.append(remover);
            var remote = function (restore) {
                if (restore) {
                    remover.attr('title', lang.get('RESTORE')).html('$svg.mi_rotate_left').one('click', function () {
                        remover.css({visibility: 'hidden'});
                        li.css({opacity: 0.4});
                        sys.threads.remove({
                            id: li.attr('id'),
                            restore: true
                        }, function (rez) {
                            li.css({opacity: ''});
                            if (rez.ok) {
                                remover.css({visibility: ''});
                                li.removeClass('removed');
                                remote(false);
                            }
                        });
                    });
                } else {
                    remover.attr('title', lang.get('DELETE')).html('$svg.fa_icon_trash_o').one('click', function () {
                        remover.css({visibility: 'hidden'});
                        li.css({opacity: 0.4});
                        sys.threads.remove({
                            id: li.attr('id'),
                            restore: false
                        }, function (rez) {
                            li.css({opacity: ''});
                            if (rez.ok) {
                                remover.css({visibility: ''});
                                li.addClass('removed');
                                remote(true);
                            }
                        });
                    });
                }
            };

            remote(li.hasClass('removed'));

            admin.append($('<span/>').attr('title', lang.get('MOVE')).html('$svg.fa_icon_location_arrow').on('click', function () {
                var move = $(this);
                var parent = li.scrollParent();
                var edit = $('<div class="admin_threads"/>').addClass('flexible flexo').css({
                    top: (li.height() / 2) + li.offset().top - parent.offset().top + parent[0].scrollTop,
                    left: move.offset().left - parent.offset().left
                });

                var cancel = $('<button/>').addClass('flexable').html('$svg.mi_cancel').on('click', function () {
                    edit.remove();
                });

                var forums = $('<select class="flexable expand" add="sys.admin_forums.create" placeholder="forum" url="/forums" multiple="multiple" />');
                var save = $('<button/>').addClass('flexable').html('$svg.fa_icon_check ' + lang.get('SAVE'));
                save.on('click', function () {

                    var parents_val = [];
                    $.each(forums.val(), function () {
                        parents_val.push('Forums(' + this + ')');
                    });
                    api.post("/threads", {
                        action: 'move', id: li.attr('id'), parents: parents_val
                    }, function (rez) {
                        edit.remove();
                        li.pulse();
                         ajax.reload();
                    }, function () {
                        alert();
                    });
                });


                edit.append(forums).append(save).append(cancel);

                posts_list.before(edit);
                forums.selectable({position: 'before'});

                api.post("/threads", {
                    action: 'get',
                    id: li.attr('id')
                }, function (rez) {
                    var parents = [];
                    $.each(rez.parents, function () {
                        if (this.match(/Forums\(([0-9a-z]+)\)/i) != null) {
                            parents.push(this.replace(/Forums\(([0-9a-z]+)\)/i, '$1'));
                        }
                    });
                    forums.val(parents).trigger('search', [parents])
                });

            }));
        });
    }
};
