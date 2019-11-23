var sys = (sys === undefined) ? {} : sys;
sys = $.extend({}, sys, {

        /**
         * Detect device and browser for client
         */
        device: function () {

            var device = navigator.appName;
            var ua = navigator.userAgent.toLowerCase();
            if (ua.indexOf("Edge".toLowerCase()) >= 0) {
                device = "Edge";
            } else if (ua.indexOf("Opera".toLowerCase()) >= 0) {
                device = "Opera";
            } else if (ua.indexOf("Chromium".toLowerCase()) >= 0) {
                device = "Chromium";
            } else if (ua.indexOf("Chrome".toLowerCase()) >= 0) {
                device = "Chrome";
            } else if (ua.indexOf("Safari".toLowerCase()) >= 0) {
                device = "Safari";
            } else if (ua.indexOf("Firefox".toLowerCase()) >= 0) {
                device = "Firefox";
            } else if (ua.indexOf("MSIE".toLowerCase()) >= 0) {
                device = "Internet Explorer";
            } else if (device === undefined || device === null) {
                device = "Unknown";
            }

            var os = navigator.platform;
            if (ua.indexOf("Windows Phone".toLowerCase()) >= 0) {
                os = "Windows Mobile";
            } else if (ua.indexOf("Windows".toLowerCase()) >= 0) {
                os = "Windows";
            } else if (ua.indexOf("Android".toLowerCase()) >= 0) {
                os = "Android";
            } else if (ua.indexOf("iPhone".toLowerCase()) >= 0 || ua.indexOf("iPad") >= 0) {
                os = "Apple iOs";
            } else if (ua.indexOf("MacOS".toLowerCase()) >= 0 || ua.indexOf("Mac OS") >= 0 || ua.indexOf("Macintosh") >= 0) {
                os = "Apple Macintosh";
            } else if (ua.indexOf("Ubuntu".toLowerCase()) >= 0) {
                os = "Ubuntu Linux";
            } else if (ua.indexOf("Debian".toLowerCase()) >= 0) {
                os = "Debian Linux";
            } else if (ua.indexOf("Linux".toLowerCase()) >= 0) {
                os = "Linux";
            } else if (ua.indexOf("Firefox".toLowerCase()) >= 0) {
                os = "Firefox";
            } else if (os === undefined || os === null) {
                os = "Unknown";
            }

            return {
                device: device,
                os: os
            }
        },

        /**
         * Is it a mobile phone?
         */
        isMobile: function () {
            return !!(navigator.userAgent.match(/Android/i)
                || navigator.userAgent.match(/webOS/i)
                || navigator.userAgent.match(/iPhone/i)
                || navigator.userAgent.match(/iPad/i)
                || navigator.userAgent.match(/iPod/i)
                || navigator.userAgent.match(/BlackBerry/i)
                || navigator.userAgent.match(/Windows Phone/i));
        },
    }
);

