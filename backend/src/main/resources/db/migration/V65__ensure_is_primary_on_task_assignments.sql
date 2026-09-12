-- Migration V65: Ensure is_primary column on task_assignments is updated and consistent
-- Note: is_primary was already added in V64 with DEFAULT FALSE. This migration ensures data consistency across all rows.
UPDATE task_assignments 
SET is_primary = FALSE 
WHERE is_primary IS NULL;

