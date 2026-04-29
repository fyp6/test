package com.mycompany.project.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

import com.mycompany.project.dao.AppointmentDao;
import com.mycompany.project.dao.AuditDao;
import com.mycompany.project.dao.ClinicDao;
import com.mycompany.project.dao.PolicyDao;
import com.mycompany.project.dao.ReportDao;
import com.mycompany.project.dao.ServiceDao;
import com.mycompany.project.dao.TimeslotDao;
import com.mycompany.project.dao.UserDao;
import com.mycompany.project.model.ClinicBean;
import com.mycompany.project.model.ServiceBean;
import com.mycompany.project.model.TimeslotBean;
import com.mycompany.project.model.UserBean;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/admin/users", "/admin/reports", "/admin/policy", "/admin/audit", "/admin/catalog"})
public class AdminServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();
    private final ClinicDao clinicDao = new ClinicDao();
    private final ServiceDao serviceDao = new ServiceDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final ReportDao reportDao = new ReportDao();
    private final PolicyDao policyDao = new PolicyDao();
    private final AuditDao auditDao = new AuditDao();
    private final TimeslotDao timeslotDao = new TimeslotDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String path = req.getServletPath();
            switch (path) {
                case "/admin/users":
                    loadUsersPage(req);
                    req.getRequestDispatcher("/WEB-INF/views/admin-users.jsp").forward(req, resp);
                    break;
                case "/admin/reports":
                    loadReportsPage(req);
                    req.getRequestDispatcher("/WEB-INF/views/admin-reports.jsp").forward(req, resp);
                    break;
                case "/admin/audit":
                    loadAuditPage(req);
                    req.getRequestDispatcher("/WEB-INF/views/admin-audit.jsp").forward(req, resp);
                    break;
                case "/admin/catalog":
                    loadCatalogPage(req);
                    req.getRequestDispatcher("/WEB-INF/views/admin-catalog.jsp").forward(req, resp);
                    break;
                default:
                    loadPolicyPage(req);
                    req.getRequestDispatcher("/WEB-INF/views/admin-policy.jsp").forward(req, resp);
                    break;
            }
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        try {
            switch (path) {
                case "/admin/users":
                    handleUsersPost(req);
                    resp.sendRedirect(req.getContextPath() + "/admin/users");
                    break;
                case "/admin/policy":
                    handlePolicyPost(req);
                    resp.sendRedirect(req.getContextPath() + "/admin/policy");
                    break;
                case "/admin/catalog":
                    handleCatalogPost(req);
                    resp.sendRedirect(buildCatalogRedirect(req));
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    break;
            }
        } catch (SQLException ex) {
            if ("/admin/users".equals(path)) {
                req.getSession().setAttribute("flashError", resolveUserErrorMessage(ex));
                resp.sendRedirect(req.getContextPath() + "/admin/users");
                return;
            }
            if ("/admin/catalog".equals(path)) {
                req.getSession().setAttribute("flashError", "Catalog update failed: " + ex.getMessage());
                resp.sendRedirect(buildCatalogRedirect(req));
                return;
            }
            throw new ServletException(ex);
        }
    }

    private void loadUsersPage(HttpServletRequest req) throws SQLException {
        req.setAttribute("users", userDao.listUsers());
        req.setAttribute("clinics", clinicDao.listAll());

        Long editUserId = parseLongOrNull(req.getParameter("editUserId"));
        if (editUserId != null) {
            req.setAttribute("editUser", userDao.findById(editUserId));
        }
    }

    private void loadReportsPage(HttpServletRequest req) throws SQLException {
        req.setAttribute("clinics", clinicDao.listAll());
        req.setAttribute("services", serviceDao.listAll());

        Long clinicId = parseLongOrNull(req.getParameter("clinicId"));
        Long serviceId = parseLongOrNull(req.getParameter("serviceId"));
        Integer month = parseIntOrNull(req.getParameter("month"));
        Integer year = parseIntOrNull(req.getParameter("year"));
        String status = req.getParameter("status");

        LocalDate now = LocalDate.now();
        if (month == null) {
            month = now.getMonthValue();
        }
        if (year == null) {
            year = now.getYear();
        }

        req.setAttribute("selectedClinicId", clinicId);
        req.setAttribute("selectedServiceId", serviceId);
        req.setAttribute("selectedMonth", month);
        req.setAttribute("selectedYear", year);
        req.setAttribute("selectedStatus", status == null ? "" : status);

        req.setAttribute("appointmentRecords", appointmentDao.listWithFilters(clinicId, serviceId, month, year, status));
        req.setAttribute("utilizationRows", reportDao.utilization(clinicId, serviceId, month, year));
        req.setAttribute("noShowRows", reportDao.noShowSummary(month, year));
    }

    private void loadPolicyPage(HttpServletRequest req) throws SQLException {
        req.setAttribute("policies", policyDao.listAll());
    }

    private void loadCatalogPage(HttpServletRequest req) throws SQLException {
        Long clinicId = parseLongOrNull(req.getParameter("clinicId"));
        Long serviceId = parseLongOrNull(req.getParameter("serviceId"));
        LocalDate slotDate = parseDateOrNull(req.getParameter("slotDate"));
        if (slotDate == null) {
            slotDate = LocalDate.now();
        }

        req.setAttribute("clinics", clinicDao.listAll());
        req.setAttribute("services", serviceDao.listAll());
        req.setAttribute("timeslots", timeslotDao.listByFilters(clinicId, serviceId, slotDate));
        req.setAttribute("selectedClinicId", clinicId);
        req.setAttribute("selectedServiceId", serviceId);
        req.setAttribute("selectedSlotDate", slotDate.toString());
    }

    private void loadAuditPage(HttpServletRequest req) throws SQLException {
        String action = req.getParameter("action");
        String keyword = req.getParameter("keyword");

        req.setAttribute("selectedAction", action == null ? "" : action);
        req.setAttribute("selectedKeyword", keyword == null ? "" : keyword);
        req.setAttribute("auditLogs", auditDao.listLogs(action, keyword, 100));
    }

    private void handleUsersPost(HttpServletRequest req) throws SQLException {
        String action = req.getParameter("action");
        if ("create".equals(action)) {
            UserBean user = new UserBean();
            user.setUsername(req.getParameter("username"));
            user.setFullName(req.getParameter("fullName"));
            user.setEmail(req.getParameter("email"));
            String role = req.getParameter("role");
            user.setRole(role);
            user.setClinicId("STAFF".equals(role) ? parseLongOrNull(req.getParameter("clinicId")) : null);
            user.setActive(true);
            String password = req.getParameter("password");
            if (password == null || password.length() < 6) {
                return;
            }

            if (userDao.usernameExists(user.getUsername())) {
                req.getSession().setAttribute("flashError", "Username already exists.");
                return;
            }

            userDao.createUser(user, password);
            req.getSession().setAttribute("flash", "User created.");
            return;
        }

        if ("delete".equals(action)) {
            Long userId = parseLongOrNull(req.getParameter("userId"));
            if (userId != null) {
                userDao.deleteUser(userId);
            }
            return;
        }

        if ("update".equals(action)) {
            Long userId = parseLongOrNull(req.getParameter("userId"));
            if (userId == null) {
                return;
            }
            UserBean user = userDao.findById(userId);
            if (user == null) {
                return;
            }
            user.setFullName(req.getParameter("fullName"));
            user.setEmail(req.getParameter("email"));
            user.setRole(req.getParameter("role"));
            user.setClinicId(parseLongOrNull(req.getParameter("clinicId")));
            user.setActive("on".equals(req.getParameter("active")) || "true".equals(req.getParameter("active")));
            userDao.updateUser(user);
        }
    }

    private String resolveUserErrorMessage(SQLException ex) {
        if (isDuplicateUsernameError(ex)) {
            return "Username already exists.";
        }
        String message = ex.getMessage();
        return message == null || message.isBlank() ? "User operation failed." : "User operation failed: " + message;
    }

    private boolean isDuplicateUsernameError(SQLException ex) {
        String message = ex.getMessage();
        return ex.getErrorCode() == 1062
                || "23000".equals(ex.getSQLState())
                || (message != null && message.toLowerCase().contains("duplicate"));
    }

    private void handlePolicyPost(HttpServletRequest req) throws SQLException {
        policyDao.upsert("max_active_bookings", req.getParameter("max_active_bookings"));
        policyDao.upsert("cancellation_cutoff_hours", req.getParameter("cancellation_cutoff_hours"));
        policyDao.upsert("queue_enabled", req.getParameter("queue_enabled") == null ? "false" : "true");
    }

    private void handleCatalogPost(HttpServletRequest req) throws SQLException {
        String action = req.getParameter("action");
        if (action == null || action.isBlank()) {
            req.getSession().setAttribute("flashError", "Missing catalog action.");
            return;
        }

        switch (action) {
            case "clinic-create": {
                String name = req.getParameter("name");
                String location = req.getParameter("location");
                if (name == null || name.isBlank() || location == null || location.isBlank()) {
                    req.getSession().setAttribute("flashError", "Clinic name and location are required.");
                    return;
                }
                ClinicBean clinic = new ClinicBean();
                clinic.setName(name.trim());
                clinic.setLocation(location.trim());
                clinic.setWalkInEnabled(isChecked(req.getParameter("walkInEnabled")));
                clinicDao.create(clinic);
                req.getSession().setAttribute("flash", "Clinic created.");
                return;
            }
            case "clinic-update": {
                Long clinicId = parseLongOrNull(req.getParameter("clinicId"));
                ClinicBean clinic = clinicId == null ? null : clinicDao.findById(clinicId);
                if (clinic == null) {
                    req.getSession().setAttribute("flashError", "Clinic not found.");
                    return;
                }
                String name = req.getParameter("name");
                String location = req.getParameter("location");
                if (name == null || name.isBlank() || location == null || location.isBlank()) {
                    req.getSession().setAttribute("flashError", "Clinic name and location are required.");
                    return;
                }
                clinic.setName(name.trim());
                clinic.setLocation(location.trim());
                clinic.setWalkInEnabled(isChecked(req.getParameter("walkInEnabled")));
                clinicDao.update(clinic);
                req.getSession().setAttribute("flash", "Clinic updated.");
                return;
            }
            case "clinic-delete": {
                Long clinicId = parseLongOrNull(req.getParameter("clinicId"));
                if (clinicId == null) {
                    req.getSession().setAttribute("flashError", "Clinic not found.");
                    return;
                }
                clinicDao.delete(clinicId);
                req.getSession().setAttribute("flash", "Clinic deleted.");
                return;
            }
            case "service-create": {
                String name = req.getParameter("name");
                if (name == null || name.isBlank()) {
                    req.getSession().setAttribute("flashError", "Service name is required.");
                    return;
                }
                ServiceBean service = new ServiceBean();
                service.setName(name.trim());
                service.setDescription(req.getParameter("description"));
                service.setLimitedQuota(isChecked(req.getParameter("limitedQuota")));
                serviceDao.create(service);
                req.getSession().setAttribute("flash", "Service created.");
                return;
            }
            case "service-update": {
                Long serviceId = parseLongOrNull(req.getParameter("serviceId"));
                ServiceBean service = serviceId == null ? null : serviceDao.findById(serviceId);
                if (service == null) {
                    req.getSession().setAttribute("flashError", "Service not found.");
                    return;
                }
                String name = req.getParameter("name");
                if (name == null || name.isBlank()) {
                    req.getSession().setAttribute("flashError", "Service name is required.");
                    return;
                }
                service.setName(name.trim());
                service.setDescription(req.getParameter("description"));
                service.setLimitedQuota(isChecked(req.getParameter("limitedQuota")));
                serviceDao.update(service);
                req.getSession().setAttribute("flash", "Service updated.");
                return;
            }
            case "service-delete": {
                Long serviceId = parseLongOrNull(req.getParameter("serviceId"));
                if (serviceId == null) {
                    req.getSession().setAttribute("flashError", "Service not found.");
                    return;
                }
                serviceDao.delete(serviceId);
                req.getSession().setAttribute("flash", "Service deleted.");
                return;
            }
            case "slot-create": {
                Long clinicId = parseLongOrNull(req.getParameter("clinicId"));
                Long serviceId = parseLongOrNull(req.getParameter("serviceId"));
                TimeslotBean slot = new TimeslotBean();
                if (clinicId == null || serviceId == null) {
                    req.getSession().setAttribute("flashError", "Clinic and service are required.");
                    return;
                }
                slot.setClinicId(clinicId);
                slot.setServiceId(serviceId);
                slot.setSlotDate(parseDateOrNull(req.getParameter("slotDate")));
                slot.setStartTime(parseTimeOrNull(req.getParameter("startTime")));
                slot.setEndTime(parseTimeOrNull(req.getParameter("endTime")));
                Integer capacity = parseIntOrNull(req.getParameter("capacity"));
                if (slot.getSlotDate() == null || slot.getStartTime() == null || slot.getEndTime() == null
                        || capacity == null || capacity < 1) {
                    req.getSession().setAttribute("flashError", "Complete all timeslot fields.");
                    return;
                }
                if (!slot.getEndTime().isAfter(slot.getStartTime())) {
                    req.getSession().setAttribute("flashError", "End time must be after start time.");
                    return;
                }
                slot.setCapacity(capacity);
                timeslotDao.create(slot);
                req.getSession().setAttribute("flash", "Timeslot created.");
                return;
            }
            case "slot-delete": {
                Long timeslotId = parseLongOrNull(req.getParameter("timeslotId"));
                if (timeslotId == null) {
                    req.getSession().setAttribute("flashError", "Timeslot not found.");
                    return;
                }
                timeslotDao.delete(timeslotId);
                req.getSession().setAttribute("flash", "Timeslot deleted.");
                return;
            }
            default:
                req.getSession().setAttribute("flashError", "Unknown catalog action.");
        }
    }

    private String buildCatalogRedirect(HttpServletRequest req) {
        StringBuilder url = new StringBuilder(req.getContextPath() + "/admin/catalog");
        String clinicId = req.getParameter("redirectClinicId");
        String serviceId = req.getParameter("redirectServiceId");
        String slotDate = req.getParameter("redirectSlotDate");
        boolean hasQuery = false;

        if (clinicId != null && !clinicId.isBlank()) {
            url.append(hasQuery ? "&" : "?").append("clinicId=").append(encode(clinicId));
            hasQuery = true;
        }
        if (serviceId != null && !serviceId.isBlank()) {
            url.append(hasQuery ? "&" : "?").append("serviceId=").append(encode(serviceId));
            hasQuery = true;
        }
        if (slotDate != null && !slotDate.isBlank()) {
            url.append(hasQuery ? "&" : "?").append("slotDate=").append(encode(slotDate));
        }

        return url.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isChecked(String value) {
        return value != null && ("on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value));
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.valueOf(value);
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }

    private LocalTime parseTimeOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalTime.parse(value);
    }
}
