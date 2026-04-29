package com.mycompany.project.util;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class SchemaInitializer {
    private static final String DEMO_PASSWORD = "123123";

    private SchemaInitializer() {
    }

    public static void initialize() {
        initialize(LocalDate.now());
    }

    public static void initialize(LocalDate startDate) {
        try (Connection conn = DBUtil.getConnection()) {
            createTables(conn);
            seedCoreData(conn);
            seedTimeslots(conn, startDate == null ? LocalDate.now() : startDate);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to initialize database", ex);
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS clinics ("
                + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "name VARCHAR(80) NOT NULL UNIQUE,"
                    + "location VARCHAR(80) NOT NULL,"
                + "walk_in_enabled BOOLEAN DEFAULT TRUE"
                + ") ENGINE=InnoDB"
            );

            st.execute("CREATE TABLE IF NOT EXISTS services ("
                + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "name VARCHAR(80) NOT NULL UNIQUE,"
                    + "description VARCHAR(255),"
                + "limited_quota BOOLEAN DEFAULT FALSE"
                + ") ENGINE=InnoDB"
            );

            st.execute("CREATE TABLE IF NOT EXISTS users ("
                + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "username VARCHAR(50) NOT NULL UNIQUE,"
                    + "password_hash VARCHAR(128) NOT NULL,"
                    + "full_name VARCHAR(120) NOT NULL,"
                    + "email VARCHAR(120) NOT NULL,"
                    + "role VARCHAR(20) NOT NULL,"
                    + "clinic_id BIGINT,"
                    + "active BOOLEAN DEFAULT TRUE,"
                        + "CONSTRAINT fk_user_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id)"
                        + ") ENGINE=InnoDB"
            );

            st.execute("CREATE TABLE IF NOT EXISTS timeslots ("
                        + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "clinic_id BIGINT NOT NULL,"
                    + "service_id BIGINT NOT NULL,"
                    + "slot_date DATE NOT NULL,"
                    + "start_time TIME NOT NULL,"
                    + "end_time TIME NOT NULL,"
                    + "capacity INT NOT NULL,"
                    + "CONSTRAINT fk_slot_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id),"
                    + "CONSTRAINT fk_slot_service FOREIGN KEY (service_id) REFERENCES services(id),"
                        + "CONSTRAINT uq_slot UNIQUE (clinic_id, service_id, slot_date, start_time)"
                        + ") ENGINE=InnoDB"
            );

            // If the schema was created externally (e.g. via sql/init-mysql.sql), the UNIQUE constraint may be
            // missing. Try to add it so seedTimeslots() stays idempotent.
            try {
                st.execute("ALTER TABLE timeslots ADD CONSTRAINT uq_slot UNIQUE (clinic_id, service_id, slot_date, start_time)");
            } catch (SQLException ignored) {
                // ignore if already exists or cannot be added (e.g., duplicates already present)
            }

            st.execute("CREATE TABLE IF NOT EXISTS appointments ("
                        + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "patient_id BIGINT NOT NULL,"
                    + "clinic_id BIGINT NOT NULL,"
                    + "service_id BIGINT NOT NULL,"
                    + "timeslot_id BIGINT NOT NULL,"
                    + "status VARCHAR(20) NOT NULL,"
                    + "reason VARCHAR(255),"
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "CONSTRAINT fk_appt_user FOREIGN KEY (patient_id) REFERENCES users(id),"
                    + "CONSTRAINT fk_appt_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id),"
                    + "CONSTRAINT fk_appt_service FOREIGN KEY (service_id) REFERENCES services(id),"
                        + "CONSTRAINT fk_appt_slot FOREIGN KEY (timeslot_id) REFERENCES timeslots(id)"
                        + ") ENGINE=InnoDB"
            );

            st.execute("CREATE TABLE IF NOT EXISTS queue_tickets ("
                        + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "patient_id BIGINT NOT NULL,"
                    + "clinic_id BIGINT NOT NULL,"
                    + "service_id BIGINT NOT NULL,"
                    + "queue_date DATE NOT NULL,"
                    + "queue_number INT NOT NULL,"
                    + "status VARCHAR(20) NOT NULL,"
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "CONSTRAINT fk_queue_user FOREIGN KEY (patient_id) REFERENCES users(id),"
                    + "CONSTRAINT fk_queue_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id),"
                    + "CONSTRAINT fk_queue_service FOREIGN KEY (service_id) REFERENCES services(id),"
                        + "CONSTRAINT uq_queue_number UNIQUE (clinic_id, service_id, queue_date, queue_number)"
                        + ") ENGINE=InnoDB"
            );

            st.execute("CREATE TABLE IF NOT EXISTS notifications ("
                        + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "user_id BIGINT NOT NULL,"
                    + "title VARCHAR(120) NOT NULL,"
                    + "body VARCHAR(255) NOT NULL,"
                    + "type VARCHAR(40) NOT NULL,"
                    + "is_read BOOLEAN DEFAULT FALSE,"
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                        + "CONSTRAINT fk_notice_user FOREIGN KEY (user_id) REFERENCES users(id)"
                        + ") ENGINE=InnoDB"
            );

            st.execute("CREATE TABLE IF NOT EXISTS policy_settings ("
                    + "setting_key VARCHAR(80) PRIMARY KEY,"
                        + "setting_value VARCHAR(120) NOT NULL"
                        + ") ENGINE=InnoDB"
            );

            st.execute("CREATE TABLE IF NOT EXISTS audit_logs ("
                        + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "staff_id BIGINT,"
                    + "action VARCHAR(120) NOT NULL,"
                    + "details VARCHAR(255),"
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                        + "CONSTRAINT fk_audit_user FOREIGN KEY (staff_id) REFERENCES users(id)"
                        + ") ENGINE=InnoDB"
            );
        }
    }

    private static void seedCoreData(Connection conn) throws SQLException {
        if (count(conn, "clinics") == 0) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO clinics(name, location, walk_in_enabled) VALUES (?, ?, ?)")) {
                insertClinic(ps, "Chai Wan Clinic", "Chai Wan", true);
                insertClinic(ps, "Tseung Kwan O Clinic", "Tseung Kwan O", true);
                insertClinic(ps, "Sha Tin Clinic", "Sha Tin", true);
                insertClinic(ps, "Tuen Mun Clinic", "Tuen Mun", false);
                insertClinic(ps, "Tsing Yi Clinic", "Tsing Yi", true);
            }
        }

        if (count(conn, "services") == 0) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO services(name, description, limited_quota) VALUES (?, ?, ?)")) {
                insertService(ps, "General Consultation", "Primary doctor consultation", false);
                insertService(ps, "Vaccination", "Vaccination and follow-up", true);
                insertService(ps, "Health Screening", "Basic health screening package", true);
            }
        }

            ensureDemoUsers(conn);

            if (count(conn, "notifications") == 0) {
                insertNotification(conn, "patient1", "Welcome to CCHC",
                    "Your patient account is ready. You can start booking now.", "SYSTEM");
                insertNotification(conn, "patient2", "Appointment Reminder",
                    "Your demo appointment is ready to review in the portal.", "INFO");
                insertNotification(conn, "staff1", "Roster Updated",
                    "Front desk duty roster for the week has been published.", "SYSTEM");
            }

            if (count(conn, "audit_logs") == 0) {
                insertAuditLog(conn, "staff1", "CREATE_USER",
                    "username=patient1; status=created demo account");
                insertAuditLog(conn, "staff2", "UPDATE_POLICY",
                    "setting=queue_enabled; value=true");
                insertAuditLog(conn, "staff3", "CREATE_TIMESLOT",
                    "clinic=Sha Tin Clinic; slots=15; date=2026-04-24");
                insertAuditLog(conn, "staff4", "APPROVE_APPOINTMENT",
                    "patient=patient2; status=confirmed");
                insertAuditLog(conn, "staff5", "HANDLE_QUEUE",
                    "clinic=Tsing Yi Clinic; queue_number=2; status=expired");
            }

        upsertPolicy(conn, "max_active_bookings", "3");
        upsertPolicy(conn, "cancellation_cutoff_hours", "4");
        upsertPolicy(conn, "queue_enabled", "true");
    }

    private static void seedTimeslots(Connection conn, LocalDate startDate) throws SQLException {
        // Ensure demo timeslots exist for a future window.
        // Use INSERT IGNORE so this method is idempotent and can safely run on every startup.
        // This also fixes cases where a user imported minimal seed data (e.g., only one day of slots)
        // which would otherwise block further seeding.
        final int daysToGenerate = 30;
        final LocalTime[] starts = new LocalTime[]{
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        };

        List<Long> clinicIds = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM clinics")) {
            while (rs.next()) {
                clinicIds.add(rs.getLong(1));
            }
        }

        List<ServiceSeed> services = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, limited_quota FROM services")) {
            while (rs.next()) {
                services.add(new ServiceSeed(rs.getLong(1), rs.getBoolean(2)));
            }
        }

        if (clinicIds.isEmpty() || services.isEmpty()) {
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO timeslots(clinic_id, service_id, slot_date, start_time, end_time, capacity) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            for (long clinicId : clinicIds) {
                for (ServiceSeed service : services) {
                    int capacity = service.limitedQuota ? 5 : 8;
                    for (int i = 0; i < daysToGenerate; i++) {
                        LocalDate date = startDate.plusDays(i);
                        for (LocalTime start : starts) {
                            ps.setLong(1, clinicId);
                            ps.setLong(2, service.id);
                            ps.setDate(3, Date.valueOf(date));
                            ps.setTime(4, Time.valueOf(start));
                            ps.setTime(5, Time.valueOf(start.plusMinutes(45)));
                            ps.setInt(6, capacity);
                            ps.addBatch();
                        }
                    }
                }
            }
            ps.executeBatch();
        }
    }

    private static final class ServiceSeed {
        private final long id;
        private final boolean limitedQuota;

        private ServiceSeed(long id, boolean limitedQuota) {
            this.id = id;
            this.limitedQuota = limitedQuota;
        }
    }

    private static int count(Connection conn, String table) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static void upsertPolicy(Connection conn, String key, String value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO policy_settings(setting_key, setting_value) VALUES (?, ?) "
                        + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    private static void ensureDemoUsers(Connection conn) throws SQLException {
        List<UserSeed> users = List.of(
                new UserSeed("admin", "System Administrator", "admin@cchc.hk", "ADMIN", null),
                new UserSeed("staff1", "Front Desk Staff", "staff1@cchc.hk", "STAFF", 1L),
                new UserSeed("staff2", "Nursing Staff", "staff2@cchc.hk", "STAFF", 2L),
                new UserSeed("staff3", "Registration Staff", "staff3@cchc.hk", "STAFF", 3L),
                new UserSeed("staff4", "Billing Staff", "staff4@cchc.hk", "STAFF", 4L),
                new UserSeed("staff5", "Queue Supervisor", "staff5@cchc.hk", "STAFF", 5L),
                new UserSeed("patient1", "Demo Patient 1", "patient1@example.com", "PATIENT", null),
                new UserSeed("patient2", "Demo Patient 2", "patient2@example.com", "PATIENT", null),
                new UserSeed("patient3", "Demo Patient 3", "patient3@example.com", "PATIENT", null),
                new UserSeed("patient4", "Demo Patient 4", "patient4@example.com", "PATIENT", null),
                new UserSeed("patient5", "Demo Patient 5", "patient5@example.com", "PATIENT", null),
                new UserSeed("patient6", "Demo Patient 6", "patient6@example.com", "PATIENT", null),
                new UserSeed("patient7", "Demo Patient 7", "patient7@example.com", "PATIENT", null),
                new UserSeed("patient8", "Demo Patient 8", "patient8@example.com", "PATIENT", null),
                new UserSeed("patient9", "Demo Patient 9", "patient9@example.com", "PATIENT", null),
                new UserSeed("patient10", "Demo Patient 10", "patient10@example.com", "PATIENT", null),
                new UserSeed("patient11", "Demo Patient 11", "patient11@example.com", "PATIENT", null),
                new UserSeed("patient12", "Demo Patient 12", "patient12@example.com", "PATIENT", null),
                new UserSeed("patient13", "Demo Patient 13", "patient13@example.com", "PATIENT", null),
                new UserSeed("patient14", "Demo Patient 14", "patient14@example.com", "PATIENT", null),
                new UserSeed("patient15", "Demo Patient 15", "patient15@example.com", "PATIENT", null),
                new UserSeed("patient16", "Demo Patient 16", "patient16@example.com", "PATIENT", null),
                new UserSeed("patient17", "Demo Patient 17", "patient17@example.com", "PATIENT", null),
                new UserSeed("patient18", "Demo Patient 18", "patient18@example.com", "PATIENT", null),
                new UserSeed("patient19", "Demo Patient 19", "patient19@example.com", "PATIENT", null),
                new UserSeed("patient20", "Demo Patient 20", "patient20@example.com", "PATIENT", null)
        );

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users(username, password_hash, full_name, email, role, clinic_id, active) "
                        + "VALUES (?, ?, ?, ?, ?, ?, TRUE) "
                        + "ON DUPLICATE KEY UPDATE "
                        + "password_hash = VALUES(password_hash), "
                        + "full_name = VALUES(full_name), "
                        + "email = VALUES(email), "
                        + "role = VALUES(role), "
                        + "clinic_id = VALUES(clinic_id), "
                        + "active = TRUE")) {
            for (UserSeed user : users) {
                upsertDemoUser(ps, user.username, DEMO_PASSWORD, user.fullName, user.email, user.role, user.clinicId);
            }
        }
    }

    private static void insertNotification(Connection conn, String username, String title, String body, String type)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO notifications(user_id, title, body, type, is_read) VALUES (?, ?, ?, ?, ?)")) {
            ps.setLong(1, userId(conn, username));
            ps.setString(2, title);
            ps.setString(3, body);
            ps.setString(4, type);
            ps.setBoolean(5, false);
            ps.executeUpdate();
        }
    }

    private static void insertAuditLog(Connection conn, String staffUsername, String action, String details)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO audit_logs(staff_id, action, details) VALUES (?, ?, ?)")) {
            ps.setLong(1, userId(conn, staffUsername));
            ps.setString(2, action);
            ps.setString(3, details);
            ps.executeUpdate();
        }
    }

    private static long userId(Connection conn, String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Missing demo user: " + username);
                }
                return rs.getLong(1);
            }
        }
    }

    private static final class UserSeed {
        private final String username;
        private final String fullName;
        private final String email;
        private final String role;
        private final Long clinicId;

        private UserSeed(String username, String fullName, String email, String role, Long clinicId) {
            this.username = username;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
            this.clinicId = clinicId;
        }
    }

    private static void upsertDemoUser(PreparedStatement ps, String username, String password, String fullName,
                                       String email, String role, Long clinicId) throws SQLException {
        ps.setString(1, username);
        ps.setString(2, PasswordUtil.sha256(password));
        ps.setString(3, fullName);
        ps.setString(4, email);
        ps.setString(5, role);
        if (clinicId == null) {
            ps.setNull(6, java.sql.Types.BIGINT);
        } else {
            ps.setLong(6, clinicId);
        }
        ps.executeUpdate();
    }

    private static void insertClinic(PreparedStatement ps, String name, String location, boolean walkInEnabled) throws SQLException {
        ps.setString(1, name);
        ps.setString(2, location);
        ps.setBoolean(3, walkInEnabled);
        ps.executeUpdate();
    }

    private static void insertService(PreparedStatement ps, String name, String description, boolean limitedQuota) throws SQLException {
        ps.setString(1, name);
        ps.setString(2, description);
        ps.setBoolean(3, limitedQuota);
        ps.executeUpdate();
    }

}
