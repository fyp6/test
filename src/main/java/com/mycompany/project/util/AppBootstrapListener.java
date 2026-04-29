package com.mycompany.project.util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppBootstrapListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            SchemaInitializer.initialize();
            sce.getServletContext().log("Database initialized successfully.");
        } catch (RuntimeException ex) {
            sce.getServletContext().log("Database initialization failed. Application will continue to start.", ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // no-op
    }
}
