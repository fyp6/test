package com.mycompany.project.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.project.model.AppointmentBean;
import com.mycompany.project.model.TimeslotBean;
import com.mycompany.project.util.DBUtil;

public class AppointmentDao {
    public List<TimeslotBean> findAvailableSlots(long clinicId, long serviceId, LocalDate date) throws SQLException {
        String sql = "SELECT t.id, t.clinic_id, t.service_id, t.slot_date, t.start_time, t.end_time, t.capacity, "
                + "COUNT(a.id) AS booked_count "
                + "FROM timeslots t "
                + "LEFT JOIN appointments a ON a.timeslot_id = t.id AND a.status NOT IN ('CANCELLED', 'REJECTED') "
                + "WHERE t.clinic_id = ? AND t.service_id = ? AND t.slot_date = ? "
                + "GROUP BY t.id, t.clinic_id, t.service_id, t.slot_date, t.start_time, t.end_time, t.capacity "
                + "ORDER BY t.start_time";

        List<TimeslotBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setLong(2, serviceId);
            ps.setDate(3, Date.valueOf(date));
            LocalDateTime now = LocalDateTime.now();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TimeslotBean slot = new TimeslotBean();
                    slot.setId(rs.getLong("id"));
                    slot.setClinicId(rs.getLong("clinic_id"));
                    slot.setServiceId(rs.getLong("service_id"));
                    slot.setSlotDate(rs.getDate("slot_date").toLocalDate());
                    slot.setStartTime(rs.getTime("start_time").toLocalTime());
                    slot.setEndTime(rs.getTime("end_time").toLocalTime());
                    slot.setCapacity(rs.getInt("capacity"));
                    slot.setBookedCount(rs.getInt("booked_count"));
                    if (isPast(slot, now)) {
                        continue;
                    }
                    list.add(slot);
                }
            }
        }
        return list;
    }

    public String bookAppointment(long patientId, long clinicId, long serviceId, long timeslotId, int maxActiveBookings)
            throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                LocalDateTime slotStart = findTimeslotStart(conn, timeslotId);
                if (slotStart == null) {
                    conn.rollback();
                    return "Timeslot not found.";
                }
                if (slotStart.isBefore(LocalDateTime.now())) {
                    conn.rollback();
                    return "Selected slot is in the past.";
                }

                int active = countActiveBookings(conn, patientId);
                if (active >= maxActiveBookings) {
                    conn.rollback();
                    return "Max active booking limit reached.";
                }

                if (hasDuplicate(conn, patientId, timeslotId)) {
                    conn.rollback();
                    return "You already booked this slot.";
                }

                if (!hasCapacity(conn, timeslotId)) {
                    conn.rollback();
                    return "This slot is full.";
                }

                boolean approvalRequired = requiresApprovalForService(conn, serviceId);
                String status = approvalRequired ? "PENDING" : "BOOKED";
                String reason = approvalRequired ? "Awaiting staff approval" : null;

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO appointments(patient_id, clinic_id, service_id, timeslot_id, status, reason) "
                                + "VALUES (?, ?, ?, ?, ?, ?)")) {
                    ps.setLong(1, patientId);
                    ps.setLong(2, clinicId);
                    ps.setLong(3, serviceId);
                    ps.setLong(4, timeslotId);
                    ps.setString(5, status);
                    if (reason == null) {
                        ps.setNull(6, Types.VARCHAR);
                    } else {
                        ps.setString(6, reason);
                    }
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

    public String reschedule(long appointmentId, long patientId, long newTimeslotId) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                LocalDateTime slotStart = findTimeslotStart(conn, newTimeslotId);
                if (slotStart == null) {
                    conn.rollback();
                    return "Timeslot not found.";
                }
                if (slotStart.isBefore(LocalDateTime.now())) {
                    conn.rollback();
                    return "Selected slot is in the past.";
                }

                if (!hasCapacity(conn, newTimeslotId)) {
                    conn.rollback();
                    return "Selected slot is full.";
                }

                if (hasDuplicate(conn, patientId, newTimeslotId)) {
                    conn.rollback();
                    return "You already booked the selected slot.";
                }

                long clinicId;
                long serviceId;
                try (PreparedStatement findSlot = conn.prepareStatement(
                        "SELECT clinic_id, service_id FROM timeslots WHERE id = ?")) {
                    findSlot.setLong(1, newTimeslotId);
                    try (ResultSet rs = findSlot.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return "Timeslot not found.";
                        }
                        clinicId = rs.getLong("clinic_id");
                        serviceId = rs.getLong("service_id");
                    }
                }

                boolean approvalRequired = requiresApprovalForService(conn, serviceId);
                String status = approvalRequired ? "PENDING" : "BOOKED";
                String reason = approvalRequired ? "Awaiting staff approval after reschedule" : "Rescheduled by patient";

                try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE appointments SET timeslot_id = ?, clinic_id = ?, service_id = ?, status = ?, reason = ? "
                        + "WHERE id = ? AND patient_id = ? AND status IN ('BOOKED', 'ARRIVED', 'PENDING', 'APPROVED')")) {
                    ps.setLong(1, newTimeslotId);
                    ps.setLong(2, clinicId);
                    ps.setLong(3, serviceId);
                    ps.setString(4, status);
                    ps.setString(5, reason);
                    ps.setLong(6, appointmentId);
                    ps.setLong(7, patientId);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return "Appointment not eligible for reschedule.";
                    }
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

    public void cancel(long appointmentId, long actorUserId, String reason, boolean byClinic) throws SQLException {
        String statusReason = (byClinic ? "Cancelled by clinic: " : "Cancelled by patient: ") + reason;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE appointments SET status = 'CANCELLED', reason = ? WHERE id = ?")) {
            ps.setString(1, statusReason);
            ps.setLong(2, appointmentId);
            ps.executeUpdate();
        }
    }

    public void updateAttendance(long appointmentId, String status, String reason) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE appointments SET status = ?, reason = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setString(2, reason);
            ps.setLong(3, appointmentId);
            ps.executeUpdate();
        }
    }

    public List<AppointmentBean> listByPatient(long patientId) throws SQLException {
        String sql = baseSelect() + " WHERE a.patient_id = ? ORDER BY t.slot_date DESC, t.start_time DESC";
        List<AppointmentBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public AppointmentBean findById(long appointmentId) throws SQLException {
        String sql = baseSelect() + " WHERE a.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public List<AppointmentBean> listForClinicDay(long clinicId, LocalDate date) throws SQLException {
        String sql = baseSelect() + " WHERE a.clinic_id = ? AND t.slot_date = ? ORDER BY t.start_time";
        List<AppointmentBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public List<AppointmentBean> listForClinicDayExcludingPending(long clinicId, LocalDate date) throws SQLException {
        String sql = baseSelect() + " WHERE a.clinic_id = ? AND t.slot_date = ? AND a.status <> 'PENDING' ORDER BY t.start_time";
        List<AppointmentBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public List<AppointmentBean> listPendingForClinicDay(long clinicId, LocalDate date) throws SQLException {
        String sql = baseSelect() + " WHERE a.clinic_id = ? AND t.slot_date = ? AND a.status = 'PENDING' ORDER BY t.start_time";
        List<AppointmentBean> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public List<AppointmentBean> listWithFilters(Long clinicId, Long serviceId, Integer month, Integer year, String status)
            throws SQLException {
        StringBuilder sql = new StringBuilder(baseSelect() + " WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (clinicId != null) {
            sql.append(" AND a.clinic_id = ?");
            params.add(clinicId);
        }
        if (serviceId != null) {
            sql.append(" AND a.service_id = ?");
            params.add(serviceId);
        }
        if (month != null) {
            sql.append(" AND MONTH(t.slot_date) = ?");
            params.add(month);
        }
        if (year != null) {
            sql.append(" AND YEAR(t.slot_date) = ?");
            params.add(year);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND a.status = ?");
            params.add(status);
        }

        sql.append(" ORDER BY t.slot_date DESC, t.start_time DESC");

        List<AppointmentBean> list = new ArrayList<>();
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

    private String baseSelect() {
        return "SELECT a.id, a.patient_id, u.full_name AS patient_name, a.clinic_id, c.name AS clinic_name, "
                + "a.service_id, s.name AS service_name, a.timeslot_id, t.slot_date, t.start_time, t.end_time, "
                + "a.status, a.reason, a.created_at "
                + "FROM appointments a "
                + "JOIN users u ON u.id = a.patient_id "
                + "JOIN clinics c ON c.id = a.clinic_id "
                + "JOIN services s ON s.id = a.service_id "
                + "JOIN timeslots t ON t.id = a.timeslot_id";
    }

    private AppointmentBean map(ResultSet rs) throws SQLException {
        AppointmentBean bean = new AppointmentBean();
        bean.setId(rs.getLong("id"));
        bean.setPatientId(rs.getLong("patient_id"));
        bean.setPatientName(rs.getString("patient_name"));
        bean.setClinicId(rs.getLong("clinic_id"));
        bean.setClinicName(rs.getString("clinic_name"));
        bean.setServiceId(rs.getLong("service_id"));
        bean.setServiceName(rs.getString("service_name"));
        bean.setTimeslotId(rs.getLong("timeslot_id"));
        bean.setSlotDate(rs.getDate("slot_date").toLocalDate());
        Time start = rs.getTime("start_time");
        Time end = rs.getTime("end_time");
        bean.setStartTime(start == null ? null : start.toLocalTime());
        bean.setEndTime(end == null ? null : end.toLocalTime());
        bean.setStatus(rs.getString("status"));
        bean.setReason(rs.getString("reason"));
        bean.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return bean;
    }

    private int countActiveBookings(Connection conn, long patientId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM appointments WHERE patient_id = ? AND status IN ('BOOKED', 'ARRIVED', 'PENDING', 'APPROVED')")) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private boolean hasDuplicate(Connection conn, long patientId, long timeslotId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM appointments WHERE patient_id = ? AND timeslot_id = ? AND status IN ('BOOKED', 'ARRIVED', 'PENDING', 'APPROVED')")) {
            ps.setLong(1, patientId);
            ps.setLong(2, timeslotId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean hasCapacity(Connection conn, long timeslotId) throws SQLException {
        int capacity;
        try (PreparedStatement ps = conn.prepareStatement("SELECT capacity FROM timeslots WHERE id = ?")) {
            ps.setLong(1, timeslotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                capacity = rs.getInt(1);
            }
        }

        int booked;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM appointments WHERE timeslot_id = ? AND status NOT IN ('CANCELLED', 'REJECTED')")) {
            ps.setLong(1, timeslotId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                booked = rs.getInt(1);
            }
        }
        return booked < capacity;
    }

    public boolean requiresApprovalForService(long serviceId) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            return requiresApprovalForService(conn, serviceId);
        }
    }

    public boolean requiresApprovalForTimeslot(long timeslotId) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            return requiresApprovalForTimeslot(conn, timeslotId);
        }
    }

    public String approve(long appointmentId, String reason) throws SQLException {
        return updateApprovalStatus(appointmentId, reason, "APPROVED");
    }

    public String reject(long appointmentId, String reason) throws SQLException {
        return updateApprovalStatus(appointmentId, reason, "REJECTED");
    }

    private boolean requiresApprovalForService(Connection conn, long serviceId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT limited_quota FROM services WHERE id = ?")) {
            ps.setLong(1, serviceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }

    private boolean requiresApprovalForTimeslot(Connection conn, long timeslotId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT s.limited_quota FROM timeslots t JOIN services s ON s.id = t.service_id WHERE t.id = ?")) {
            ps.setLong(1, timeslotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }

    private String updateApprovalStatus(long appointmentId, String reason, String targetStatus) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String currentStatus;
                try (PreparedStatement ps = conn.prepareStatement("SELECT patient_id, status FROM appointments WHERE id = ?")) {
                    ps.setLong(1, appointmentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return "Appointment not found.";
                        }
                        currentStatus = rs.getString("status");
                    }
                }

                if (!"PENDING".equals(currentStatus)) {
                    conn.rollback();
                    return "Only pending bookings can be reviewed.";
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE appointments SET status = ?, reason = ? WHERE id = ?")) {
                    ps.setString(1, targetStatus);
                    ps.setString(2, reason == null || reason.isBlank() ? ("APPROVED".equals(targetStatus) ? "Approved by staff" : "Rejected by staff") : reason);
                    ps.setLong(3, appointmentId);
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

    private LocalDateTime findTimeslotStart(Connection conn, long timeslotId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT slot_date, start_time FROM timeslots WHERE id = ?")) {
            ps.setLong(1, timeslotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                LocalDate slotDate = rs.getDate("slot_date").toLocalDate();
                Time startTime = rs.getTime("start_time");
                return LocalDateTime.of(slotDate, startTime.toLocalTime());
            }
        }
    }

    private boolean isPast(TimeslotBean slot, LocalDateTime now) {
        LocalDateTime slotStart = LocalDateTime.of(slot.getSlotDate(), slot.getStartTime());
        return slotStart.isBefore(now);
    }
}
