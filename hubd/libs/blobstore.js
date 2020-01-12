var blobstore = {
    /**
     * poppable image
     */
    popimg: function () {
        $('#middle').on('click', '.img a, a.popimage', function () {
            var a = $(this);
            var mask = $('<div class="mask"/>');
            var img = $('<img class="maxi"/>').attr('src', a.attr('href'));
            img.bind('load', function () {
                img.imgViewer({
                    legend: a.text(),
                    onReady: function () {
                    }
                });
            });
            mask.append(img).on('click', function () {
                mask.remove();
            });
            $(document.body).append(mask.fadeOut(0));
            mask.fadeIn(300);


            return false;
        });
    },
    show: function (where) {
        //!TODO! do galery function

    },

    /**
     * Image selector for add image to zone / form
     *
     * @param where add documents thumbs ?
     * @param button to action
     * @param droppable JQObject where drop doc is possible
     * @param width of the thumb needed
     * @param height of the thumb needed
     * @param docs initials ?
     * @param uniq document it is ?
     */
    button: function (where, button, droppable, width, height, docs, uniq) {
        if (uniq === undefined) {
            uniq = false;
        }
        if (!uniq) {
            where.sortable({
                cursor: 'move',
                items: '.img',
                appendTo: document.body,
                containment: "parent"
            });
        }
        var makeitem = function (reader) {
            var image = $('<img width="' + width + '" height="' + height + '" />').attr('draggable', false);
            var imgbox = $('<div class="img" />');
            var figure = $('<figure />');
            imgbox.append(figure.append(image));
            if (uniq) {
                where.html(imgbox);
            } else {
                where.append(imgbox);
            }
            imgbox.append($('<span class="rm" />').html('$svg.mi_remove_circle_outline').click(function () {
                if (reader !== undefined) {
                    reader.abort();
                }
                $(this).parent().remove();
            }));
            return {
                image: image,
                imgbox: imgbox,
                assign: function (data) {
                    var timeoutinfo = -1;
                    var infos = $('<input size="4" type="text" />').on('click', function () {
                        if ($(this).val() === '') {
                            $(this).val('© ');
                        }
                    }).on('input change', function () {
                        var ele = $(this);
                        clearTimeout(timeoutinfo);
                        timeoutinfo = setTimeout(function () {
                            if (ele.val() !== '© ') {
                                api.post('/gallery', {action: 'text', id: data.id, text: ele.val()}, function (rez) {
                                    if (rez.ok) {
                                        ele.pulse();
                                    }
                                });
                            }
                        }, 700, ele)

                    });

                    imgbox.append(infos);

                    if (data.text !== undefined) {
                        infos.val(data.text);
                    }
                    var name = uniq ? 'docs' : 'docs[]';

                    image.removeAttr('style').attr('src', constants.cdnurl + '/files/' + data.id + '@' + width + 'x' + height)
                        .after($('<input type="hidden" name="' + name + '" value="' + data.id + '" />'));

                    if (!uniq && document.queryCommandSupported('insertText')) {
                        imgbox.prepend($('<span class="add" />').html('$svg.mi_add_circle_outline').on('click', function () {

                            var scrollHeight = 0;
                            var scroll = null;
                            try {
                                scroll = where.scrollParent();
                                scrollHeight = scroll[0].scrollTop;
                            } catch (e) {
                            }
                            document.execCommand("insertText", false, ' [Photos(' + data.id + ') ]');

                            if (scroll !== null) {
                                scroll.scrollTop(scrollHeight);
                            }
                        }));
                    }

                }
            };
        };

        var choosen = function (file, reader) {
            var percent = $("<figcaption />").html(lang.get('LOADING') + ' <span>0%</span>');
            var obj = makeitem(reader);
            obj.image.css({
                opacity: 0.3
            });

            where.append(obj.imgbox.append(percent));

            var preview = new FileReader();
            preview.onload = function (e) {
                obj.image.attr('src', preview.result);
            };
            preview.readAsDataURL(file);
            obj.percent = percent;
            return obj;
        };
        var progress = function (percent, obj) {
            obj.percent.find('span').html(Math.floor(percent) + "%");
        };
        var finish = function (data, obj) {
            obj.percent.remove();
            obj.assign(data);
        };
        var send = function (file) {
            if (WebSocket === undefined) {
                return;
            }
            var upsocket = new WebSocket(constants.apiurl.replace(/^http/, 'ws') + '/up');
            var reader = new FileReader();
            reader.onload = function (e) {
                upsocket.send(e.target.result);
            };
            reader.onabort = function () {
                upsocket.close(1000, "cancel");
            };
            var obj = choosen(file, reader);
            upsocket.onmessage = function (event) {
                try {
                    var data = eval("(" + event.data + ")");
                    if (data.id === undefined) {
                        progress(data.percent, obj);
                    } else {
                        finish(data, obj);
                        upsocket.close(1000, "done");
                    }
                } catch (e) {
                    log(e);
                    upsocket.close(1000, "error");
                }
            };
            var interval = setInterval(function () {
                if (upsocket.readyState === 1) {
                    clearInterval(interval);
                    upsocket.send(JSON.stringify({type: file.type, size: file.size, name: file.name}));
                    reader.readAsArrayBuffer(file);
                }
            }, 200);

            upsocket.onerror = function (event) {
                upsocket.close(1000, "error");
            };
        };
        var upfiles = function (files) {
            var error = [];
            for (var i = 0; i < files.length; i++) {
                var file = files[i];
                if (file.type === undefined || !file.type.match(/image\/(jpg|jpeg|png|gif)/)) {
                    error.push(lang.get('FILE_TYPE_ERROR', file.name));
                } else {
                    send(file);
                }
            }
            if (error.length > 0) {
                alert(error.join("<br/>"));
            }
        };

        button.on('click', function () {
            var file_input = $('<input type="file" accept=".jpg, .jpeg, .png, .gif" multiple="true" />');
            file_input.on('change', function () {
                upfiles(this.files);
                file_input.remove();
            }).trigger('click');
        });

        droppable.on('drop', function (event) {
            event.preventDefault();
            event.stopPropagation();
            upfiles(event.originalEvent.dataTransfer.files);
            where.scrollParent().scrollTo(where, {offset: -20})
        });
        if (docs !== undefined && docs.length > 0) {

            for (var i = 0; i < docs.length; i++) {
                var obj = makeitem();
                obj.assign({id: docs[i].id, text: docs[i].text});
            }
        }
    },
    /**
     * Image changeable
     * @param image can be changed
     * @param finished function when upload completed
     */
    image: function (image, finished) {
        var init_src = image.attr('src');
        var preview = new FileReader();
        preview.onload = function (e) {
            image.css({opacity: 0.1});
            image.attr('src', preview.result);
        };
        var send = function (file) {
            if (WebSocket === undefined) {
                return;
            }
            var upsocket = new WebSocket(constants.apiurl.replace(/^http/, 'ws') + '/up');
            var reader = new FileReader();
            reader.onload = function (e) {
                upsocket.send(e.target.result);
            };
            reader.onabort = function () {
                upsocket.close(1000, "cancel");
            };

            preview.readAsDataURL(file);

            upsocket.onmessage = function (event) {
                try {
                    var data = eval("(" + event.data + ")");
                    if (data.id === undefined) {
                        image.css({opacity: (data.percent / 100)});
                    } else {
                        image.css({opacity: ''});
                        image.attr('src', data.src + '@' + image.attr('width') + 'x' + image.attr('height'));
                        image.parent().find('source').each(function () {
                            var source = $(this);
                            source.attr('srcset', data.src + '@' + source.attr('width') + 'x' + source.attr('height'));
                        });
                        if (finished !== undefined) {
                            finished(data.id);
                        }
                        upsocket.close(1000, "done");
                    }
                } catch (e) {
                    log(e);
                    image.attr('src', init_src);
                    upsocket.close(1000, "error");
                }
            };
            var interval = setInterval(function () {
                if (upsocket.readyState === 1) {
                    clearInterval(interval);
                    upsocket.send(JSON.stringify({type: file.type, size: file.size, name: file.name}));
                    reader.readAsArrayBuffer(file);
                }
            }, 200);

            upsocket.onerror = function (event) {
                upsocket.close(1000, "error");
                image.attr('src', init_src);
            };
        };

        var file_input = $('<input type="file" accept=".jpg, .jpeg, .png, .gif" />');
        file_input.on('change', function () {
            var error = [];
            var file = this.files[0];
            if (file.type === undefined || !file.type.match(/image\/(jpg|jpeg|png|gif)/)) {
                error.push(lang.get('FILE_TYPE_ERROR', file.name));
            } else {
                send(file);
            }
            if (error.length > 0) {
                alert(error.join("<br/>"));
            }
            file_input.remove();
        }).trigger('click');
    },
    /**
     *
     * @param choose
     * @param success
     * @param progress
     * @param preview
     * @param error
     * @param file
     */
    uploader: function (choose, success, progress, preview, error, file) {

        var def = function () {
        };
        choose = (choose === null || choose === undefined) ? def : choose;
        success = (success === null || success === undefined) ? def : success;
        error = (error === null || error === undefined) ? def : error;
        progress = (progress === null || progress === undefined) ? def : progress;
        preview = (preview === null || preview === undefined) ? def : preview;


        var send = function (file) {
            if (WebSocket===undefined) {
                return;
            }

            var upsocket = new WebSocket(constants.apiurl.replace(/^http/, 'ws') + '/up');
            var reader = new FileReader();
            reader.onload = function (e) {
                upsocket.send(e.target.result);
            };
            reader.onabort = function () {
                upsocket.close(1000, "cancel");
            };

            preview(file);

            upsocket.onmessage = function (event) {
                try {
                    var data = eval("(" + event.data + ")");
                    if (data.id === undefined) {
                        progress(data.percent);
                    } else {
                        upsocket.close(1000, "done");
                        success(data);
                    }
                } catch (e) {
                    upsocket.close(1000, "error");
                }
            };
            var interval = setInterval(function () {
                if (upsocket.readyState === 1) {
                    clearInterval(interval);
                    upsocket.send(JSON.stringify({type: file.type, size: file.size, name: file.name}));
                    reader.readAsArrayBuffer(file);
                }
            }, 200);

            upsocket.onerror = function (event) {
                upsocket.close(1000, "error");
                error();
            };
        };

        if (file !== null && file !== undefined) {
            var errors = [];
            if (file.type === undefined || constants.files_type.indexOf(file.type) === -1) {
                errors.push(lang.get('FILE_TYPE_ERROR', file.name));
                error(errors);
            } else {
                choose(file);
                send(file);
            }
            return;
        }


        var file_input = $('<input type="file" />').css({
            position: 'absolute',
            width: 1,
            height: 1,
            right: 0,
            bottom: 0
        }).attr('accept', constants.files_type.join(', '));
        $(document.body).append(file_input);
        file_input.on('change', function () {
            var errors = [];
            var file = this.files[0];
            if (file.type === undefined || constants.files_type.indexOf(file.type) === -1) {
                errors.push(lang.get('FILE_TYPE_ERROR', file.name));
                error(errors);
            } else {
                choose(file);
                send(file);
            }
            file_input.remove();
        }).trigger('click');
    }
};