<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.util.List" %>
<%@ page import="com.mycompany.project.model.UserBean" %>
<%@ page import="com.mycompany.project.model.ClinicBean" %>
<%@ page import="com.mycompany.project.model.ServiceBean" %>
<%@ page import="com.mycompany.project.model.TimeslotBean" %>
<%@ page import="com.mycompany.project.model.AppointmentBean" %>
<%@ include file="/WEB-INF/views/partials/header.jspf" %>
<%
    UserBean user = (UserBean) session.getAttribute("currentUser");
    List<ClinicBean> clinics = (List<ClinicBean>) request.getAttribute("clinics");
    List<ServiceBean> services = (List<ServiceBean>) request.getAttribute("services");
    List<TimeslotBean> slots = (List<TimeslotBean>) request.getAttribute("slots");
    List<AppointmentBean> myAppointments = (List<AppointmentBean>) request.getAttribute("myAppointments");
    List<AppointmentBean> pendingAppointments = (List<AppointmentBean>) request.getAttribute("pendingAppointments");
    List<AppointmentBean> dailyAppointments = (List<AppointmentBean>) request.getAttribute("dailyAppointments");
    String slotDate = (String) request.getAttribute("slotDate");
    Long selectedClinicId = (Long) request.getAttribute("selectedClinicId");
    Long selectedServiceId = (Long) request.getAttribute("selectedServiceId");
    Long selectedDailyClinicId = (Long) request.getAttribute("selectedDailyClinicId");
%>

