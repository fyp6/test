<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.mycompany.project.model.UserBean" %>
<%@ include file="/WEB-INF/views/partials/header.jspf" %>
<%
    UserBean user = (UserBean) session.getAttribute("currentUser");
%>

<div class="grid">
    <section class="card">
        <h2>Profile Management</h2>
        <% if (request.getAttribute("success") != null) { %>
            <div class="flash flash-ok"><%= request.getAttribute("success") %></div>
        <% } %>
        <form method="post" action="<%= request.getContextPath() %>/profile">
            <label>Full Name
                <input type="text" name="fullName" value="<%= user == null ? "" : user.getFullName() %>" required>
            </label>
            <label>Email
                <input type="email" name="email" value="<%= user == null ? "" : user.getEmail() %>" required>
            </label>
            <label>New Password (optional)
                <input type="password" name="newPassword">
            </label>
            <button type="submit">Update Profile</button>
        </form>
    </section>
</div>

<%@ include file="/WEB-INF/views/partials/footer.jspf" %>
