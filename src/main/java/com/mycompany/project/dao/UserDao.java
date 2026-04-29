package com.mycompany.project.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.project.model.UserBean;
import com.mycompany.project.util.DBUtil;
import com.mycompany.project.util.PasswordUtil;

public class UserDao {
    public UserBean authenticate(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ? AND active = TRUE";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.sha256(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public boolean registerPatient(UserBean user) throws SQLException {
        if (usernameExists(user.getUsername())) {
            return false;
        }
        String sql = "INSERT INTO users(username, password_hash, full_name, email, role, clinic_id, active) "
                + "VALUES (?, ?, ?, ?, 'PATIENT', NULL, TRUE)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, PasswordUtil.sha256(user.getPassword()));
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<UserBean> listUsers() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY id";
        List<UserBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public List<UserBean> listByRole(String role) throws SQLException {
        String sql = "SELECT * FROM users WHERE role = ? AND active = TRUE ORDER BY id";
        List<UserBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public UserBean findById(long id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public void createUser(UserBean user, String rawPassword) throws SQLException {
        String sql = "INSERT INTO users(username, password_hash, full_name, email, role, clinic_id, active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, PasswordUtil.sha256(rawPassword));
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getRole());
            if (user.getClinicId() == null) {
                ps.setNull(6, java.sql.Types.BIGINT);
            } else {
                ps.setLong(6, user.getClinicId());
            }
            ps.setBoolean(7, user.isActive());
            ps.executeUpdate();
        }
    }

    public void updateUser(UserBean user) throws SQLException {
        String sql = "UPDATE users SET full_name=?, email=?, role=?, clinic_id=?, active=? WHERE id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getRole());
            if (user.getClinicId() == null) {
                ps.setNull(4, java.sql.Types.BIGINT);
            } else {
                ps.setLong(4, user.getClinicId());
            }
            ps.setBoolean(5, user.isActive());
            ps.setLong(6, user.getId());
            ps.executeUpdate();
        }
    }

    public void deleteUser(long id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void updateProfile(long userId, String fullName, String email, String newPassword) throws SQLException {
        boolean changePassword = newPassword != null && !newPassword.isBlank();
        String sql = changePassword
                ? "UPDATE users SET full_name=?, email=?, password_hash=? WHERE id=?"
                : "UPDATE users SET full_name=?, email=? WHERE id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            if (changePassword) {
                ps.setString(3, PasswordUtil.sha256(newPassword));
                ps.setLong(4, userId);
            } else {
                ps.setLong(3, userId);
            }
            ps.executeUpdate();
        }
    }

    private UserBean map(ResultSet rs) throws SQLException {
        UserBean bean = new UserBean();
        bean.setId(rs.getLong("id"));
        bean.setUsername(rs.getString("username"));
        bean.setFullName(rs.getString("full_name"));
        bean.setEmail(rs.getString("email"));
        bean.setRole(rs.getString("role"));
        long clinicId = rs.getLong("clinic_id");
        bean.setClinicId(rs.wasNull() ? null : clinicId);
        bean.setActive(rs.getBoolean("active"));
        return bean;
    }
}
