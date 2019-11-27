var data = {
    /**
     * Save data at key
     *
     * @param key where store data
     * @param data who stored at key
     * @param temp is for session ?
     */
    set: function (key, data, temp) {
        var store = temp === true ? window.sessionStorage : window.localStorage;
        if (store === undefined) {
            return;
        }
        store.setItem("data_" + key, JSON.stringify(data));
    },
    /**
     * Get data at key
     *
     * @param key where store data
     * @param temp is for session ?
     * @param def failure value
     */
    get: function (key, temp, def) {
        var store = temp === true ? window.sessionStorage : window.localStorage;
        if (store === undefined) {
            return (def !== undefined) ? def : null;
        }
        key = "data_" + key;
        if (store.getItem(key) !== null) {
            return JSON.parse(store.getItem(key));
        } else {
            return (def !== undefined) ? def : null;
        }
    },
    /**
     * Remove data at key
     *
     * @param key where to remove
     * @param temp is for session ?
     */
    remove: function (key, temp) {
        var store = temp === true ? window.sessionStorage : window.localStorage;
        if (store === undefined) {
            return null;
        }
        store.removeItem("data_" + key)
    }
};