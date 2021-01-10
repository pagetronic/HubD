var webpush = {
    capable: function () {
        return typeof navigator.serviceWorker === "object";
    },
    worker: function (after, onerror) {
        navigator.serviceWorker.register('/push.js').then(function () {
            navigator.serviceWorker.ready.then(after, onerror).catch(function () {
                onerror();
            });
        });
    },
    control: function (obj, follow, unfollow) {
        if (follow === undefined) {
            follow = function () {
            }
        }
        if (unfollow === undefined) {
            unfollow = function () {
            }
        }
        webpush.worker(function (swr) {
            swr.pushManager.getSubscription().then(function (subscription) {

                if (!subscription) {
                    unfollow();
                } else {
                    api.post("/notices", {
                        action: 'control',
                        obj: obj,
                        config: webpush.config(subscription)
                    }, function (rez) {
                        if (rez.follow === true) {
                            follow();
                        } else {
                            unfollow();
                        }
                    }, unfollow);
                }

            }, unfollow).catch(function () {
                unfollow();
            });
        }, unfollow);
    },
    enable: function (obj, onsuccess, onerror) {
        if (onsuccess === undefined) {
            onsuccess = function () {
            }
        }
        if (onerror === undefined) {
            onerror = function () {
            }
        }

        if (!webpush.capable()) {
            onerror();
        }

        webpush.worker(function () {
            Notification.requestPermission().then(function (result) {
                if (result !== 'granted') {
                    sys.confirm(lang.get('CONFIRMATION', 'ucfirst'), lang.get('SUBSCRIPTION_LOCKED'), -1,  function () {
                        webpush.enable(obj, onsuccess, onerror);
                    }, onerror);
                    return;
                }

                webpush.worker(function (swr) {
                    swr.pushManager.subscribe({
                        userVisibleOnly: true,
                        applicationServerKey: constants.vapId
                    }).then(
                        function (subscription) {
                            if (subscription === undefined || subscription === null || subscription.endpoint === undefined || subscription.getKey === undefined) {
                                onerror();
                                return;
                            }
                            api.post("/notices", {
                                action: 'subscribe',
                                obj: obj,
                                device: sys.device(),
                                config: webpush.config(subscription)
                            }, onsuccess, onerror);
                        }, function (e) {
                            if (e.code === 11) {
                                swr.unregister().then(function () {
                                    webpush.enable(obj, onsuccess, onerror);
                                }, onerror);
                            } else {
                                onerror();
                            }

                        }).catch(function () {
                        onerror();
                    });
                }, onerror);
            }, onerror).catch(function () {
                onerror();
            });
        }, onerror);
    },

    disable: function (obj, onsuccess, onerror) {
        webpush.worker(function (swr) {

            swr.pushManager.getSubscription().then(function (subscription) {
                if (subscription === undefined || subscription === null || subscription.endpoint === undefined || subscription.getKey === undefined) {
                    onerror();
                    return;
                }
                swr.pushManager.getSubscription().then(function () {

                    api.post("/notices", {
                        action: 'unsubscribe',
                        obj: obj,
                        config: webpush.config(subscription)
                    }, onsuccess, onerror);
                }, onerror).catch(function () {
                    onerror();
                });
            }, onerror);
        }, onerror);
    },
    remove: function (id) {
        var force = function () {
            api.post("/notices", {
                action: 'unpush',
                id: id
            });
        };
        webpush.worker(function (swr) {
            swr.pushManager.getSubscription().then(function (subscription) {
                if (subscription) {
                    var finished = function () {
                        swr.unregister().then(function (registed) {
                        });
                    };
                    api.post("/notices", {
                        action: 'unpush',
                        id: id,
                        config: webpush.config(subscription)
                    }, finished);
                } else {
                    force();
                }
            }, force).catch(function () {
                force();
            });
        });
    },
    get: function (success, paging) {
        webpush.worker(function (swr) {
            swr.pushManager.getSubscription().then(function (subscription) {
                if (subscription) {
                    api.post("/notices", {
                        action: 'get',
                        paging: paging,
                        config: webpush.config(subscription)
                    }, success);
                }
            });
        });
    },
    buttons: function () {

        var follows = $('[follow]');
        if (!webpush.capable()) {
            follows.remove();
            return;
        }
        follows.dids = [];
        follows.each(function () {
            var ele = $(this);

            var obj = ele.attr('follow');
            if (follows.dids.indexOf(obj) >= 0) {
                return;
            }
            follows.dids.push(obj);

            var fos = null;
            var uns = null;
            var eles = $('[follow=' + $.escapeSelector(obj) + ']');

            eles.each(function () {
                var fo = $('<span />').append('$svg.mi_notifications_none').append(lang.get('FOLLOW'));
                var un = $('<span />').append('$svg.mi_notifications').append(lang.get('UNFOLLOW')).css({display: 'none'});

                un.find('svg').addClass('follow');
                fo.find('svg').addClass('unfollow');
                $(this).html(fo).append(un);
                fos = fos === null ? fo : fos.add(fo);
                uns = uns === null ? un : uns.add(un);
            });


            var follow = function (disable) {
                fos.css({display: 'none'}).off('click');
                uns.css({display: 'block'}).off('click');
                if (disable === false) {
                    eles.removeClass('active').addClass('disable');
                    return;
                }
                uns.one('click', function () {
                    unfollow(false);
                    webpush.disable(obj, unfollow, follow);
                });
                eles.removeClass('disable').addClass('active');
            };

            var unfollow = function (disable) {
                fos.css({display: 'block'}).off('click');
                uns.css({display: 'none'}).off('click');
                if (disable === false) {
                    eles.removeClass('active').addClass('disable');
                    return;
                }
                fos.one('click', function () {
                    follow(false);
                    webpush.enable(obj, follow, unfollow);
                });
                eles.removeClass('disable active');

            };

            webpush.control(obj, follow, unfollow);
        });
    },
    config: function (subscription) {
        return {
            endpoint: subscription.endpoint,
            key: btoa(String.fromCharCode.apply(null, new Uint8Array(subscription.getKey('p256dh')))).replace(/\+/g, '-').replace(/\//g, '_'),
            auth: btoa(String.fromCharCode.apply(null, new Uint8Array(subscription.getKey('auth')))).replace(/\+/g, '-').replace(/\//g, '_')
        }
    },
    test: function () {
        if (!webpush.capable()) {
            alert(lang.get('WEBPUSH_IMPOSSIBLE'));
            return;
        }
        webpush.worker(function () {
            Notification.requestPermission().then(function (result) {
                if (result !== 'granted') {
                    alert(lang.get('SUBSCRIPTION_LOCKED'));
                    return;
                }

                webpush.worker(function (swr) {
                    swr.pushManager.subscribe({
                        userVisibleOnly: true,
                        applicationServerKey: constants.vapId
                    }).then(function (subscription) {
                        webpush.worker(function (swr) {
                            swr.pushManager.getSubscription().then(function (subscription) {
                                if (subscription) {
                                    api.post("/notices", {
                                        action: 'test',
                                        config: webpush.config(subscription)
                                    }, function (rez) {
                                        if (rez.ok) {
                                            sys.toast("pushed");
                                            return;
                                        }
                                        sys.alert("ERROR");
                                    });
                                }
                            });
                        });
                    });
                });
            });
        });
    }
};
