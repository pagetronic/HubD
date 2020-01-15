var login = {
    init: function () {
        login.register();
        login.login();
        login.recover();
        login.password();
    },
    add: function (after) {


        var popper = pop(false, 500);
        popper.header(lang.get('ACCOUNT_CREATE'));
        var form = $('<div class="createaccount" />');
        var name = $('<input autocomplete="off" />').attr('placeholder', lang.get('NAME'));
        form.append(name);
        var email = $('<input autocomplete="off" />').attr('placeholder', lang.get('EMAIL').ucfirst());
        form.append(email);
        var avatar = $('<img width="32" height="32" />').attr('src', constants.logo + '@32x32').attr('title', lang.get('AVATAR'));
        form.append(avatar);
        var submit = $('<button />').text(lang.get('SAVE'));
        form.append(submit);
        popper.content(form);
        var data = {action: 'create'};
        avatar.on('click', function () {
            blobstore.image(avatar, function (avatar) {
                data.avatar = avatar;
            });
        });
        submit.on('click', function () {
            $('.error_input').removeClass('error_input');
            if (name.val() === '') {
                name.addClass('error_input');
                return;
            }
            data.name = name.val();
            if (email.val() !== '') {
                data.email = email.val();
            }

            popper.loading(true);
            api.post('/accounts', data, function (rez) {
                popper.loading(false);
                popper.close();
                if (after !== undefined) {
                    after(rez.id);
                }
            })
        })
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
                            iconURL: constants.logo + '@256x256'
                        }));
                    }
                    document.location.reload();
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
                    document.location.reload();
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
            var autologin = function () {
                navigator.credentials.get({
                    password: true,
                    unmediated: false
                }).then(function (credential) {
                        if (credential === null || credential === undefined) {
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
            };
            if (navigator.credentials.preventSilentAccess !== undefined) {
                navigator.credentials.preventSilentAccess().then(autologin);
            } else {
                autologin();
            }
        }
    }
};