(function ($) {

    var TOOLBAR = 'toolbar';

    /* STATES */
    var HOME = 'home';
    var CONTENT = 'content';
    var TECHNICAL = 'technical';

    sakai.loadTranslations('org.sakaiproject.feedback.bundle.feedback', feedback);

    feedback.switchState = function (state) {

	    $('#feedback-toolbar > li > span').removeClass('current');
        $('#feedback-' + state + 'item > span').addClass('current');

        if (HOME === state) {

            feedback.utils.renderTemplate(HOME, {featureSuggestionUrl: feedback.startupArgs.featureSuggestionUrl}, 'feedback-content');

            $(document).ready(function () {

                feedback.fitFrame();

                $('#feedback-reportcontentlink').click(function (e) {
                    feedback.switchState(CONTENT);
                });

                $('#feedback-reporttechnicallink').click(function (e) {
                    feedback.switchState(TECHNICAL);
                });
            });
        } else if (CONTENT === state) {

            feedback.utils.renderTemplate(CONTENT, {siteId: feedback.startupArgs.siteId, siteUpdaters: feedback.startupArgs.siteUpdaters}, 'feedback-content');

            $(document).ready(function () {

                feedback.addMouseUpToTextArea();
                feedback.fitFrame();

                $('#feedback-form').ajaxForm(feedback.getFormOptions());

                $('#feedback-attachment').MultiFile( {
                    max: 5,
                    namePattern: '$name_$i'
                });
            });
        } else if (TECHNICAL === state) {
            feedback.utils.renderTemplate(TECHNICAL , {siteId: feedback.startupArgs.siteId, siteUpdaters: feedback.startupArgs.siteUpdaters}, 'feedback-content');

            $(document).ready(function () {

                feedback.addMouseUpToTextArea();
                feedback.fitFrame();

                $('#feedback-form').ajaxForm(feedback.getFormOptions());

                $('#feedback-attachment').MultiFile( {
                    max: 5,
                    namePattern: '$name_$i'
                });
            });
        }

        return false;
    };

    feedback.fitFrame = function () {

        try {
            if (window.frameElement) {
                setMainFrameHeight(window.frameElement.id);
            }
        } catch (err) { }
    };

    feedback.addMouseUpToTextArea = function () {

        $('textarea').mouseup(function (e) {
            feedback.fitFrame();
        });
    };

    feedback.getFormOptions = function () {

        return {
            dataType: 'html',
            iframe: true,
            timeout: 30000,
            success: function (responseText, statusText, xhr) {
                feedback.switchState(HOME);
            },
            error: function (xmlHttpRequest, textStatus, errorThrown) {
            }
        };
    };

    feedback.utils.renderTemplate(TOOLBAR , {featureSuggestionUrl: feedback.startupArgs.featureSuggestionUrl}, 'feedback-toolbar');

    $(document).ready(function () {

        $('#feedback-homeitem').click(function (e) {
            return feedback.switchState(HOME);
        });

        $('#feedback-contentitem').click(function (e) {
            return feedback.switchState(CONTENT);
        });

        $('#feedback-technicalitem').click(function (e) {
            return feedback.switchState(TECHNICAL);
        });
    });

    feedback.switchState(HOME);

}) (jQuery);
