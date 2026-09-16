-- ============================================================
-- FLYWAY MIGRATION V83: ALLOW OVERLOAD ALLOCATION PERCENTAGE
-- Epic: NCL-06 (Phan bo nguon luc theo tuan)
-- Story: NCL-06-CN-003 & NCL-06-CN-007
-- Cho phep ty le phan tram phan bo vuot qua 100% (toi da 200%)
-- khi duoc phe duyet vuot tai theo quy tac QTN-11
-- ============================================================

ALTER TABLE weekly_project_allocations 
DROP CHECK chk_wpa_allocation_percentage;

ALTER TABLE weekly_project_allocations 
ADD CONSTRAINT chk_wpa_allocation_percentage 
CHECK (allocation_percentage IS NULL OR (allocation_percentage >= 0.00 AND allocation_percentage <= 200.00));