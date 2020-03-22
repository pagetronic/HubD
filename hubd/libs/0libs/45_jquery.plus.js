/*!
 *
 *      Copyright 2016 PAGE Laurent
 *      Released under no license
 *
 */

$.fn.pulse = function (delay, after) {
	if (delay === undefined) {
		delay = 100;
	} else {
		delay = delay / 2;
	}
	var ele = this;
	ele.stop(true);
	ele.fadeTo(delay, 0.3, function () {
		ele.fadeTo(delay, 1, function () {
			ele.css({
				opacity: ''
			});
			if (after !== undefined) {
				after();
			}
		});
	});
	return this;
};

$.fn.formize = function () {
	var data = this.find(':input').filter(function (index, element) {
		return $(element).val() !== "";
	}).serialize();
	return data !== "" ? '?' + data.replaceAll('%2C', ',') : "";
};

$.fn.slowRemove = function (time, func) {
	if (time === 0) {
		this.remove();
		if (func !== undefined) {
			func();
		}
		return;
	}
	if (typeof time === "function") {
		func = time;
		time = 300;
	}
	if (time === undefined) {
		time = 300;
	}
	this.fadeOut(time, function () {
		this.remove();
		if (func !== undefined) {
			func();
		}
	});
	return this;
};

$.fn.autosize = function () {

	var correcter = function (ele) {
		ele = $(ele);
		ele.css({'overflow-y': 'hidden', 'height': 'auto'});
		var scrollHeight = ele[0].scrollHeight;
		var prev_heigh = ele.height();
		ele.removeAttr('style');
		if (scrollHeight === null) {
			scrollHeight = ele.prop('scrollHeight');
		}
		scrollHeight = scrollHeight - parseInt(ele.css('padding-top')) - parseInt(ele.css('padding-bottom'));
		ele.height(scrollHeight);
		return scrollHeight - prev_heigh;
	};

	this.on('input change', function () {
		correcter(this);
	});

	for (var i = 0; i < this.length; i++) {
		correcter(this[i]);
	}
	return this;
};

$.fn.isScrollable = function () {
	try {
		return this[0].scrollHeight > this[0].clientHeight;
	} catch (e) {
		return false;
	}
};
$.fn.isScrollBottom = function () {
	try {
		return this[0].scrollHeight === this[0].clientHeight + this[0].scrollTop;
	} catch (e) {
		return false;
	}
};


$.fn.isInView = function () {
	var parent_rect = this.scrollParent()[0].getBoundingClientRect();
	var current_rect = this[0].getBoundingClientRect();
	return parent_rect.y + parent_rect.height >= current_rect.y;
};
