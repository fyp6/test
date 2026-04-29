package com.mycompany.project.web;

import com.mycompany.project.model.UserBean;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class SecurityUtil {
    private SecurityUtil() {
    }

    public static UserBean currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        return (UserBean) session.getAttribute("currentUser");
    }

    public static boolean hasRole(UserBean user, String role) {
        return user != null && role.equalsIgnoreCase(user.getRole());
    }

    public static boolean hasAnyRole(UserBean user, String... roles) {
        if (user == null) {
            return false;
        }
        for (String role : roles) {
            if (role.equalsIgnoreCase(user.getRole())) {
                return true;
            }
        }
        return false;
    }
}
