var sakai = sakai || {};
sakai.loadTranslations = function (path, obj) {

    var url = '/direct/feedback/nositeid/translations.json?path=' + path;
    
    $.ajax({
        url: url,
        async: false,
        dataType: 'json',
        success: function (data, textStatus, jqXHR) {
            obj.i18n = data.data;
        },
        error: function (jqXHR, textStatus, errorThrown) {
            console.error('Failed to load transalations from ' + url);
        }
    });
};
