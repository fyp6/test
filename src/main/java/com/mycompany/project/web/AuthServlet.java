package com.mycompany.project.web;

import java.io.IOException;
import java.sql.SQLException;

import com.mycompany.project.dao.NotificationDao;
import com.mycompany.project.dao.UserDao;
import com.mycompany.project.model.UserBean;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/login", "/register", "/logout"})
public class AuthServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();
    private final NotificationDao notificationDao = new NotificationDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/logout".equals(path)) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        if ("/register".equals(path)) {
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        try {
            switch (path) {
                case "/register":
                    handleRegister(req, resp);
                    break;
                case "/login":
                    handleLogin(req, resp);
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    break;
            }
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        UserBean user = userDao.authenticate(username, password);
        if (user == null) {
            req.setAttribute("error", "Invalid username or password.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("currentUser", user);
        resp.sendRedirect(req.getContextPath() + "/dashboard");
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        if (fullName == null || fullName.isBlank() || username == null || username.isBlank()
                || password == null || password.length() < 6 || !password.equals(confirmPassword)) {
            req.setAttribute("error", "Please complete all required fields. Password min length is 6.");
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        UserBean user = new UserBean();
        user.setFullName(fullName.trim());
        user.setEmail(email == null ? "" : email.trim());
        user.setUsername(username.trim());
        user.setPassword(password);

        if (!userDao.registerPatient(user)) {
            req.setAttribute("error", "Username already exists.");
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        UserBean created = userDao.authenticate(username.trim(), password);
        if (created != null) {
            notificationDao.create(created.getId(), "Registration completed", "Welcome to CCHC patient portal.", "ACCOUNT");
        }

        req.setAttribute("success", "Registration successful. Please login.");
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
    }
}
