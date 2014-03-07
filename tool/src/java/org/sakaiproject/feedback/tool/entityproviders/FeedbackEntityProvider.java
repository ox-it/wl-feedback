package org.sakaiproject.feedback.tool.entityproviders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import lombok.Setter;

import org.apache.log4j.Logger;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.util.ResourceLoader;

public class FeedbackEntityProvider extends AbstractEntityProvider implements AutoRegisterEntityProvider, Outputable, Describeable, ActionsExecutable {
	
	public final static String ENTITY_PREFIX = "feedback";

	private final Logger LOG = Logger.getLogger(getClass());

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
}
