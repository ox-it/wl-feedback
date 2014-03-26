package org.sakaiproject.feedback.tool.entityproviders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import lombok.Setter;

import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileItem;

import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.feedback.util.Constants;
import org.sakaiproject.feedback.util.SakaiProxy;
import org.sakaiproject.util.RequestFilter;
import org.sakaiproject.util.ResourceLoader;

public class FeedbackEntityProvider extends AbstractEntityProvider implements AutoRegisterEntityProvider, Outputable, Describeable, ActionsExecutable {
	
	public final static String ENTITY_PREFIX = "feedback";

	private final Logger logger = Logger.getLogger(getClass());

    private SakaiProxy sakaiProxy = null;
    public void setSakaiProxy(SakaiProxy sakaiProxy) {
        this.sakaiProxy = sakaiProxy;
    }

	public Object getSampleEntity() {
		return null;
	}

	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}

	public String[] getHandledOutputFormats() {
		return new String[] { Formats.JSON };
	}

	@EntityCustomAction(action = "translations", viewKey = EntityView.VIEW_SHOW)
	public Map getTranslationsForPath(EntityView view, Map<String, Object> params) {
		
		String userId = developerHelperService.getCurrentUserId();
		
		if (userId == null) {
			throw new EntityException("You must be logged in to retrieve translations", "", HttpServletResponse.SC_UNAUTHORIZED);
		}

        String bundlePath = (String) params.get("path");

        if (bundlePath == null) {
			throw new EntityException("You need to supply the bundle path as the 'path' parameter", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        ResourceLoader loader = new ResourceLoader(bundlePath);
        return loader;
	}

	@EntityCustomAction(action = "reportcontent", viewKey = EntityView.VIEW_EDIT)
	public String handleContentReport(EntityView view, Map<String, Object> params) {
		
		String userId = developerHelperService.getCurrentUserId();
		
		if (userId == null) {
			throw new EntityException("You must be logged in to post a content report", "", HttpServletResponse.SC_UNAUTHORIZED);
		}

        String siteId = view.getPathSegment(1);

		if (siteId == null) {
			throw new EntityException("You must supply a site id to post a technical report", "", HttpServletResponse.SC_BAD_REQUEST);
		}

        if (logger.isDebugEnabled()) logger.debug("Site ID: " + siteId);

        String title = (String) params.get("title");
        String description = (String) params.get("description");

        if (title == null || title.length() == 0) {
			logger.error("No title. Returning BAD REQUEST ...");
			throw new EntityException("You need to supply a title", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (description == null || description.length() == 0) {
			logger.error("No description. Returning BAD REQUEST ...");
			throw new EntityException("You need to supply a description", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (logger.isDebugEnabled()) logger.debug("title: " + title + ". description: " + description);

        List<FileItem> attachments = getAttachments(params);

        sakaiProxy.sendEmail(userId, siteId, Constants.CONTENT, title, description, attachments);

        return "success";
	}

	@EntityCustomAction(action = "reporttechnical", viewKey = EntityView.VIEW_EDIT)
	public String handleFunctionalityReport(EntityView view, Map<String, Object> params) {
		
		String userId = developerHelperService.getCurrentUserId();
		
		if (userId == null) {
			throw new EntityException("You must be logged in to post a technical report", "", HttpServletResponse.SC_UNAUTHORIZED);
		}

        String siteId = view.getPathSegment(1);

		if (siteId == null) {
			throw new EntityException("You must supply a site id to post a technical report", "", HttpServletResponse.SC_BAD_REQUEST);
		}

        if (logger.isDebugEnabled()) logger.debug("Site ID: " + siteId);

        String title = (String) params.get("title");
        String description = (String) params.get("description");

        if (title == null || title.length() == 0) {
			logger.error("No title. Returning BAD REQUEST ...");
			throw new EntityException("You need to supply a title", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (description == null || description.length() == 0) {
			logger.error("No description. Returning BAD REQUEST ...");
			throw new EntityException("You need to supply a description", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (logger.isDebugEnabled()) logger.debug("title: " + title + ". description: " + description);

        List<FileItem> attachments = getAttachments(params);

        sakaiProxy.sendEmail(userId, siteId, Constants.TECHNICAL, title, description, attachments);

        return "success";
	}

	private List<FileItem> getAttachments(Map<String, Object> params) {

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
			}
		}

		return fileItems;
	}
}
