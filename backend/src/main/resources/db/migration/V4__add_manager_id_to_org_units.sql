-- Bổ sung cột manager_id vào bảng org_units để lưu ID người quản lý đơn vị (Tương thích cả MySQL và H2)
ALTER TABLE org_units ADD COLUMN manager_id BIGINT NULL;
