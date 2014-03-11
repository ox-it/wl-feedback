package org.sakaiproject.feedback.tool;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.feedback.util.SakaiProxy;
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

        if (userId == null) {
            logger.error("No current user. Throwing a ServletException ...");
			throw new ServletException("You are not currently logged in.");
        }

		request.setAttribute("sakaiHtmlHead", (String) request.getAttribute("sakai.html.head"));
        request.setAttribute("userId", userId);
        request.setAttribute("language", (new ResourceLoader(userId)).getLocale().getLanguage());
        request.setAttribute("featureSuggestionUrl", sakaiProxy.getConfigString("featureSuggestionUrl", "http://www.jazzwax.com"));

        request.getRequestDispatcher("/WEB-INF/bootstrap.jsp").include(request, response);
	}
}
