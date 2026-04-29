package com.mycompany.project.web;

import java.io.IOException;
import java.sql.SQLException;

import com.mycompany.project.dao.NotificationDao;
import com.mycompany.project.model.UserBean;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/notifications")
public class NotificationServlet extends HttpServlet {
    private final NotificationDao notificationDao = new NotificationDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserBean current = SecurityUtil.currentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        try {
            notificationDao.markAllRead(current.getId());
            req.getSession().setAttribute("flash", "Notifications marked as read.");
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }

        String back = req.getParameter("back");
        if (back == null || back.isBlank()) {
            back = "/dashboard";
        }
        resp.sendRedirect(req.getContextPath() + back);
    }
}
