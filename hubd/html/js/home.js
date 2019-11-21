sys.home = {
    pagination: {
        pages: function (paging) {
            if (paging.next === undefined) {
                return;
            }
            var scrollable = $('#lateral');
            var ul = scrollable.find('ul');
            var paginer = scrollable.find('li').last();
            scrollable.on('scroll', function () {
                if (paginer !== null && paginer.is(':in-viewport')) {
                    paginer = null;
                    var loading = $('<li/>').append(sys.loading(30, 'div'));
                    ul.append(loading);
                    api.get('/pages', {paging: paging.next}, function (rez) {
                        loading.remove();
                        if (rez.paging.next !== undefined) {
                            paging = rez.paging;
                        } else {
                            scrollable.off('scroll');
                        }
                        for (var i in rez.result) {
                            var item = rez.result[i];
                            var li = $('<li />');

                            var a = $('<a/>').attr('href', item.url);
                            if (item.logo !== undefined) {
                                a.append('<img src="' + item.logo + '@40x25" width="40" height="25" />')
                            } else {
                                a.append('$svg.fa_icon_newspaper_o');
                            }
                            a.append((item.title !== undefined) ? item.title : 'no title');

                            li.append(a);

                            if (item.intro !== undefined) {
                                li.append($('<p/>').html(item.intro.replace(/<\/?([a-z]+)>/, '')));
                            }

                            ul.append(li);
                            paginer = li;
                        }
                        scrollable.trigger('scroll');
                        sys.svg();
                    })
                }
            }).trigger('scroll');
        }
    }


};
