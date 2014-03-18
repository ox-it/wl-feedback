package org.sakaiproject.feedback.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import org.sakaiproject.emailtemplateservice.model.RenderedTemplate;
import org.sakaiproject.emailtemplateservice.service.EmailTemplateService;
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
    private EmailTemplateService emailTemplateService;

    @Setter
    private ArrayList<String> emailTemplates;

    public void init() {

        emailTemplateService.processEmailTemplates(emailTemplates);
    }

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

    private String getSiteContactEmail(String siteId) {

        try {
            Site site = siteService.getSite(siteId);
            return site.getProperties().getProperty(Site.PROP_SITE_CONTACT_EMAIL);
        } catch (Exception e) {
            logger.error("Failed to get contact email for site : " + siteId + ". Returning null ...");
        }

        return null;
    }

    private String getSiteProperty(String siteId, String name) {

        try {
            Site site = siteService.getSite(siteId);
            return site.getProperties().getProperty(name);
        } catch (Exception e) {
            logger.error("Failed to get property '" + name + "' for site : " + siteId + ". Returning null ...");
        }

        return null;
    }

	public void sendEmail(String fromUserId, String siteId, String feedbackType
                            , String subject, String content
                            , List<FileItem> fileItems, boolean sendMeACopy) {

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

        String contactEmail = getSiteProperty(siteId, Site.PROP_SITE_CONTACT_EMAIL);

        if (contactEmail == null || contactEmail.isEmpty()) {
            logger.error("No contact email for site: " + siteId + ". No email will be sent.");
            return;
        }


        String siteLocale = getSiteProperty(siteId, "locale_string");

        Locale locale = null;

        if (siteLocale != null ) {

            String[] localeParts = siteLocale.split("_");

            if (localeParts.length == 1) {
                locale = new Locale(localeParts[0]);
            } else if (localeParts.length == 2) {
                locale = new Locale(localeParts[0], localeParts[1]);
            } else {
                // Get the default Sakai locale
            }
        } else {
            // Get the default Sakai locale
        }

        if (logger.isDebugEnabled()) {
            logger.debug("fromName: " + fromName);
            logger.debug("fromEmail: " + fromEmail);
            logger.debug("contactEmail: " + contactEmail);
        }

        Map<String, String> replacementValues = new HashMap<String, String>();
        replacementValues.put("fromName", fromName);
        replacementValues.put("title", title);
        replacementValues.put("content", content);

		final EmailMessage msg = new EmailMessage();

		msg.setFrom(new EmailAddress(fromEmail, fromName));
        msg.setContentType(ContentType.TEXT_PLAIN);

        Rendered Template template
             = emailTemplateService.getRenderedTemplate("feedback.contactProblem", user.getReference(), replacementValues);

        if (template == null) {
            log.warn("SakaiProxy.sendEmail() no template with key: " + emailTemplateKey + ". Email will be sent as is ...");
		    msg.setSubject(title);
		    msg.setBody(content);
        } else {
		    msg.setSubject(template.getRenderedSubject());
		    msg.setBody(template.getRenderedMessage());
        }

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
