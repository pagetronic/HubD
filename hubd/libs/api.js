var api = {
    /**
     * Post data to Api
     * @param url path of the request
     * @param data object to send to api
     * @param successFunc function to execute on success
     * @param errorFunc function to execute on error
     * @param api set to false if request is on the current domain
     * @returns XMLHttpRequest object
     */
    post: function (url, data, successFunc, errorFunc, api) {
        if (api === undefined) {
            api = true;
        }
        if (data !== undefined && data.lng === undefined) {
            data.lng = sys.lng
        }
        return $.ajax({
            type: "POST",
            url: (api ? constants.apiurl : '') + url,
            dataType: "json",
            xhrFields: {
                withCredentials: true
            },
            headers: {"Content-Type": "application/json", "X-Requested-With": "XMLHttpRequest"},
            data: JSON.stringify(data),
            success: function (json) {
                if (successFunc !== undefined) {
                    try {
                        successFunc(json);
                    } catch (e) {
                        log(e);
                    }
                }
            },
            error: function (e) {
                if (e.statusText !== 'abort') {
                    if (errorFunc !== undefined) {
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
    },
    /**
     * Get data from Api
     * @param url path of the request
     * @param data object to send to api (query parameters)
     * @param successFunc function to execute on success
     * @param errorFunc function to execute on error
     * @param api set to false if request is on the current domain
     * @returns XMLHttpRequest object
     */
    get: function (url, data, successFunc, errorFunc, api) {

        if (typeof data === "function") {
            successFunc = data;
            errorFunc = successFunc;
            api = errorFunc;
        }
        if (api === undefined) {
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
                if (successFunc !== undefined) {
                    try {
                        successFunc(json);
                    } catch (e) {
                        log(e);
                    }
                }

            },
            error: function (e) {
                if (e.statusText !== 'abort') {
                    if (errorFunc !== undefined) {
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
    }
};