var ajax = {
	init: function () {
		$(window).on('popstate.ajax', function (event) {
			event = event.originalEvent;
			if (!$(document.body).hasClass('hide_loading')) {
				if (event !== undefined && event.state !== undefined && event.state !== null && event.state.ajax === true) {
					sys.wait(true);
					ajax.load(sys.uri(), true, undefined, function () {
						sys.wait(false);
					}, event.state.scroll);
					return;
				}
				if (document.location.hash.match("#.*")) {
					var center = $('#center');
					center.animate({
						scrollTop: $(document.location.hash).position().top - center.height() / 2
					}, 300);
					return;
				}

			}
			document.location.href = sys.uri();

		});
		if (!settings.get('noajax')) {
			sys.replaceState(document.location.href, true);
		}

	},
	hide: function () {
		var center = $('#center');
		history.pushState({
			ajax: false
		}, document.title, document.location.href);
		$(document.body).addClass('hide_loading').html(sys.loading(50).css({marginTop: 200}));

	},
	it: function (where) {
		if (where === undefined) {
			where = $('#menu, header, #center');
		}
		where.find("a[href][noajax]:not([onclick])").off('click.noajax').on('click.noajax', function (e) {
			if ($(this).attr('target') !== '_blank' && !$(this).attr('href').startsWith("/")) {
				ajax.hide();
			}
			return true;
		});

		where.find("a[href]:not([noajax]):not([onclick]):not([href^=\\/files\\/]):not([href^=http])").off('click.ajax').on('click.ajax', function (e) {
			if (e.ctrlKey || e.shiftKey || settings.get('noajax')) {
				return true;
			}
			e.preventDefault();
			e.stopPropagation();
			if ($(this).attr('noajax')) {
				return false;
			}

			var href = $(this).attr('href');
			if (href.startsWith('#')) {
				var center = $('#center');
				var dest = $(this.href);
				center.animate({
					scrollTop: $(this.href).position().top - center.height() / 3
				}, 300, function () {
					dest.pulse();
				});
			} else {
				ajax.load(href);
			}
			return false;
		});
	},
	get: function (url, func, error) {

		var headers = {
			'X-Requested-With': 'XMLHttpRequest'
		};
		$.ajax({
			url: url,
			headers: headers,
			success: func,
			error: error
		});
	},
	unload_func: null,
	unload: function (func) {
		if (func === undefined) {
			if (ajax.unload_func !== null) {
				ajax.unload_func();
				ajax.unload_func = null;
			}

		} else {
			ajax.unload_func = func;
		}
	},
	reload: function (silent, after) {
		ajax.load(sys.uri(), (silent !== undefined && silent), undefined, function () {
			if (typeof after === 'function') {
				after();
			}
		});
	},
	load: function (url, silent, success, after, scroll) {
		silent = silent === undefined ? false : silent;
		if (success === undefined || success === null) {
			success = function (html) {

				if (xhr.getResponseHeader('X-Title')) {
					document.title = $('<span />').html(decodeURIComponent(xhr.getResponseHeader('X-Title')).replace(/\+/g, ' ')).text();
				}
				ajax.unload();
				if (!silent) {
					try {
						var canonical = xhr.getAllResponseHeaders().match(/Link: ?<([^>]+)>; ?rel="?canonical"?/mi);
						if (canonical !== null && canonical.length > 0) {
							url = canonical[0].replace(/Link: ?<([^>]+)>; ?rel="?canonical"?/i, '$1');
						}
					} catch (e) {
						log(e);
					}
					sys.pushState(url);
					sys.stats.pageview();
				}

				var body = $(html);
				var center = $('#center');
				$('#menu').html(body.find('#menu').html());
				center.html(body.find('#center').html());
				$('header h1').html(body.find('header h1').html());
				$('header').attr('style', body.find('header').attr('style'));
				sys.load();

				sys.wait(false);

				if (scroll !== undefined) {
					center.scrollTo(scroll * center[0].scrollHeight);
				} else if (!silent) {
					center.scrollTo(0);
				}

				if (after !== undefined) {
					after();
				}

			}


		}

		xhr.abort();
		if (!silent) {
			sys.wait(true);
			sys.replaceState(document.location.href);
		}
		$('#menu').trigger('ajax');
		xhr = $.ajax({
			url: url,
			headers: {
				'X-Requested-With': 'XMLHttpRequest'
			},
			success: success,
			error: function () {
				document.location.href = url;
			}
		});
	}
};
