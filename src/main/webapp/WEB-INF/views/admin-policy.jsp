<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="com.mycompany.project.model.PolicyBean" %>
<%@ include file="/WEB-INF/views/partials/header.jspf" %>
<%
    List<PolicyBean> policies = (List<PolicyBean>) request.getAttribute("policies");
    String maxActive = "3";
    String cutoff = "4";
    String queueEnabled = "true";
    if (policies != null) {
        for (PolicyBean p : policies) {
            if ("max_active_bookings".equals(p.getKey())) {
                maxActive = p.getValue();
            }
            if ("cancellation_cutoff_hours".equals(p.getKey())) {
                cutoff = p.getValue();
            }
            if ("queue_enabled".equals(p.getKey())) {
                queueEnabled = p.getValue();
            }
        }
    }
%>

<div class="grid">
    <section class="card">
        <h2>Admin Policy Settings (Extra Feature)</h2>
        <form method="post" action="<%= request.getContextPath() %>/admin/policy">
            <label>Max Active Bookings Per Patient
                <input type="number" name="max_active_bookings" value="<%= maxActive %>" min="1" required>
            </label>
            <label>Cancellation/Reschedule Cutoff (hours before slot)
                <input type="number" name="cancellation_cutoff_hours" value="<%= cutoff %>" min="1" required>
            </label>
            <label>
                <input type="checkbox" name="queue_enabled" value="true" <%= "true".equalsIgnoreCase(queueEnabled) ? "checked" : "" %>>
                Enable Walk-in Queue
            </label>
            <button type="submit">Save Policy</button>
        </form>
    </section>
</div>

<%@ include file="/WEB-INF/views/partials/footer.jspf" %>
