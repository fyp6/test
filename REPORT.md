# Enterprise Systems Development (ITP4511) Coursework Report

## Front Page
- Course Title: Higher Diploma in Software Engineering (IT114105)
- Module: Enterprise Systems Development (ITP4511)
- Project: CCHC Community Clinic Appointment and Queue System (Prototype)
- Group No: [Fill in]
- Student 1 Name / ID: [Fill in]
- Student 2 Name / ID: [Fill in]
- Submission Week: 35

## 1. Assumptions and Requirements
### 1.1 Assumptions
- One prototype deployment with shared central database.
- Each staff account is optionally linked to one clinic.
- Queue is same-day only.
- Patient can only self-manage own appointments.
- Admin policy values are applied globally.

### 1.2 User Roles and Main Use Cases
- Patient: register, login, view clinics/services, book/reschedule/cancel appointments, join queue, view queue status, receive notifications, update profile.
- Staff: view daily appointments, approve/reject limited-quota booking requests, update attendance (arrived/completed/no-show), call next queue ticket, skip/serve/expire ticket, report operational issues.
- Admin: manage users, manage clinics/services/opening hours, view reports, review incident logs, review staff issue reports, configure policy settings (extra feature).

### 1.3 Functional Requirements Mapping
- Appointment booking/management: implemented in Appointment module, including staff approval/rejection workflow for limited-quota services.
- Walk-in queue management: implemented in Queue module.
- Notifications (3+ types): appointment confirmed/updated/cancelled, queue called/skipped/expired, upcoming appointment reminders.
- Staff issue reporting: operational incidents are submitted by staff and stored in audit logs for admin review.
- Analytics/reporting: appointment records, utilization rate, no-show summary.
- Account and role management: registration/login/session RBAC/user admin/profile management.
- Clinic/service/opening-hours management: implemented in Admin catalog page.
- Extra feature selected: admin policy settings page.

## 2. Sitemap
- /login
- /register
- /dashboard
- /appointments
- /queue
- /profile
- /admin/users
- /admin/catalog
- /admin/reports
- /admin/audit
- /admin/policy
- /issues
- /logout

Navigation notes:
- All authenticated users can access dashboard, appointments, queue, profile.
- Staff and admin can access the operational issue reporting page.
- Only admin can access /admin/*.

## 3. MVC Architecture
### 3.1 Controller (Servlet)
- AuthServlet: login/register/logout.
- DashboardServlet: role-based dashboard loading.
- AppointmentServlet: book, reschedule, cancel, attendance updates.
- AppointmentServlet: book, reschedule, cancel, approval/rejection, attendance updates.
- QueueServlet: join queue and queue progression.
- IssueReportServlet: staff operational issue reporting.
- ProfileServlet: profile update and password change.
- AdminServlet: users, catalog, reports, audit logs, policy pages.
- NotificationServlet: mark notifications read.

### 3.2 Model (JavaBean + DAO)
- JavaBeans: UserBean, ClinicBean, ServiceBean, TimeslotBean, AppointmentBean, QueueTicketBean, NotificationBean, PolicyBean.
- DAO classes: UserDao, ClinicDao, ServiceDao, AppointmentDao, QueueDao, NotificationDao, PolicyDao, ReportDao, AuditDao.
- Persistence: JDBC with MySQL database.

### 3.3 View (JSP + Custom Tag)
- JSP pages under WEB-INF/views.
- JSP Action usage: <jsp:useBean> and <jsp:setProperty> in login/register pages.
- Custom Taglib: cchc:roleBadge for role display.

### 3.4 Security
- AuthFilter protects restricted pages.
- Session-based login control.
- Role-based authorization for admin pages and staff operations.

## 4. Database Structure
### 4.1 Main Tables
- clinics(id, name, location, walk_in_enabled)
- services(id, name, description, limited_quota)
- users(id, username, password_hash, full_name, email, role, clinic_id, active)
- timeslots(id, clinic_id, service_id, slot_date, start_time, end_time, capacity)
- appointments(id, patient_id, clinic_id, service_id, timeslot_id, status, reason, created_at)
- queue_tickets(id, patient_id, clinic_id, service_id, queue_date, queue_number, status, created_at)
- notifications(id, user_id, title, body, type, is_read, created_at)
- policy_settings(setting_key, setting_value)
- audit_logs(id, staff_id, action, details, created_at)

### 4.2 ERD (text)
- users (patient/staff/admin) -> appointments (1-to-many)
- clinics -> timeslots (1-to-many)
- services -> timeslots (1-to-many)
- timeslots -> appointments (1-to-many)
- users (patient) -> queue_tickets (1-to-many)
- users -> notifications (1-to-many)

## 5. Major Characteristics and Design
- Centralized booking and queue workflow across all clinics.
- Capacity and duplicate booking checks in DAO transaction logic.
- Queue progression methods: call next, skip, served, expired.
- Policy-driven behavior:
  - max active bookings
  - cancellation/reschedule cutoff hours
  - queue enable/disable
- Staff and admin operational actions are written to audit log.
- Staff operational issues are written to audit log and surfaced in admin incident logs.
- Notification-driven UX after key events.

## 6. Project Schedule
| Week | Task |
|---|---|
| 30 | Requirements analysis, assumptions, ERD draft |
| 31 | MVC skeleton, JDBC setup, schema initialization |
| 32 | Authentication, session checks, role controls |
| 33 | Appointment and queue modules |
| 34 | Notifications, reporting, admin policy page |
| 35 | Testing, bug fixes, documentation, demo preparation |

## 7. Conclusions
This prototype demonstrates how Jakarta EE enterprise web technologies can centralize clinic appointment and queue management while preserving secure role-based access. The system improves visibility of bookings and queue flow, reduces manual operations, and provides basic analytics to support service accountability.

## 8. Skill Checklist
- [x] JSP/Servlet dynamic pages
- [x] Browser input handling via Servlet
- [x] JSP Action tags (<jsp:useBean>, <jsp:setProperty>)
- [x] Custom Taglib (role badge)
- [x] JavaBeans
- [x] JDBC database connection
- [x] Session checking
- [x] Login control
- [x] MVC model application

## 9. Demonstration Checklist
- Start server and deploy WAR.
- Login as admin/staff/patient demo accounts.
- Show booking flow and queue flow.
- Show notifications and role-based dashboard.
- Show reports and policy update.
- Explain MVC file structure and DAO SQL logic.
