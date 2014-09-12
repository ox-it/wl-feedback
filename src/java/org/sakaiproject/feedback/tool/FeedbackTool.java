package org.sakaiproject.feedback.tool;

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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
public class FeedbackTool extends HttpServlet {
    
    private static final long serialVersionUID = 1L;

    private static final Log logger = LogFactory.getLog(FeedbackTool.class);

    private SakaiProxy sakaiProxy = null;

    private SecurityService securityService = null;

    private SiteService siteService = null;


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

        Site site;
        try {
            site = siteService.getSite(siteId);
        } catch (IdUnusedException e) {
            throw new RuntimeException("The site cannot be found with siteId: " + siteId, e);
        }

        Map<String, String> siteUpdaters = new HashMap<String, String>();
        boolean hasViewPermission = securityService.unlock("roster.viewallmembers", site.getReference());
        if(hasViewPermission) {
            siteUpdaters = sakaiProxy.getSiteUpdaters(siteId);
        }

        if (userId != null) {

            ResourceLoader rl = new ResourceLoader("org.sakaiproject.feedback.bundle.ui");

            request.setAttribute("language", rl.getLocale().getLanguage());
            request.setAttribute("siteUpdaters", siteUpdaters);
            request.setAttribute("i18n", rl);
        } else {
            // No logged in user. The content report will be hidden.
            Locale requestLocale = request.getLocale();
            request.setAttribute("language", requestLocale.getLanguage());
            ResourceBundle rb = ResourceBundle.getBundle("org.sakaiproject.feedback.bundle.ui", requestLocale);
            Map<String, String> bundleMap = new HashMap<String, String>();
            for (String key : rb.keySet()) {
                bundleMap.put(key, rb.getString(key));
            }
            request.setAttribute("i18n", bundleMap);

            if (sakaiProxy.getConfigBoolean("user.recaptcha.enabled", false)) {
                String publicKey = sakaiProxy.getConfigString("user.recaptcha.public-key", "");
                request.setAttribute("recaptchaPublicKey", publicKey);
            }
        }

        request.setAttribute("enableTechnical", 
            (sakaiProxy.getConfigString(Constants.PROP_TECHNICAL_ADDRESS, null) == null)
                ? false : true);

        request.setAttribute("sakaiHtmlHead", (String) request.getAttribute("sakai.html.head"));
        request.setAttribute("userId", (userId == null) ? "" : userId);
        request.setAttribute("siteId", siteId);
        request.setAttribute("featureSuggestionUrl", sakaiProxy.getConfigString("feedback.featureSuggestionUrl", ""));
        request.setAttribute("helpPagesUrl", sakaiProxy.getConfigString("feedback.helpPagesUrl", ""));
        request.setAttribute("helpdeskUrl", sakaiProxy.getConfigString("feedback.helpdeskUrl", ""));
        request.setAttribute("supplementaryInfo", sakaiProxy.getConfigString("feedback.supplementaryInfo", ""));
        request.setAttribute("maxAttachmentsMB", sakaiProxy.getAttachmentLimit());
        request.setAttribute("technicalToAddress", sakaiProxy.getConfigString(Constants.PROP_TECHNICAL_ADDRESS, null));

        String contactName = null;
        String siteEmail = site.getProperties().getProperty(Site.PROP_SITE_CONTACT_EMAIL);
        if (siteEmail!=null && !siteEmail.isEmpty()){
            contactName = site.getProperties().getProperty(Site.PROP_SITE_CONTACT_NAME);
        }
        else if (!hasViewPermission){
            contactName = sakaiProxy.getConfigString("ui.service", "Sakai") + " Team<" + sakaiProxy.getConfigString("mail.support", "") + ">";
        }
        request.setAttribute("contactName", contactName);

        response.setContentType("text/html");
        request.getRequestDispatcher("/WEB-INF/bootstrap.jsp").include(request, response);
    }
}
