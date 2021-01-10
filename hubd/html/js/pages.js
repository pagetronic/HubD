sys.pages = {
    init: function () {
        sys.nocopy();
    },
    qrBox: function () {
        var question = $('#qrbox');


        question.locker = false;
        $('#post_question').on('click', function () {

            Cookies.set('qrbox', 'yes', {
                expires: new Date(new Date().getTime() + 3600000)
            });

            if (question.locker) {
                return;
            }
            question.locker = true;
            sys.threads.send(question, function (msg) {
                question.html(sys.loading(70, 'div'));
                var ready = function () {
                    document.location.href = msg.url;
                };
                webpush.enable('Posts(' + msg.thread.id + ')', ready, ready);
            });
        });

        if (sys.user.id !== null || question.length === 0 || Cookies.get('qrbox') === 'yes') {
            return;
        }

        var timer = -1;
        var stopQr = function () {
            clearTimeout(timer);
            scroller.off('scroll.qr');
        };


        var showQr = function () {
            if ($('#consentbox').length > 0 || (sys.pub && Cookies.get('consent') === null)) {
                return;
            }
            stopQr();
            var close = $('<span class="close"/>').html('$svg.mi_close');
            var mask = $('<div class="mask" />').css({zIndex: 20000}).on('click', function () {
                question.removeAttr('style');
                close.detach();
                mask.detach();
            });
            close.on('click', function () {
                question.removeAttr('style');
                close.detach();
                mask.detach();
                Cookies.set('qrbox', 'yes', {
                    expires: new Date(new Date().getTime() + 3600000)
                });
            });
            $(document.body).append(mask);
            question.prepend(close);
            question.css({
                width: question.width(),
                padding: 15,
                position: 'fixed',
                boxShadow: 'rgba(51, 51, 51, 0.78) 0px 0px 9px 8px'
            });
            var top = (window.innerHeight - question.outerHeight()) / 3;
            question.animate({
                top: top,
                border: '1px solid #666',
                borderRadius: 8
            }, 400);
            sys.svg();
        };

        var scroller = $('#middle');
        if (!scroller.isScrollable()) {
            scroller = $(window);
        }
        scroller.on('scroll.qr', function () {
            if (question.is(':in-viewport') && scroller.scrollTop() > window.innerHeight) {
                showQr();
            }
        });
        timer = setTimeout(showQr, 30000);
        question.on('click', stopQr);
        $('#ancrage a').on('click', stopQr);

        ajax.unload(stopQr);
    },
    edit: {
        init: function (id, docs) {
            if (id === '') {
                id = undefined;
            }

            blobstore.button($('#imgs'), $('#send_file'), $('#middle'), 224, 126, docs);
            ocode.link($('.links_tag'), 'tarea');

            $('.save_edit, .save_draft').on('click', function () {
                sys.pages.edit.save($('#edit_form'), $(this).hasClass('save_draft'));
            });
            $('.delete_draft').on('click', function () {
                confirm(lang.get("DELETE"), lang.get("DELETE_CONFIRM") + ' ?', 5, function () {
                    api.post('/edit', {
                        action: "draft",
                        revision: $('[name=revision]').val(),
                        remove: true
                    }, function (msg) {
                        if (msg.ok === true) {
                            $('#edit_form').html('<h3><span class="error">' + lang.get('REMOVED') + '</span></h3>');
                            $('.cmd_top').remove();
                            ajax.load("/draft");
                        } else if (msg.error) {
                            alert(lang.get(msg.error));
                        }
                    });
                });
            });


            $('.delete_page').click(function () {
                confirm(lang.get("DELETE"), lang.get("DELETE_CONFIRM") + ' ?', 5, function () {
                    api.post('/edit', {
                        action: "publish",
                        id: id,
                        remove: true
                    }, function (msg) {
                        if (msg.ok === true) {
                            $('#edit_form').html('<h3><span class="error">' + lang.get('REMOVED') + '</span></h3>');
                            $('.cmd_top').remove();
                            ajax.reload();
                        } else if (msg.error) {
                            alert(lang.get(msg.error));
                        }
                    });
                });
            });

            $('.liaisons').sortable({
                cursor: 'move',
                items: '> div',
                forceHelperSize: true,
                tolerance: "pointer",
                helper: 'clone',
                appendTo: 'body:eq(0)',
                containment: "parent"
            });

            sys.pages.edit.liaison($('#parents_liaisons'), 'parents', id);
            sys.pages.edit.liaison($('#childrens_liaisons'), 'childrens', id, $('#childrens_liaisons').attr('deletable') !== undefined);
            sys.pages.edit.liaison($('#author_add'), 'users');
        },
        save: function (ele, draft) {


            $('.error').remove();
            var inputs = ele.find('input,textarea,button');
            inputs.prop('disabled', true);
            var data = {action: draft ? "draft" : "publish"};
            data.domain = document.location.hostname;
            inputs.each(function () {
                var el = $(this);
                if (el.attr('name') !== undefined) {
                    if (el.attr('name').endsWith('[]')) {
                        var name = el.attr('name').substring(0, el.attr('name').length - 2);
                        if (data[name] === undefined) {
                            data[name] = [];
                        }
                        if (el.val() !== "") {
                            data[name].push(el.val());
                        }
                    } else {
                        data[el.attr('name')] = el.val();

                    }
                }
            });

            api.post('/edit', data, function (msg) {
                $('.error').remove();
                if (msg.errors !== undefined) {
                    var first_error;
                    for (var i in msg.errors) {
                        var input;
                        if (msg.errors[i].element === "parents") {
                            input = ele.find('#parents_liaisons').eq(0);
                        } else if (msg.errors[i].element === "childrens") {
                            input = ele.find('#childrens_liaisons').eq(0);
                        } else if (msg.errors[i].element === "users") {
                            input = ele.find('#author_add').eq(0);
                        } else {
                            input = ele.find('[name=' + msg.errors[i].element + ']:eq(0)');
                        }
                        if (first_error === undefined) {
                            first_error = input;
                        }
                        var error = $('<span class="error">' + lang.get(msg.errors[i].message) + '</span>');
                        error.fadeOut(0);
                        input.after(error);
                        error.fadeIn(700);
                    }
                    sys.scrollto(first_error, 300);

                } else if (msg.error !== undefined) {
                    alert(lang.get(msg.error));
                } else {
                    document.location.href = '/' + msg.url;
                }

                inputs.prop('disabled', false);
            });
        },
        liaison: function (ele, input, id, deletable) {
            var remove = function () {
                var parent = $(this).parent();
                ids = sys.pull(ids, parent.find('input').val());
                parent.slowRemove(300);
            };

            if (deletable === undefined || deletable) {

                ele.parent().find(".label").each(function () {
                    $(this).append($('<span class="rm" />').html('$svg.mi_delete_forever').on('click', remove))
                });
            }

            var ids = [];
            ele.parent().find('input').each(function () {
                ids.push(this.value);
            });
            if (id !== undefined) {
                ids.push(id);
            }

            ele.selectable({
                url: input === 'users' ? '/users' : '/edit', filter: ids, select: function (id, name) {
                    var label = $('<div class="label" />')
                        .append(name)
                        .append('<input type="hidden" name="' + input + '[]" value="' + id + '"/>')
                        .append($('<span class="rm" />').html('$svg.mi_delete_forever').on('click', remove));
                    if (input === 'users') {
                        label.addClass('user');
                    }
                    label.fadeOut(0);
                    ele.parent().append(label.fadeIn(300));
                    ids.push(id);
                }
            });
        }
    },
    admin: {
        init: function (id) {
            sys.pages.admin.forums($('#links_forum'), id);
            sys.pages.admin.childrens($('#childrens'), id);
            sys.pages.admin.parents($('#parents'), id);
        },
        parents: function (ele, id) {

            var filter = [];
            ele.find('li[id]:not(.ln)').each(function () {
                filter.push(this.id);
            });
            filter.push(id);

            var btn = $('<span/>').addClass('add').html('$svg.mi_playlist_add').attr('title', lang.get('ADD'));
            ele.find('.hr').append(btn);
            ele.find('.hr').selectable({
                url: '/edit', filter: filter, select: function (parent_id, name) {
                    api.post("/edit", {action: 'parents_add', id: id, parent_id: parent_id}, function (rez) {
                        if (rez.ok) {
                            ajax.reload();
                        } else {
                            alert(lang.get("EXISTS"));
                        }
                    })
                }
            });

            ele.find('li[id]:not(.ln)').each(function () {
                $(this).prepend($('<span/>').addClass('remove').attr('title', lang.get('DELETE')).html('$svg.fa_icon_times_circle').on('click', function () {

                    var li = $(this).parent();
                    confirm(lang.get("DELETE"), lang.get("DELETE_CONFIRM") + ' ?', 3, function () {
                        api.post("/edit", {
                            action: 'parents_remove',
                            id: id,
                            parent_id: li.attr('id')
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
                items: 'li[id]:not(.ln)',
                appendTo: document.body,
                containment: "parent",
                update: function (event, ui) {
                    var parents = [];
                    ele.find('li[id]').each(function () {
                        parents.push(this.id);
                    });
                    api.post('/edit', {action: 'parents_sort', id: id, parents: parents});

                }
            });
        },
        childrens: function (ele, id) {
            var filter = [];
            ele.find('li[id]:not(.ln)').each(function () {
                filter.push(this.id);
            });
            filter.push(id);

            var btn = $('<span/>').addClass('add').html('$svg.mi_playlist_add').attr('title', lang.get('ADD'));
            ele.find('.hr').append(btn);
            ele.find('.hr').selectable({
                url: '/edit', filter: filter, select: function (children_id, name) {
                    api.post("/edit", {action: 'childrens_add', id: id, children_id: children_id}, function (rez) {
                        if (rez.ok) {
                            ajax.reload();
                        } else {
                            alert(lang.get("EXISTS"));
                        }
                    })
                }
            });

            ele.find('li[id]:not(.ln)').each(function () {
                $(this).prepend($('<span/>').addClass('remove').attr('title', lang.get('DELETE')).html('$svg.fa_icon_times_circle').on('click', function () {
                    var li = $(this).parent();
                    confirm(lang.get("DELETE"), lang.get("DELETE_CONFIRM") + ' ?', 3, function () {
                        api.post("/edit", {
                            action: 'childrens_remove',
                            id: id,
                            children_id: li.attr('id')
                        }, function (rez) {
                            if (rez.ok) {
                                li.slowRemove();

                            } else {
                                alert(lang.get(rez.error));
                            }
                        });
                    });
                }));
            });

            ele.sortable({
                cursor: 'move',
                items: 'li[id]:not(.ln)',
                appendTo: document.body,
                containment: "parent",
                update: function (event, ui) {
                    var childrens = [];
                    ele.find('li[id]:not(.ln)').each(function () {
                        childrens.push(this.id);
                    });
                    api.post('/edit', {action: 'childrens_sort', id: id, childrens: childrens});

                }
            });
        },
        forums: function (ele, id) {

            var filter = [];
            ele.find('li[id]:not(.ln)').each(function () {
                filter.push(this.id);
            });

            ele.find('li[id]:not(.ln)').each(function () {
                $(this).prepend($('<span/>').addClass('remove').attr('title', lang.get('DELETE')).html('$svg.fa_icon_times_circle').on('click', function () {
                    var li = $(this).parent();
                    confirm(lang.get("DELETE"), lang.get("DELETE_CONFIRM") + ' ?', 3, function () {
                        api.post("/edit", {
                            action: 'forums_remove',
                            id: id,
                            forum_id: li.attr('id')
                        }, function (rez) {
                            if (!rez.ok) {
                                alert(lang.get("NOT_EXISTS"));
                            } else {
                                li.slowRemove();
                                ajax.reload();
                            }
                        });
                    });
                }));
            });


            var btn = $('<span/>').addClass('add').html('$svg.mi_playlist_add').attr('title', lang.get('ADD'));
            ele.find('.hr').append(btn);
            ele.find('.hr').selectable({
                url: '/forums', filter: filter, select: function (forum_id, name) {
                    api.post("/edit", {action: 'forums_add', id: id, forum_id: forum_id}, function (rez) {
                        if (rez.ok) {
                            ajax.reload();
                        } else {
                            alert(lang.get("EXISTS"));
                        }
                    });
                }
            });
            ele.sortable({
                cursor: 'move',
                items: 'li[id]:not(.ln)',
                appendTo: document.body,
                containment: "parent",
                update: function (event, ui) {
                    var forums = [];
                    ele.find('li[id]:not(.ln)').each(function () {
                        forums.push(this.id);
                    });
                    api.post('/edit', {action: 'forums_sort', id: id, forums: forums});

                }
            });
        },
        keywords: function (id) {
            var form = $('<div class="split" />');
            var keywords = $('<input autocomplete="off" name="keywords" />').attr('placeholder', "keywords,key,words,ect");

            var submit = $('<button />').html('$svg.mi_insert_link').append('autolink');

            var popper = pop(false, 500);
            submit.on('click', function () {
                popper.loading(true);
                api.post('/edit', {
                    action: 'keywords',
                    id: id,
                    keywords: keywords.val().split(/[ ]?,[ ]?/)
                }, function (rez) {
                    $('.autolinks').remove();
                    if (rez.ok) {
                        sys.toast(rez.links.length + " pages linked", 2000);
                        if (rez.links.length > 0) {
                            var autolinks = $('<ol class="autolinks"/>');
                            $(rez.links).each(function () {
                                autolinks.append($('<li style="margin-top: 15px" />')
                                    .append($('<a />').text(this).attr('href', this))
                                );
                            });
                            form.append(autolinks);
                            ajax.it(autolinks);
                            popper.pulse();
                        }
                    } else {
                        alert(rez.error);
                    }
                    popper.loading(false);
                });
            });
            form.append(keywords).append(submit);

            popper.content(form);
            popper.header("Autolink " + $('#breadcrumb .title').first().text());
            popper.loading(true);
            api.post("/edit", {action: "getKeywords", id: id}, function (rez) {
                if (rez.keywords) {
                    keywords.val(rez.keywords.join(', '));
                }
                popper.loading(false);
                popper.height(popper.pop.height());
                keywords.focus();
            });
        }
    }
};