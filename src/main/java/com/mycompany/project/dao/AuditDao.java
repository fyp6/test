package com.mycompany.project.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.project.model.AuditLogBean;
import com.mycompany.project.util.DBUtil;

public class AuditDao {
    public void log(Long staffId, String action, String details) {
        String sql = "INSERT INTO audit_logs(staff_id, action, details) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (staffId == null) {
                ps.setNull(1, java.sql.Types.BIGINT);
            } else {
                ps.setLong(1, staffId);
            }
            ps.setString(2, action);
            ps.setString(3, details);
            ps.executeUpdate();
        } catch (SQLException ex) {
            // Logging must not break user flow.
        }
    }

    public List<AuditLogBean> listLogs(String action, String keyword, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT a.id, a.staff_id, COALESCE(u.full_name, 'System') AS staff_name, ")
                .append("a.action, a.details, a.created_at ")
                .append("FROM audit_logs a ")
                .append("LEFT JOIN users u ON u.id = a.staff_id ")
                .append("WHERE 1=1");

        List<Object> params = new ArrayList<>();
        if (action != null && !action.isBlank()) {
            sql.append(" AND a.action = ?");
            params.add(action.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (a.action LIKE ? OR a.details LIKE ? OR COALESCE(u.full_name, 'System') LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        sql.append(" ORDER BY a.created_at DESC LIMIT ?");
        params.add(limit);

        List<AuditLogBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLogBean bean = new AuditLogBean();
                    bean.setId(rs.getLong("id"));
                    long staffId = rs.getLong("staff_id");
                    bean.setStaffId(rs.wasNull() ? null : staffId);
                    bean.setStaffName(rs.getString("staff_name"));
                    bean.setAction(rs.getString("action"));
                    bean.setDetails(rs.getString("details"));
                    bean.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(bean);
                }
            }
        }
        return list;
    }

    public List<AuditLogBean> listLogsByStaff(long staffId, String action, String keyword, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT a.id, a.staff_id, COALESCE(u.full_name, 'System') AS staff_name, ")
                .append("a.action, a.details, a.created_at ")
                .append("FROM audit_logs a ")
                .append("LEFT JOIN users u ON u.id = a.staff_id ")
                .append("WHERE a.staff_id = ?");

        List<Object> params = new ArrayList<>();
        params.add(staffId);

        if (action != null && !action.isBlank()) {
            sql.append(" AND a.action = ?");
            params.add(action.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (a.action LIKE ? OR a.details LIKE ? OR COALESCE(u.full_name, 'System') LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        sql.append(" ORDER BY a.created_at DESC LIMIT ?");
        params.add(limit);

        List<AuditLogBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLogBean bean = new AuditLogBean();
                    bean.setId(rs.getLong("id"));
                    long rowStaffId = rs.getLong("staff_id");
                    bean.setStaffId(rs.wasNull() ? null : rowStaffId);
                    bean.setStaffName(rs.getString("staff_name"));
                    bean.setAction(rs.getString("action"));
                    bean.setDetails(rs.getString("details"));
                    bean.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(bean);
                }
            }
        }
        return list;
    }
}
