(function ($) {

    var TOOLBAR = 'toolbar';

    /* STATES */
    var HOME = 'home';
    var CONTENT = 'content';
    var TECHNICAL = 'technical';

    var loggedIn = (feedback.userId != '') ? true : false;

    feedback.switchState = function (state) {

        $('#feedback-toolbar > li > span').removeClass('current');

        $('#feedback-' + state + '-item > span').addClass('current');

        if (HOME === state) {

            feedback.utils.renderTemplate(HOME, { featureSuggestionUrl: feedback.featureSuggestionUrl,
                                                    supplementaryInfo: feedback.supplementaryInfo,
                                                    helpPagesUrl: feedback.helpPagesUrl,
                                                    loggedIn: loggedIn }, 'feedback-content');

            $(document).ready(function () {

                if (feedback.helpPagesUrl.length > 0 ) {
                    $('#feedback-help-item').show().css('display', 'inline');
                    $('#feedback-help-wrapper').show();
                }

                $('#feedback-report-content-link').click(function (e) {
                    feedback.switchState(CONTENT);
                });

                if (feedback.enableTechnical) {
                    $('#feedback-technical-item').show().css('display', 'inline');
                    $('#feedback-report-technical-wrapper').show();
                    $('#feedback-report-technical-link').click(function (e) {
                        feedback.switchState(TECHNICAL);
                    });
                } else {
                    $('#feedback-technical-setup-instruction').show();
                }

                if (feedback.featureSuggestionUrl.length > 0) {
                    $('#feedback-suggest-feature-wrapper').show();
                } else {
                    $('#feedback-feature-suggestion-setup-instruction').show();
                }

                if (feedback.supplementaryInfo.length > 0) {
                    $('#feedback-supplementary-info').show();
                }

                $('.feedback-explanation-link').click(function (e) {

                    $(this).next().toggle({ duration: 'fast',
                                            complete: function () {
                                                feedback.fitFrame();
                                            } });
                });

                feedback.fitFrame();
            });
        } else if (CONTENT === state) {

            feedback.utils.renderTemplate(state, { siteId: feedback.siteId,
                                                    siteUpdaters: feedback.siteUpdaters }, 'feedback-content');

            $(document).ready(function () {

                feedback.addMouseUpToTextArea();
                feedback.fitFrame();

                if (feedback.siteUpdaters.length > 0) {
                    $('#feedback-siteupdaters-wrapper').show();
                }

                $('#feedback-form').ajaxForm(feedback.getFormOptions());

                $('#feedback-max-attachments-mb').html(feedback.maxAttachmentsMB);

                $('#feedback-attachment').MultiFile( {
                    max: 5,
                    namePattern: '$name_$i'
                });
            });
        } else if (TECHNICAL === state) {

            feedback.utils.renderTemplate(state, { siteId: feedback.siteId }, 'feedback-content');

            $(document).ready(function () {

                feedback.addMouseUpToTextArea();

                if (!loggedIn) {
                    // Not logged in, show the sender email box.
                    $('#feedback-sender-address').show();

                    if (feedback.recaptchaPublicKey.length > 0) {
                        // Recaptcha is enabled, show it.
                        Recaptcha.create(feedback.recaptchaPublicKey, "feedback-recaptcha-block",
                            {
                                theme: "red",
                                callback: function () {

                                    feedback.fitFrame();
                                    $('#feedback-recaptcha-wrapper').show();
                                }
                            }
                        );
                    }
                }

                feedback.fitFrame();

                $('#feedback-form').ajaxForm(feedback.getFormOptions(feedback.userId.length > 0));

                $('#feedback-max-attachments-mb').html(feedback.maxAttachmentsMB);

                $('#feedback-attachment').MultiFile( {
                    max: 5,
                    namePattern: '$name_$i'
                } );

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

    feedback.getFormOptions = function (loggedIn) {

        return {
            dataType: 'html',
            iframe: true,
            timeout: 30000,
            success: function (responseText, statusText, xhr) {

                if (responseText === 'RECAPTCHA FAILURE') {
                    $('#feedback-recaptcha-error').show();
                    setTimeout(function () {
                        $('#feedback-recaptcha-error').fadeOut();
                    }, 2000);
                    Recaptcha.reload();
                } else {
                    feedback.switchState(HOME);
                }
            },
            error: function (xmlHttpRequest, textStatus, errorThrown) {
            },
            beforeSubmit: function (formArray, $form, options) {
                for (var i=0,j=formArray.length;i<j;i++) {
                    var el = formArray[i];
                    if (el.name === 'title') {
                        if (el.value.length < 8 || el.value.length > 40) {
                            alert(feedback.i18n.mandatory_title_warning);
                            return false;
                        }
                    } else if (el.name === 'description') {
                        if (el.value.length < 32) {
                            alert(feedback.i18n.mandatory_description_warning);
                            return false;
                        }
                    } else if (!loggedIn && el.name === 'senderaddress') {
                        if (el.value.length == 0) {
                            alert(feedback.i18n.mandatory_email_warning);
                            return false;
                        }
                    }
                }
                return true;
            }
        };
    };

    var loggedIn = (feedback.userId != '') ? true : false;
    feedback.utils.renderTemplate(TOOLBAR , { featureSuggestionUrl: feedback.featureSuggestionUrl,
                                                loggedIn: loggedIn,
                                                helpPagesUrl: feedback.helpPagesUrl }, 'feedback-toolbar');

    $(document).ready(function () {

        $('#feedback-home-item').click(function (e) {
            return feedback.switchState(HOME);
        });

        $('#feedback-content-item').click(function (e) {
            return feedback.switchState(CONTENT);
        });

        $('#feedback-technical-item').click(function (e) {
            return feedback.switchState(TECHNICAL);
        });

        if (feedback.helpPagesUrl.length > 0 ) {
            $('#feedback-help-item').show();
        }
    });

    feedback.switchState(HOME);

}) (jQuery);
