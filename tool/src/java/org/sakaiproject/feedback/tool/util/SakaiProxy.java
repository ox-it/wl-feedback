package org.sakaiproject.feedback.util;

import org.sakaiproject.tool.api.SessionManager;

import lombok.Setter;

public class SakaiProxy {

    @Setter
    private SessionManager sessionManager;

    public String getCurrentUserId() {
        return sessionManager.getCurrentSessionUserId();
    }
}
