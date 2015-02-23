package org.sakaiproject.feedback.tool;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.feedback.util.Constants;
import org.sakaiproject.feedback.util.SakaiProxy;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.util.ResourceLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

/**
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
public class FeedbackTool extends HttpServlet {
    
    private static final long serialVersionUID = 1L;

    private static final Log logger = LogFactory.getLog(FeedbackTool.class);

    private static final String TEAM = "Team";

    private SakaiProxy sakaiProxy = null;

    private SecurityService securityService = null;

    private SiteService siteService = null;

    private final String[] DYNAMIC_PROPERTIES = { "help_tooltip",  "overview", "technical_setup_instruction", "report_technical_tooltip", "short_technical_description",
            "suggest_feature_tooltip", "feature_description", "technical_instruction",  "error"};

    public static final String FORWARD_SLASH = "FORWARD_SLASH";

    private static ResourceLoader rb = new ResourceLoader("org.sakaiproject.feedback");

    public void init(ServletConfig config) throws ServletException {

        super.init(config);

        try {
            ApplicationContext context
                = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
            sakaiProxy = (SakaiProxy) context.getBean("org.sakaiproject.feedback.util.SakaiProxy");
            securityService = (SecurityService) context.getBean("org.sakaiproject.authz.api.SecurityService");
            siteService = (SiteService) context.getBean("org.sakaiproject.site.api.SiteService");

        } catch (Throwable t) {
            throw new ServletException("Failed to initialise FeedbackTool servlet.", t);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String userId = sakaiProxy.getCurrentUserId();

        String siteId = sakaiProxy.getCurrentSiteId();


        String contactUsSiteId = (String) request.getSession().getAttribute("contact.us.origin.site");
        if (contactUsSiteId!=null) {
            siteId = contactUsSiteId;
        }
        if (siteId.equals("!error")) { // if site is unavailable then retrieve siteId
            siteId = request.getParameter("siteId");
        }

        boolean siteExists = siteService.siteExists(siteId);

        Site site = null;
        if (siteExists){
            try {
                site = siteService.getSite(siteId);
            } catch (IdUnusedException e) {
                throw new RuntimeException("The site cannot be found with siteId: " + siteId, e);
            }
        }

        Map<String, String> siteUpdaters = new HashMap<String, String>();
        Map<String, String> emailRecipients = new LinkedHashMap<String, String>();

        String serviceName = sakaiProxy.getConfigString("ui.service", "Sakai");
        boolean hasViewPermission = false;
        if (siteExists){
            hasViewPermission = securityService.unlock("roster.viewallmembers", site.getReference());
            if(hasViewPermission) {
                siteUpdaters = sakaiProxy.getSiteUpdaters(siteId);
            }
            addRecipients(site, emailRecipients, siteUpdaters, serviceName);
        }
        else {
            String siteContact = serviceName+  " "  + TEAM;
            String siteEmail = sakaiProxy.getConfigString(Constants.PROP_TECHNICAL_ADDRESS, null);
            emailRecipients.put(siteEmail, siteContact);
        }

        if (userId != null) {
            setMapAttribute(request, "siteUpdaters", emailRecipients);
        } else {
            if (sakaiProxy.getConfigBoolean("user.recaptcha.enabled", false)) {
                String publicKey = sakaiProxy.getConfigString("user.recaptcha.public-key", "");
                setStringAttribute(request, "recaptchaPublicKey", publicKey);
            }
        }

        setMapAttribute(request, "i18n", getBundle(serviceName));
        setStringAttribute(request, "language", rb.getLocale().getLanguage());
        request.setAttribute("enableTechnical",
            (sakaiProxy.getConfigString(Constants.PROP_TECHNICAL_ADDRESS, null) == null)
                ? false : true);

        request.setAttribute("sakaiHtmlHead", (String) request.getAttribute("sakai.html.head"));
        setStringAttribute(request, "userId", (userId == null) ? "" : userId);
        setStringAttribute(request, "siteId", siteId.replaceAll("/", FORWARD_SLASH));
        request.setAttribute("siteExists", siteExists);
        setStringAttribute(request, "featureSuggestionUrl", sakaiProxy.getConfigString("feedback.featureSuggestionUrl", ""));
        setStringAttribute(request, "helpPagesUrl", sakaiProxy.getConfigString("feedback.helpPagesUrl", ""));
        setStringAttribute(request, "helpdeskUrl", sakaiProxy.getConfigString("feedback.helpdeskUrl", ""));
        setStringAttribute(request, "supplementaryInfo", sakaiProxy.getConfigString("feedback.supplementaryInfo", ""));
        request.setAttribute("maxAttachmentsMB", sakaiProxy.getAttachmentLimit());
        setStringAttribute(request, "technicalToAddress", sakaiProxy.getConfigString(Constants.PROP_TECHNICAL_ADDRESS, null));

        String contactName = null;
        String siteEmail = null;
        if (siteExists){
            siteEmail = site.getProperties().getProperty(Site.PROP_SITE_CONTACT_EMAIL);
        }

        if (siteEmail!=null && !siteEmail.isEmpty() && siteExists){
            contactName = site.getProperties().getProperty(Site.PROP_SITE_CONTACT_NAME);
        }
        else if (!hasViewPermission){
            contactName = serviceName + " " + TEAM + " <" + sakaiProxy.getConfigString("mail.support", "") + ">";
        }
        setStringAttribute(request, "contactName", contactName);

        response.setContentType("text/html");
        request.getRequestDispatcher("/WEB-INF/bootstrap.jsp").include(request, response);
    }

	private void setStringAttribute(HttpServletRequest request, String key, String value){
		request.setAttribute(key, StringEscapeUtils.escapeJavaScript(value));
	}

	private void setMapAttribute(HttpServletRequest request, String key, Map<String, String> map){
        for (String s : map.keySet()) {
			map.put(s, StringEscapeUtils.escapeJavaScript(map.get(s)));
		}
		request.setAttribute(key, map);
	}

    private void addRecipients(Site site, Map<String, String> emailRecipients, Map<String, String> siteUpdaters, String serviceName) {
        String siteContact = site.getProperties().getProperty(Site.PROP_SITE_CONTACT_NAME);
        String siteEmail = site.getProperties().getProperty(Site.PROP_SITE_CONTACT_EMAIL);
        if (siteEmail!=null && !siteEmail.isEmpty()){
            emailRecipients.put(siteEmail, siteContact + " (site contact)");
        }
        else if (siteUpdaters.isEmpty()){
            siteContact = serviceName+  " "  + TEAM;
            siteEmail = sakaiProxy.getConfigString(Constants.PROP_TECHNICAL_ADDRESS, null);
            emailRecipients.put(siteEmail, siteContact);
        }
        emailRecipients.putAll(siteUpdaters);
    }

    private Map<String, String> getBundle(String serviceName) {
        Map<String, String> bundleMap = new HashMap<String, String>();
        for (Object key : rb.keySet()) {
            bundleMap.put((String) key, rb.getString((String) key));
        }
        formatProperties(rb, bundleMap, serviceName);
        return bundleMap;
    }

    private void formatProperties(ResourceLoader rb, Map<String, String> bundleMap, String serviceName) {

        for (String property : DYNAMIC_PROPERTIES) {
            bundleMap.put(property, MessageFormat.format(rb.getString(property), new String[]{serviceName}));
        }

        if (serviceName!=null && !serviceName.isEmpty()){
            bundleMap.put("technical_link", MessageFormat.format(rb.getString("technical_link"), new String[]{serviceName}));
        }
        else {
            bundleMap.put("technical_link", rb.getString("ask_link"));
        }
    }
}
