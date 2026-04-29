-- init-mysql.sql
-- Schema and sample data for the CCHC clinic application (MySQL)
-- WARNING: This script drops tables with the same names if they exist.

CREATE DATABASE IF NOT EXISTS cchc_clinic
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE cchc_clinic;

-- Drop dependent tables first
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS queue_tickets;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS timeslots;
DROP TABLE IF EXISTS services;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS clinics;
DROP TABLE IF EXISTS policy_settings;

-- Create clinics
CREATE TABLE clinics (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  location VARCHAR(255),
  walk_in_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create services
CREATE TABLE services (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  limited_quota BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create users
CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  password_hash CHAR(64) NOT NULL,
  full_name VARCHAR(255),
  email VARCHAR(255),
  role VARCHAR(50) NOT NULL,
  clinic_id BIGINT,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_users_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create timeslots
CREATE TABLE timeslots (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  clinic_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  slot_date DATE NOT NULL,
  start_time TIME,
  end_time TIME,
  capacity INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_timeslots_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE,
  CONSTRAINT fk_timeslots_service FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create appointments
CREATE TABLE appointments (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  patient_id BIGINT NOT NULL,
  clinic_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  timeslot_id BIGINT NOT NULL,
  status VARCHAR(50) NOT NULL,
  reason TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_appointments_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE,
  CONSTRAINT fk_appointments_service FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE,
  CONSTRAINT fk_appointments_timeslot FOREIGN KEY (timeslot_id) REFERENCES timeslots(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create queue_tickets
CREATE TABLE queue_tickets (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  patient_id BIGINT NOT NULL,
  clinic_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  queue_date DATE NOT NULL,
  queue_number INT NOT NULL,
  status VARCHAR(50) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_queue_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_queue_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE,
  CONSTRAINT fk_queue_service FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create notifications
CREATE TABLE notifications (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(255),
  body TEXT,
  type VARCHAR(50),
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create policy_settings
CREATE TABLE policy_settings (
  setting_key VARCHAR(100) NOT NULL PRIMARY KEY,
  setting_value VARCHAR(1000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create audit_logs
CREATE TABLE audit_logs (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  staff_id BIGINT,
  action VARCHAR(255) NOT NULL,
  details TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_audit_staff FOREIGN KEY (staff_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sample data
INSERT INTO clinics (name, location, walk_in_enabled) VALUES
('Chai Wan Clinic', 'Chai Wan', TRUE),
('Tseung Kwan O Clinic', 'Tseung Kwan O', TRUE),
('Sha Tin Clinic', 'Sha Tin', TRUE),
('Tuen Mun Clinic', 'Tuen Mun', FALSE),
('Tsing Yi Clinic', 'Tsing Yi', TRUE);

INSERT INTO services (name, description, limited_quota) VALUES
('General Consultation', 'Primary doctor consultation', FALSE),
('Vaccination', 'Vaccination and follow-up', TRUE),
('Health Screening', 'Basic health screening package', TRUE);

INSERT INTO users (username, password_hash, full_name, email, role, clinic_id, active) VALUES
('admin', SHA2('123123', 256), 'System Administrator', 'admin@cchc.hk', 'ADMIN', NULL, TRUE),
('staff1', SHA2('123123', 256), 'Front Desk Staff', 'staff1@cchc.hk', 'STAFF', 1, TRUE),
('staff2', SHA2('123123', 256), 'Nursing Staff', 'staff2@cchc.hk', 'STAFF', 2, TRUE),
('staff3', SHA2('123123', 256), 'Registration Staff', 'staff3@cchc.hk', 'STAFF', 3, TRUE),
('staff4', SHA2('123123', 256), 'Billing Staff', 'staff4@cchc.hk', 'STAFF', 4, TRUE),
('staff5', SHA2('123123', 256), 'Queue Supervisor', 'staff5@cchc.hk', 'STAFF', 5, TRUE),
('patient1', SHA2('123123', 256), 'Demo Patient 1', 'patient1@example.com', 'PATIENT', NULL, TRUE),
('patient2', SHA2('123123', 256), 'Demo Patient 2', 'patient2@example.com', 'PATIENT', NULL, TRUE),
('patient3', SHA2('123123', 256), 'Demo Patient 3', 'patient3@example.com', 'PATIENT', NULL, TRUE),
('patient4', SHA2('123123', 256), 'Demo Patient 4', 'patient4@example.com', 'PATIENT', NULL, TRUE),
('patient5', SHA2('123123', 256), 'Demo Patient 5', 'patient5@example.com', 'PATIENT', NULL, TRUE),
('patient6', SHA2('123123', 256), 'Demo Patient 6', 'patient6@example.com', 'PATIENT', NULL, TRUE),
('patient7', SHA2('123123', 256), 'Demo Patient 7', 'patient7@example.com', 'PATIENT', NULL, TRUE),
('patient8', SHA2('123123', 256), 'Demo Patient 8', 'patient8@example.com', 'PATIENT', NULL, TRUE),
('patient9', SHA2('123123', 256), 'Demo Patient 9', 'patient9@example.com', 'PATIENT', NULL, TRUE),
('patient10', SHA2('123123', 256), 'Demo Patient 10', 'patient10@example.com', 'PATIENT', NULL, TRUE),
('patient11', SHA2('123123', 256), 'Demo Patient 11', 'patient11@example.com', 'PATIENT', NULL, TRUE),
('patient12', SHA2('123123', 256), 'Demo Patient 12', 'patient12@example.com', 'PATIENT', NULL, TRUE),
('patient13', SHA2('123123', 256), 'Demo Patient 13', 'patient13@example.com', 'PATIENT', NULL, TRUE),
('patient14', SHA2('123123', 256), 'Demo Patient 14', 'patient14@example.com', 'PATIENT', NULL, TRUE),
('patient15', SHA2('123123', 256), 'Demo Patient 15', 'patient15@example.com', 'PATIENT', NULL, TRUE),
('patient16', SHA2('123123', 256), 'Demo Patient 16', 'patient16@example.com', 'PATIENT', NULL, TRUE),
('patient17', SHA2('123123', 256), 'Demo Patient 17', 'patient17@example.com', 'PATIENT', NULL, TRUE),
('patient18', SHA2('123123', 256), 'Demo Patient 18', 'patient18@example.com', 'PATIENT', NULL, TRUE),
('patient19', SHA2('123123', 256), 'Demo Patient 19', 'patient19@example.com', 'PATIENT', NULL, TRUE),
('patient20', SHA2('123123', 256), 'Demo Patient 20', 'patient20@example.com', 'PATIENT', NULL, TRUE);

INSERT INTO timeslots (clinic_id, service_id, slot_date, start_time, end_time, capacity) VALUES
(1, 1, '2026-04-23', '09:00:00', '09:45:00', 8),
(1, 2, '2026-04-23', '10:00:00', '10:45:00', 5),
(1, 3, '2026-04-23', '11:00:00', '11:45:00', 5),
(2, 1, '2026-04-23', '09:00:00', '09:45:00', 8),
(2, 2, '2026-04-23', '10:00:00', '10:45:00', 5),
(2, 3, '2026-04-23', '11:00:00', '11:45:00', 5),
(3, 1, '2026-04-24', '09:00:00', '09:45:00', 8),
(3, 2, '2026-04-24', '10:00:00', '10:45:00', 5),
(3, 3, '2026-04-24', '11:00:00', '11:45:00', 5),
(4, 1, '2026-04-24', '14:00:00', '14:45:00', 8),
(4, 2, '2026-04-24', '15:00:00', '15:45:00', 5),
(4, 3, '2026-04-24', '16:00:00', '16:45:00', 5),
(5, 1, '2026-04-25', '09:00:00', '09:45:00', 8),
(5, 2, '2026-04-25', '10:00:00', '10:45:00', 5),
(5, 3, '2026-04-25', '11:00:00', '11:45:00', 5);

INSERT INTO appointments (patient_id, clinic_id, service_id, timeslot_id, status, reason, created_at) VALUES
(7, 1, 1, 1, 'BOOKED', 'Annual checkup', NOW()),
(8, 1, 2, 2, 'COMPLETED', 'Vaccination follow-up', NOW()),
(9, 1, 3, 3, 'BOOKED', 'Health screening', NOW()),
(10, 2, 1, 4, 'CANCELLED', 'Rescheduled by patient', NOW()),
(11, 2, 2, 5, 'BOOKED', 'Booster shot', NOW()),
(12, 2, 3, 6, 'NO_SHOW', 'Missed appointment', NOW());

INSERT INTO queue_tickets (patient_id, clinic_id, service_id, queue_date, queue_number, status, created_at) VALUES
(13, 3, 1, '2026-04-23', 1, 'WAITING', NOW()),
(14, 3, 2, '2026-04-23', 2, 'CALLED', NOW()),
(15, 4, 3, '2026-04-23', 1, 'SERVED', NOW()),
(16, 4, 1, '2026-04-24', 3, 'WAITING', NOW()),
(17, 5, 2, '2026-04-24', 1, 'WAITING', NOW()),
(18, 5, 3, '2026-04-24', 2, 'EXPIRED', NOW());

INSERT INTO notifications (user_id, title, body, type, is_read, created_at) VALUES
(7, 'Appointment Confirmed', 'Your demo appointment has been confirmed.', 'INFO', FALSE, NOW()),
(8, 'Vaccination Reminder', 'Please arrive 10 minutes early for your vaccination slot.', 'REMINDER', FALSE, NOW()),
(9, 'Screening Reminder', 'Bring your health record to the screening appointment.', 'REMINDER', FALSE, NOW()),
(2, 'Roster Updated', 'Front desk duty roster for the week has been published.', 'SYSTEM', FALSE, NOW()),
(3, 'Queue Summary Ready', 'Queue summary is available for review.', 'SYSTEM', FALSE, NOW()),
(13, 'Queue Update', 'Your walk-in number is now being processed.', 'INFO', FALSE, NOW());

INSERT INTO policy_settings (setting_key, setting_value) VALUES
('max_active_bookings', '3'),
('cancellation_cutoff_hours', '4'),
('queue_enabled', 'true');

INSERT INTO audit_logs (staff_id, action, details, created_at) VALUES
(2, 'CREATE_USER', 'username=patient1; status=created demo account', NOW()),
(3, 'UPDATE_POLICY', 'setting=queue_enabled; value=true', NOW()),
(4, 'CREATE_TIMESLOT', 'clinic=Sha Tin Clinic; slots=15; date=2026-04-24', NOW()),
(5, 'APPROVE_APPOINTMENT', 'patient=patient2; status=confirmed', NOW()),
(6, 'HANDLE_QUEUE', 'clinic=Tsing Yi Clinic; queue_number=2; status=expired', NOW());

-- Helpful indexes
CREATE INDEX idx_timeslots_clinic_service_date ON timeslots (clinic_id, service_id, slot_date);
CREATE INDEX idx_appointments_patient ON appointments (patient_id);
CREATE INDEX idx_queue_clinic_service_date ON queue_tickets (clinic_id, service_id, queue_date);

-- End of file
