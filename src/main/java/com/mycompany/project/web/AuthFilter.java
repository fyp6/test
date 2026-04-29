package com.mycompany.project.web;

import java.io.IOException;

import com.mycompany.project.model.UserBean;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebFilter("/*")
public class AuthFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        UserBean user = SecurityUtil.currentUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        if (path.startsWith("/admin") && !SecurityUtil.hasRole(user, "ADMIN")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    private boolean isPublic(String path) {
        return path.equals("/")
                || path.equals("/index.html")
                || path.equals("/index.jsp")
                || path.equals("/login")
                || path.equals("/register")
                || path.equals("/logout")
                || path.startsWith("/resources/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/favicon");
    }
}
