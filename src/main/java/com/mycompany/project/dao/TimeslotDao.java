package com.mycompany.project.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.project.model.TimeslotBean;
import com.mycompany.project.util.DBUtil;

public class TimeslotDao {
    public List<TimeslotBean> listByFilters(Long clinicId, Long serviceId, LocalDate date) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.id, t.clinic_id, c.name AS clinic_name, t.service_id, s.name AS service_name, ")
                .append("t.slot_date, t.start_time, t.end_time, t.capacity, COUNT(a.id) AS booked_count ")
                .append("FROM timeslots t ")
                .append("JOIN clinics c ON c.id = t.clinic_id ")
                .append("JOIN services s ON s.id = t.service_id ")
                .append("LEFT JOIN appointments a ON a.timeslot_id = t.id AND a.status NOT IN ('CANCELLED', 'REJECTED') ")
                .append("WHERE 1=1");

        List<Object> params = new ArrayList<>();
        if (clinicId != null) {
            sql.append(" AND t.clinic_id = ?");
            params.add(clinicId);
        }
        if (serviceId != null) {
            sql.append(" AND t.service_id = ?");
            params.add(serviceId);
        }
        if (date != null) {
            sql.append(" AND t.slot_date = ?");
            params.add(Date.valueOf(date));
        }

        sql.append(" GROUP BY t.id, t.clinic_id, c.name, t.service_id, s.name, t.slot_date, t.start_time, t.end_time, t.capacity")
                .append(" ORDER BY t.slot_date DESC, t.start_time ASC");

        List<TimeslotBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public TimeslotBean findById(long id) throws SQLException {
        String sql = "SELECT t.id, t.clinic_id, c.name AS clinic_name, t.service_id, s.name AS service_name, "
                + "t.slot_date, t.start_time, t.end_time, t.capacity "
                + "FROM timeslots t JOIN clinics c ON c.id = t.clinic_id JOIN services s ON s.id = t.service_id "
                + "WHERE t.id = ?";
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

    public void create(TimeslotBean slot) throws SQLException {
        String sql = "INSERT INTO timeslots(clinic_id, service_id, slot_date, start_time, end_time, capacity) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, slot.getClinicId());
            ps.setLong(2, slot.getServiceId());
            ps.setDate(3, Date.valueOf(slot.getSlotDate()));
            ps.setTime(4, Time.valueOf(slot.getStartTime()));
            ps.setTime(5, Time.valueOf(slot.getEndTime()));
            ps.setInt(6, slot.getCapacity());
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM timeslots WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private TimeslotBean map(ResultSet rs) throws SQLException {
        TimeslotBean bean = new TimeslotBean();
        bean.setId(rs.getLong("id"));
        bean.setClinicId(rs.getLong("clinic_id"));
        bean.setClinicName(rs.getString("clinic_name"));
        bean.setServiceId(rs.getLong("service_id"));
        bean.setServiceName(rs.getString("service_name"));
        bean.setSlotDate(rs.getDate("slot_date").toLocalDate());
        bean.setStartTime(rs.getTime("start_time").toLocalTime());
        bean.setEndTime(rs.getTime("end_time").toLocalTime());
        bean.setCapacity(rs.getInt("capacity"));
        if (hasColumn(rs, "booked_count")) {
            bean.setBookedCount(rs.getInt("booked_count"));
        }
        return bean;
    }

    private boolean hasColumn(ResultSet rs, String column) {
        try {
            rs.findColumn(column);
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }
}