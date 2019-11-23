sys.consent = {
    pub: function () {
        var ads = $('.adsbygoogle');
        if (ads.length > 0) {
            setTimeout(function () {
                var adblock = (ads.css('display') === 'none' || (ads.css('-moz-binding') && ads.css('-moz-binding').indexOf('about') !== -1));
                /*
                api.post("/profile", {
                    action: 'adblock',
                    accept: adblock
                });
                */
                if (adblock) {
                    log('AdBlock detected');


                    /*

                    sys.confirm(lang.get('ERROR'), lang.get('ADBLOCK'), -1, function () {
                        document.location.reload();
                    }, function () {
                        history.back();
                    });
                    $('section, .post:not(:first) .content').css({
                        color: 'red',
                        fontWeight: 'bold'
                    }).find('p').html(lang.get('ADBLOCK'));
                    $('.rating, #qrbox').remove();
                    */
                }
            }, 3000, ads);
        }
    },
    facebook: function () {
        /*var facefollow = $('.facefollow');
        if (facefollow.length > 0) {
            $(document.body).prepend('<div id="fb-root"></div>').prepend('<script async defer crossorigin="anonymous" src="https://connect.facebook.net/fr_FR/sdk.js#xfbml=1&version=v3.2&appId=188176021589020&autoLogAppEvents=1"></script>');
            facefollow.each(function () {
                $(this).append('<div class="fb-page" data-href="https://www.facebook.com/pagetronic" data-tabs="" data-width="250" data-height="70" data-small-header="true" data-adapt-container-width="true" data-hide-cover="true" data-show-facepile="true"></div>');
            });
        }
*/
    },
    choose: function () {
        $('#cookies_ads').find('input[type=checkbox]').prop('checked', Cookies.get('consent') !== null).on('change', function () {
            if ($(this).prop('checked')) {
                Cookies.set('consent', 'yes', {
                    expires: new Date(new Date().getTime() + 365 * 24 * 3600000),
                    domain: document.location.host
                });
                sys.consent.log('mano', true);
            } else {
                Cookies.remove('consent');
                $('#cookies_ads_perso').prop('checked', false);
                sys.consent.log('mano', false);
            }
        });
        if (sys.consent.cantrack()) {
            $('#cookies_ads_perso').removeClass('none').find('input[type=checkbox]').prop('checked', Cookies.get('consent') === 'personalized').on('change', function () {
                if ($(this).prop('checked')) {
                    Cookies.set('consent', 'personalized', {
                        expires: new Date(new Date().getTime() + 365 * 24 * 3600000),
                        domain: document.location.host
                    });
                    $('#cookies_ads').prop('checked', true);
                    sys.consent.log('perso', true);
                } else {
                    Cookies.set('consent', 'rejected', {
                        expires: new Date(new Date().getTime() + 365 * 24 * 3600000),
                        domain: document.location.host
                    });
                    sys.consent.log('perso', false);
                }
            });
        }
        $('#cookies_stats').find('input[type=checkbox]').prop('checked', Cookies.get('stats') === 'yes').on('change', function () {
            if ($(this).prop('checked')) {
                Cookies.set('stats', 'yes', {
                    expires: new Date(new Date().getTime() + 365 * 24 * 3600000),
                    domain: document.location.host
                });
                sys.consent.log('stats', true);
            } else {
                Cookies.set('stats', 'no', {
                    expires: new Date(new Date().getTime() + 365 * 24 * 3600000),
                    domain: document.location.host
                });
                sys.consent.log('stats', false);
            }
        });
    },
    message: function () {
        if (!sys.pub) {
            return;
        }
        if (document.location.pathname === '/profile') {
            return;
        }
        /* $('.adsbygoogle[data-adsbygoogle-status!=done]').each(function () {
             (adsbygoogle = window.adsbygoogle || []).push({});
         });
         */
        if (Cookies.get('consent') === null) {
            (adsbygoogle = window.adsbygoogle || []).requestNonPersonalizedAds = 1;
            log('Waiting cookies consent');
        } else if (!sys.consent.cantrack() || Cookies.get('consent') === 'yes' || Cookies.get('consent') === null || Cookies.get('consent') === 'rejected') {
            (adsbygoogle = window.adsbygoogle || []).requestNonPersonalizedAds = 1;
            log('Non-personalized Ads' + (sys.consent.cantrack() ? '' : ' (DNT detected)'));
        } else {
            log('Personalized Ads accepted');
        }

        if (Cookies.get('consent') !== null) {
            sys.consent.facebook();
        }
        var doCookies = function () {
            // (adsbygoogle = window.adsbygoogle || []).pauseAdRequests = 0;

            $('.adsbygoogle[data-adsbygoogle-status!=done]').each(function () {
                (adsbygoogle = window.adsbygoogle || []).push({});
            });
            if (!sys.debug) {
                if (Cookies.get('stats') === 'yes' && !sys.user.admin) {
                    gtag('js', new Date());
                    var title = document.title;
                    if (constants.domains.length > 1) {
                        title += ' (' + sys.lng + ')';
                    }
                    gtag('config', sys.analytics, {
                        'page_title': title,
                        'anonymize_ip': true,
                        'linker': {
                            'domains': constants.domains
                        }
                    });
                    log('Stats did')

                } else {
                    log(sys.user.admin ? 'No statistics for admin' : 'User refuses statistics');
                }
            }
        };
        if (Cookies.get('consent') !== null) {
            doCookies();
        }

        if ((!sys.consent.cantrack() && Cookies.get('consent') === 'yes') || Cookies.get('consent') === 'rejected' || Cookies.get('consent') === 'personalized') {
            return;
        }

        var consent = $('<div id="consent"/>');
        var consentbox = $('<div id="consentbox" />');
        consent.append(consentbox);

        if (Cookies.get('consent') === null) {

            var cookies_alert = $('<p/>').html(lang.get("COOKIES_ALERT"));
            var cookies_accept = $('<a>' + lang.get("ACCEPT").toLowerCase() + '</a>');
            var fired = false;
            var fireCookie = function () {
                if (fired) {
                    return;
                }
                log('Cookies consent accepted');
                fired = true;
                Cookies.set('consent', 'yes', {
                    expires: new Date(new Date().getTime() + 365 * 24 * 3600000),
                    domain: document.location.host
                });
                if (Cookies.get('stats') !== 'no') {
                    Cookies.set('stats', 'yes', {
                        expires: new Date(new Date().getTime() + 365 * 24 * 3600000),
                        domain: document.location.host
                    });
                }
                cookies_alert.slowRemove(300, function () {
                    if (sys.consent.cantrack()) {
                        consentPerso();
                    } else {
                        setTimeout(function () {
                            consent.slowRemove(500);
                        }, 2000);
                    }
                });
                doCookies();
            };
            var firetimeout = setTimeout(function () {
                $('#middle').one('scroll.consent', fireCookie);
                $(window).one('scroll.consent', fireCookie);
            }, 3000);
            cookies_accept.on('click', function () {
                sys.consent.log('mano', true);
                clearTimeout(firetimeout);
                $('#middle').off('scroll.consent');
                $(window).off('scroll.consent');
                fireCookie();
            });

            consentbox.append(cookies_alert.append($('<span class="btnperso" />').append(cookies_accept)));
        }


        var consentPerso = function () {
            var personalized_alert = $('<p/>').html(lang.get("COOKIES_PERSONALIZED_ALERT"));
            var personalized_accept = $('<a>' + lang.get("ACCEPT").toLowerCase() + '</a>');
            personalized_accept.on('click', function () {
                log('Personalized Ads, reload page');
                Cookies.set('consent', 'personalized', {
                    expires: new Date(new Date().getTime() + 365 * 24 * 3600000),
                    domain: document.location.host
                });
                consent.slowRemove(300, function () {
                    sys.consent.log('perso', true, function () {
                        document.location.reload();
                    });
                });
            });
            var personalized_reject = $('<a>' + lang.get("REFUSE") + '</a>');
            personalized_reject.on('click', function () {
                log('Non-personalized Ads');
                Cookies.set('consent', 'rejected', {
                    expires: new Date(new Date().getTime() + 365 * 24 * 3600000),
                    domain: document.location.host
                });
                sys.consent.log('perso', false);
                consent.slowRemove(300);
            });
            consentbox.prepend(personalized_alert.append(
                $('<span class="btnperso" />').append(personalized_accept).append(' / ').append(personalized_reject)
            ));
        };


        if (sys.consent.cantrack() && Cookies.get('consent') !== null && Cookies.get('consent') !== 'personalized') {
            consentPerso();
        }

        consentbox.append($('<p />').html(lang.get("COOKIES_INFOS")));

        if (Cookies.get('consent') !== 'personalized') {
            $(document.body).append(consent.fadeOut(0).fadeIn(300));
        }
    },
    cantrack: function () {
        if (navigator.doNotTrack === undefined) {
            return true;
        }
        return navigator.doNotTrack !== '1';
    },
    log: function (type, accept, after) {
        api.post("/profile", {
            action: 'consent',
            type: type,
            accept: accept,
            id: sys.sysId()
        }, function () {
            if (after !== undefined) {
                after();
            }
        }, function () {

        });
    }
};