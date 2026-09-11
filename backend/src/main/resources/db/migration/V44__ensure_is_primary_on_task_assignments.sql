-- Migration V44: Ensure is_primary column exists on task_assignments
SET @dbname = DATABASE();
SET @tablename = "task_assignments";
SET @columnname = "is_primary";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  "SELECT 1",
  "ALTER TABLE task_assignments ADD COLUMN is_primary BOOLEAN NOT NULL DEFAULT FALSE"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

