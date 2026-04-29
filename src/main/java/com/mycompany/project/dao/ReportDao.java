package com.mycompany.project.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mycompany.project.util.DBUtil;

public class ReportDao {
    public List<Map<String, Object>> utilization(Long clinicId, Long serviceId, int month, int year) throws SQLException {
        String sql = "SELECT c.name AS clinic_name, s.name AS service_name, "
                + "COUNT(DISTINCT a.id) AS booked, COUNT(DISTINCT t.id) AS offered "
                + "FROM timeslots t "
                + "JOIN clinics c ON c.id = t.clinic_id "
                + "JOIN services s ON s.id = t.service_id "
                + "LEFT JOIN appointments a ON a.timeslot_id = t.id AND a.status IN ('BOOKED', 'ARRIVED', 'COMPLETED') "
                + "WHERE MONTH(t.slot_date) = ? AND YEAR(t.slot_date) = ? "
                + (clinicId != null ? "AND t.clinic_id = ? " : "")
                + (serviceId != null ? "AND t.service_id = ? " : "")
                + "GROUP BY c.name, s.name ORDER BY c.name, s.name";

        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setInt(i++, month);
            ps.setInt(i++, year);
            if (clinicId != null) {
                ps.setLong(i++, clinicId);
            }
            if (serviceId != null) {
                ps.setLong(i, serviceId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int booked = rs.getInt("booked");
                    int offered = rs.getInt("offered");
                    double rate = offered == 0 ? 0.0 : (booked * 100.0 / offered);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("clinic", rs.getString("clinic_name"));
                    row.put("service", rs.getString("service_name"));
                    row.put("booked", booked);
                    row.put("offered", offered);
                    row.put("rate", String.format("%.1f%%", rate));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    public List<Map<String, Object>> noShowSummary(int month, int year) throws SQLException {
        String sql = "SELECT c.name AS clinic_name, s.name AS service_name, COUNT(*) AS no_show_count "
                + "FROM appointments a "
                + "JOIN clinics c ON c.id = a.clinic_id "
                + "JOIN services s ON s.id = a.service_id "
                + "JOIN timeslots t ON t.id = a.timeslot_id "
                + "WHERE a.status = 'NO_SHOW' AND MONTH(t.slot_date) = ? AND YEAR(t.slot_date) = ? "
                + "GROUP BY c.name, s.name ORDER BY no_show_count DESC";

        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("clinic", rs.getString("clinic_name"));
                    row.put("service", rs.getString("service_name"));
                    row.put("noShow", rs.getInt("no_show_count"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }
}
