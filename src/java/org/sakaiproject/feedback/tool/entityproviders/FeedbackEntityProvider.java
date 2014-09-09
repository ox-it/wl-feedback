package org.sakaiproject.feedback.tool.entityproviders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileItem;

import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.RequestAware;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.extension.RequestGetter;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.feedback.db.Database;
import org.sakaiproject.feedback.exception.AttachmentsTooBigException;
import org.sakaiproject.feedback.util.Constants;
import org.sakaiproject.feedback.util.SakaiProxy;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.user.api.User;
import org.sakaiproject.util.RequestFilter;

import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaFactory;
import net.tanesha.recaptcha.ReCaptchaResponse;

public class FeedbackEntityProvider extends AbstractEntityProvider implements AutoRegisterEntityProvider, Outputable, Describeable, ActionsExecutable, RequestAware {
	
	public final static String ENTITY_PREFIX = "feedback";

    // Error codes start
    private final static String ATTACHMENTS_TOO_BIG = "ATTACHMENTS_TOO_BIG";
    private final static String BAD_DESCRIPTION = "BAD_DESCRIPTION";
    private final static String BAD_RECIPIENT = "BAD_RECIPIENT";
    private final static String BAD_REQUEST = "BAD_REQUEST";
    private final static String BAD_TITLE = "BAD_TITLE";
    private final static String ERROR = "ERROR";
    private final static String NO_SENDER_ADDRESS = "NO_SENDER_ADDRESS";
    private final static String RECAPTCHA_FAILURE = "RECAPTCHA_FAILURE";
    private final static String SUCCESS = "SUCCESS";
    // Error codes end

	private final Logger logger = Logger.getLogger(getClass());

    private SakaiProxy sakaiProxy = null;
    public void setSakaiProxy(SakaiProxy sakaiProxy) {
        this.sakaiProxy = sakaiProxy;
    }

    private Database db = null;
    public void setDb(Database db) {
        this.db = db;
    }

    private RequestGetter requestGetter = null;

    private long maxAttachmentsBytes = 0L;

    public void init() {

        maxAttachmentsBytes = sakaiProxy.getAttachmentLimit() * 1024 * 1024;

        if (logger.isDebugEnabled()) {
            logger.debug("maxAttachmentsBytes = " + maxAttachmentsBytes);
        }
    }

