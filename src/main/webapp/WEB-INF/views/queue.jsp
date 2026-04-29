<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="com.mycompany.project.model.UserBean" %>
<%@ page import="com.mycompany.project.model.ClinicBean" %>
<%@ page import="com.mycompany.project.model.ServiceBean" %>
<%@ page import="com.mycompany.project.model.QueueTicketBean" %>
<%@ include file="/WEB-INF/views/partials/header.jspf" %>
<%
    UserBean user = (UserBean) session.getAttribute("currentUser");
    List<ClinicBean> clinics = (List<ClinicBean>) request.getAttribute("clinics");
    List<ServiceBean> services = (List<ServiceBean>) request.getAttribute("services");
    List<QueueTicketBean> myTickets = (List<QueueTicketBean>) request.getAttribute("myTickets");
    List<QueueTicketBean> queueList = (List<QueueTicketBean>) request.getAttribute("queueList");
    Boolean queueEnabled = (Boolean) request.getAttribute("queueEnabled");
    Long selectedClinicId = (Long) request.getAttribute("selectedClinicId");
    Long selectedServiceId = (Long) request.getAttribute("selectedServiceId");
    String queueDate = (String) request.getAttribute("queueDate");
%>

<div class="grid">
    <section class="card">
        <h2>Walk-in Queue</h2>
        <p>Status: <strong><%= Boolean.TRUE.equals(queueEnabled) ? "Enabled" : "Disabled" %></strong></p>

        <% if (user != null && "PATIENT".equals(user.getRole())) { %>
            <form method="post" action="<%= request.getContextPath() %>/queue">
                <input type="hidden" name="action" value="join">
                <label>Clinic
                    <select name="clinicId" required>
                        <option value="">-- Select clinic --</option>
                        <% if (clinics != null) {
                            for (ClinicBean c : clinics) { %>
                            <option value="<%= c.getId() %>"><%= c.getName() %></option>
                        <% }} %>
                    </select>
                </label>
                <label>Service
                    <select name="serviceId" required>
                        <option value="">-- Select service --</option>
                        <% if (services != null) {
                            for (ServiceBean s : services) { %>
                            <option value="<%= s.getId() %>"><%= s.getName() %></option>
                        <% }} %>
                    </select>
                </label>
                <label>Date
                    <input type="date" name="queueDate" value="<%= queueDate == null ? "" : queueDate %>" required>
                </label>
                <button type="submit">Join Queue</button>
            </form>
        <% } %>
    </section>

    <% if (user != null && ("STAFF".equals(user.getRole()) || "ADMIN".equals(user.getRole()))) { %>
    <section class="card">
        <h2>Queue Control</h2>
        <form method="get" action="<%= request.getContextPath() %>/queue">
            <% if (!"STAFF".equals(user.getRole())) { %>
                <label>Clinic
                    <select name="clinicId" required>
                        <option value="">-- Select clinic --</option>
                        <% if (clinics != null) {
                            for (ClinicBean c : clinics) { %>
                            <option value="<%= c.getId() %>" <%= selectedClinicId != null && selectedClinicId == c.getId() ? "selected" : "" %>><%= c.getName() %></option>
                        <% }} %>
                    </select>
                </label>
            <% } else { %>
                <p class="muted">Clinic is fixed to your assigned clinic.</p>
            <% } %>
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
                <input type="date" name="queueDate" value="<%= queueDate == null ? "" : queueDate %>" required>
            </label>
            <button type="submit">Load Queue</button>
        </form>

        <form method="post" action="<%= request.getContextPath() %>/queue" class="inline">
            <input type="hidden" name="action" value="next">
            <% if (selectedClinicId != null) { %><input type="hidden" name="clinicId" value="<%= selectedClinicId %>"><% } %>
            <% if (selectedServiceId != null) { %><input type="hidden" name="serviceId" value="<%= selectedServiceId %>"><% } %>
            <input type="hidden" name="queueDate" value="<%= queueDate == null ? "" : queueDate %>">
            <button type="submit" <%= selectedServiceId == null ? "disabled" : "" %>>Call Next</button>
            <% if (selectedServiceId == null) { %>
                <p class="muted">Select a service first to enable Call Next.</p>
            <% } %>
        </form>
    </section>
    <% } %>

    <section class="card">
        <h2>Queue Status</h2>
        <div class="table-wrap">
            <table>
                <thead><tr><th>No.</th><th>Patient</th><th>Clinic</th><th>Service</th><th>Status</th><th>ETA</th><th>Actions</th></tr></thead>
                <tbody>
                <% if (queueList != null && !queueList.isEmpty()) {
                    for (QueueTicketBean q : queueList) { %>
                    <tr>
                        <td><%= q.getQueueNumber() %></td>
                        <td><%= q.getPatientName() %></td>
                        <td><%= q.getClinicName() %></td>
                        <td><%= q.getServiceName() %></td>
                        <td><%= q.getStatus() %></td>
                        <td><%= q.getEstimatedWaitMinutes() %> min</td>
                        <td>
                            <% if (user != null && ("STAFF".equals(user.getRole()) || "ADMIN".equals(user.getRole()))) { %>
                                <form method="post" action="<%= request.getContextPath() %>/queue" class="inline">
                                    <input type="hidden" name="action" value="served">
                                    <input type="hidden" name="ticketId" value="<%= q.getId() %>">
                                    <input type="hidden" name="patientId" value="<%= q.getPatientId() %>">
                                    <button type="submit">Served</button>
                                </form>
                                <form method="post" action="<%= request.getContextPath() %>/queue" class="inline">
                                    <input type="hidden" name="action" value="skip">
                                    <input type="hidden" name="ticketId" value="<%= q.getId() %>">
                                    <input type="hidden" name="patientId" value="<%= q.getPatientId() %>">
                                    <button type="submit" class="btn-alt">Skip</button>
                                </form>
                                <form method="post" action="<%= request.getContextPath() %>/queue" class="inline">
                                    <input type="hidden" name="action" value="expired">
                                    <input type="hidden" name="ticketId" value="<%= q.getId() %>">
                                    <input type="hidden" name="patientId" value="<%= q.getPatientId() %>">
                                    <button type="submit" class="btn-danger">Expire</button>
                                </form>
                            <% } else { %>
                                <span class="muted">view only</span>
                            <% } %>
                        </td>
                    </tr>
                <%  }
                } else { %>
                    <tr><td colspan="7" class="muted">No queue records loaded.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>

    <% if (myTickets != null) { %>
    <section class="card">
        <h2>My Queue History</h2>
        <div class="table-wrap">
            <table>
                <thead><tr><th>Date</th><th>Clinic</th><th>Service</th><th>No.</th><th>Status</th></tr></thead>
                <tbody>
                <% if (!myTickets.isEmpty()) {
                    for (QueueTicketBean q : myTickets) { %>
                    <tr>
                        <td><%= q.getQueueDate() %></td>
                        <td><%= q.getClinicName() %></td>
                        <td><%= q.getServiceName() %></td>
                        <td><%= q.getQueueNumber() %></td>
                        <td><%= q.getStatus() %></td>
                    </tr>
                <%  }
                } else { %>
                    <tr><td colspan="5" class="muted">No queue history.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>
    <% } %>
</div>

<%@ include file="/WEB-INF/views/partials/footer.jspf" %>
