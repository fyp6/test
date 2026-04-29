# CCHC Community Clinic Appointment and Queue System (Prototype)

This project is a Jakarta EE 10 MVC web application for ITP4511 coursework.

## Tech Stack
- Jakarta EE 10 (Servlet/JSP)
- JavaBeans + DAO (MVC)
- JDBC + MySQL
- Session-based login and role access control
- Custom JSP Taglib (`cchc:roleBadge`)

## Default Accounts
- All seeded demo accounts use password `123123`.
- Admin: `admin`
- Staff: `staff1` to `staff5`
- Patient: `patient1` to `patient20`

## Main Modules
- Login/Registration/Profile management
- Appointment booking/reschedule/cancel/attendance
- Staff booking approval/rejection for limited-quota services
- Staff operational issue reporting
- Walk-in queue join/call/skip/served/expired
- Role-based notifications
- Admin user management
- Admin clinic/service/opening-hours catalog management
- Admin reporting (records, utilization, no-show summary)
- Admin incident log review
- Extra feature: Admin policy settings

## Database Initialization
- The app uses MySQL database `cchc_clinic`.
- On startup, `DBUtil` creates database `cchc_clinic` automatically if it does not exist.
- On startup, `AppBootstrapListener` calls `SchemaInitializer` to create tables and seed default data.

Startup with XAMPP MySQL
- Start XAMPP and enable MySQL (MariaDB).
- Ensure username/password in `DBUtil` match your local MySQL account.
- In NetBeans, run/deploy the project on GlassFish.

## Submission Files
- `REPORT.md` contains required report sections.
- `WORK_BREAKDOWN.md` contains student workload table.

## Notes Before Submission
- Fill in student names/IDs/group number in report.
- Update work breakdown with actual member contributions.
- Demonstrate key role-based workflows in lab session.


要求
System Users and Roles   
The implementation of the CCHC Community Clinic Appointment & Queue System aims to support the 
following users: Patients, Clinic Staff, and Administrators. Public users will be required to register as patients. 
Clinic Staff and Administrators will be created by the system administrator. 
Patients use the system to:  
• Public users must register before accessing patient functions. 
• Register/log in 
• View clinic services and available timeslots 
• Book, reschedule, and cancel appointments 
• Join a same-day walk-in queue (if the clinic accepts walk-ins) 
• View personal booking records and queue status 
• Receive system notifications (e.g., appointment confirmed, reminder, cancellation outcome, 
missed appointment) 
• Update password and personal profile information 
1 
Clinic Staff (Front Desk / Nurse)  use the system to: 
• View daily appointment lists and walk-in queues by clinic and service 
• Approve or reject certain booking requests (e.g., limited quota services) 
• Manage check-in (arrived / no-show) and queue progression (call next, skip, completed) 
• Record basic visit outcomes (e.g., “Completed”, “No-show”, “Cancelled by clinic”) 
• Report operational issues (e.g., doctor unavailable, service suspended) 
Administrator (System Administrator / Clinic Manager) 
• Create and manage user accounts (Patient / Staff / Admin) 
• Configure clinics, services, opening hours, and capacity rules (quota per service per timeslot) 
• View analytics and generate reports (per clinic/service/month) 
• Review incident logs (e.g., repeated no-shows, frequent cancellations) 
• Enforce policy settings (e.g., max active bookings per patient) 
Core Function Requirements 
A. Appointment Booking and Management 
1. Clinic & Service Listing 
o Display list of clinics and services 
o Filter by clinic, service type, date, and availability 
2. Book Appointments 
o Patients select a clinic, service, date, and available time slot 
o System prevents double bookings (per patient and per slot capacity) 
o Booking confirmation page and record saved 
3. Rescheduling and Cancellation 
o Patients reschedule within allowed rules (e.g., before cutoff time) 
o Patients cancel bookings, freeing the slot 
o Staff can cancel on behalf of the clinic (with reason stored) 
4. Check-in and Attendance 
o Staff can mark “Arrived”, “Completed”, or “No-show” 
o Patients can view appointment status 
B. Walk-in Queue Management 
1. Join Queue 
o Patients join same-day queue for a clinic/service if enabled 
o System assigns a queue number and estimated waiting time (simple estimate acceptable) 
2. Queue Progression 
o Staff call next, skip, or mark as served 
o Patients view live (or near real-time) queue status (refresh-based acceptable) 
3. Queue Rules 
2 
o Prevent multiple active queue tickets for the same patient in the same clinic/service/day 
C. Notifications 
1. Implement at least three types of notifications shown after login : 
o Appointment confirmed or updated 
o Appointment or clinic cancelled 
o Reminder for upcoming appointment 
o Queue called, skipped, or expired (any relevant set) 
Ensure notifications are role-specific (Patient vs Staff vs Admin dashboards) 
D. Analytics / Reporting (for Administrators) 
1. View appointment records with filters 
o e.g. Filter by clinic, service, month/year, status (completed, no-show, cancelled) 
2. Utilisation Rate 
o Booking Rate by clinic and service (month/year) 
o Example: booked slots ÷ total offered slots for selected clinic/service/month 
3. No-show Summary 
o Count of no-shows by clinic/service/month (simple table acceptable) 
o Account and Role Management 
E. Account and Role Management  
1. User Management (Admin) 
o List users 
o Create, edit, or delete users 
o Assign roles and clinic links for staff (if applicable) 
2. Profile Management (All roles) 
o Update passwords and personal details 
3. Login Control 
o Role-based access to pages/features 
o Session management to secure restricted pages 
F. Extra Feature 
You are expected to work on one of the extra features: 
o Batch import services/timeslots/users (CSV upload) 
o Basic charts for utilisation/no-shows  
o Admin policy settings page (max bookings, cancellation cutoff, queue enable/disable) 
o Email-style formatting of notifications  
o Audit trail table for staff actions  