<div class="grid">
    <section class="card">
        <h2>Search Available Slots</h2>
        <form method="get" action="<%= request.getContextPath() %>/appointments">
            <label>Clinic
                <select name="clinicId" required>
                    <option value="">-- Select clinic --</option>
                    <% if (clinics != null) {
                        for (ClinicBean c : clinics) { %>
                        <option value="<%= c.getId() %>" <%= selectedClinicId != null && selectedClinicId == c.getId() ? "selected" : "" %>><%= c.getName() %></option>
                    <% }} %>
                </select>
            </label>
            <label>Service
                <select name="serviceId" required>
                    <option value="">-- Select service --</option>
                    <% if (services != null) {
                        for (ServiceBean s : services) { %>
                        <option value="<%= s.getId() %>" <%= selectedServiceId != null && selectedServiceId == s.getId() ? "selected" : "" %>><%= s.getName() %></option>
                    <% }} %>
                </select>
            </label>
            <label>Date
                <input type="date" name="slotDate" value="<%= slotDate == null ? "" : slotDate %>" min="<%= LocalDate.now() %>" required>
            </label>
            <button type="submit">Find Slots</button>
        </form>

        <div class="table-wrap">
            <table>
                <thead><tr><th>ID</th><th>Time</th><th>Capacity</th><th>Available</th><th>Action</th></tr></thead>
                <tbody>
                <% if (slots != null && !slots.isEmpty()) {
                    for (TimeslotBean s : slots) { %>
                        <tr>
                            <td><%= s.getId() %></td>
                            <td><%= s.getStartTime() %> - <%= s.getEndTime() %></td>
                            <td><%= s.getCapacity() %></td>
                            <td><%= s.getAvailableCount() %></td>
                            <td>
                                <% if (user != null && "PATIENT".equals(user.getRole()) && s.getAvailableCount() > 0) { %>
                                <form method="post" action="<%= request.getContextPath() %>/appointments" class="inline">
                                    <input type="hidden" name="action" value="book">
                                    <input type="hidden" name="clinicId" value="<%= s.getClinicId() %>">
                                    <input type="hidden" name="serviceId" value="<%= s.getServiceId() %>">
                                    <input type="hidden" name="timeslotId" value="<%= s.getId() %>">
                                    <button type="submit">Book</button>
                                </form>
                                <% } else { %>
                                <span class="muted">N/A</span>
                                <% } %>
                            </td>
                        </tr>
                <%  }
                } else { %>
                    <tr><td colspan="5" class="muted">No slots loaded. Select clinic/service/date to search.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>

    <% if (user != null && "PATIENT".equals(user.getRole())) { %>
    <section class="card">
        <h2>My Booking Records</h2>
        <p class="muted">Reschedule by entering a new timeslot ID from the available slots table.</p>
        <div class="table-wrap">
            <table>
                <thead><tr><th>ID</th><th>Clinic</th><th>Service</th><th>Date</th><th>Status</th><th>Actions</th></tr></thead>
                <tbody>
                <% if (myAppointments != null && !myAppointments.isEmpty()) {
                    for (AppointmentBean a : myAppointments) { %>
                        <tr>
                            <td><%= a.getId() %></td>
                            <td><%= a.getClinicName() %></td>
                            <td><%= a.getServiceName() %></td>
                            <td><%= a.getSlotDate() %> <%= a.getStartTime() %></td>
                            <td><%= a.getStatus() %></td>
                            <td>
                                <form method="post" action="<%= request.getContextPath() %>/appointments" class="inline">
                                    <input type="hidden" name="action" value="reschedule">
                                    <input type="hidden" name="appointmentId" value="<%= a.getId() %>">
                                    <input type="number" name="timeslotId" class="compact-field" placeholder="new slot id">
                                    <button type="submit">Reschedule</button>
                                </form>
                                <form method="post" action="<%= request.getContextPath() %>/appointments" class="inline">
                                    <input type="hidden" name="action" value="cancel">
                                    <input type="hidden" name="appointmentId" value="<%= a.getId() %>">
                                    <input type="text" name="reason" class="compact-field" placeholder="reason">
                                    <button type="submit" class="btn-danger">Cancel</button>
                                </form>
                            </td>
                        </tr>
                <%  }
                } else { %>
                    <tr><td colspan="6" class="muted">No appointment records.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>
    <% } %>

    <% if (user != null && ("STAFF".equals(user.getRole()) || "ADMIN".equals(user.getRole()))) { %>
    <section class="card">
        <h2>Pending Booking Requests</h2>
        <% if ("ADMIN".equals(user.getRole())) { %>
        <form method="get" action="<%= request.getContextPath() %>/appointments">
            <input type="hidden" name="slotDate" value="<%= slotDate == null ? "" : slotDate %>">
            <label>Clinic
                <select name="dailyClinicId" required>
                    <option value="">-- Select clinic --</option>
                    <% if (clinics != null) {
                        for (ClinicBean c : clinics) { %>
                        <option value="<%= c.getId() %>" <%= selectedDailyClinicId != null && selectedDailyClinicId == c.getId() ? "selected" : "" %>><%= c.getName() %></option>
                    <% }} %>
                </select>
            </label>
            <button type="submit">Load Requests</button>
        </form>
        <% } else { %>
            <p class="muted">Clinic is fixed to your assigned clinic.</p>
        <% } %>
        <div class="table-wrap">
            <table>
                <thead><tr><th>Patient</th><th>Service</th><th>Date</th><th>Time</th><th>Reason</th><th>Actions</th></tr></thead>
                <tbody>
                <% if (pendingAppointments != null && !pendingAppointments.isEmpty()) {
                    for (AppointmentBean a : pendingAppointments) { %>
                    <tr>
                        <td><%= a.getPatientName() %></td>
                        <td><%= a.getServiceName() %></td>
                        <td><%= a.getSlotDate() %></td>
                        <td><%= a.getStartTime() %> - <%= a.getEndTime() %></td>
                        <td><%= a.getReason() == null ? "" : a.getReason() %></td>
                        <td>
                            <form method="post" action="<%= request.getContextPath() %>/appointments" class="inline">
                                <input type="hidden" name="action" value="approve">
                                <input type="hidden" name="appointmentId" value="<%= a.getId() %>">
                                <input type="text" name="reason" class="compact-field" placeholder="optional reason">
                                <button type="submit">Approve</button>
                            </form>
                            <form method="post" action="<%= request.getContextPath() %>/appointments" class="inline">
                                <input type="hidden" name="action" value="reject">
                                <input type="hidden" name="appointmentId" value="<%= a.getId() %>">
                                <input type="text" name="reason" class="compact-field" placeholder="rejection reason">
                                <button type="submit" class="btn-danger">Reject</button>
                            </form>
                        </td>
                    </tr>
                <%  }
                } else { %>
                    <tr><td colspan="6" class="muted">No pending booking requests.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>

    <section class="card">
        <h2>Booking Review and Attendance Management</h2>
        <div class="table-wrap">
            <table>
                <thead><tr><th>Appointment</th><th>Patient</th><th>Service</th><th>Date</th><th>Current</th><th>Review / Update</th></tr></thead>
                <tbody>
                <% if (dailyAppointments != null && !dailyAppointments.isEmpty()) {
                    for (AppointmentBean a : dailyAppointments) { %>
                        <tr>
                            <td>#<%= a.getId() %></td>
                            <td><%= a.getPatientName() %></td>
                            <td><%= a.getServiceName() %></td>
                            <td><%= a.getSlotDate() %> <%= a.getStartTime() %></td>
                            <td><%= a.getStatus() %></td>
                            <td>
                                <% if ("BOOKED".equals(a.getStatus()) || "APPROVED".equals(a.getStatus()) || "ARRIVED".equals(a.getStatus())) { %>
                                <form method="post" action="<%= request.getContextPath() %>/appointments" class="inline">
                                    <input type="hidden" name="action" value="attendance">
                                    <input type="hidden" name="appointmentId" value="<%= a.getId() %>">
                                    <select name="status" class="compact-select">
                                        <option value="ARRIVED">Arrived</option>
                                        <option value="COMPLETED">Completed</option>
                                        <option value="NO_SHOW">No-show</option>
                                    </select>
                                    <input type="text" name="reason" class="compact-field" placeholder="reason">
                                    <button type="submit">Save</button>
                                </form>
                                <% } else { %>
                                <span class="muted">view only</span>
                                <% } %>
                            </td>
                        </tr>
                <%  }
                } else { %>
                    <tr><td colspan="6" class="muted">No appointments loaded for this clinic/day.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>
    <% } %>
</div>

<%@ include file="/WEB-INF/views/partials/footer.jspf" %>
