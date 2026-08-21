-- ============================================================
-- FLYWAY MIGRATION V1: USER MANAGEMENT SCHEMA & SEED DATA
-- ============================================================

-- 1. Create Roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL
);

-- 2. Create Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

-- 3. Create Departments table
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    CONSTRAINT fk_departments_parent FOREIGN KEY (parent_id) REFERENCES departments (id)
);

-- 4. Create Employees table
CREATE TABLE IF NOT EXISTS employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE,
    department_id BIGINT,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    is_outsourced BOOLEAN DEFAULT FALSE,
    standard_hours_per_week INT DEFAULT 40,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments (id)
);

-- 5. Create Audit Logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    record_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 6. Seed initial 7 System Roles
INSERT INTO roles (id, code, name) VALUES
(1, 'VT-01', 'Ban giám đốc'),
(2, 'VT-02', 'Quản lý dự án'),
(3, 'VT-03', 'Quản lý nguồn lực'),
(4, 'VT-04', 'Nhân viên chuyên môn'),
(5, 'VT-05', 'Nhân sự'),
(6, 'VT-06', 'Quản trị viên'),
(7, 'VT-07', 'Nhân viên công ty');
