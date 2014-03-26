package org.sakaiproject.feedback.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Map;
import java.text.MessageFormat;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.mail.util.ByteArrayDataSource;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.email.api.Attachment;
import org.sakaiproject.email.api.ContentType;
import org.sakaiproject.email.api.EmailAddress;
import org.sakaiproject.email.api.EmailAddress.RecipientType;
import org.sakaiproject.email.api.EmailMessage;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
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
    private ToolManager toolManager;

    @Setter
    private UserDirectoryService userDirectoryService;

    @Setter
    private SiteService siteService;

    @Setter
    private EmailService emailService;

    @Setter
    private ArrayList<String> emailTemplates;

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

	public String getCurrentSiteId() {
		return toolManager.getCurrentPlacement().getContext();
	}

    private Site getSite(String siteId) {

        try {
            return siteService.getSite(siteId);
        } catch (Exception e) {
            logger.error("Failed to get site for id : " + siteId + ". Returning null ...");
        }

        return null;
    }

    public String getSiteProperty(String siteId, String name) {

        try {
            Site site = siteService.getSite(siteId);
            return site.getProperties().getProperty(name);
        } catch (Exception e) {
            logger.error("Failed to get property '" + name + "' for site : " + siteId + ". Returning null ...");
        }

        return null;
    }

    public Map<String, String> getSiteUpdaters(String siteId) {

        try {
            Site site = siteService.getSite(siteId);
            Map<String, String> map = new HashMap<String, String>();
            for (String userId : site.getUsersIsAllowed(SiteService.SECURE_UPDATE_SITE)) {
                User user = userDirectoryService.getUser(userId);
                map.put(user.getEmail(), user.getDisplayName());
            }
            return map;
        } catch (Exception e) {
            logger.error("Failed to get site updaters for site : " + siteId + ". Returning an empty map ...");
            return new HashMap<String, String>();
        }
    }

	public void sendEmail(String fromUserId, String toEmail, String siteId, String feedbackType
                            , String userTitle, String userContent
                            , List<FileItem> fileItems) {

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

        final User user = getUser(fromUserId);

        final String fromEmail = user.getEmail();
        final String fromName = user.getDisplayName();

        if (fromEmail == null) {
            logger.error("No email for reporter: " + fromUserId + ". No email will be sent.");
            return;
        }

        final String siteLocale = getSiteProperty(siteId, "locale_string");

        Locale locale = null;

        if (siteLocale != null ) {

            String[] localeParts = siteLocale.split("_");

            if (localeParts.length == 1) {
                locale = new Locale(localeParts[0]);
            } else if (localeParts.length == 2) {
                locale = new Locale(localeParts[0], localeParts[1]);
            } else {
                locale = Locale.getDefault();
            }
        } else {
            locale = Locale.getDefault();
        }

        final ResourceBundle rb = ResourceBundle.getBundle("org.sakaiproject.feedback.bundle.feedback", locale);

        String subjectTemplate = null;
        
        if (feedbackType.equals(Constants.CONTENT)) {
            subjectTemplate = rb.getString("content_email_subject_template");
        } else {
            subjectTemplate = rb.getString("technical_email_subject_template");
        }

        final String formattedSubject
            = MessageFormat.format(subjectTemplate, new String[] {fromName});

        final Site site = getSite(siteId);

        final String siteTitle = site.getTitle();

        final String siteUrl = serverConfigurationService.getPortalUrl() + "/site/" + site.getId();

        final String instance = serverConfigurationService.getServerIdInstance();

        final String bodyTemplate = rb.getString("email_body_template");
        final String formattedBody
            = MessageFormat.format(bodyTemplate, new String[] {user.getId(),
                                                                    user.getEid(),
                                                                    fromName,
                                                                    fromEmail,
                                                                    siteTitle,
                                                                    siteId,
                                                                    siteUrl,
                                                                    instance,
                                                                    userTitle,
                                                                    userContent});

        if (logger.isDebugEnabled()) {
            logger.debug("fromName: " + fromName);
            logger.debug("fromEmail: " + fromEmail);
            logger.debug("toEmail: " + toEmail);
            logger.debug("userContent: " + userContent);
            logger.debug("userTitle: " + userTitle);
            logger.debug("subjectTemplate: " + subjectTemplate);
            logger.debug("bodyTemplate: " + bodyTemplate);
            logger.debug("formattedSubject: " + formattedSubject);
            logger.debug("formattedBody: " + formattedBody);
        }

		final EmailMessage msg = new EmailMessage();

		msg.setFrom(new EmailAddress(fromEmail, fromName));
        msg.setContentType(ContentType.TEXT_PLAIN);

		msg.setSubject(formattedSubject);
		msg.setBody(formattedBody);

        if (attachments != null) {
        	for (Attachment attachment : attachments) {
        		msg.addAttachment(attachment);
        	}
        }

		if (feedbackType.equals(Constants.CONTENT)) {
            // Copy the sender in
			msg.addRecipient(RecipientType.CC, fromName, fromEmail);
		}

		msg.addRecipient(RecipientType.TO, toEmail);

        new Thread(new Runnable() {
            public void run() {
		        try {
			        emailService.send(msg, true);
                } catch (Exception e) {
                    logger.error("Failed to send email.", e);
                }
            }
        }).start();
	}
}