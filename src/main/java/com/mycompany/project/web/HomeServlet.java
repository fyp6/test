package com.mycompany.project.web;

import java.io.IOException;

import com.mycompany.project.model.UserBean;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserBean user = SecurityUtil.currentUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
        } else {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        }
    }
}
