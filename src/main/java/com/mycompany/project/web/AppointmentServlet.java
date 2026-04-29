package com.mycompany.project.web;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.mycompany.project.dao.AppointmentDao;
import com.mycompany.project.dao.AuditDao;
import com.mycompany.project.dao.ClinicDao;
import com.mycompany.project.dao.NotificationDao;
import com.mycompany.project.dao.PolicyDao;
import com.mycompany.project.dao.ServiceDao;
import com.mycompany.project.model.AppointmentBean;
import com.mycompany.project.model.UserBean;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/appointments")
public class AppointmentServlet extends HttpServlet {
    private final AppointmentDao appointmentDao = new AppointmentDao();
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
            req.setAttribute("clinics", clinicDao.listAll());
            req.setAttribute("services", serviceDao.listAll());

            String clinicParam = req.getParameter("clinicId");
            String serviceParam = req.getParameter("serviceId");
            String dateParam = req.getParameter("slotDate");
            LocalDate date = (dateParam == null || dateParam.isBlank()) ? LocalDate.now() : LocalDate.parse(dateParam);
            req.setAttribute("slotDate", date.toString());

            if (clinicParam != null && serviceParam != null && !clinicParam.isBlank() && !serviceParam.isBlank()) {
                long clinicId = Long.parseLong(clinicParam);
                long serviceId = Long.parseLong(serviceParam);
                req.setAttribute("selectedClinicId", clinicId);
                req.setAttribute("selectedServiceId", serviceId);
                req.setAttribute("slots", appointmentDao.findAvailableSlots(clinicId, serviceId, date));
            }

