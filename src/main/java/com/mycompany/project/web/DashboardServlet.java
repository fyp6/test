package com.mycompany.project.web;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.mycompany.project.dao.AppointmentDao;
import com.mycompany.project.dao.NotificationDao;
import com.mycompany.project.dao.QueueDao;
import com.mycompany.project.model.AppointmentBean;
import com.mycompany.project.model.UserBean;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    private final NotificationDao notificationDao = new NotificationDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final QueueDao queueDao = new QueueDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserBean user = SecurityUtil.currentUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        try {
            if (SecurityUtil.hasRole(user, "PATIENT")) {
                List<AppointmentBean> appointments = appointmentDao.listByPatient(user.getId());
                createUpcomingReminders(user, appointments);
                req.setAttribute("appointments", appointments);
                req.setAttribute("queueTickets", queueDao.listByPatient(user.getId()));
            } else if (SecurityUtil.hasRole(user, "STAFF") && user.getClinicId() != null) {
                req.setAttribute("clinicAppointments", appointmentDao.listForClinicDay(user.getClinicId(), LocalDate.now()));
            }
            req.setAttribute("notifications", notificationDao.listByUser(user.getId(), 10));
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }

        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req, resp);
    }

    private void createUpcomingReminders(UserBean user, List<AppointmentBean> appointments) throws SQLException {
        if (appointments == null || appointments.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindowEnd = now.plusHours(24);

        for (AppointmentBean appointment : appointments) {
            if (!"BOOKED".equals(appointment.getStatus())
                    && !"APPROVED".equals(appointment.getStatus())
                    && !"ARRIVED".equals(appointment.getStatus())) {
                continue;
            }

            if (appointment.getSlotDate() == null || appointment.getStartTime() == null) {
                continue;
            }

            LocalDateTime slotDateTime = LocalDateTime.of(appointment.getSlotDate(), appointment.getStartTime());
            if (slotDateTime.isBefore(now) || slotDateTime.isAfter(reminderWindowEnd)) {
                continue;
            }

            String title = "Upcoming appointment reminder";
            String body = "Appointment #" + appointment.getId()
                    + " at " + appointment.getClinicName()
                    + " / " + appointment.getServiceName()
                    + " on " + appointment.getSlotDate()
                    + " " + appointment.getStartTime() + ".";

            if (!notificationDao.existsByUserTypeAndBody(user.getId(), "REMINDER", body)) {
                notificationDao.create(user.getId(), title, body, "REMINDER");
            }
        }
    }
}
