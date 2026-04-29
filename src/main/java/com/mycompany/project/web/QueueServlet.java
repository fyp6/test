package com.mycompany.project.web;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

import com.mycompany.project.dao.AuditDao;
import com.mycompany.project.dao.ClinicDao;
import com.mycompany.project.dao.NotificationDao;
import com.mycompany.project.dao.PolicyDao;
import com.mycompany.project.dao.QueueDao;
import com.mycompany.project.dao.ServiceDao;
import com.mycompany.project.model.ClinicBean;
import com.mycompany.project.model.QueueTicketBean;
import com.mycompany.project.model.UserBean;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/queue")
public class QueueServlet extends HttpServlet {
    private final QueueDao queueDao = new QueueDao();
    private final ClinicDao clinicDao = new ClinicDao();
    private final ServiceDao serviceDao = new ServiceDao();
    private final NotificationDao notificationDao = new NotificationDao();
    private final PolicyDao policyDao = new PolicyDao();
    private final AuditDao auditDao = new AuditDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserBean current = SecurityUtil.currentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        try {
            req.setAttribute("queueEnabled", policyDao.getBoolean("queue_enabled", true));
            req.setAttribute("clinics", clinicDao.listAll());
            req.setAttribute("services", serviceDao.listAll());

            if (SecurityUtil.hasRole(current, "PATIENT")) {
                req.setAttribute("myTickets", queueDao.listByPatient(current.getId()));
            }

            Long clinicId = SecurityUtil.hasRole(current, "STAFF")
                    ? current.getClinicId()
                    : parseLongOrNull(req.getParameter("clinicId"));
            Long serviceId = parseLongOrNull(req.getParameter("serviceId"));
            LocalDate day = parseDateOrToday(req.getParameter("queueDate"));
            req.setAttribute("queueDate", day.toString());
            req.setAttribute("selectedClinicId", clinicId);
            req.setAttribute("selectedServiceId", serviceId);

            if (clinicId != null && serviceId != null) {
                req.setAttribute("queueList", queueDao.listByClinicServiceDay(clinicId, serviceId, day));
            }
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }

        req.getRequestDispatcher("/WEB-INF/views/queue.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserBean current = SecurityUtil.currentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String action = req.getParameter("action");
        try {
            if (action != null) {
                switch (action) {
                    case "join":
                        joinQueue(req, current);
                        break;
                    case "next":
                        callNext(req, current);
                        break;
                    case "skip":
                    case "served":
                    case "expired":
                        updateTicket(req, current, action);
                        break;
                    default:
                        break;
                }
            }
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }

        resp.sendRedirect(req.getContextPath() + "/queue");
    }

    private void joinQueue(HttpServletRequest req, UserBean current) throws SQLException {
        if (!SecurityUtil.hasRole(current, "PATIENT")) {
            req.getSession().setAttribute("flashError", "Only patients can join queue.");
            return;
        }
        if (!policyDao.getBoolean("queue_enabled", true)) {
            req.getSession().setAttribute("flashError", "Queue is temporarily disabled.");
            return;
        }

        Long clinicIdValue = parseLongOrNull(req.getParameter("clinicId"));
        Long serviceIdValue = parseLongOrNull(req.getParameter("serviceId"));
        if (clinicIdValue == null || serviceIdValue == null) {
            req.getSession().setAttribute("flashError", "Clinic and service are required.");
            return;
        }

        long clinicId = clinicIdValue;
        long serviceId = serviceIdValue;
        LocalDate date = parseDateOrToday(req.getParameter("queueDate"));
        if (!LocalDate.now().equals(date)) {
            req.getSession().setAttribute("flashError", "Walk-in queue is same-day only.");
            return;
        }

        ClinicBean clinic = clinicDao.findById(clinicId);
        if (clinic == null) {
            req.getSession().setAttribute("flashError", "Clinic not found.");
            return;
        }
        if (!clinic.isWalkInEnabled()) {
            req.getSession().setAttribute("flashError", "This clinic does not accept walk-ins.");
            return;
        }

        String error = queueDao.joinQueue(current.getId(), clinicId, serviceId, date);
        if (error != null) {
            req.getSession().setAttribute("flashError", error);
            return;
        }

        notificationDao.create(current.getId(), "Queue joined",
                "You have joined the queue for today.", "QUEUE");
        req.getSession().setAttribute("flash", "Queue ticket created.");
    }

    private void callNext(HttpServletRequest req, UserBean current) throws SQLException {
        if (!SecurityUtil.hasAnyRole(current, "STAFF", "ADMIN")) {
            req.getSession().setAttribute("flashError", "Permission denied.");
            return;
        }

        Long selectedClinicId = SecurityUtil.hasRole(current, "STAFF")
            ? current.getClinicId()
            : parseLongOrNull(req.getParameter("clinicId"));
        if (selectedClinicId == null) {
            req.getSession().setAttribute("flashError", "Clinic is required.");
            return;
        }

        Long selectedServiceId = parseLongOrNull(req.getParameter("serviceId"));
        if (selectedServiceId == null) {
            req.getSession().setAttribute("flashError", "Service is required.");
            return;
        }

        long clinicId = selectedClinicId;
        long serviceId = selectedServiceId;
        LocalDate day = parseDateOrToday(req.getParameter("queueDate"));

        QueueTicketBean next = queueDao.findFirstWaiting(clinicId, serviceId, day);
        if (next == null) {
            req.getSession().setAttribute("flashError", "No waiting patient.");
            return;
        }

        queueDao.updateQueueStatus(next.getId(), "CALLED");
        notificationDao.create(next.getPatientId(), "Queue called",
                "Ticket #" + next.getQueueNumber() + " is now called.", "QUEUE");
        auditDao.log(current.getId(), "QUEUE_NEXT", "ticketId=" + next.getId());
        req.getSession().setAttribute("flash", "Called ticket #" + next.getQueueNumber());
    }

    private void updateTicket(HttpServletRequest req, UserBean current, String action) throws SQLException {
        if (!SecurityUtil.hasAnyRole(current, "STAFF", "ADMIN")) {
            req.getSession().setAttribute("flashError", "Permission denied.");
            return;
        }

        long ticketId = Long.parseLong(req.getParameter("ticketId"));
        String targetStatus;
        String title;
        String body;
        switch (action) {
            case "skip":
                targetStatus = "SKIPPED";
                title = "Queue skipped";
                body = "Your queue ticket was skipped. Please check with front desk.";
                break;
            case "served":
                targetStatus = "SERVED";
                title = "Queue served";
                body = "Your queue ticket has been completed.";
                break;
            default:
                targetStatus = "EXPIRED";
                title = "Queue expired";
                body = "Your queue ticket has expired.";
                break;
        }

        queueDao.updateQueueStatus(ticketId, targetStatus);

        Long patientId = parseLongOrNull(req.getParameter("patientId"));
        if (patientId != null) {
            notificationDao.create(patientId, title, body, "QUEUE");
        }

        auditDao.log(current.getId(), "QUEUE_STATUS", "ticketId=" + ticketId + "; status=" + targetStatus);
        req.getSession().setAttribute("flash", "Queue ticket updated to " + targetStatus + ".");
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    private LocalDate parseDateOrToday(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(value);
    }
}
