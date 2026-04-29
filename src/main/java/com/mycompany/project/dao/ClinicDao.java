package com.mycompany.project.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.project.model.ClinicBean;
import com.mycompany.project.util.DBUtil;

public class ClinicDao {
    public List<ClinicBean> listAll() throws SQLException {
        String sql = "SELECT * FROM clinics ORDER BY name";
        List<ClinicBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ClinicBean bean = new ClinicBean();
                bean.setId(rs.getLong("id"));
                bean.setName(rs.getString("name"));
                bean.setLocation(rs.getString("location"));
                bean.setWalkInEnabled(rs.getBoolean("walk_in_enabled"));
                list.add(bean);
            }
        }
        return list;
    }

    public ClinicBean findById(long id) throws SQLException {
        String sql = "SELECT * FROM clinics WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ClinicBean bean = new ClinicBean();
                    bean.setId(rs.getLong("id"));
                    bean.setName(rs.getString("name"));
                    bean.setLocation(rs.getString("location"));
                    bean.setWalkInEnabled(rs.getBoolean("walk_in_enabled"));
                    return bean;
                }
            }
        }
        return null;
    }

    public void create(ClinicBean clinic) throws SQLException {
        String sql = "INSERT INTO clinics(name, location, walk_in_enabled) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clinic.getName());
            ps.setString(2, clinic.getLocation());
            ps.setBoolean(3, clinic.isWalkInEnabled());
            ps.executeUpdate();
        }
    }

    public void update(ClinicBean clinic) throws SQLException {
        String sql = "UPDATE clinics SET name = ?, location = ?, walk_in_enabled = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clinic.getName());
            ps.setString(2, clinic.getLocation());
            ps.setBoolean(3, clinic.isWalkInEnabled());
            ps.setLong(4, clinic.getId());
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM clinics WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
