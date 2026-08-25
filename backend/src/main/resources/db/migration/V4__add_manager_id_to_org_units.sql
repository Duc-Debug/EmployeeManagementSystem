-- Bổ sung cột manager_id vào bảng org_units để lưu ID người quản lý đơn vị
ALTER TABLE org_units ADD COLUMN manager_id BIGINT NULL AFTER description;
ALTER TABLE org_units ADD CONSTRAINT fk_org_units_manager FOREIGN KEY (manager_id) REFERENCES employees(id) ON DELETE RESTRICT;