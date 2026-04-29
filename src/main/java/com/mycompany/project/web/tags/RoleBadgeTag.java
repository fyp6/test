package com.mycompany.project.web.tags;

import java.io.IOException;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;

public class RoleBadgeTag extends SimpleTagSupport {
    private String role;

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public void doTag() throws JspException, IOException {
        String safeRole = role == null ? "UNKNOWN" : role.toUpperCase();
        String css = "badge-patient";
        if ("ADMIN".equals(safeRole)) {
            css = "badge-admin";
        } else if ("STAFF".equals(safeRole)) {
            css = "badge-staff";
        }

        JspWriter out = getJspContext().getOut();
        out.write("<span class='role-badge " + css + "'>" + escapeHtml(safeRole) + "</span>");
    }

    private String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
