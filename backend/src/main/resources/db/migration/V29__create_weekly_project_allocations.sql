CREATE TABLE IF NOT EXISTS weekly_project_allocations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    employee_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    year_number INT NOT NULL,
    week_number INT NOT NULL,
    allocated_hours DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_emp_proj_year_week UNIQUE (employee_id, project_id, year_number, week_number),
    CONSTRAINT chk_wpa_allocated_hours CHECK (allocated_hours >= 0.00),
    CONSTRAINT fk_wpa_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_wpa_project FOREIGN KEY (project_id) REFERENCES projects(id)
);
