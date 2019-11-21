var login = {
    init: function () {
        login.register();
        login.login();
        login.recover();
        login.password();
    },
    register: function () {

        var register = $('#register');
        register.on('submit', function () {
            register.find('.error_input').addClass('error_input');
            register.find('.error').remove();
            var data = {
                action: 'register',
                email: register.find('[name=email]').val(),
                'new-password': register.find('[name=new-password]').val()
            };

            var name = register.find('[name=name]');
            if (name.length > 0) {
                data.name = name.val();
            }
            var key = register.find('[name=key]');
            if (key.length > 0) {
                data.key = key.val();
            }
            register.find('[settings]').each(function () {
                if (data.settings === undefined) {
                    data.settings = {};
                }
                data.settings[this.name] = this.value;
            });

            register.find('input').attr('disabled', 'disabled');
            register.css({opacity: 0.4});

            api.post("/profile", data, function (rez) {
                if (rez.ok) {
                    if (window.PasswordCredential) {
                        navigator.credentials.store(new PasswordCredential({
                            id: data.email,
                            name: data.name,
                            password: data['new-password'],
                            iconURL: constants.logo
                        }));
                    }
                    document.location.href = '/';
                } else {
                    register.find('input').removeAttr('disabled');
                    register.css({opacity: ''});
                    if (rez.errors !== undefined) {
                        for (var key in rez.errors) {
                            var input = register.find('[name=' + key + ']');
                            input.addClass('error_input');
                            for (var i in rez.errors[key]) {
                                input.after($('<span class="error"/>').text(lang.get(rez.errors[key][i])));
                            }
                        }
                    }

                }
            });
            return false;
        });
    },
    login: function () {
        var login = $('#login');
        login.on('submit', function () {

            login.find('input').attr('disabled', 'disabled');
            login.css({opacity: 0.4});

            api.post("/profile", {
                action: 'login',
                email: login.find('[name=email]').val(),
                password: login.find('[name=password]').val()
            }, function (rez) {
                if (rez.ok) {
                    document.location.reload();
                } else {
                    login.find('input').removeAttr('disabled');
                    login.css({opacity: ''});
                    login.find('[name=email],[name=password]').val('').addClass('error_input');
                }
            });
            return false;
        });

    },
    recover: function () {
        var recover = $('#recover');
        recover.on('submit', function () {

            $('#recover input').attr('disabled', 'disabled');
            $('#recover').css({opacity: 0.4});
            api.post("/profile", {
                action: 'recover',
                email: $('#recover [name=email]').val()
            }, function (rez) {
                recover.css({opacity: ''});
                if (rez.ok) {
                    $('#recover').html('<h3>' + lang.get("ACTIVATION_MAIL_TITLE") + '</h3>')
                        .append('<p>' + lang.get("ACTIVATION_MAIL_SENT") + '</p>');
                } else {
                    recover.find('input').removeAttr('disabled');
                    recover.find('[name=email]').val('').addClass('error_input');
                }
            });
            return false;
        });

    },
    password: function () {
        var password = $('#password');
        password.on('submit', function () {
            password.find('input').attr('disabled', 'disabled');
            password.css({opacity: 0.4});
            api.post("/profile", {
                action: 'password',
                email: $('#password [name=email]').val(),
                password: $('#password [name=password]').val(),
                activate: $('#password [name=activate]').val()
            }, function (rez) {
                password.css({opacity: ''});
                if (rez.ok) {
                    $('#password').remove();
                    document.location.href = document.location.pathname;
                } else {
                    password.find('input').removeAttr('disabled');
                    password.find('[name=email]').val('').addClass('error_input');
                }
            });
            return false;
        });
    },
    autologin: function () {
        if (sys.user.id === null && window.PasswordCredential !== undefined &&
            document.location.pathname !== '/profile' && data.get("autologin", true) === null) {
            navigator.credentials.preventSilentAccess().then(function () {
                navigator.credentials.get({
                    password: true,
                    unmediated: false
                }).then(function (credential) {
                        if (credential === null) {
                            data.set("autologin", false, true);
                            return;
                        }
                        api.post("/profile", {
                            action: 'login',
                            email: credential.id,
                            password: credential.password
                        }, function (rez) {
                            if (rez.ok) {
                                document.location.reload();
                            } else {
                                document.location.href = '/profile';
                            }
                        });
                    }
                );
            });
        }
    }
};