            if (SecurityUtil.hasRole(current, "PATIENT")) {
                req.setAttribute("myAppointments", appointmentDao.listByPatient(current.getId()));
            } else if (SecurityUtil.hasAnyRole(current, "STAFF", "ADMIN")) {
                Long clinicId = SecurityUtil.hasRole(current, "STAFF") ? current.getClinicId() : parseLongOrNull(req.getParameter("dailyClinicId"));
                req.setAttribute("selectedDailyClinicId", clinicId);
                if (clinicId != null) {
                    req.setAttribute("pendingAppointments", appointmentDao.listPendingForClinicDay(clinicId, date));
                    req.setAttribute("dailyAppointments", appointmentDao.listForClinicDayExcludingPending(clinicId, date));
                }
            }
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }

        req.getRequestDispatcher("/WEB-INF/views/appointments.jsp").forward(req, resp);
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
                    case "book":
                        handleBook(req, current);
                        break;
                    case "reschedule":
                        handleReschedule(req, current);
                        break;
                    case "cancel":
                        handleCancel(req, current);
                        break;
                    case "attendance":
                        handleAttendance(req, current);
                        break;
                    case "approve":
                        handleReview(req, current, true);
                        break;
                    case "reject":
                        handleReview(req, current, false);
                        break;
                    default:
                        break;
                }
            }
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }

        resp.sendRedirect(req.getContextPath() + "/appointments");
    }

    private void handleBook(HttpServletRequest req, UserBean current) throws SQLException {
        if (!SecurityUtil.hasRole(current, "PATIENT")) {
            req.getSession().setAttribute("flashError", "Only patients can create bookings.");
            return;
        }

        long clinicId = Long.parseLong(req.getParameter("clinicId"));
        long serviceId = Long.parseLong(req.getParameter("serviceId"));
        long timeslotId = Long.parseLong(req.getParameter("timeslotId"));

        int maxBookings = policyDao.getInt("max_active_bookings", 3);
        boolean approvalRequired = appointmentDao.requiresApprovalForService(serviceId);
        String error = appointmentDao.bookAppointment(current.getId(), clinicId, serviceId, timeslotId, maxBookings);
        if (error != null) {
            req.getSession().setAttribute("flashError", error);
            return;
        }

        if (approvalRequired) {
            notificationDao.create(current.getId(), "Appointment pending approval",
                "Your booking request has been submitted for staff review.", "APPOINTMENT");
            req.getSession().setAttribute("flash", "Appointment request submitted for approval.");
        } else {
            notificationDao.create(current.getId(), "Appointment confirmed",
                "Your booking has been confirmed successfully.", "APPOINTMENT");
            req.getSession().setAttribute("flash", "Appointment booked successfully.");
        }
    }

    private void handleReschedule(HttpServletRequest req, UserBean current) throws SQLException {
        if (!SecurityUtil.hasRole(current, "PATIENT")) {
            req.getSession().setAttribute("flashError", "Only patients can reschedule.");
            return;
        }

        long appointmentId = Long.parseLong(req.getParameter("appointmentId"));
        long timeslotId = Long.parseLong(req.getParameter("timeslotId"));
        AppointmentBean appointment = appointmentDao.findById(appointmentId);
        if (appointment == null || appointment.getPatientId() != current.getId()) {
            req.getSession().setAttribute("flashError", "Appointment not found.");
            return;
        }

        int cutoffHours = policyDao.getInt("cancellation_cutoff_hours", 4);
        if (!withinCutoff(appointment, cutoffHours)) {
            req.getSession().setAttribute("flashError", "Reschedule cutoff has passed.");
            return;
        }

        boolean approvalRequired = appointmentDao.requiresApprovalForTimeslot(timeslotId);
        String error = appointmentDao.reschedule(appointmentId, current.getId(), timeslotId);
        if (error != null) {
            req.getSession().setAttribute("flashError", error);
            return;
        }

        if (approvalRequired) {
            notificationDao.create(current.getId(), "Appointment pending approval",
                "Your rescheduled booking request has been submitted for staff review.", "APPOINTMENT");
            req.getSession().setAttribute("flash", "Reschedule request submitted for approval.");
        } else {
            notificationDao.create(current.getId(), "Appointment updated",
                "Your appointment was rescheduled.", "APPOINTMENT");
            req.getSession().setAttribute("flash", "Appointment rescheduled.");
        }
    }

    private void handleCancel(HttpServletRequest req, UserBean current) throws SQLException {
        long appointmentId = Long.parseLong(req.getParameter("appointmentId"));
        String reason = req.getParameter("reason") == null ? "No reason" : req.getParameter("reason").trim();

        AppointmentBean appointment = appointmentDao.findById(appointmentId);
        if (appointment == null) {
            req.getSession().setAttribute("flashError", "Appointment not found.");
            return;
        }

        boolean byClinic = SecurityUtil.hasAnyRole(current, "STAFF", "ADMIN");
        if (!byClinic) {
            if (appointment.getPatientId() != current.getId()) {
                req.getSession().setAttribute("flashError", "You can only cancel your own appointment.");
                return;
            }
            int cutoffHours = policyDao.getInt("cancellation_cutoff_hours", 4);
            if (!withinCutoff(appointment, cutoffHours)) {
                req.getSession().setAttribute("flashError", "Cancellation cutoff has passed.");
                return;
            }
        }

        appointmentDao.cancel(appointmentId, current.getId(), reason, byClinic);
        notificationDao.create(appointment.getPatientId(),
                byClinic ? "Appointment cancelled by clinic" : "Appointment cancelled",
                byClinic ? "Clinic cancelled your appointment: " + reason : "You cancelled your appointment.",
                "APPOINTMENT");
        auditDao.log(current.getId(), "APPOINTMENT_CANCEL", "appointmentId=" + appointmentId + "; reason=" + reason);
        req.getSession().setAttribute("flash", "Appointment cancelled.");
    }

    private void handleAttendance(HttpServletRequest req, UserBean current) throws SQLException {
        if (!SecurityUtil.hasAnyRole(current, "STAFF", "ADMIN")) {
            req.getSession().setAttribute("flashError", "Permission denied.");
            return;
        }

        long appointmentId = Long.parseLong(req.getParameter("appointmentId"));
        String status = req.getParameter("status");
        String reason = req.getParameter("reason") == null ? "Updated by staff" : req.getParameter("reason");

        AppointmentBean appointment = appointmentDao.findById(appointmentId);
        if (appointment == null) {
            req.getSession().setAttribute("flashError", "Appointment not found.");
            return;
        }

        if (SecurityUtil.hasRole(current, "STAFF") && current.getClinicId() != null
                && current.getClinicId() != appointment.getClinicId()) {
            req.getSession().setAttribute("flashError", "Staff can only update own clinic appointments.");
            return;
        }

        if (!"BOOKED".equals(appointment.getStatus()) && !"APPROVED".equals(appointment.getStatus())
            && !"ARRIVED".equals(appointment.getStatus())) {
            req.getSession().setAttribute("flashError", "Only active appointments can be updated.");
            return;
        }

        appointmentDao.updateAttendance(appointmentId, status, reason);
        notificationDao.create(appointment.getPatientId(), "Appointment status updated",
                "Status is now: " + status, "APPOINTMENT");
        auditDao.log(current.getId(), "APPOINTMENT_STATUS", "appointmentId=" + appointmentId + "; status=" + status);
        req.getSession().setAttribute("flash", "Appointment status updated.");
    }

    private void handleReview(HttpServletRequest req, UserBean current, boolean approve) throws SQLException {
        if (!SecurityUtil.hasAnyRole(current, "STAFF", "ADMIN")) {
            req.getSession().setAttribute("flashError", "Permission denied.");
            return;
        }

        long appointmentId = Long.parseLong(req.getParameter("appointmentId"));
        AppointmentBean appointment = appointmentDao.findById(appointmentId);
        if (appointment == null) {
            req.getSession().setAttribute("flashError", "Appointment not found.");
            return;
        }

        if (SecurityUtil.hasRole(current, "STAFF") && current.getClinicId() != null
                && current.getClinicId() != appointment.getClinicId()) {
            req.getSession().setAttribute("flashError", "Staff can only review own clinic bookings.");
            return;
        }

        String reason = req.getParameter("reason");
        String error = approve
            ? appointmentDao.approve(appointmentId, reason)
            : appointmentDao.reject(appointmentId, reason);
        if (error != null) {
            req.getSession().setAttribute("flashError", error);
            return;
        }

        notificationDao.create(appointment.getPatientId(),
                approve ? "Appointment confirmed" : "Appointment rejected",
                approve ? "Your booking request has been approved." : "Your booking request has been rejected.",
                "APPOINTMENT");
        auditDao.log(current.getId(), approve ? "APPOINTMENT_APPROVE" : "APPOINTMENT_REJECT",
                "appointmentId=" + appointmentId + "; reason=" + (reason == null ? "" : reason));
        req.getSession().setAttribute("flash", approve ? "Appointment approved." : "Appointment rejected.");
    }

    private boolean withinCutoff(AppointmentBean appointment, int cutoffHours) {
        LocalDateTime slotDateTime = LocalDateTime.of(appointment.getSlotDate(), appointment.getStartTime());
        return LocalDateTime.now().isBefore(slotDateTime.minusHours(cutoffHours));
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }
}
