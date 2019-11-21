var data = {
	set: function (key, data, temp) {
		var store = (temp !== undefined && temp) ? window.sessionStorage : window.localStorage;
		if (store === undefined) {
			return;
		}
		key = "data_" + key;
		store.setItem(key, JSON.stringify(data));
	},
	get: function (key, temp, def) {
		var store = (temp !== undefined && temp) ? window.sessionStorage : window.localStorage;
		if ( store ===  undefined) {
			return (def !== undefined) ? def : null;
		}
		key = "data_" + key;
		if (store.getItem(key) !== null) {
			return JSON.parse(store.getItem(key));
		} else {
			return (def !== undefined) ? def : null;
		}
	},
	remove: function (key, temp) {
		var store = (temp !== undefined && temp) ? window.sessionStorage : window.localStorage;
		if ( store ===  undefined) {
			return null;
		}
		key = "data_" + key;
		store.removeItem(key)
	}
}