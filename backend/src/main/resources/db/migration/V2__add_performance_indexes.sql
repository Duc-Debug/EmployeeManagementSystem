-- ============================================================
-- FLYWAY MIGRATION V2: PERFORMANCE INDEXES
-- ============================================================

-- Optimize employee lookups by department and user
CREATE INDEX idx_employees_department_id ON employees(department_id);
CREATE INDEX idx_employees_user_id ON employees(user_id);

-- Optimize audit log lookups by user, table, record, and creation timestamp
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_table_record ON audit_logs(table_name, record_id);
