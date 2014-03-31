package org.sakaiproject.feedback.db;

import java.sql.Connection;

import org.sakaiproject.db.api.SqlService;

import org.apache.log4j.Logger;

public class Database {

	private Logger logger = Logger.getLogger(Database.class);

    private SqlService sqlService;
    public void setSqlService(SqlService sqlService) {
        this.sqlService = sqlService;
    }

    private String insertReportSql
        = "INSERT INTO sakai_feedback (user_id, site_id, report_type, title, content) VALUES(?,?,?,?,?)";

    public void init() {
        
        // Load the ddl strings from resources and execute them
        sqlService.ddl(this.getClass().getClassLoader(), "createtables");
    }

    public void logReport(String userId, String siteId, String type, String title, String content) {

        Connection conn = null;

        try {
            conn = sqlService.borrowConnection();
            sqlService.dbInsert(conn, insertReportSql, new String[] {userId, siteId, type, title, content}, null);
        } catch (Exception e) {
            logger.error("Failed to insert feedback report.", e);
        } finally {
            if (conn != null) {
                sqlService.returnConnection(conn);
            }
        }
    }
}
