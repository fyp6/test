package com.mycompany.project.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.project.model.PolicyBean;
import com.mycompany.project.util.DBUtil;

public class PolicyDao {
    public String get(String key, String defaultValue) throws SQLException {
        String sql = "SELECT setting_value FROM policy_settings WHERE setting_key = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return defaultValue;
    }

    public int getInt(String key, int defaultValue) throws SQLException {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) throws SQLException {
        String value = get(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    public List<PolicyBean> listAll() throws SQLException {
        String sql = "SELECT setting_key, setting_value FROM policy_settings ORDER BY setting_key";
        List<PolicyBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PolicyBean bean = new PolicyBean();
                bean.setKey(rs.getString("setting_key"));
                bean.setValue(rs.getString("setting_value"));
                list.add(bean);
            }
        }
        return list;
    }

    public void upsert(String key, String value) throws SQLException {
        String sql = "INSERT INTO policy_settings(setting_key, setting_value) VALUES (?, ?) "
            + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }
}
