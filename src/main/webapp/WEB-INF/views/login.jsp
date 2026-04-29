<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:useBean id="loginForm" class="com.mycompany.project.model.LoginFormBean" scope="request" />
<jsp:setProperty name="loginForm" property="*" />
<%@ include file="/WEB-INF/views/partials/header.jspf" %>

<div class="grid">
    <section class="card">
        <h2>Login</h2>
        <% if (request.getAttribute("error") != null) { %>
            <div class="flash flash-error"><%= request.getAttribute("error") %></div>
        <% } %>
        <% if (request.getAttribute("success") != null) { %>
            <div class="flash flash-ok"><%= request.getAttribute("success") %></div>
        <% } %>
        <form method="post" action="<%= request.getContextPath() %>/login">
            <label>Username
                <input type="text" name="username" value="<%= loginForm.getUsername() == null ? "" : loginForm.getUsername() %>" required>
            </label>
            <label>Password
                <input type="password" name="password" required>
            </label>
            <button type="submit">Sign In</button>
        </form>
        <p>No account? <a href="<%= request.getContextPath() %>/register">Create patient account</a></p>
    </section>
</div>

<%@ include file="/WEB-INF/views/partials/footer.jspf" %>
