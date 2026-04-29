package com.mycompany.project.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.project.model.ServiceBean;
import com.mycompany.project.util.DBUtil;

public class ServiceDao {
    public List<ServiceBean> listAll() throws SQLException {
        String sql = "SELECT * FROM services ORDER BY name";
        List<ServiceBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ServiceBean bean = new ServiceBean();
                bean.setId(rs.getLong("id"));
                bean.setName(rs.getString("name"));
                bean.setDescription(rs.getString("description"));
                bean.setLimitedQuota(rs.getBoolean("limited_quota"));
                list.add(bean);
            }
        }
        return list;
    }

    public ServiceBean findById(long id) throws SQLException {
        String sql = "SELECT * FROM services WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ServiceBean bean = new ServiceBean();
                    bean.setId(rs.getLong("id"));
                    bean.setName(rs.getString("name"));
                    bean.setDescription(rs.getString("description"));
                    bean.setLimitedQuota(rs.getBoolean("limited_quota"));
                    return bean;
                }
            }
        }
        return null;
    }

    public void create(ServiceBean service) throws SQLException {
        String sql = "INSERT INTO services(name, description, limited_quota) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, service.getName());
            ps.setString(2, service.getDescription());
            ps.setBoolean(3, service.isLimitedQuota());
            ps.executeUpdate();
        }
    }

    public void update(ServiceBean service) throws SQLException {
        String sql = "UPDATE services SET name = ?, description = ?, limited_quota = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, service.getName());
            ps.setString(2, service.getDescription());
            ps.setBoolean(3, service.isLimitedQuota());
            ps.setLong(4, service.getId());
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM services WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
