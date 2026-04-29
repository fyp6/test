<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="com.mycompany.project.model.ClinicBean" %>
<%@ page import="com.mycompany.project.model.ServiceBean" %>
<%@ page import="com.mycompany.project.model.AuditLogBean" %>
<%@ page import="com.mycompany.project.util.AuditLogDisplayUtil" %>
<%@ include file="/WEB-INF/views/partials/header.jspf" %>
<%
    ClinicBean fixedClinic = (ClinicBean) request.getAttribute("fixedClinic");
    List<ClinicBean> clinics = (List<ClinicBean>) request.getAttribute("clinics");
    List<ServiceBean> services = (List<ServiceBean>) request.getAttribute("services");
    List<AuditLogBean> issueHistory = (List<AuditLogBean>) request.getAttribute("issueHistory");
%>

<div class="grid">
    <section class="card">
        <h2>Operational Issue Reporting</h2>
        <p class="muted">Report doctor unavailable, service suspended, equipment failure, or other operational incidents. Admins will see the entry in Incident Logs.</p>
        <form method="post" action="<%= request.getContextPath() %>/issues">
            <label>Issue Type
                <select name="issueType" required>
                    <option value="DOCTOR_UNAVAILABLE">Doctor Unavailable</option>
                    <option value="SERVICE_SUSPENDED">Service Suspended</option>
                    <option value="EQUIPMENT_FAILURE">Equipment Failure</option>
                    <option value="SYSTEM_OUTAGE">System Outage</option>
                    <option value="OTHER">Other</option>
                </select>
            </label>
            <label>Severity
                <select name="severity" required>
                    <option value="LOW">Low</option>
                    <option value="MEDIUM" selected>Medium</option>
                    <option value="HIGH">High</option>
                    <option value="URGENT">Urgent</option>
                </select>
            </label>
            <% if (fixedClinic != null) { %>
                <label>Clinic
                    <input type="text" value="<%= fixedClinic.getName() %>" readonly>
                </label>
                <input type="hidden" name="clinicId" value="<%= fixedClinic.getId() %>">
            <% } else { %>
                <label>Clinic
                    <select name="clinicId">
                        <option value="">-- Optional --</option>
                        <% if (clinics != null) {
                            for (ClinicBean clinic : clinics) { %>
                            <option value="<%= clinic.getId() %>"><%= clinic.getName() %></option>
                        <%  }
                        } %>
                    </select>
                </label>
            <% } %>
            <label>Service
                <select name="serviceId">
                    <option value="">-- Optional --</option>
                    <% if (services != null) {
                        for (ServiceBean service : services) { %>
                        <option value="<%= service.getId() %>"><%= service.getName() %></option>
                    <%  }
                    } %>
                </select>
            </label>
            <label>Title
                <input type="text" name="title" maxlength="120" placeholder="Doctor on sick leave" required>
            </label>
            <label>Details
                <textarea name="details" maxlength="255" rows="5" placeholder="Describe the operational impact and any immediate action taken" required></textarea>
            </label>
            <button type="submit">Submit Report</button>
        </form>
    </section>

    <section class="card">
        <h2>My Recent Reports</h2>
        <div class="table-wrap">
            <table>
                <thead><tr><th>Time</th><th>Action</th><th>Details</th></tr></thead>
                <tbody>
                <% if (issueHistory != null && !issueHistory.isEmpty()) {
                    for (AuditLogBean log : issueHistory) { %>
                        <tr>
                            <td><%= log.getCreatedAt() %></td>
                            <td><%= log.getAction() %></td>
                            <td class="audit-details"><%= AuditLogDisplayUtil.formatDetails(log.getAction(), log.getDetails()) %></td>
                        </tr>
                <%  }
                } else { %>
                    <tr><td colspan="3" class="muted">No issue reports yet.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>
</div>

<%@ include file="/WEB-INF/views/partials/footer.jspf" %>