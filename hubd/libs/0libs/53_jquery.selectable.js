(function ($) {

    var blur = 'blur';
    var blurtimeout = 700;

    var selectClass = function (element, options) {
        var search_term = null;
        if (options === undefined) {
            options = {};
        }
        var seclss = {
            element: element,
            options: options,
            debug: '',

            pop: $('<div class="popSelectable" />'),
            search: $('<input type="text" class="search" />').attr('placeholder', lang.get('SEARCH').ucfirst()).hide(),
            list: $('<ul class="quickSelectable"/>'),
            wait: $('<li class="disable">' + lang.get('LOADING') + '</li>').addClass('placeholder'),
            xhr: {
                abort: function () {

                }
            },
            selector: $('<div />'),
            keyboardlist: function (list, event) {
                if (event.keyCode === 38 || event.keyCode === 40 || event.keyCode === 13) {
                    var lis = list.find('li');
                    if (list.find('li.active').length === 0) {
                        var li;
                        if (event.keyCode === 38) {
                            li = lis.eq(lis.length - 1);
                            li.addClass('active');
                        } else if (event.keyCode === 40) {
                            li = lis.eq(0);
                            li.addClass('active');
                        } else {
                            return true;
                        }
                        try {
                            list.scrollTop(li.offset().top - list.offset().top + list.scrollTop() - list.height() / 2 + li.height());
                        } catch (e) {
                            return true;
                        }
                        return false;
                    }
                    var active = list.find('li.active').eq(0);
                    var pos = list.find("li").index(active);
                    lis.removeClass('active');

                    if (event.keyCode === 38) {
                        var li = lis.eq(pos - 1);
                        if (li.length === 1) {
                            li.addClass('active');
                        } else {
                            li = lis.last();
                            li.addClass('active');
                        }
                        try {
                            list.scrollTop(li.offset().top - list.offset().top + list.scrollTop() - list.height() / 2 + li.height());
                        } catch (e) {
                            return true;
                        }
                    } else if (event.keyCode === 40) {
                        var li = lis.eq(pos + 1);
                        if (li.length === 1) {
                            li.addClass('active');
                        } else {
                            li = lis.first();
                            li.addClass('active');
                        }
                        try {
                            list.scrollTop(li.offset().top - list.offset().top + list.scrollTop() - list.height() / 2 + li.height());
                        } catch (e) {
                            return true;
                        }
                    } else {
                        lis.eq(pos).trigger('mousedown').trigger('click');
                    }
                    return false;
                }
            },
            identifyTitle: function (result) {
                var title = result.name;
                if (title === undefined) {
                    title = result.title;
                }
                if (title === undefined) {
                    title = result.id;
                }
                return title;
            },
            getFilter: function () {
                if (typeof seclss.options.filter === 'function') {
                    return seclss.options.filter();
                }
                var filter = seclss.options.filter;
                if (filter === undefined) {
                    filter = [];
                }
                if (seclss.element.attr("filter") !== undefined) {
                    $.each(seclss.element.attr("filter").split(','), function () {
                        filter.push(this);
                    });
                    if (seclss.element.attr('multiple') !== undefined && seclss.element.attr('multiple') === 'multiple') {
                        $.each(seclss.element.val(), function () {
                            filter.push(this);
                        });
                    } else {
                        filter.push(seclss.element.val())
                    }
                }
                if (typeof seclss.element.val() === 'object') {
                    $.each(seclss.element.val(), function () {
                        filter.push(this);
                    });
                } else {
                    filter.push(seclss.element.val());
                }

                return filter.length > 0 ? filter : null;
            },
            loadData: function (select, url, search, id, list, paging, append) {

                if (append === undefined || append === false) {
                    list.html(seclss.wait);
                } else {
                    list.append(seclss.wait);
                }
                try {
                    seclss.xhr.abort();
                } catch (e) {
                }

                seclss.rePosition();
                seclss.xhr = api.post(url, {
                    action: 'search',
                    search: search,
                    parent: id,
                    paging: paging,
                    filter: seclss.getFilter(),
                    lng: sys.lng,
                    domain: document.location.hostname
                }, function (rez) {
                    seclss.wait.detach();
                    seclss.makeItems(url, rez.result, list, select);
                    if (rez.paging !== undefined && rez.paging.next !== undefined) {
                        list.on('scroll', function () {
                            if (this.scrollTop + list.innerHeight() + 20 > this.scrollHeight) {
                                list.off('scroll');
                                seclss.loadData(select, url, search, id, list, rez.paging.next, true);
                            }
                        }).trigger('scroll');
                    }
                    seclss.rePosition();
                }, function () {
                    list.html('<li>' + lang.get('ERROR') + '</li>');
                    seclss.rePosition();
                });
            },
            makeItems: function (url, items, list, select) {


                $.each(items, function (i, item) {

                    var li = $('<li/>');
                    var title = seclss.identifyTitle(item);

                    if (item.svg !== undefined) {

                        title = item.svg + title;

                    } else if (item.logo !== undefined) {
                        var logo = item.logo;
                        if (logo.startsWith("http")) {
                            logo = '<img src="' + logo + '@16x16" />';
                        }
                        title = logo + title;
                    }
                    if (item.infos !== undefined) {
                        title += '<small>' + item.infos + '</small>';
                    }

                    if (item.uitools !== undefined) {
                        li.attr('style', item.uitools);
                    }
                    if (item.className !== undefined) {
                        li.addClass(item.className);
                    }

                    li.append($('<span/>').html(title));
                    if (item.childrens !== undefined && item.childrens > 0) {
                        var childrens = $('<span class="childrensSelectable" />').html(item.childrens);
                        li.prepend(childrens);
                        childrens.one('mousedown click', function (e) {
                            childrens.off('mousedown click');
                            e.stopPropagation();
                            e.preventDefault();
                            var sub = $('<ul/>');
                            li.append(sub);
                            sub.append(seclss.wait);
                            sub.css({
                                width: li.outerWidth(),
                                top: list.position().top + li.position().top + parseInt(list.css('margin-top')) + childrens.height(),
                                left: list.position().left + childrens.width()
                            });

                            seclss.loadData(select, url, null, item.id, sub, null, false);

                            return false;
                        });

                    }

                    li.one('mousedown click', function (e) {
                        li.off('mousedown click');
                        if (e.button !== undefined && e.button > 1) {
                            return;
                        }

                        e.stopPropagation();
                        e.preventDefault();
                        select(item.id, title, item);
                        if (seclss.options.selection !== undefined) {
                            seclss.options.selection(item);
                        }
                        seclss.selector.hide(200);
                        li.remove();
                        if (list.find('li').length === 0) {
                            search_term = null;
                        }
                        return false;


                    });
                    list.append(li);
                });

                if (list.find('li').length === 0) {
                    list.append('<li class="disable">' + lang.get('NO_RESULTS') + '</li>');
                }
                seclss.rePosition();
            },
            rePosition: function () {

                if (seclss.selector.hasClass('coverSelectable')) {
                    return;
                }
                var pop_top = seclss.pop.offset().top;
                var sp_height = $(window).innerHeight();
                var pop_height = seclss.pop.outerHeight();
                if (navigator.userAgent.match(/Android/i) || navigator.userAgent.match(/webOS/i) || navigator.userAgent.match(/iPhone/i)
                    || navigator.userAgent.match(/iPad/i) || navigator.userAgent.match(/iPod/i) || navigator.userAgent.match(/BlackBerry/i)
                    || navigator.userAgent.match(/Windows Phone/i)) {
                    seclss.selector.removeClass('above').addClass('coverSelectable');

                } else if (pop_top + pop_height > sp_height && pop_top - pop_height > 0) {
                    seclss.selector.addClass('above');

                } else if (pop_top + pop_height < sp_height) {
                    seclss.selector.removeClass('above');

                } else {
                    seclss.selector.removeClass('above').addClass('coverSelectable');

                }


            },
            quickSelectable: function () {
                seclss.element.attr('noajax', true);

                seclss.element.on('click', function () {

                    seclss.selector.addClass('divSelectable');

                    seclss.pop.append(seclss.search).append(seclss.list);

                    seclss.selector.append(seclss.pop);

                    var position = seclss.element.offset();

                    $(this).append(seclss.selector.show());

                    seclss.search.show().focus();

                    var timer = -1;
                    seclss.search.on('keydown', function (e) {
                        return seclss.keyboardlist(seclss.list, e);
                    }).on('input', function () {
                        clearTimeout(timer);
                        if (search_term === seclss.search.val()) {
                            return true;
                        }
                        search_term = seclss.search.val();
                        seclss.list.html(seclss.wait);
                        timer = setTimeout(function () {
                            seclss.loadData(seclss.options.select, seclss.options.url, search_term, null, seclss.list, null, false);
                        }, 700);
                    }).trigger('input');

                    seclss.search.one(blur, function () {
                        var timerhide = setTimeout(function () {
                            seclss.selector.hide(200);
                            if (seclss.options.blur !== undefined) {
                                seclss.options.blur();
                            }
                        }, blurtimeout);
                        seclss.search.one('focus', function () {
                            clearTimeout(timerhide);
                        });
                    });


                    var query = '';
                    if (seclss.search.val() === '' && seclss.options.query !== undefined) {
                        if (typeof seclss.options.query === 'function') {
                            query = seclss.options.query();
                        } else {
                            query = seclss.options.query;
                        }
                        search_term = null;
                        seclss.search.val(query).trigger('input');
                    }

                    return false;
                });
            },
            selectSelectable: function () {

                seclss.pop.addClass('rel');

                var multiple = seclss.element.attr('multiple') !== undefined && seclss.element.attr('multiple') === 'multiple';

                seclss.selector.attr('class', seclss.element.attr('class'));
                seclss.selector.addClass('divSelectable');
                seclss.selector.addClass('select');
                seclss.element.removeAttr('class');


                seclss.element.data('selector', seclss.selector);
                seclss.element.css({position: 'fixed', top: -1300, left: -1300});

                var text = $('<span class="text" />');
                seclss.selector.append(text);

                var add = $('<span class="add" />');

                if (seclss.element.attr('add') !== undefined) {
                    add.html('$svg.mi_add').on('click', function () {
                        eval(seclss.element.attr('add'))(function (id, name) {

                            seclss.element.find('option[value=' + id + ']').remove();
                            seclss.element.append('<option>' + name + '</option>').val(id).trigger('change')
                        });
                        return false;
                    });
                    seclss.selector.append(add);
                } else if (seclss.options.add !== undefined) {
                    add.html('$svg.mi_add').on('click', function () {
                        seclss.options.add(function (id, name) {

                            seclss.element.find('option[value=' + id + ']').remove();
                            seclss.element.append('<option>' + name + '</option>').val(id).trigger('change')
                        });
                        return false;
                    });
                    seclss.selector.append(add);
                }

                var clearfunc = function () {
                    remover.hide();
                    if (seclss.element.attr('placeholder') !== undefined) {
                        text.html(seclss.element.attr('placeholder')).addClass('placeholder');
                    } else {
                        text.html(lang.get('CHOOSE')).addClass('placeholder');
                    }
                    seclss.element.val(null).trigger('change');
                    return false;
                };

                var remover = $('<span class="clear" />').html('$svg.mi_clear').hide().on('click', clearfunc);
                seclss.element.on('clear', clearfunc);
                seclss.selector.append(remover);


                var url = (seclss.options.url !== undefined) ? seclss.options.url : seclss.element.attr('url');

                if (url !== undefined) {
                    seclss.pop.append(seclss.search);
                } else {
                    url = null;
                }


                seclss.pop.append(seclss.list).hide();


                seclss.selector.append(seclss.pop);
                if (options.position === 'before') {
                    seclss.element.before(seclss.selector);
                } else {
                    seclss.element.after(seclss.selector);
                }

                var pse = {
                    abort: function () {

                    }
                };


                var setValue = function () {

                    if (seclss.element.val() === null) {
                        text.html(seclss.element.attr('placeholder')).addClass('placeholder');
                        return;
                    }
                    if (multiple && seclss.element.val().length > 0) {
                        text.html('').removeClass('placeholder');
                        seclss.selector.removeClass('multiple');
                        if (seclss.element.val().length === 1) {
                            var opt = seclss.element.find('option:selected');
                            var icon = opt.attr('icon');
                            var html = opt.html();
                            if (icon !== undefined) {
                                html = icon + html;
                            }
                            text.html(html).removeClass('placeholder');

                            remover.show();
                        } else {
                            seclss.selector.addClass('multiple');

                            text.find('.val').remove();
                            $.each(seclss.element.val(), function () {
                                var value = this;
                                var option = seclss.element.find('option[value=' + value + ']');
                                option.attr('selected', 'selected');
                                var sel = $('<span class="val" />').html(option.html()).attr('value', value);
                                sel.append($('<span class="clear" />').html('$svg.mi_clear').on('click', function () {
                                    var current = value;
                                    sel.slowRemove(function () {
                                        search_term = null;
                                        seclss.element.find('option[value=' + current + ']').remove();
                                        var values = seclss.element.val();
                                        if (values.indexOf(current) > -1) {
                                            values.splice(values.indexOf(current), 1);
                                        }
                                        seclss.element.val(values).trigger('change');
                                    });
                                    return false;
                                }));
                                text.append(sel);
                            });
                            try {
                                text.sortable('destroy');
                            } catch (e) {
                            }
                            text.sortable({
                                cursor: 'ew-resize',
                                items: '.val',
                                containment: text,
                                axis: "x",
                                update: function () {
                                    var vals = [];
                                    text.find('.val[value]').each(function () {
                                        var value = $(this).attr('value');
                                        vals.push(value);
                                        seclss.element.append(seclss.element.find('option[value=' + value + ']').first().attr('selected', 'selected'));
                                    });
                                    seclss.element.val(vals).trigger('change');
                                }
                            });
                            remover.hide();
                        }

                    } else if (!multiple && seclss.element.val() !== undefined && seclss.element.val() !== null && seclss.element.val() !== '') {

                        var opt = seclss.element.find('option:selected');
                        var icon = opt.attr('icon');
                        var html = opt.html();
                        if (icon !== undefined) {
                            html = icon + html;
                        }
                        text.html(html).removeClass('placeholder');


                        if (url != null || seclss.element.find('option:not([value])').length > 0) {
                            remover.show();
                        } else {
                            remover.hide();
                        }

                    } else if (seclss.element.attr('placeholder') !== undefined) {
                        text.html(seclss.element.attr('placeholder')).addClass('placeholder');
                        remover.hide();
                    } else {
                        text.html(lang.get('CHOOSE')).addClass('placeholder');
                        remover.hide();
                    }
                };

                seclss.element.on('change', setValue);

                if (seclss.element.attr('value') !== undefined && seclss.element.attr('value') !== '' && seclss.element.attr('url') === undefined) {
                    seclss.element.find('option').removeAttr('selected').each(function () {
                        if (seclss.element.attr('value') === this.value) {
                            $(this).attr('selected', 'selected');
                            remover.show();
                        }
                    });
                }


                var remote = function () {
                    var timer = -1;
                    if (url !== null) {
                        seclss.search.one(blur, function () {
                            var timerhide = setTimeout(function () {
                                try {
                                    pse.abort();
                                } catch (e) {
                                }
                                seclss.selector.hide(200);
                            }, blurtimeout);

                            seclss.search.one('focus', function () {
                                clearTimeout(timerhide);
                            });
                        }).off('keydown').on('keydown', function (e) {
                            return seclss.keyboardlist(seclss.list, e);
                        }).off('input').on('input', function () {
                            clearTimeout(timer);
                            if (search_term === seclss.search.val()) {
                                return true;
                            }
                            search_term = seclss.search.val();
                            seclss.list.html(seclss.wait);
                            timer = setTimeout(function () {
                                seclss.loadData(function (id, name) {
                                    seclss.element.find('option[value=' + id + ']').remove();
                                    seclss.element.append($('<option selected="selected"/>').html(name).attr('value', id)).trigger('change');
                                    setValue();
                                    $(this).remove();
                                }, url, search_term, null, seclss.list, null, false);
                            }, 300);
                        }).trigger('input');


                    } else {

                        seclss.element.one(blur, function () {
                            var timerhide = setTimeout(function () {
                                try {
                                    pse.abort();
                                } catch (e) {
                                }
                                seclss.selector.hide(200);
                            }, blurtimeout);
                            seclss.search.one('focus', function () {
                                clearTimeout(timerhide);
                            });
                        }).off('keydown').on('keydown', function (e) {
                            return seclss.keyboardlist(seclss.list, e);
                        });

                    }

                };

                seclss.selector.on('click', function () {

                    seclss.selector.show();

                    if (url !== null) {
                        seclss.search.focus();
                    } else {
                        var options = [];
                        seclss.element.find('option').each(function () {
                            var opt = $(this);
                            var html = opt.html();
                            var value = opt.attr('value');
                            var icon = opt.attr('icon');
                            var style = opt.attr('style');
                            if (value === undefined) {
                                value = html;
                            }
                            if (icon !== undefined) {
                                html = icon + html;
                            }

                            if (html !== "") {
                                options.push({name: html, id: value, style: style});
                            }
                        });
                        seclss.list.find('li').remove();
                        seclss.makeItems(url, options, seclss.list, function (id, name) {
                            seclss.element.val(id).trigger('change');
                        });
                        seclss.element.focus();

                    }
                    seclss.rePosition();
                });

                seclss.selector.show = function () {
                    seclss.selector.removeClass('error_input');
                    seclss.selector.css({minWidth: seclss.selector.width()});
                    add.hide();
                    remover.hide();
                    if (url !== null) {
                        text.hide();
                        seclss.selector.addClass('input');

                        seclss.search.show().trigger('input');
                        seclss.search.focus();
                    } else {
                        seclss.element.focus()
                    }
                    seclss.pop.show();
                    seclss.selector.addClass('open');
                    remote();
                    return seclss.selector;
                };

                seclss.selector.hide = function () {
                    seclss.selector.css({minWidth: ''});
                    seclss.pop.find('.subSelectable').remove();
                    seclss.selector.removeClass('open');

                    seclss.pop.hide();
                    seclss.search.hide();
                    add.show();
                    text.show();
                    setValue();
                    seclss.element.focus();
                    return seclss.selector;
                };

                seclss.selector.hide();

                if (seclss.element.attr('url') !== undefined) {
                    seclss.element.on('search', function (event, values, after) {

                        if (typeof values === 'string') {
                            values = [values];
                        }
                        text.html(lang.get('LOADING')).removeClass('placeholder').addClass('blink');

                        var addVal = function (i) {
                            try {
                                api.post(url, {
                                    action: 'search',
                                    search: values[i],
                                    value: true,
                                    domain: document.location.hostname
                                }, function (rez) {
                                    if (rez.result !== undefined && rez.result.length > 0) {
                                        var title = seclss.identifyTitle(rez.result[0]);
                                        seclss.element.find('option[value=' + rez.result[0].id + ']').remove();
                                        seclss.element.append($('<option selected="selected"/>').html(title).val(rez.result[0].id));
                                    } else {
                                        values.slice(i, 1);
                                    }
                                    i++;
                                    if (i < values.length) {
                                        addVal(i);
                                    } else {

                                        setValue();
                                        text.removeClass('blink');

                                        if (after !== undefined) {
                                            after();
                                        }
                                    }
                                });
                            } catch (e) {
                                text.removeClass('blink');
                                log('error addValue in selectable()');
                            }
                        };

                        addVal(0);

                    });

                    if (seclss.element.attr('value') !== undefined && seclss.element.attr('value') !== '') {
                        seclss.element.trigger('search', seclss.element.attr('value'));
                    }
                }

                seclss.selector.on('reload', function () {
                    search_term = null;
                    seclss.list.html('');
                });

                seclss.element.on('keypress', function (e) {
                    if (e.keyCode === 32) {
                        seclss.selector.show();
                    }
                });


            }

        };
        $(window).on('resize', seclss.rePosition);
        return seclss;
    };

    $.fn.selectable = function (options) {
        var tagName = this.prop("tagName");
        if (tagName === undefined || tagName.match(/^select$/i) === null && this.find('select').length === 0) {
            selectClass(this, options).quickSelectable();
        } else {
            var selects = this.find('select');
            if (selects.length === 0) {
                selects = this;
            }
            selects.each(function () {
                selectClass($(this), options).selectSelectable();
            });
        }
        return this;
    };

})(jQuery);

