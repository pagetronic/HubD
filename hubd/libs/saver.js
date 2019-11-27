var saver = {
    init: function () {
        this.purge();
        this.save();
    },
    /**
     * Save entry in every input form contais saver tag
     */
    save: function () {
        if (typeof localStorage !== undefined) {
            $('[saver]').each(function () {
                var saver = $(this);

                var key = "saver_" + saver.attr('saver');
                var save = function () {
                    var data = JSON.stringify({
                        date: Date.now(),
                        value: saver.val()
                    });
                    localStorage.setItem(key, data);
                };
                saver.parents('form').on('submit', function () {
                    save();
                });
                var timer = -1;
                saver.on('save', save).on('input', function () {
                    clearTimeout(timer);
                    timer = setTimeout(function () {
                        save();
                    }, 700);
                });

                if (saver.val() !== '') {
                    save();
                } else {
                    if (localStorage.getItem(key) !== null) {
                        try {
                            saver.val(JSON.parse(localStorage.getItem(key)).value).trigger('input');
                        } catch (e) {
                            localStorage.removeItem(key);
                        }
                    }
                }
            });
        }
    },
    /**
     * remove data saved
     *
     * @param inputs where remove is needed
     */
    remove: function (inputs) {
        if (typeof localStorage !== undefined) {
            inputs.each(function () {
                localStorage.removeItem("saver_" + $(this).attr('saver'));
            });
        }
    },
    /**
     * Purge all form saved
     */
    purge: function () {
        if (typeof localStorage !== undefined) {
            for (var i = 0; i < localStorage.length; i++) {
                var key = localStorage.key(i);
                if (key.startsWith("saver_")) {
                    try {
                        var saved = JSON.parse(localStorage.getItem(key));
                        if (saved.date !== undefined && Date.now() > saved.date + 18 * 3600 * 1000) {
                            localStorage.removeItem(localStorage.key(i));
                        }
                    } catch (e) {
                    }
                }
            }
        }
    }
};