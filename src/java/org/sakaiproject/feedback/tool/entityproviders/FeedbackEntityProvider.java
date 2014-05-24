package org.sakaiproject.feedback.tool.entityproviders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

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
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.feedback.db.Database;
import org.sakaiproject.feedback.util.Constants;
import org.sakaiproject.feedback.util.SakaiProxy;
import org.sakaiproject.util.RequestFilter;

import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaFactory;
import net.tanesha.recaptcha.ReCaptchaResponse;

public class FeedbackEntityProvider extends AbstractEntityProvider implements AutoRegisterEntityProvider, Outputable, Describeable, ActionsExecutable, RequestAware {
	
	public final static String ENTITY_PREFIX = "feedback";

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

	public Object getSampleEntity() {

		return null;
	}

	public String getEntityPrefix() {

		return ENTITY_PREFIX;
	}

	public String[] getHandledOutputFormats() {

		return new String[] { Formats.JSON };
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

        if (userId == null && Constants.CONTENT.equals(type)) {
			logger.error("Not logged in for content report. Returning BAD REQUEST ...");
			throw new EntityException("You must be logged in to post a content report", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        final String siteId = view.getPathSegment(1);

		if (siteId == null) {
			throw new EntityException("You must supply a site id to post a technical report", "", HttpServletResponse.SC_BAD_REQUEST);
		}

        if (logger.isDebugEnabled()) logger.debug("Site ID: " + siteId);

        final String title = (String) params.get("title");
        final String description = (String) params.get("description");

        if (title == null || title.length() == 0) {
			logger.error("No title. Returning BAD REQUEST ...");
			throw new EntityException("You need to supply a title", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (description == null || description.length() == 0) {
			logger.error("No description. Returning BAD REQUEST ...");
			throw new EntityException("You need to supply a description", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (logger.isDebugEnabled()) logger.debug("title: " + title + ". description: " + description);

        String toAddress = null;

        boolean addNoContactMessage = false;

        String senderAddress = "";

        if (type.equals(Constants.TECHNICAL)) {
            toAddress = sakaiProxy.getConfigString("feedback.technicalAddress", null);
            if (userId == null) {

                // Recaptcha
                if (sakaiProxy.getConfigBoolean("recaptcha.enabled", false)) {
                    String publicKey = sakaiProxy.getConfigString("recaptcha.public-key", "");
                    String privateKey = sakaiProxy.getConfigString("recaptcha.private-key", "");
                    ReCaptcha captcha = ReCaptchaFactory.newReCaptcha(publicKey, privateKey, false);
                    String challengeField = (String) params.get("recaptcha_challenge_field");
                    String responseField = (String) params.get("recaptcha_response_field");
                    if (challengeField == null) challengeField = "";
                    if (responseField == null) responseField = "";
                    String remoteAddress = requestGetter.getRequest().getRemoteAddr();
                    ReCaptchaResponse response = captcha.checkAnswer(remoteAddress, challengeField, responseField);
                    if (!response.isValid()) {
			            throw new SecurityException("Recaptcha Error: Your recaptcha response did not match.");
                    }
                }

                senderAddress = (String) params.get("senderaddress");
                if (senderAddress == null || senderAddress.length() == 0) {
                    logger.error("No sender email address for non logged in user. Returning BAD REQUEST ...");
                    throw new EntityException("No sender email address for non logged in user", "", HttpServletResponse.SC_BAD_REQUEST);
                }
            } else {
                senderAddress = sakaiProxy.getUser(userId).getEmail();
            }
        } else {
            senderAddress = sakaiProxy.getUser(userId).getEmail();
            toAddress = (String) params.get("contactemail");

            if (toAddress == null || toAddress.isEmpty()) {
                toAddress = (String) params.get("alternativerecipient");
                addNoContactMessage = true;
            }
        }

        if (toAddress == null || toAddress.isEmpty()) {
            logger.error("No recipient. Returning BAD REQUEST ...");
            throw new EntityException("You need to supply a recipient", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        final List<FileItem> attachments = getAttachments(params);

        sakaiProxy.sendEmail(userId, senderAddress, toAddress, addNoContactMessage, siteId, type, title, description, attachments);

        db.logReport(userId, senderAddress, siteId, type, title, description);

        return "success";
    }

	private List<FileItem> getAttachments(final Map<String, Object> params) {

		final List<FileItem> fileItems = new ArrayList<FileItem>();

		final String uploadsDone = (String) params.get(RequestFilter.ATTR_UPLOADS_DONE);

		if (uploadsDone != null && uploadsDone.equals(RequestFilter.ATTR_UPLOADS_DONE)) {
			logger.debug("UPLOAD STATUS: " + params.get("upload.status"));

			try {
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
			} catch (Exception e) {
                logger.error("Failed to completely parse attachments.", e);
			}
		}

		return fileItems;
	}
}
