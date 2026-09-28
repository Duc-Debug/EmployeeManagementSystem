-- ============================================================
-- FLYWAY MIGRATION V63: ENFORCE UNIQUE ACTIVE RESOURCE RESERVATIONS
-- Epic: NCL-06 (Phan bo nguon luc theo tuan)
-- Story: NCL-06-CN-005 (Giu cho nguon luc cho du an du kien - QTN-13)
-- ============================================================

-- 1. Tao Generated Column active_flag chi nhan gia tri 1 khi status = 'ACTIVE', nguoc lai la NULL
ALTER TABLE resource_reservations
    ADD COLUMN active_flag TINYINT GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN 1 ELSE NULL END);

-- 2. Tao Unique Index de dam bao moi slot (project, employee, year, week) chi co toi da 1 ban ghi ACTIVE
-- Trong InnoDB MySQL, nhieu dong co gia tri NULL van khong vi pham UNIQUE index
ALTER TABLE resource_reservations
    ADD CONSTRAINT uq_rr_active_slot 
    UNIQUE (project_id, employee_id, year_number, week_number, active_flag);
