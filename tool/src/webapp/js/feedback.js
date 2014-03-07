(function ($) {

    /* STATES */
    var HOME = 'home';
    var CONTENT = 'content';
    var FUNCTIONALITY = 'functionality';

    sakai.loadTranslations('org.sakaiproject.feedback.bundle.feedback', feedback);

    feedback.switchState = function (state) {

        if (HOME === state) {
            feedback.utils.renderTemplate(HOME, {}, 'feedback-content');

            $(document).ready(function () {

                feedback.fitFrame();

                $('#feedback-reportcontentlink').click(function (e) {
                    feedback.switchState(CONTENT);
                });

                $('#feedback-reportfunctionalitylink').click(function (e) {
                    feedback.switchState(FUNCTIONALITY);
                });
            });
        } else if (CONTENT === state) {
            feedback.utils.renderTemplate(CONTENT, {}, 'feedback-content');

            $(document).ready(function () {

                feedback.addMouseUpToTextArea();
                feedback.fitFrame();
            });
        } else if (FUNCTIONALITY === state) {
            feedback.utils.renderTemplate(FUNCTIONALITY , {}, 'feedback-content');

            $(document).ready(function () {
                feedback.fitFrame();
            });
        }
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

    feedback.switchState(HOME);
}) (jQuery);
