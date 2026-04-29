package com.mycompany.project.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.project.model.QueueTicketBean;
import com.mycompany.project.util.DBUtil;

public class QueueDao {
    public String joinQueue(long patientId, long clinicId, long serviceId, LocalDate date) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (hasActiveTicket(conn, patientId, clinicId, serviceId, date)) {
                    conn.rollback();
                    return "You already have an active queue ticket today for this clinic/service.";
                }

                int nextNumber = 1;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COALESCE(MAX(queue_number), 0) + 1 FROM queue_tickets "
                                + "WHERE clinic_id = ? AND service_id = ? AND queue_date = ?")) {
                    ps.setLong(1, clinicId);
                    ps.setLong(2, serviceId);
                    ps.setDate(3, Date.valueOf(date));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            nextNumber = rs.getInt(1);
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO queue_tickets(patient_id, clinic_id, service_id, queue_date, queue_number, status) "
                                + "VALUES (?, ?, ?, ?, ?, 'WAITING')")) {
                    ps.setLong(1, patientId);
                    ps.setLong(2, clinicId);
                    ps.setLong(3, serviceId);
                    ps.setDate(4, Date.valueOf(date));
                    ps.setInt(5, nextNumber);
                    ps.executeUpdate();
                }

                conn.commit();
                return null;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void updateQueueStatus(long ticketId, String status) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE queue_tickets SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setLong(2, ticketId);
            ps.executeUpdate();
        }
    }

    public List<QueueTicketBean> listByPatient(long patientId) throws SQLException {
        String sql = baseSelect() + " WHERE q.patient_id = ? ORDER BY q.queue_date DESC, q.queue_number";
        List<QueueTicketBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs, conn));
                }
            }
        }
        return list;
    }

    public List<QueueTicketBean> listByClinicServiceDay(long clinicId, long serviceId, LocalDate day) throws SQLException {
        String sql = baseSelect() + " WHERE q.clinic_id = ? AND q.service_id = ? AND q.queue_date = ? "
                + "ORDER BY q.queue_number";
        List<QueueTicketBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setLong(2, serviceId);
            ps.setDate(3, Date.valueOf(day));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs, conn));
                }
            }
        }
        return list;
    }

    public QueueTicketBean findFirstWaiting(long clinicId, long serviceId, LocalDate day) throws SQLException {
        String sql = baseSelect() + " WHERE q.clinic_id = ? AND q.service_id = ? AND q.queue_date = ? "
                + "AND q.status = 'WAITING' ORDER BY q.queue_number LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setLong(2, serviceId);
            ps.setDate(3, Date.valueOf(day));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs, conn);
                }
            }
        }
        return null;
    }

    private String baseSelect() {
        return "SELECT q.id, q.patient_id, u.full_name AS patient_name, q.clinic_id, c.name AS clinic_name, "
                + "q.service_id, s.name AS service_name, q.queue_date, q.queue_number, q.status, q.created_at "
                + "FROM queue_tickets q "
                + "JOIN users u ON u.id = q.patient_id "
                + "JOIN clinics c ON c.id = q.clinic_id "
                + "JOIN services s ON s.id = q.service_id";
    }

    private QueueTicketBean map(ResultSet rs, Connection conn) throws SQLException {
        QueueTicketBean bean = new QueueTicketBean();
        bean.setId(rs.getLong("id"));
        bean.setPatientId(rs.getLong("patient_id"));
        bean.setPatientName(rs.getString("patient_name"));
        bean.setClinicId(rs.getLong("clinic_id"));
        bean.setClinicName(rs.getString("clinic_name"));
        bean.setServiceId(rs.getLong("service_id"));
        bean.setServiceName(rs.getString("service_name"));
        bean.setQueueDate(rs.getDate("queue_date").toLocalDate());
        bean.setQueueNumber(rs.getInt("queue_number"));
        bean.setStatus(rs.getString("status"));
        bean.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        bean.setEstimatedWaitMinutes(calcWaitMinutes(conn, bean));
        return bean;
    }

    private int calcWaitMinutes(Connection conn, QueueTicketBean bean) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM queue_tickets WHERE clinic_id=? AND service_id=? AND queue_date=? "
                        + "AND queue_number < ? AND status IN ('WAITING', 'CALLED')")) {
            ps.setLong(1, bean.getClinicId());
            ps.setLong(2, bean.getServiceId());
            ps.setDate(3, Date.valueOf(bean.getQueueDate()));
            ps.setInt(4, bean.getQueueNumber());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) * 10;
            }
        }
    }

    private boolean hasActiveTicket(Connection conn, long patientId, long clinicId, long serviceId, LocalDate date)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM queue_tickets WHERE patient_id=? AND clinic_id=? AND service_id=? AND queue_date=? "
                        + "AND status IN ('WAITING', 'CALLED')")) {
            ps.setLong(1, patientId);
            ps.setLong(2, clinicId);
            ps.setLong(3, serviceId);
            ps.setDate(4, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
