<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="com.mycompany.project.model.UserBean" %>
<%@ page import="com.mycompany.project.model.AppointmentBean" %>
<%@ page import="com.mycompany.project.model.QueueTicketBean" %>
<%@ page import="com.mycompany.project.model.NotificationBean" %>
<%@ include file="/WEB-INF/views/partials/header.jspf" %>
<%
    UserBean user = (UserBean) session.getAttribute("currentUser");
    List<NotificationBean> notifications = (List<NotificationBean>) request.getAttribute("notifications");
    List<AppointmentBean> appointments = (List<AppointmentBean>) request.getAttribute("appointments");
    List<QueueTicketBean> queueTickets = (List<QueueTicketBean>) request.getAttribute("queueTickets");
    List<AppointmentBean> clinicAppointments = (List<AppointmentBean>) request.getAttribute("clinicAppointments");
%>

<div class="grid">
    <section class="card">
        <h2>Notifications</h2>
        <form method="post" action="<%= request.getContextPath() %>/notifications" class="inline">
            <input type="hidden" name="back" value="/dashboard">
            <button type="submit">Mark All Read</button>
        </form>
        <div class="table-wrap">
            <table>
                <thead>
                <tr><th>Type</th><th>Title</th><th>Message</th><th>Time</th></tr>
                </thead>
                <tbody>
                <% if (notifications != null && !notifications.isEmpty()) {
                    for (NotificationBean n : notifications) { %>
                        <tr>
                            <td><%= n.getType() %></td>
                            <td><%= n.getTitle() %></td>
                            <td><%= n.getBody() %></td>
                            <td><%= n.getCreatedAt() %></td>
                        </tr>
                <%  }
                } else { %>
                    <tr><td colspan="4" class="muted">No notifications.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>

    <% if (user != null && "PATIENT".equals(user.getRole())) { %>
    <section class="card">
        <h2>My Appointments</h2>
        <div class="table-wrap">
            <table>
                <thead><tr><th>Clinic</th><th>Service</th><th>Date</th><th>Time</th><th>Status</th></tr></thead>
                <tbody>
                <% if (appointments != null && !appointments.isEmpty()) {
                    for (AppointmentBean a : appointments) { %>
                        <tr>
                            <td><%= a.getClinicName() %></td>
                            <td><%= a.getServiceName() %></td>
                            <td><%= a.getSlotDate() %></td>
                            <td><%= a.getStartTime() %> - <%= a.getEndTime() %></td>
                            <td><%= a.getStatus() %></td>
                        </tr>
                <%  }
                } else { %>
                    <tr><td colspan="5" class="muted">No appointments yet.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>

    <section class="card">
        <h2>My Queue Tickets</h2>
        <div class="table-wrap">
            <table>
                <thead><tr><th>Clinic</th><th>Service</th><th>Date</th><th>No.</th><th>Status</th><th>ETA</th></tr></thead>
                <tbody>
                <% if (queueTickets != null && !queueTickets.isEmpty()) {
                    for (QueueTicketBean q : queueTickets) { %>
                        <tr>
                            <td><%= q.getClinicName() %></td>
                            <td><%= q.getServiceName() %></td>
                            <td><%= q.getQueueDate() %></td>
                            <td><%= q.getQueueNumber() %></td>
                            <td><%= q.getStatus() %></td>
                            <td><%= q.getEstimatedWaitMinutes() %> min</td>
                        </tr>
                <%  }
                } else { %>
                    <tr><td colspan="6" class="muted">No queue tickets yet.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>
    <% } %>

    <% if (user != null && "STAFF".equals(user.getRole())) { %>
    <section class="card">
        <h2>Today's Clinic Appointments</h2>
        <div class="table-wrap">
            <table>
                <thead><tr><th>Patient</th><th>Service</th><th>Date</th><th>Time</th><th>Status</th></tr></thead>
                <tbody>
                <% if (clinicAppointments != null && !clinicAppointments.isEmpty()) {
                    for (AppointmentBean a : clinicAppointments) { %>
                        <tr>
                            <td><%= a.getPatientName() %></td>
                            <td><%= a.getServiceName() %></td>
                            <td><%= a.getSlotDate() %></td>
                            <td><%= a.getStartTime() %></td>
                            <td><%= a.getStatus() %></td>
                        </tr>
                <%  }
                } else { %>
                    <tr><td colspan="5" class="muted">No appointments for today.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
        <p><a href="<%= request.getContextPath() %>/issues">Report an operational issue</a></p>
    </section>
    <% } %>

    <% if (user != null && "ADMIN".equals(user.getRole())) { %>
    <section class="card">
        <h2>Admin Quick Actions</h2>
        <p><a href="<%= request.getContextPath() %>/admin/users">Manage users and roles</a></p>
        <p><a href="<%= request.getContextPath() %>/admin/reports">View reporting dashboard</a></p>
        <p><a href="<%= request.getContextPath() %>/admin/policy">Configure policy settings</a></p>
    </section>
    <% } %>
</div>

<%@ include file="/WEB-INF/views/partials/footer.jspf" %>
