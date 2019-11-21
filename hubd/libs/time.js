var time = {

	init: function () {
		setInterval(function () {
			$('time').each(function () {
				try {
					$(this).html(time.since(this.attributes["datetime"].value, parseInt(this.attributes["level"].value)));
				} catch (e) {
				}
			});
		}, 10000);
	},
	since: function (from, level) {
		if (level === undefined) {
			level = 2;
		}
		if ((typeof from) === "string") {
			from = new Date(from);
		}
		var duration = Date.now() - from.getTime();
		var past = duration < 0;

		if (duration < 60000 && duration > -60000) {
			return lang.get("JUST_NOW");
		}
		duration = Math.abs(duration);
		var DAYS_PER_YEAR = 365.24225;
		var M_PER_SECOND = 1000;
		var M_PER_MINUTE = 60 * M_PER_SECOND;
		var M_PER_HOUR = 60 * M_PER_MINUTE;
		var M_PER_DAY = 24 * M_PER_HOUR;
		var M_PER_WEEKS = 7 * M_PER_DAY;
		var M_PER_MONTH = Math.floor((DAYS_PER_YEAR / 12) * M_PER_DAY);
		var M_PER_YEAR = Math.floor(DAYS_PER_YEAR * M_PER_DAY);


		var durationMillis = duration;

		var years = Math.floor(durationMillis / M_PER_YEAR);
		durationMillis = durationMillis - (years * M_PER_YEAR);

		var months = Math.floor(durationMillis / M_PER_MONTH);
		durationMillis = durationMillis - (months * M_PER_MONTH);

		var weeks = Math.floor(durationMillis / M_PER_WEEKS);
		durationMillis = durationMillis - (weeks * M_PER_WEEKS);

		var days = Math.floor(durationMillis / M_PER_DAY);
		durationMillis = durationMillis - (days * M_PER_DAY);

		var hours = Math.floor(durationMillis / M_PER_HOUR);
		durationMillis = durationMillis - (hours * M_PER_HOUR);

		var minutes = Math.floor(durationMillis / M_PER_MINUTE);
		durationMillis = durationMillis - (minutes * M_PER_MINUTE);

		var since = "";

		var space_num = " ";
		var space = "";
		while (level > 0) {
			var effect = false;
			if (years > 0) {
				since += space + years + space_num + (years > 1 ? lang.get("YEARS") : lang.get("YEAR"));
				years = 0;
				effect = true;
			} else if (months > 0) {
				since += space + months + space_num + (months > 1 ? lang.get("MONTHS") : lang.get("MONTH"));
				months = 0;
				effect = true;
			} else if (weeks > 0) {
				since += space + weeks + space_num + (weeks > 1 ? lang.get("WEEKS") : lang.get("WEEK"));
				weeks = 0;
				effect = true;
			} else if (days > 0) {
				since += space + days + space_num + (days > 1 ? lang.get("DAYS") : lang.get("DAY"));
				days = 0;
				effect = true;
			} else if (hours > 0) {
				since += space + hours + space_num + (hours > 1 ? lang.get("HOURS") : lang.get("HOUR"));
				hours = 0;
				effect = true;
			} else if (minutes > 0) {
				since += space + minutes + space_num + (minutes > 1 ? lang.get("MINUTES") : lang.get("MINUTE"));
				minutes = 0;
				effect = true;
			}
			level--;
			if (effect) {
				if (level === 1) {
					space = " " + lang.get("AND") + " ";
				} else {
					space = ", ";
				}
			}
		}
		if (!past) {
			return lang.get("SINCE_AGO", since);
		} else {
			return lang.get("SINCE_IN", since);
		}


	},
	format: function (date_) {
		var date = new Date(date_);
		return date.toLocaleString([], {
			weekday: "long",
			year: "numeric",
			month: "long",
			hour: "2-digit",
			minute: "2-digit",
			day: "2-digit"
		});
	},
	formatDate: function (date_) {
		var date = new Date(date_);
		return date.toLocaleString([], {
			day: "2-digit",
			year: "numeric",
			month: "long"
		});
	},
	formatDateShort: function (date_) {
		var date = new Date(date_);
		return date.toLocaleString([], {
			year: "2-digit",
			day: "2-digit",
			month: "2-digit"
		});
	},
	formatDateFull: function (date_) {
		var date = new Date(date_);
		return date.toLocaleString([], {
			weekday: "long",
			year: "numeric",
			month: "long",
			day: "numeric",
			hour: "2-digit",
			minute: "2-digit"
		});
	}

};