-- Migration V44: Ensure is_primary column on task_assignments is updated and consistent
-- Note: is_primary was already added in V43 with DEFAULT FALSE. This migration ensures data consistency across all rows.
UPDATE task_assignments 
SET is_primary = FALSE 
WHERE is_primary IS NULL;

