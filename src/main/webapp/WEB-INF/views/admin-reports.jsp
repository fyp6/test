<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="com.mycompany.project.model.ClinicBean" %>
<%@ page import="com.mycompany.project.model.ServiceBean" %>
<%@ page import="com.mycompany.project.model.AppointmentBean" %>
<%@ include file="/WEB-INF/views/partials/header.jspf" %>
<%
    List<ClinicBean> clinics = (List<ClinicBean>) request.getAttribute("clinics");
    List<ServiceBean> services = (List<ServiceBean>) request.getAttribute("services");
    List<AppointmentBean> appointmentRecords = (List<AppointmentBean>) request.getAttribute("appointmentRecords");
    List<Map<String, Object>> utilizationRows = (List<Map<String, Object>>) request.getAttribute("utilizationRows");
    List<Map<String, Object>> noShowRows = (List<Map<String, Object>>) request.getAttribute("noShowRows");
    Long selectedClinicId = (Long) request.getAttribute("selectedClinicId");
    Long selectedServiceId = (Long) request.getAttribute("selectedServiceId");
    Integer selectedMonth = (Integer) request.getAttribute("selectedMonth");
    Integer selectedYear = (Integer) request.getAttribute("selectedYear");
    String selectedStatus = (String) request.getAttribute("selectedStatus");
%>

<div class="grid">
    <section class="card">
        <h2>Reporting Filters</h2>
        <form method="get" action="<%= request.getContextPath() %>/admin/reports">
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
            <label>Month <input type="number" min="1" max="12" name="month" value="<%= selectedMonth == null ? "" : selectedMonth %>"></label>
            <label>Year <input type="number" min="2020" max="2100" name="year" value="<%= selectedYear == null ? "" : selectedYear %>"></label>
            <label>Status
                <select name="status">
                    <option value="" <%= (selectedStatus == null || selectedStatus.isBlank()) ? "selected" : "" %>>All</option>
                    <option value="BOOKED" <%= "BOOKED".equals(selectedStatus) ? "selected" : "" %>>BOOKED</option>
                    <option value="PENDING" <%= "PENDING".equals(selectedStatus) ? "selected" : "" %>>PENDING</option>
                    <option value="APPROVED" <%= "APPROVED".equals(selectedStatus) ? "selected" : "" %>>APPROVED</option>
                    <option value="ARRIVED" <%= "ARRIVED".equals(selectedStatus) ? "selected" : "" %>>ARRIVED</option>
                    <option value="COMPLETED" <%= "COMPLETED".equals(selectedStatus) ? "selected" : "" %>>COMPLETED</option>
                    <option value="NO_SHOW" <%= "NO_SHOW".equals(selectedStatus) ? "selected" : "" %>>NO_SHOW</option>
                    <option value="REJECTED" <%= "REJECTED".equals(selectedStatus) ? "selected" : "" %>>REJECTED</option>
                    <option value="CANCELLED" <%= "CANCELLED".equals(selectedStatus) ? "selected" : "" %>>CANCELLED</option>
                </select>
            </label>
            <button type="submit">Run Report</button>
        </form>
    </section>

    <section class="card">
        <h2>Appointment Records</h2>
        <div class="table-wrap">
            <table>
                <thead><tr><th>ID</th><th>Patient</th><th>Clinic</th><th>Service</th><th>Date</th><th>Status</th><th>Reason</th></tr></thead>
                <tbody>
                <% if (appointmentRecords != null && !appointmentRecords.isEmpty()) {
                    for (AppointmentBean a : appointmentRecords) { %>
                    <tr>
                        <td><%= a.getId() %></td>
                        <td><%= a.getPatientName() %></td>
                        <td><%= a.getClinicName() %></td>
                        <td><%= a.getServiceName() %></td>
                        <td><%= a.getSlotDate() %> <%= a.getStartTime() %></td>
                        <td><%= a.getStatus() %></td>
                        <td><%= a.getReason() == null ? "" : a.getReason() %></td>
                    </tr>
                <%  }
                } else { %>
                    <tr><td colspan="7" class="muted">No appointment records found.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>

    <section class="card">
        <h2>Utilization Rate (Booked / Offered)</h2>
        <div class="table-wrap">
            <table>
                <thead><tr><th>Clinic</th><th>Service</th><th>Booked</th><th>Offered</th><th>Rate</th></tr></thead>
                <tbody>
                <% if (utilizationRows != null && !utilizationRows.isEmpty()) {
                    for (Map<String, Object> row : utilizationRows) { %>
                    <tr>
                        <td><%= row.get("clinic") %></td>
                        <td><%= row.get("service") %></td>
                        <td><%= row.get("booked") %></td>
                        <td><%= row.get("offered") %></td>
                        <td><%= row.get("rate") %></td>
                    </tr>
                <%  }
                } else { %>
                    <tr><td colspan="5" class="muted">No utilization data.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>

    <section class="card">
        <h2>No-show Summary</h2>
        <div class="table-wrap">
            <table>
                <thead><tr><th>Clinic</th><th>Service</th><th>No-show Count</th></tr></thead>
                <tbody>
                <% if (noShowRows != null && !noShowRows.isEmpty()) {
                    for (Map<String, Object> row : noShowRows) { %>
                    <tr>
                        <td><%= row.get("clinic") %></td>
                        <td><%= row.get("service") %></td>
                        <td><%= row.get("noShow") %></td>
                    </tr>
                <%  }
                } else { %>
                    <tr><td colspan="3" class="muted">No no-show data.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>
</div>

<%@ include file="/WEB-INF/views/partials/footer.jspf" %>
