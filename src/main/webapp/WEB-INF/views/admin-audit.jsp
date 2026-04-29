<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="com.mycompany.project.model.AuditLogBean" %>
<%@ page import="com.mycompany.project.util.AuditLogDisplayUtil" %>
<%@ include file="/WEB-INF/views/partials/header.jspf" %>
<%
    List<AuditLogBean> auditLogs = (List<AuditLogBean>) request.getAttribute("auditLogs");
    String selectedAction = (String) request.getAttribute("selectedAction");
    String selectedKeyword = (String) request.getAttribute("selectedKeyword");
%>

<div class="grid">
    <section class="card">
        <h2>Incident Log Review</h2>
        <form method="get" action="<%= request.getContextPath() %>/admin/audit">
            <label>Action
                <select name="action">
                    <option value="" <%= selectedAction == null || selectedAction.isBlank() ? "selected" : "" %>>All</option>
                    <option value="APPOINTMENT_CANCEL" <%= "APPOINTMENT_CANCEL".equals(selectedAction) ? "selected" : "" %>>APPOINTMENT_CANCEL</option>
                    <option value="APPOINTMENT_STATUS" <%= "APPOINTMENT_STATUS".equals(selectedAction) ? "selected" : "" %>>APPOINTMENT_STATUS</option>
                    <option value="QUEUE_NEXT" <%= "QUEUE_NEXT".equals(selectedAction) ? "selected" : "" %>>QUEUE_NEXT</option>
                    <option value="QUEUE_STATUS" <%= "QUEUE_STATUS".equals(selectedAction) ? "selected" : "" %>>QUEUE_STATUS</option>
                    <option value="ISSUE_REPORTED" <%= "ISSUE_REPORTED".equals(selectedAction) ? "selected" : "" %>>ISSUE_REPORTED</option>
                </select>
            </label>
            <label>Keyword
                <input type="text" name="keyword" value="<%= selectedKeyword == null ? "" : selectedKeyword %>" placeholder="NO_SHOW, CANCELLED, reason...">
            </label>
            <button type="submit">Search</button>
        </form>
        <p class="muted">Use the keyword filter to spot repeated no-shows, cancellation patterns, or staff issue reports in the details field.</p>
    </section>

    <section class="card">
        <h2>Latest Audit Entries</h2>
        <div class="table-wrap">
            <table class="audit-log-table">
                <thead><tr><th>Time</th><th>Staff</th><th>Action</th><th>Details</th></tr></thead>
                <tbody>
                <% if (auditLogs != null && !auditLogs.isEmpty()) {
                    for (AuditLogBean log : auditLogs) { %>
                    <tr>
                        <td><%= log.getCreatedAt() %></td>
                        <td><%= log.getStaffName() == null ? "System" : log.getStaffName() %></td>
                        <td><%= log.getAction() %></td>
                        <td class="audit-details"><%= AuditLogDisplayUtil.formatDetails(log.getAction(), log.getDetails()) %></td>
                    </tr>
                <%  }
                } else { %>
                    <tr><td colspan="4" class="muted">No audit logs found.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>
</div>

<%@ include file="/WEB-INF/views/partials/footer.jspf" %>