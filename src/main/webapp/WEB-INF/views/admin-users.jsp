<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="com.mycompany.project.model.UserBean" %>
<%@ page import="com.mycompany.project.model.ClinicBean" %>
<%@ include file="/WEB-INF/views/partials/header.jspf" %>
<%
    List<UserBean> users = (List<UserBean>) request.getAttribute("users");
    List<ClinicBean> clinics = (List<ClinicBean>) request.getAttribute("clinics");
    UserBean editUser = (UserBean) request.getAttribute("editUser");
%>

<div class="grid">
    <section class="card">
        <h2>Create User (Staff/Admin/Patient)</h2>
        <form method="post" action="<%= request.getContextPath() %>/admin/users" data-user-form>
            <input type="hidden" name="action" value="create">
            <label>Username <input type="text" name="username" required></label>
            <label>Full Name <input type="text" name="fullName" required></label>
            <label>Email <input type="email" name="email" required></label>
            <label>Password <input type="password" name="password" required></label>
            <label>Role
                <select name="role" required data-role-select>
                    <option value="PATIENT">PATIENT</option>
                    <option value="STAFF">STAFF</option>
                    <option value="ADMIN">ADMIN</option>
                </select>
            </label>
            <div data-clinic-field hidden>
                <label>Clinic (for staff)
                    <select name="clinicId" data-clinic-select required>
                        <option value="">-- none --</option>
                        <% if (clinics != null) {
                            for (ClinicBean c : clinics) { %>
                            <option value="<%= c.getId() %>"><%= c.getName() %></option>
                        <% }} %>
                    </select>
                </label>
            </div>
            <button type="submit">Create User</button>
        </form>
    </section>

    <section class="card">
        <h2>Edit User</h2>
        <% if (editUser == null) { %>
            <p class="muted">Select Edit from the user list to load a user here.</p>
        <% } else { %>
        <form method="post" action="<%= request.getContextPath() %>/admin/users" data-user-form>
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="userId" value="<%= editUser.getId() %>">
            <label>Username
                <input type="text" value="<%= editUser.getUsername() %>" readonly>
            </label>
            <label>Full Name
                <input type="text" name="fullName" value="<%= editUser.getFullName() %>" required>
            </label>
            <label>Email
                <input type="email" name="email" value="<%= editUser.getEmail() %>" required>
            </label>
            <label>Role
                <select name="role" required data-role-select>
                    <option value="PATIENT" <%= "PATIENT".equals(editUser.getRole()) ? "selected" : "" %>>PATIENT</option>
                    <option value="STAFF" <%= "STAFF".equals(editUser.getRole()) ? "selected" : "" %>>STAFF</option>
                    <option value="ADMIN" <%= "ADMIN".equals(editUser.getRole()) ? "selected" : "" %>>ADMIN</option>
                </select>
            </label>
            <div data-clinic-field <%= "STAFF".equals(editUser.getRole()) ? "" : "hidden" %>>
                <label>Clinic (for staff)
                    <select name="clinicId" data-clinic-select required>
                        <option value="">-- none --</option>
                        <% if (clinics != null) {
                            for (ClinicBean c : clinics) { %>
                            <option value="<%= c.getId() %>" <%= editUser.getClinicId() != null && editUser.getClinicId() == c.getId() ? "selected" : "" %>><%= c.getName() %></option>
                        <% }} %>
                    </select>
                </label>
            </div>
            <label>
                <input type="checkbox" name="active" value="true" <%= editUser.isActive() ? "checked" : "" %>>
                Active account
            </label>
            <button type="submit">Save Changes</button>
        </form>
        <% } %>
    </section>

    <section class="card">
        <h2>User List</h2>
        <div class="table-wrap">
            <table>
                <thead><tr><th>ID</th><th>Username</th><th>Name</th><th>Email</th><th>Role</th><th>Clinic ID</th><th>Active</th><th>Actions</th></tr></thead>
                <tbody>
                <% if (users != null && !users.isEmpty()) {
                    for (UserBean u : users) { %>
                    <tr>
                        <td><%= u.getId() %></td>
                        <td><%= u.getUsername() %></td>
                        <td><%= u.getFullName() %></td>
                        <td><%= u.getEmail() %></td>
                        <td><%= u.getRole() %></td>
                        <td><%= u.getClinicId() == null ? "-" : u.getClinicId() %></td>
                        <td><%= u.isActive() %></td>
                        <td>
                            <a href="<%= request.getContextPath() %>/admin/users?editUserId=<%= u.getId() %>" class="btn-alt">Edit</a>
                            <form method="post" action="<%= request.getContextPath() %>/admin/users" class="inline">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="userId" value="<%= u.getId() %>">
                                <button type="submit" class="btn-danger">Delete</button>
                            </form>
                        </td>
                    </tr>
                <% }} else { %>
                    <tr><td colspan="8" class="muted">No users.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>
</div>

<script>
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-user-form]').forEach(function (form) {
        var roleSelect = form.querySelector('[data-role-select]');
        var clinicField = form.querySelector('[data-clinic-field]');
        var clinicSelect = form.querySelector('[data-clinic-select]');

        if (!roleSelect || !clinicField || !clinicSelect) {
            return;
        }

        var syncClinicField = function () {
            var showClinic = roleSelect.value === 'STAFF';
            clinicField.hidden = !showClinic;
            clinicSelect.disabled = !showClinic;

            if (!showClinic) {
                clinicSelect.value = '';
            }
        };

        roleSelect.addEventListener('change', syncClinicField);
        syncClinicField();
    });
});
</script>

<%@ include file="/WEB-INF/views/partials/footer.jspf" %>