	public Object getSampleEntity() {
		return null;
	}

	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}

	public String[] getHandledOutputFormats() {
		return new String[] { Formats.JSON, Formats.HTML };
	}

    public void setRequestGetter(RequestGetter rg) {
        this.requestGetter = rg;
    }

	@EntityCustomAction(action = "reportcontent", viewKey = EntityView.VIEW_EDIT)
	public String handleContentReport(EntityView view, Map<String, Object> params) {
        return handleReport(view, params, Constants.CONTENT);
	}

	@EntityCustomAction(action = "reporttechnical", viewKey = EntityView.VIEW_EDIT)
	public String handleTechnicalReport(EntityView view, Map<String, Object> params) {
        return handleReport(view, params, Constants.TECHNICAL);
	}

	private String handleReport(final EntityView view, final Map<String, Object> params, final String type) {

		final String userId = developerHelperService.getCurrentUserId();

        if (view.getPathSegments().length != 3) {
            return BAD_REQUEST;
        }

        final String siteId = view.getPathSegment(1);

        if (logger.isDebugEnabled()) logger.debug("Site ID: " + siteId);

        final String title = (String) params.get("title");
        final String description = (String) params.get("description");

        if (title == null || title.isEmpty()) {
			logger.error("Title incorrect. Returning " + BAD_TITLE + " ...");
            return BAD_TITLE;
        }

        if (description == null || description.isEmpty()) {
			logger.error("No description. Returning " + BAD_DESCRIPTION + " ...");
            return BAD_DESCRIPTION;
        }

        if (logger.isDebugEnabled()) logger.debug("title: " + title + ". description: " + description);

        String toAddress = null;

        boolean addNoContactMessage = false;

        // The senderAddress can be either picked up from the current user's
        // account, or manually entered by the user submitting the report.
        String senderAddress = null;
        toAddress = sakaiProxy.getSiteProperty(siteId, Site.PROP_SITE_CONTACT_EMAIL);

        if (userId != null) {
			senderAddress = sakaiProxy.getUser(userId).getEmail();

            String alternativeRecipientId = (String) params.get("alternativerecipient");

            if (alternativeRecipientId != null && alternativeRecipientId.length() > 0) {
                // The site has no contact email. The user has selected one from
                // the list of site updaters.
                User alternativeRecipientUser = sakaiProxy.getUser(alternativeRecipientId);

                if (alternativeRecipientUser != null) {
                    toAddress = alternativeRecipientUser.getEmail();
                    addNoContactMessage = true;
                } else {
                    logger.error("No user for id '" + alternativeRecipientId + "'. Returning BAD_RECIPIENT ...");
                    return BAD_RECIPIENT;
                }
            } else {
                // The site has a contact email. Use it as the toAddress.
                toAddress = sakaiProxy.getConfigString(Constants.PROP_TECHNICAL_ADDRESS, null);
            }

		}
		else {
            // Recaptcha
            if (sakaiProxy.getConfigBoolean("user.recaptcha.enabled", false)) {
                String publicKey = sakaiProxy.getConfigString("user.recaptcha.public-key", "");
                String privateKey = sakaiProxy.getConfigString("user.recaptcha.private-key", "");
                ReCaptcha captcha = ReCaptchaFactory.newReCaptcha(publicKey, privateKey, false);
                String challengeField = (String) params.get("recaptcha_challenge_field");
                String responseField = (String) params.get("recaptcha_response_field");
                if (challengeField == null) challengeField = "";
                if (responseField == null) responseField = "";
                String remoteAddress = requestGetter.getRequest().getRemoteAddr();
                ReCaptchaResponse response = captcha.checkAnswer(remoteAddress, challengeField, responseField);
                if (!response.isValid()) {
                    logger.warn("Recaptcha failed with this message: " + response.getErrorMessage());
                    return RECAPTCHA_FAILURE;
                }
            }

            senderAddress = (String) params.get("senderaddress");
            if (senderAddress == null || senderAddress.length() == 0) {
                logger.error("No sender email address for non logged in user. Returning BAD REQUEST ...");
                return BAD_REQUEST;
            }


            if (toAddress==null){
                toAddress = sakaiProxy.getConfigString(Constants.PROP_TECHNICAL_ADDRESS, null);
            }
		}

        if (toAddress == null || toAddress.isEmpty()) {
            logger.error("No recipient. Returning BAD REQUEST ...");
            return BAD_REQUEST;
        }

        if (senderAddress != null && senderAddress.length() > 0) {
            List<FileItem> attachments = null;
        
            try {
                attachments = getAttachments(params);
                sakaiProxy.sendEmail(userId, senderAddress, toAddress, addNoContactMessage, siteId, type, title, description, attachments);
                db.logReport(userId, senderAddress, siteId, type, title, description);
                return SUCCESS;
            } catch (AttachmentsTooBigException atbe) {
                logger.error("The total size of the attachments exceeded the permitted limit of " + maxAttachmentsBytes + ". '" + ATTACHMENTS_TOO_BIG + "' will be returned to the client.");
                return ATTACHMENTS_TOO_BIG;
            } catch (Exception e) {
                logger.error("Caught exception while sending email or generating report. '" + ERROR + "' will be returned to the client.", e);
                return ERROR;
            }
        } else {
            logger.error("Failed to determine a sender address No email or report will be generated. '"  + NO_SENDER_ADDRESS + "' will be returned to the client.");
            return NO_SENDER_ADDRESS;
        }
    }

	private List<FileItem> getAttachments(final Map<String, Object> params) throws Exception {

		final List<FileItem> fileItems = new ArrayList<FileItem>();

		final String uploadsDone = (String) params.get(RequestFilter.ATTR_UPLOADS_DONE);

		if (uploadsDone != null && uploadsDone.equals(RequestFilter.ATTR_UPLOADS_DONE)) {
			logger.debug("UPLOAD STATUS: " + params.get("upload.status"));

            FileItem attachment1 = (FileItem) params.get("attachment_0");
            if (attachment1 != null && attachment1.getSize() > 0) {
                fileItems.add(attachment1);
            }
            FileItem attachment2 = (FileItem) params.get("attachment_1");
            if (attachment2 != null && attachment2.getSize() > 0) {
                fileItems.add(attachment2);
            }
            FileItem attachment3 = (FileItem) params.get("attachment_2");
            if (attachment3 != null && attachment3.getSize() > 0) {
                fileItems.add(attachment3);
            }
            FileItem attachment4 = (FileItem) params.get("attachment_3");
            if (attachment4 != null && attachment4.getSize() > 0) {
                fileItems.add(attachment4);
            }
            FileItem attachment5 = (FileItem) params.get("attachment_4");
            if (attachment5 != null && attachment5.getSize() > 0) {
                fileItems.add(attachment5);
            }
		}

        long totalSize = 0L;

        for (FileItem fileItem : fileItems) {
            totalSize += fileItem.getSize();
        }

        if (totalSize > maxAttachmentsBytes) {
            throw new AttachmentsTooBigException();
        }

		return fileItems;
	}
}
