var api = {
	post: function (url, data, successFunc, errorFunc, api) {
		if ( api ===  undefined) {
			api = true;
		}
		if (data !== undefined && data.lng === undefined) {
			data.lng = sys.lng
		}
		var xhrapi = $.ajax({
			type: "POST",
			url: (api ? constants.apiurl : '') + url,
			dataType: "json",
			xhrFields: {
				withCredentials: true
			},
			headers: {"Content-Type": "application/json", "X-Requested-With": "XMLHttpRequest"},
			data: JSON.stringify(data),
			success: function (json) {
				if ( successFunc !==  undefined) {
					try {
						successFunc(json);
					} catch (e) {
						log(e);
					}
				}
			},
			error: function (e) {
				if (e.statusText !== 'abort') {
					if ( errorFunc !==  undefined) {
						try {
							errorFunc(e.responseJSON);
						} catch (e) {
							log(e);
						}
					}
					sys.wait(false);
				}
			}
		});
		return xhrapi;
	},
	get: function (url, data, successFunc, errorFunc, api) {

		if (typeof data === "function") {
			successFunc = data;
			errorFunc = successFunc;
			api = errorFunc;
		}
		if ( api ===  undefined) {
			api = true;
		}
		var xhrapi = $.ajax({
			type: "GET",
			url: (api ? constants.apiurl : '') + url,
			dataType: "json",
			xhrFields: {
				withCredentials: true
			},
			headers: {"Content-Type": "application/json", "X-Requested-With": "XMLHttpRequest"},
			data: data,
			success: function (json) {
				if ( successFunc !==  undefined) {
					try {
						successFunc(json);
					} catch (e) {
						log(e);
					}
				}

			},
			error: function (e) {
				if (e.statusText !== 'abort') {
					if ( errorFunc !==  undefined) {
						try {
							errorFunc(e.responseJSON);
						} catch (e) {
							log(e);
						}
					}
					sys.wait(false);
				}
			}
		});
		return xhrapi;
	},
	paginer: function (url, ul, liCreator, data, key) {

		var scrollerFinder = function () {
			return ul.isScrollable() ? ul : ul.scrollParent();
		};

		var scroller = scrollerFinder();
		var paging;
		var loading = sys.loading(30, 'li');
		var get = function () {
			scroller.off('scroll.paginer');
			ul.append(loading);
			api.get(url, {paging: paging}, make);
		};
		var make = function (data) {
			if (key !== undefined && data[key] !== undefined) {
				data = data[key];
			}
			loading.detach();
			if (data.result.length === 0 && paging === undefined) {
				ul.html($('<li/>').html(lang.get('EMPTY')));
				return;
			}

			var li;
			$.each(data.result, function (i, item) {
				li = liCreator(item);
				ul.append(li);
				ajax.it(li);
			});

			paging = data.paging.next;
			if (paging !== undefined) {
				scroller = scrollerFinder();
				scroller.on('scroll.paginer', function (e) {
					if (li.isInView()) {
						get();
					}
				}).trigger('scroll');
			}

		};

		if (data !== undefined) {
			make(data);
		} else {
			get();
		}
	}
};