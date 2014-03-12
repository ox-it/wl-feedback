package org.sakaiproject.feedback.util;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.mail.util.ByteArrayDataSource;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.email.api.*;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

import lombok.Setter;

public class SakaiProxy {

	private static final Log logger = LogFactory.getLog(SakaiProxy.class);

    @Setter
    private ServerConfigurationService serverConfigurationService;

    @Setter
    private SessionManager sessionManager;

    @Setter
    private UserDirectoryService userDirectoryService;

    @Setter
    private SiteService siteService;

    @Setter
    private EmailService emailService;

    public String getConfigString(String name, String defaultValue) {
        return serverConfigurationService.getString(name, defaultValue);
    }

    public String getCurrentUserId() {
        return sessionManager.getCurrentSessionUserId();
    }

    public User getUser(String userId) {

        try {
            return userDirectoryService.getUser(userId);
        } catch (UserNotDefinedException unde) {
            logger.error("No user with id: " + userId + ". Returning null ...");
            return null;
        }
    }

    private String getSiteContactEmail(String siteId) {

        try {
            Site site = siteService.getSite(siteId);
            return site.getProperties().getProperty(Site.PROP_SITE_CONTACT_EMAIL);
        } catch (Exception e) {
            logger.error("Failed to get contact email for site : " + siteId + ". Returning null ...");
        }

        return null;
    }

	public void sendEmail(String fromUserId, String siteId, String feedbackType, String subject, String content, List<FileItem> fileItems, boolean sendMeACopy) {

		final List<Attachment> attachments = new ArrayList<Attachment>();
		if (fileItems.size() > 0) {
			for (FileItem fileItem : fileItems) {
				String name = fileItem.getName();

				if (name.contains("/")) {
					name = name.substring(name.lastIndexOf("/") + 1);
                } else if (name.contains("\\")) {
					name = name.substring(name.lastIndexOf("\\") + 1);
                }

				attachments.add(new Attachment(new ByteArrayDataSource(fileItem.get(), fileItem.getContentType()), name));
			}
		}

        User user = getUser(fromUserId);

        String fromEmail = user.getEmail();
        String fromName = user.getDisplayName();

        if (fromEmail == null) {
            logger.error("No email for reporter: " + fromUserId + ". No email will be sent.");
            return;
        }

        String contactEmail = getSiteContactEmail(siteId);

        if (contactEmail == null || contactEmail.isEmpty()) {
            logger.error("No contact email for site: " + siteId + ". No email will be sent.");
            return;
        }

		EmailMessage msg = new EmailMessage();

		String replyToName = fromName;
		String replyToEmail = fromEmail;

		msg.setFrom(new EmailAddress(replyToEmail, replyToName));

		msg.setSubject(subject);
		// set content type based on editor used
        msg.setContentType(ContentType.TEXT_PLAIN);
		msg.setBody(content);

        if (attachments != null) {
        	for (Attachment attachment : attachments) {
        		msg.addAttachment(attachment);
        	}
        }

		if (sendMeACopy) {
		    // Send a copy
			msg.addRecipient(RecipientType.CC, fromName, fromEmail);
		}

		msg.addRecipient(RecipientType.TO, contactEmail);

		//msg.addHeader("Content-Transfer-Encoding", "quoted-printable");

		try {
			emailService.send(msg, true);
		} catch (Exception e) {
		}
	}
}
