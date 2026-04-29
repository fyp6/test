package com.mycompany.project.web;

import java.io.IOException;
import java.sql.SQLException;

import com.mycompany.project.dao.UserDao;
import com.mycompany.project.model.UserBean;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserBean current = SecurityUtil.currentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String newPassword = req.getParameter("newPassword");

        try {
            userDao.updateProfile(current.getId(), fullName, email, newPassword);
            UserBean refreshed = userDao.findById(current.getId());
            req.getSession().setAttribute("currentUser", refreshed);
            req.setAttribute("success", "Profile updated.");
            req.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(req, resp);
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }
}
