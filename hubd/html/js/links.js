sys.links = {
    make: function (where, link) {
        var close = $('<span class="close"/>').html('$svg.fa_icon_close');
        where.html("");
        close.on('click', function () {
            where.html($('<input type="hidden" name="link_remove" value="true" />'));
        });
        var link_image = $('<input type="hidden" name="link_image"/>');

        var thumb = $('<div class="thumb" />');

        var title = $('<h2 name="link_title" contenteditable="true" />').html(link.title).attr('title', lang.get('EDITABLE'));
        where.append(close).append(title);
        where.append(thumb).append(link_image);


        var description = $('<p name="link_description" contenteditable="true" />').html(link.description).attr('title', lang.get('EDITABLE'));
        where.append(description);

        if (link.logos === undefined) {
            link.logos = [];
            if (link.logo !== undefined) {
                link.logos.push(link.logo);
            }
        }
        if (link.logos.length > 0) {
            var img = $('<img width="220" height="160" />');
            thumb.append(img);
            var set = function (logo) {
                logo = encodeURI(logo.replaceAll("^([^\\#]+).*", "$1")).replace("?", encodeURIComponent("?"));
                img.removeAttr('src');
                img.attr('src', constants.cdnurl + '/files/' + logo + '@220x160.jpg');
                link_image.val(logo);
            };
            if (link.logos !== undefined && link.logos.length > 0) {

                set(link.logos[0]);
                if (link.logos.length > 1) {
                    var position = 0;
                    var choice = $('<div class="choice" />');
                    choice.append($('<span />').append('$svg.fa_icon_chevron_left').on('click', function () {
                        position = position === 0 ? link.logos.length - 1 : Math.max(0, position - 1);
                        set(link.logos[position]);
                    }));
                    choice.append($('<span />').append('$svg.fa_icon_chevron_right').on('click', function () {
                        position = position === link.logos.length - 1 ? 0 : position + 1;
                        set(link.logos[position]);
                    }));
                    choice.append($('<span />').css({marginLeft: 5}).append('$svg.fa_icon_close').on('click', function () {
                        thumb.remove();
                        link_image.remove();
                    }));
                    thumb.prepend(choice);
                }

            }

        } else {
            thumb.remove();
        }

        where.append($('<div class="url" name="link_url" />').val(link.url).append($('<a/>').attr('href', link.url).text(link.url.replace(/^(https?:\/\/)?([^/]+)\/.*/, '$2'))));


    },
    timeout: -1,
    preview: function (url, preview, success) {
        clearTimeout(sys.links.timeout);
        sys.links.timeout = setTimeout(function () {
            api.post("/tools", {
                action: 'scrap',
                url: url
            }, function (rez) {
                if (typeof preview === 'function') {
                    preview(rez);
                } else {
                    sys.links.make(preview, rez);
                    if (success !== undefined) {
                        success(rez);
                    }

                }
            }, log);
        }, 700);
    }
};