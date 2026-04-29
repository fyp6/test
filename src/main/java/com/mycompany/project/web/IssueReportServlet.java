package com.mycompany.project.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.mycompany.project.dao.AuditDao;
import com.mycompany.project.dao.ClinicDao;
import com.mycompany.project.dao.NotificationDao;
import com.mycompany.project.dao.ServiceDao;
import com.mycompany.project.dao.UserDao;
import com.mycompany.project.model.AuditLogBean;
import com.mycompany.project.model.ClinicBean;
import com.mycompany.project.model.ServiceBean;
import com.mycompany.project.model.UserBean;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/issues")
public class IssueReportServlet extends HttpServlet {
    private final AuditDao auditDao = new AuditDao();
    private final ClinicDao clinicDao = new ClinicDao();
    private final ServiceDao serviceDao = new ServiceDao();
    private final NotificationDao notificationDao = new NotificationDao();
    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserBean current = SecurityUtil.currentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if (!SecurityUtil.hasAnyRole(current, "STAFF", "ADMIN")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            req.setAttribute("services", serviceDao.listAll());
            if (SecurityUtil.hasRole(current, "STAFF") && current.getClinicId() != null) {
                ClinicBean fixedClinic = clinicDao.findById(current.getClinicId());
                if (fixedClinic != null) {
                    req.setAttribute("fixedClinic", fixedClinic);
                } else {
                    req.setAttribute("clinics", clinicDao.listAll());
                }
            } else {
                req.setAttribute("clinics", clinicDao.listAll());
            }
            List<AuditLogBean> issueHistory = auditDao.listLogsByStaff(current.getId(), "ISSUE_REPORTED", null, 20);
            req.setAttribute("issueHistory", issueHistory);
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }

        req.getRequestDispatcher("/WEB-INF/views/staff-issues.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserBean current = SecurityUtil.currentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if (!SecurityUtil.hasAnyRole(current, "STAFF", "ADMIN")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            handleSubmit(req, current);
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }

        resp.sendRedirect(req.getContextPath() + "/issues");
    }

    private void handleSubmit(HttpServletRequest req, UserBean current) throws SQLException {
        String issueType = normalize(req.getParameter("issueType"), "OTHER");
        String severity = normalize(req.getParameter("severity"), "MEDIUM");
        String title = normalizeRequired(req.getParameter("title"));
        String details = normalizeRequired(req.getParameter("details"));

        if (title == null || details == null) {
            req.getSession().setAttribute("flashError", "Please complete the issue title and details.");
            return;
        }

        Long clinicId = current.getClinicId();
        if (clinicId == null) {
            clinicId = parseLongOrNull(req.getParameter("clinicId"));
        }
        Long serviceId = parseLongOrNull(req.getParameter("serviceId"));

        ClinicBean clinic = clinicId == null ? null : clinicDao.findById(clinicId);
        ServiceBean service = serviceId == null ? null : serviceDao.findById(serviceId);

        String summary = buildSummary(issueType, severity, clinic, service, title, details);
        auditDao.log(current.getId(), "ISSUE_REPORTED", summary);
        notifyAdmins(issueType, severity, clinic, service, title, details);

        req.getSession().setAttribute("flash", "Issue report submitted. Admins have been notified.");
    }

    private void notifyAdmins(String issueType, String severity, ClinicBean clinic, ServiceBean service,
            String title, String details) throws SQLException {
        List<UserBean> admins = userDao.listByRole("ADMIN");
        String body = buildNotificationBody(issueType, severity, clinic, service, title, details);
        for (UserBean admin : admins) {
            notificationDao.create(admin.getId(), "Operational issue reported", body, "ISSUE");
        }
    }

    private String buildSummary(String issueType, String severity, ClinicBean clinic, ServiceBean service,
            String title, String details) {
        StringBuilder summary = new StringBuilder();
        summary.append("issueType=").append(safeValue(issueType));
        summary.append("; severity=").append(safeValue(severity));
        summary.append("; clinic=").append(clinic == null ? "N/A" : safeValue(clinic.getName()));
        summary.append("; service=").append(service == null ? "N/A" : safeValue(service.getName()));
        summary.append("; title=").append(safeValue(title));
        summary.append("; details=").append(safeValue(details));
        return limit(summary.toString(), 255);
    }

    private String buildNotificationBody(String issueType, String severity, ClinicBean clinic, ServiceBean service,
            String title, String details) {
        StringBuilder body = new StringBuilder();
        body.append(safeValue(severity)).append(" / ").append(safeValue(issueType));
        body.append(" | ").append(clinic == null ? "General" : safeValue(clinic.getName()));
        if (service != null) {
            body.append(" / ").append(safeValue(service.getName()));
        }
        body.append(" | ").append(safeValue(title));
        if (details != null && !details.isBlank()) {
            body.append(" - ").append(safeValue(details));
        }
        return limit(body.toString(), 255);
    }

    private String normalize(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return safeValue(value.trim());
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return safeValue(value.trim());
    }

    private String safeValue(String value) {
        return value == null ? "" : value.replace("\r", " ").replace("\n", " ").replace(";", ",").trim();
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }
}