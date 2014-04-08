package org.sakaiproject.feedback.tool;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.feedback.util.SakaiProxy;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.util.RequestFilter;
import org.sakaiproject.util.ResourceLoader;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
public class FeedbackTool extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	private static final Log logger = LogFactory.getLog(FeedbackTool.class);

    private SakaiProxy sakaiProxy = null;

	public void init(ServletConfig config) throws ServletException {

		super.init(config);

		try {
            ApplicationContext context
                = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
            sakaiProxy = (SakaiProxy) context.getBean("org.sakaiproject.feedback.util.SakaiProxy");
		} catch (Throwable t) {
			throw new ServletException("Failed to initialise FeedbackTool servlet.", t);
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String userId = sakaiProxy.getCurrentUserId();

        String siteId = sakaiProxy.getCurrentSiteId();

        if (userId != null) {
            String contactEmail = sakaiProxy.getSiteProperty(siteId, Site.PROP_SITE_CONTACT_EMAIL);
            if (contactEmail == null) contactEmail = "";

            Map<String, String> siteUpdaters = new HashMap<String, String>();

            if (contactEmail.isEmpty()) {
                // No contact email. Load up the maintainers so the reporter can
                // pick one
                siteUpdaters = sakaiProxy.getSiteUpdaters(siteId);
            }

            ResourceLoader rl = new ResourceLoader("org.sakaiproject.feedback.bundle.ui");

            request.setAttribute("language", rl.getLocale().getLanguage());
            request.setAttribute("contactEmail", contactEmail);
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
        }

		request.setAttribute("sakaiHtmlHead", (String) request.getAttribute("sakai.html.head"));
        request.setAttribute("userId", (userId == null) ? "" : userId);
        request.setAttribute("siteId", siteId);
        request.setAttribute("featureSuggestionUrl", sakaiProxy.getConfigString("feedback.featureSuggestionUrl", ""));
        request.setAttribute("helpPagesUrl", sakaiProxy.getConfigString("feedback.helpPagesUrl", ""));
        request.setAttribute("supplementaryInfo", sakaiProxy.getConfigString("feedback.supplementaryInfo", ""));

        request.getRequestDispatcher("/WEB-INF/bootstrap.jsp").include(request, response);
	}
}
