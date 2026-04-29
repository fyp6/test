<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="com.mycompany.project.model.ClinicBean" %>
<%@ page import="com.mycompany.project.model.ServiceBean" %>
<%@ page import="com.mycompany.project.model.TimeslotBean" %>
<%@ include file="/WEB-INF/views/partials/header.jspf" %>
<%
    List<ClinicBean> clinics = (List<ClinicBean>) request.getAttribute("clinics");
    List<ServiceBean> services = (List<ServiceBean>) request.getAttribute("services");
    List<TimeslotBean> timeslots = (List<TimeslotBean>) request.getAttribute("timeslots");
    Long selectedClinicId = (Long) request.getAttribute("selectedClinicId");
    Long selectedServiceId = (Long) request.getAttribute("selectedServiceId");
    String selectedSlotDate = (String) request.getAttribute("selectedSlotDate");
%>

<div class="grid">
    <section class="card">
        <h2>Opening Hours Filter</h2>
        <form method="get" action="<%= request.getContextPath() %>/admin/catalog">
            <label>Clinic
                <select name="clinicId">
                    <option value="">All</option>
                    <% if (clinics != null) {
                        for (ClinicBean c : clinics) { %>
                        <option value="<%= c.getId() %>" <%= selectedClinicId != null && selectedClinicId == c.getId() ? "selected" : "" %>><%= c.getName() %></option>
                    <% }} %>
                </select>
            </label>
            <label>Service
                <select name="serviceId">
                    <option value="">All</option>
                    <% if (services != null) {
                        for (ServiceBean s : services) { %>
                        <option value="<%= s.getId() %>" <%= selectedServiceId != null && selectedServiceId == s.getId() ? "selected" : "" %>><%= s.getName() %></option>
                    <% }} %>
                </select>
            </label>
            <label>Date
                <input type="date" name="slotDate" value="<%= selectedSlotDate == null ? "" : selectedSlotDate %>">
            </label>
            <button type="submit">Load Opening Hours</button>
        </form>
    </section>

    <section class="card">
        <h2>Clinic Management</h2>
        <form method="post" action="<%= request.getContextPath() %>/admin/catalog">
            <input type="hidden" name="action" value="clinic-create">
            <label>Name <input type="text" name="name" required></label>
            <label>Location <input type="text" name="location" required></label>
            <label><input type="checkbox" name="walkInEnabled" value="true" checked> Walk-in Enabled</label>
            <button type="submit">Create Clinic</button>
        </form>
        <div class="table-wrap">
            <table>
                <thead><tr><th>ID</th><th>Name</th><th>Location</th><th>Walk-in</th><th>Actions</th></tr></thead>
                <tbody>
                <% if (clinics != null && !clinics.isEmpty()) {
                    for (ClinicBean c : clinics) { %>
                    <tr>
                        <td><%= c.getId() %></td>
                        <td><%= c.getName() %></td>
                        <td><%= c.getLocation() %></td>
                        <td><%= c.isWalkInEnabled() %></td>
                        <td>
                            <form method="post" action="<%= request.getContextPath() %>/admin/catalog" class="inline">
                                <input type="hidden" name="action" value="clinic-update">
                                <input type="hidden" name="clinicId" value="<%= c.getId() %>">
                                <input type="text" name="name" value="<%= c.getName() %>" class="compact-field">
                                <input type="text" name="location" value="<%= c.getLocation() %>" class="compact-field">
                                <label class="inline-flag">
                                    <input type="checkbox" name="walkInEnabled" value="true" <%= c.isWalkInEnabled() ? "checked" : "" %>> Walk-in
                                </label>
                                <button type="submit">Save</button>
                            </form>
                            <form method="post" action="<%= request.getContextPath() %>/admin/catalog" class="inline">
                                <input type="hidden" name="action" value="clinic-delete">
                                <input type="hidden" name="clinicId" value="<%= c.getId() %>">
                                <button type="submit" class="btn-danger">Delete</button>
                            </form>
                        </td>
                    </tr>
                <%  }
                } else { %>
                    <tr><td colspan="5" class="muted">No clinics found.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>

    <section class="card">
        <h2>Service Management</h2>
        <form method="post" action="<%= request.getContextPath() %>/admin/catalog">
            <input type="hidden" name="action" value="service-create">
            <label>Name <input type="text" name="name" required></label>
            <label>Description <input type="text" name="description"></label>
            <label><input type="checkbox" name="limitedQuota" value="true"> Limited Quota Requires Approval</label>
            <button type="submit">Create Service</button>
        </form>
        <div class="table-wrap">
            <table>
                <thead><tr><th>ID</th><th>Name</th><th>Description</th><th>Approval</th><th>Actions</th></tr></thead>
                <tbody>
                <% if (services != null && !services.isEmpty()) {
                    for (ServiceBean s : services) { %>
                    <tr>
                        <td><%= s.getId() %></td>
                        <td><%= s.getName() %></td>
                        <td><%= s.getDescription() == null ? "" : s.getDescription() %></td>
                        <td><%= s.isLimitedQuota() %></td>
                        <td>
                            <form method="post" action="<%= request.getContextPath() %>/admin/catalog" class="inline">
                                <input type="hidden" name="action" value="service-update">
                                <input type="hidden" name="serviceId" value="<%= s.getId() %>">
                                <input type="text" name="name" value="<%= s.getName() %>" class="compact-field">
                                <input type="text" name="description" value="<%= s.getDescription() == null ? "" : s.getDescription() %>" class="compact-field compact-field--wide">
                                <label class="inline-flag">
                                    <input type="checkbox" name="limitedQuota" value="true" <%= s.isLimitedQuota() ? "checked" : "" %>> Approval
                                </label>
                                <button type="submit">Save</button>
                            </form>
                            <form method="post" action="<%= request.getContextPath() %>/admin/catalog" class="inline">
                                <input type="hidden" name="action" value="service-delete">
                                <input type="hidden" name="serviceId" value="<%= s.getId() %>">
                                <button type="submit" class="btn-danger">Delete</button>
                            </form>
                        </td>
                    </tr>
                <%  }
                } else { %>
                    <tr><td colspan="5" class="muted">No services found.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>

    <section class="card">
        <h2>Opening Hours Management</h2>
        <form method="post" action="<%= request.getContextPath() %>/admin/catalog">
            <input type="hidden" name="action" value="slot-create">
            <input type="hidden" name="redirectClinicId" value="<%= selectedClinicId == null ? "" : selectedClinicId %>">
            <input type="hidden" name="redirectServiceId" value="<%= selectedServiceId == null ? "" : selectedServiceId %>">
            <input type="hidden" name="redirectSlotDate" value="<%= selectedSlotDate == null ? "" : selectedSlotDate %>">
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
            <label>Date <input type="date" name="slotDate" value="<%= selectedSlotDate == null ? "" : selectedSlotDate %>" required></label>
            <label>Start <input type="time" name="startTime" required></label>
            <label>End <input type="time" name="endTime" required></label>
            <label>Capacity <input type="number" name="capacity" min="1" required></label>
            <button type="submit">Create Timeslot</button>
        </form>

        <div class="table-wrap">
            <table>
                <thead><tr><th>ID</th><th>Clinic</th><th>Service</th><th>Date</th><th>Time</th><th>Capacity</th><th>Booked</th><th>Available</th><th>Actions</th></tr></thead>
                <tbody>
                <% if (timeslots != null && !timeslots.isEmpty()) {
                    for (TimeslotBean slot : timeslots) { %>
                    <tr>
                        <td><%= slot.getId() %></td>
                        <td><%= slot.getClinicName() %></td>
                        <td><%= slot.getServiceName() %></td>
                        <td><%= slot.getSlotDate() %></td>
                        <td><%= slot.getStartTime() %> - <%= slot.getEndTime() %></td>
                        <td><%= slot.getCapacity() %></td>
                        <td><%= slot.getBookedCount() %></td>
                        <td><%= slot.getAvailableCount() %></td>
                        <td>
                            <form method="post" action="<%= request.getContextPath() %>/admin/catalog" class="inline">
                                <input type="hidden" name="action" value="slot-delete">
                                <input type="hidden" name="timeslotId" value="<%= slot.getId() %>">
                                <input type="hidden" name="redirectClinicId" value="<%= selectedClinicId == null ? "" : selectedClinicId %>">
                                <input type="hidden" name="redirectServiceId" value="<%= selectedServiceId == null ? "" : selectedServiceId %>">
                                <input type="hidden" name="redirectSlotDate" value="<%= selectedSlotDate == null ? "" : selectedSlotDate %>">
                                <button type="submit" class="btn-danger">Delete</button>
                            </form>
                        </td>
                    </tr>
                <%  }
                } else { %>
                    <tr><td colspan="9" class="muted">No timeslots found for the current filter.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>
</div>

<%@ include file="/WEB-INF/views/partials/footer.jspf" %>
