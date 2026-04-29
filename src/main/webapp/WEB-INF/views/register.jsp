<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:useBean id="registerForm" class="com.mycompany.project.model.UserBean" scope="request" />
<jsp:setProperty name="registerForm" property="*" />
<%@ include file="/WEB-INF/views/partials/header.jspf" %>

<div class="grid">
    <section class="card">
        <h2>Patient Registration</h2>
        <% if (request.getAttribute("error") != null) { %>
            <div class="flash flash-error"><%= request.getAttribute("error") %></div>
        <% } %>
        <form method="post" action="<%= request.getContextPath() %>/register">
            <label>Full Name
                <input type="text" name="fullName" value="<%= registerForm.getFullName() == null ? "" : registerForm.getFullName() %>" required>
            </label>
            <label>Email
                <input type="email" name="email" value="<%= registerForm.getEmail() == null ? "" : registerForm.getEmail() %>" required>
            </label>
            <label>Username
                <input type="text" name="username" value="<%= registerForm.getUsername() == null ? "" : registerForm.getUsername() %>" required>
            </label>
            <label>Password (min 6 chars)
                <input type="password" name="password" required>
            </label>
            <label>Confirm Password
                <input type="password" name="confirmPassword" required>
            </label>
            <button type="submit">Register</button>
        </form>
    </section>
</div>

<%@ include file="/WEB-INF/views/partials/footer.jspf" %>
