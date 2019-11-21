'use strict';


self.onpush = function (event) {
    if (!event || !event.data || !event.data.json) {
        return;
    }
    var data = event.data.json();
    event.waitUntil(
        self.registration.showNotification(data.title, {
            body: data.body,
            icon: data.icon,
            tag: data.tag,
            data: data
        })
    );
};

self.onnotificationclick = function (event) {
    var url = event.notification.data.url;
    if (url === null && url === undefined) {
        url = '/';
    }
    event.waitUntil(self.clients.matchAll({
        type: 'window',
        includeUncontrolled: false
    }).then(function (clientList) {
        return self.clients.openWindow(url);
    }));

    event.notification.close();
};
