package com.mycompany.project.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.project.model.NotificationBean;
import com.mycompany.project.util.DBUtil;

public class NotificationDao {
    public void create(long userId, String title, String body, String type) throws SQLException {
        String sql = "INSERT INTO notifications(user_id, title, body, type, is_read) VALUES (?, ?, ?, ?, FALSE)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setString(3, body);
            ps.setString(4, type);
            ps.executeUpdate();
        }
    }

    public List<NotificationBean> listByUser(long userId, int limit) throws SQLException {
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        List<NotificationBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NotificationBean bean = new NotificationBean();
                    bean.setId(rs.getLong("id"));
                    bean.setUserId(rs.getLong("user_id"));
                    bean.setTitle(rs.getString("title"));
                    bean.setBody(rs.getString("body"));
                    bean.setType(rs.getString("type"));
                    bean.setRead(rs.getBoolean("is_read"));
                    bean.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(bean);
                }
            }
        }
        return list;
    }

    public boolean existsByUserTypeAndBody(long userId, String type, String body) throws SQLException {
        String sql = "SELECT 1 FROM notifications WHERE user_id = ? AND type = ? AND body = ? LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, type);
            ps.setString(3, body);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void markAllRead(long userId) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE notifications SET is_read = TRUE WHERE user_id = ?")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }
}
