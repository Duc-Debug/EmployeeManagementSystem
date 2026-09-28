-- ============================================================
-- FLYWAY MIGRATION V19: CREATE WEEKLY AVAILABILITY AND SUPPORTING TABLES (QTN-10)
-- ============================================================

-- 1. Create holidays table if not exists (Theo BangDb.pdf)
CREATE TABLE IF NOT EXISTS holidays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    holiday_date DATE NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    working_hours_deducted INT NOT NULL DEFAULT 8
);

-- 2. Create leave_requests table if not exists (Theo BangDb.pdf)
CREATE TABLE IF NOT EXISTS leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    hours_deducted DECIMAL(5,2) NOT NULL DEFAULT 8.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_leave_requests_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);

-- Index on leave_requests for fast querying approved leaves within date range
CREATE INDEX idx_leave_requests_emp_status_dates
    ON leave_requests (employee_id, status, start_date, end_date);

-- 3. Create employee_weekly_availabilities table
CREATE TABLE IF NOT EXISTS employee_weekly_availabilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    year_number INT NOT NULL,
    week_number INT NOT NULL,
    standard_hours INT NOT NULL,
    holiday_hours INT NOT NULL DEFAULT 0,
    approved_leave_hours DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    net_available_hours DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_emp_weekly_avail_year_week UNIQUE (employee_id, year_number, week_number),
    CONSTRAINT fk_emp_weekly_avail_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT chk_weekly_avail_standard_hours CHECK (standard_hours > 0 AND standard_hours <= 168),
    CONSTRAINT chk_weekly_avail_net_hours CHECK (net_available_hours >= 0)
);

CREATE INDEX idx_emp_weekly_avail_lookup
    ON employee_weekly_availabilities (employee_id, year_number, week_number);
