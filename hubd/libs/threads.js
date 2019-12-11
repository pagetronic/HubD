sys.threads = {
    init_forum: function (forum_id) {
        if (forum_id !== undefined) {
            sys.threads.follow('forums/' + forum_id);
        }
        $('.boxarea').each(function () {

            var area = $(this);
            var postbox = sys.threads.postbox(area.addClass('collapse'), true, 'forum_' + forum_id);
            blobstore.button(postbox.imgs, postbox.upload_button, area, 224, 126);
            area.locker = false;
            postbox.submit.on('click', function () {
                if (area.locker) {
                    return false;
                }
                area.locker = true;
                sys.threads.send(area, function (msg) {
                    var thread_posted = $(msg.html);
                    var thread = $('#' + msg.thread.id);
                    if (thread.length > 0) {
                        thread.replaceWith(thread_posted);
                    } else {
                        $('#posts_list').prepend(thread_posted);
                    }

                    postbox.reduce();
                    area.transfer({
                        to: thread_posted.fadeOut(0).fadeIn(800),
                        duration: 500,
                        className: 'post_transfer',
                        easing: 'easeOutCubic'
                    }, function () {
                        thread_posted.pulse();
                    });
                });
            });

        });

    },
    init_thread: function (id) {
        sys.threads.follow('threads/' + id);

        if (sys.user.id === null) {
            return;
        }
        var area = $('#reply .boxarea');
        var postbox = sys.threads.postbox(area, false, 'post_' + id);
        blobstore.button(postbox.imgs, postbox.upload_button, area, 224, 126);

        area.locker = false;
        postbox.submit.on('click', function () {
            if (area.locker) {
                return;
            }
            area.locker = true;
            sys.threads.send(area, function (msg) {

                var post_send = $(msg.html);
                var post = $('#' + msg.post.id);
                if (post.length > 0) {
                    post.replaceWith(post_send.pulse());
                } else {
                    $('#pushadd').before(post_send.pulse());
                }

            });
        });


    },

    postbox: function (where, subject, saver_id) {

        var collapse = where.hasClass('collapse');

        var format = $('<div class="flexo flexible"/>');

        var submit = $('<button tabindex="3"/>').addClass('flexable').html('$svg.mi_reply ' + lang.get('SUBMIT'));

        var title = $('<input tabindex="1" type="text" name="title" maxlength="65" />').attr('autocomplete', 'off').attr('placeholder', lang.get('POST_A_POSTS'));
        if (subject) {

            if (saver_id !== undefined) {
                title.attr('saver', 'title_' + saver_id);
            }
            where.append(title);
            title.on('keypress', function (event) {

                if (event.keyCode === 13) {
                    submit.trigger('click');
                }
            });
        }


        var textarea = $('<textarea tabindex="2" name="text" rows="4" cols="20"></textarea>').attr('placeholder', lang.get('TEXT_FORUM'));
        if (saver_id !== undefined) {
            textarea.attr('saver', 'text_' + saver_id);
        }
        var collapser = $('<div/>');
        where.append(collapser);

        collapser.append($('<div class="format"/>').append(textarea).append(format));

        var imgs = $('<div class="imgs" />');

        var link_url = $('<button><span style="text-decoration:underline">link</span></button>').addClass('flexable')
            .on('click', function () {
                ocode.tag(textarea, '[url]', '[/url]')
            });
        format.append(link_url);

        if (sys.user !== null && sys.user.editor) {

            var linker = $('<button>Obj(<em>Id</em>)</button>').addClass('flexable');
            format.append(linker);
            ocode.link(linker, textarea)
        }

        var upload_button = $('<button>$svg.mi_cloud_upload</button>').addClass('flexable')
            .attr('title', lang.get('UPLOAD_IMAGE'));
        format.append(upload_button);

        var gallery = $('<button>$svg.fa_icon_file_image_o</button>').addClass('flexable')
            .attr('title', lang.get('UPLOADED_IMAGE')).on('click', function () {
                blobstore.show(imgs)
            });
        //format.append(gallery);

        var bold = $('<button><strong>bold</strong></button>').addClass('flexable')
            .on('click', function () {
                ocode.tag(textarea, '[bold]', '[/bold]')
            });
        format.append(bold);

        var italic = $('<button><em>italic</em></button>').addClass('flexable')
            .on('click', function () {
                ocode.tag(textarea, '[italic]', '[/italic]')
            });
        format.append(italic);

        var quote = $('<button>quote</button>').addClass('flexable')
            .on('click', function () {
                ocode.tag(textarea, '[quote]', '[/quote]')
            });
        format.append(quote);

        if (sys.user !== null && sys.user.editor) {
            var dblquote = $('<button>« » </button>').addClass('flexable')
                .on('click', function () {
                    ocode.tag(textarea, '« ', ' »')
                });
            format.append(dblquote);

            var fontcase = $('<button>abcABC</button>').addClass('flexable')
                .on('click', function () {
                    ocode.reversecase(textarea)
                });
            format.append(fontcase);
        }


        var submit_box = $('<div class="flexible flexo submit"/>');


        submit_box.append(submit);


        if (subject) {

            var link = $('<div class="links"/>');

            link.loading = function () {
                link.html(sys.loading(60, 'div'));
            };
            textarea.after(link);
            textarea.on('input', function () {
                if (link.find('.close').length > 0) {
                    return;
                }
                $.each(textarea.val().split("\n"), function (i, line) {
                    if (line.match(/^(https?:\/\/)([^ ]+)(.*)?/)) {
                        var url = line.replace(/^(https?:\/\/)([^ ]+)(.*)?/g, '$1$2');
                        sys.links.preview(url, link, function (rez) {
                            if (title.val() === '') {
                                title.val(rez.title);
                            }
                        });
                        return false;
                    }
                });
            });
        }

        collapser.append(imgs).append(link).append(submit_box);

        if (collapse) {
            collapser.addClass('collapsed');
            var state = -1;
            var enlarge = function () {
                if (state <= 0) {
                    title.attr('placeholder', '…');
                    where.switchClass("collapse", "", {
                        duration: 300, complete: function () {
                            title.attr('placeholder', lang.get('TITLE_FORUM'));
                        },
                        children: true
                    });
                    state = 1;
                }
            };
            var reduce = function () {
                if (state === 1) {
                    title.attr('placeholder', '…');
                    where.switchClass("", "collapse", {
                        duration: 300,
                        complete: function () {
                            title.attr('placeholder', lang.get('POST_A_POSTS'));
                        },
                        children: true
                    });
                    state = 0;
                }
            };

            if (title.val() !== "" || textarea.val() !== "") {
                enlarge();
            }
            var timer_collapse = -1;
            title.on('focusin click touchstart', function () {
                clearTimeout(timer_collapse);
                enlarge();
            });
            where.on(
                'mouseleave',
                function (ui) {
                    timer_collapse = setTimeout(function () {
                        if (textarea.val() === '' && title.val() === '' && !textarea.is(":focus")
                            && !title.is(":focus") && imgs.html() === "") {
                            if ($('.loginer').length === 0) {
                                reduce();
                            }
                        }
                    }, 2000);

                });

        }
        textarea.autosize();


        return {
            title: title,
            text: textarea,
            submit: submit,
            submit_box: submit_box,
            imgs: imgs,
            link: link,
            upload_button: upload_button,
            reduce: reduce,
            where: where
        }
    },
    remove: function (data, after, other) {
        var ele;
        if (data.id === undefined) {
            ele = data;
            data = after;
            after = other;
        } else {
            ele = $('#' + data.id);
        }
        if (ele.hasClass('removed')) {
            data.restore = true;
        }
        data.action = "remove";
        api.post("/threads", data, function (rez) {
            if (after !== undefined) {
                after(rez);
            } else {
                if (data.restore) {
                    ele.removeClass('removed');
                } else {
                    ele.addClass('removed');
                }
            }
        });
    },
    send: function (ele, func, base) {
        ele.css({opacity: 0.3});
        $('.error').remove();

        var data = (base === undefined) ? {} : base;
        data.action = "send";
        data.tz = new Date().getTimezoneOffset();
        if (ele.page === true) {
            data.page = true;
        }

        if (sys.user.id === null) {
            data.sysId = sys.sysId();
        }
        var inputs = ele.find('input,textarea');
        inputs.each(function () {
            var el = $(this);
            if (el.attr('name') !== undefined && el.attr('name').endsWith('[]')) {
                var name = el.attr('name').substring(0, el.attr('name').length - 2);
                if (data[name] === undefined) {
                    data[name] = [];
                }
                data[name].push(el.val());
            } else if (el.attr('name') !== undefined) {
                data[el.attr('name')] = el.val();

            }
        });

        if (ele.find('.links [name=link_url]').length > 0) {
            ele.find('.links [name]').each(function () {
                var el = $(this);
                if (el.attr('name') !== undefined && el.attr('name').endsWith('[]')) {
                    var name = el.attr('name').substring(0, el.attr('name').length - 2);
                    if (data[name] === undefined) {
                        data[name] = [];
                    }
                    if (el.val() === '') {
                        data[name].push(el.text());
                    } else {
                        data[name].push(el.val());
                    }
                } else if (el.attr('name') !== undefined) {
                    if (el.val() === '') {
                        data[el.attr('name')] = el.text();
                    } else {
                        data[el.attr('name')] = el.val();
                    }
                }
            });
        }


        data.domain = document.location.hostname;
        ele.fadeTo(300, 0.5);
        api.post('/threads', data, function (msg) {
            if (msg.error !== undefined) {
                alert(lang.get(msg.error), ((msg.delay !== undefined) ? msg.delay : undefined));
            } else if (msg.errors !== undefined) {
                var first_error;
                for (var i = 0; i < msg.errors.length; i++) {
                    var input = ele.find('[name=' + msg.errors[i].element + ']').eq(0);

                    if (input.length === 0) {
                        alert(msg.errors[i].element + ': ' + lang.get(msg.errors[i].message));
                    } else {
                        if (first_error === undefined) {
                            first_error = input;
                        }
                        var error = $('<span class="error">' + lang.get(msg.errors[i].message) + '</span>');
                        error.fadeOut(0);
                        input.after(error);
                        error.fadeIn(700);

                    }
                    if (!first_error.is(':in-viewport')) {
                        sys.scrollto(first_error, 250);
                    }
                }
            } else {
                var var_inputs = ele.find('input:not([type=hidden]), textarea');
                saver.remove(var_inputs.val('').removeAttr('style'));
                ele.find('.imgs > *').remove();
                if (func !== undefined) {
                    try {
                        func(msg);
                    } catch (e) {
                        log(e);
                    }
                }
            }
            ele.fadeTo(300, 1, function () {
                ele.css({opacity: ''});
            });
            ele.locker = false;
        }, function () {
            ele.locker = false;
        });
    },
    edit: function (data) {
        var box = $('#' + data.id);
        var post = data;
        post.action = 'get';
        box.find('.boxarea').remove();
        api.post('/threads', post, function (msg) {
            if (msg.error !== undefined) {
                alert(msg.error);
                return;
            }
            var content = box.find('.content').eq(0);
            var foot = box.find('.foot');
            content.hide();
            foot.hide();

            var area = $('<div class="boxarea" />');
            content.after(area);

            var postbox = sys.threads.postbox(area, msg.title !== undefined);
            if (postbox.title !== undefined) {
                postbox.title.val(msg.title);
            }
            postbox.text.val(msg.text).trigger('input');

            var button_cancel = $('<button/>').addClass('flexable').html('$svg.mi_cancel ' + lang.get('CANCEL'));
            button_cancel.click(function () {
                area.remove();
                content.show();
                foot.show();
            });
            area.locker = false;
            postbox.submit.on('click', function () {
                if (area.locker) {
                    return;
                }
                area.locker = true;
                sys.threads.send(area, function (msg) {
                    if (msg.error !== undefined) {
                        alert(msg.error);
                        return;
                    }
                    ajax.reload();
                }, data);
            });

            postbox.submit_box.append(button_cancel);

            if (msg.link !== undefined) {
                postbox.link.loading();
                sys.links.make(postbox.link, msg.link);
                var reload = $('<span class="refresh" />').html('$svg.mi_refresh').one('click', function () {
                    postbox.link.loading();
                    sys.links.preview(msg.link.url, postbox.link);
                });
                postbox.link.prepend(reload);
            }
            postbox.submit.html('$svg.mi_save ' + lang.get('SAVE'));
            blobstore.button(postbox.imgs, postbox.upload_button, box, 224, 126, ((msg.docs !== undefined && msg.docs.length > 0) ? msg.docs : undefined));


        });
    },
    reply: function () {
        sys.scrollto("#reply", 1000, function () {
            $("#reply").effect('highlight', {
                color: '#A9DEA6'
            }, 600);
            $("#reply textarea:eq(0)").focus();
        });
    },
    rapid: function (post_id) {

        var maxlength = 180;
        var post = $('#' + post_id);
        post.find('.rapidbox').remove();
        var tips = post.find('.tips:eq(0)');
        var rapidbox = $('<div class="rapidbox" />');
        var input = $('<textarea maxlength="' + maxlength + '" placeholder="' + lang.get('RAPID_COMMENT') + '" />');


        var accept = $('<span class="accept"/>').html('$svg.mi_check');
        var count = $('<span/>').html(maxlength);


        input.keypress(function (event) {
            if (event.keyCode === 13) {
                accept.trigger('click');
            }
        });

        accept.prepend(count);
        input.on('input', function () {
            count.html(maxlength - this.value.length);
        });

        accept.on('click', function () {
            rapidbox.css({opactiy: 0.5});
            accept.css({visibility: 'hidden'})
            input.trigger('blur').prop('disabled', true);
            api.post('/threads', {
                action: "comment",
                post_id: post_id,
                text: input.val(),
                tz: new Date().getTimezoneOffset()
            }, function (msg) {
                if (msg.error !== undefined) {
                    alert(lang.get(msg.error));
                    rapidbox.css({opactiy: ''});
                    input.prop('disabled', false);
                    accept.css({visibility: ''})
                } else if (msg.html !== undefined) {
                    rapidbox.remove();
                    tips.append($(msg.html).pulse());
                }
            });
        });
        tips.after(rapidbox.append(input).append(accept));

        input.autosize().focus();


    },
    follow: function (what) {
        var ids = [];
        var update = $('<a/>').on('click', function () {
            ids = [];
             ajax.reload();
            title.reset();
            socket.unfollow(what);
        });
        $('#pushadd').append(update.hide());
        var controle = function () {
            var new_ids = [];
            for (var i = 0; i < ids.length; i++) {
                if ($('#' + ids[i]).length === 0) {
                    new_ids.push(ids[i]);
                }
            }
            ids = new_ids;
            if (ids.length === 0) {
                update.hide();
                title.reset();
            } else {
                title.count(ids.length);
            }
        };
        setInterval(controle, 600);
        var newpost = function (id) {
            ids.push(id);
            update.html((ids.length > 1) ? lang.get("POSSIBLES_UPDATE", ids.length) : lang.get("POSSIBLE_UPDATE"));
            title.count(ids.length);
            update.show();
        };

        socket.follow(what, function (msg) {
            if (msg.remove !== undefined) {
                $('#' + msg.remove).addClass("removed");
                return;
            } else if (msg.restore !== undefined) {
                $('#' + msg.restore).removeClass("removed");
                return;
            }

            if (msg.user !== undefined && msg.user !== null && msg.user.id === sys.user.id) {
                setTimeout(function () {
                    newpost(msg.id);
                    controle();
                }, 3000);
            } else {
                newpost(msg.id);
            }
        });

    },
    report: function (data) {
        var popper = pop(true);
        popper.header(lang.get('REPORT').ucfirst());
        var report = $('<div class="report" />');
        popper.content(report);
        popper.center();
        var message = $('<textarea />').attr('placeholder', lang.get('DESCRIPTION'));
        var submit = $('<button />').text(lang.get('SEND'));
        report.append(message).append(submit);
        message.autosize();

        submit.on('click', function () {
            popper.loading(true);
            api.post("/reports", {action: 'report', item: item, url: sys.uri(), message: message.val()});
            report.html('<h3>' + lang.get('THANK_YOU') + '</h3>');
            setTimeout(function () {
                popper.loading(false);
                popper.close();
            }, 1500);
        })

    }
};