var settings = {
	update: function (settings) {
		if (settings === undefined) {
			return null;
		}
		var prev = sys.sets;
		sys.sets = settings;
		sys.convert.wallet();
		if (JSON.stringify(prev.ui_style) !== JSON.stringify(settings.ui_style) && sys.user.id !== null) {
			sys.profile.updateStyle();
		}
		if (prev.ui_color !== settings.ui_color) {
			sys.profile.uiColor();
		}
	},
	set: function (key, value) {

		if (key === undefined) {
			sys.sets = {};
			data.set('settings', {});
			socket.send({
				action: "settings",
				data: {}
			});
			return;
		}

		var datax = {};
		if (typeof key === 'string') {
			datax[key] = value;
		} else {
			datax = key;
		}
		for (var key in datax) {
			if (JSON.stringify(sys.sets[key]) !== JSON.stringify(datax[key])) {
				sys.sets[key] = datax[key];
				if (sys.user.id !== null) {
					socket.send({
						action: "settings",
						data: datax
					}, function () {
						if (typeof value === 'function') {
							value();
						}
					});
				}
			}
		}

		if (!sys.user.parent) {
			data.set('settings', sys.sets);
		}
	},
	get: function (key, def) {
		if (sys.sets === undefined || sys.sets[key] === undefined) {
			return def;
		} else {
			return sys.sets[key];
		}
	},
	unpush: function (where, value) {
		var data = [];
		if (sys.sets[where] !== undefined) {
			for (var key in sys.sets[where]) {
				data.push(sys.sets[where][key]);
			}
		}
		if (data.indexOf(value) > -1) {
			data.splice(data.indexOf(value), 1);
			var update = {};
			update[where] = data;
			settings.set(update);
		}
	},
	push: function (where, value) {
		var data = [];
		if (sys.sets[where] !== undefined) {
			for (var key in sys.sets[where]) {
				data.push(sys.sets[where][key]);
			}
		}
		if (data.indexOf(value) === -1) {
			data.push(value);
			var update = {};
			update[where] = data;
			settings.set(update);
		}

	},
	exist: function (where, value) {
		return (sys.sets !== undefined && sys.sets[where] !== undefined && sys.sets[where].indexOf(value) >= 0);
	}
};