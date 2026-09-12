-- Repairs databases where V61 was recorded in Flyway history but its DDL was
-- not applied completely.  Each check makes this safe for new installations,
-- where V61 has already created the column or constraint.

DELIMITER //

CREATE PROCEDURE repair_weekly_allocation_overload_schema()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'weekly_project_allocations'
          AND column_name = 'is_overloaded'
    ) THEN
        ALTER TABLE weekly_project_allocations
            ADD COLUMN is_overloaded BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'weekly_project_allocations'
          AND column_name = 'overload_reason'
    ) THEN
        ALTER TABLE weekly_project_allocations
            ADD COLUMN overload_reason VARCHAR(1000) NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'weekly_project_allocations'
          AND column_name = 'overload_approved_by'
    ) THEN
        ALTER TABLE weekly_project_allocations
            ADD COLUMN overload_approved_by BIGINT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'weekly_project_allocations'
          AND column_name = 'overload_approved_at'
    ) THEN
        ALTER TABLE weekly_project_allocations
            ADD COLUMN overload_approved_at TIMESTAMP NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'weekly_project_allocations'
          AND constraint_name = 'fk_wpa_overload_approver'
    ) THEN
        ALTER TABLE weekly_project_allocations
            ADD CONSTRAINT fk_wpa_overload_approver
                FOREIGN KEY (overload_approved_by) REFERENCES users(id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'weekly_project_allocations'
          AND constraint_name = 'chk_overload_tracking'
    ) THEN
        ALTER TABLE weekly_project_allocations
            ADD CONSTRAINT chk_overload_tracking
                CHECK (
                    is_overloaded = FALSE
                    OR (
                        is_overloaded = TRUE
                        AND overload_reason IS NOT NULL AND CHAR_LENGTH(TRIM(overload_reason)) > 0
                        AND overload_approved_by IS NOT NULL
                        AND overload_approved_at IS NOT NULL
                    )
                );
    END IF;
END //

CALL repair_weekly_allocation_overload_schema() //
DROP PROCEDURE repair_weekly_allocation_overload_schema //

DELIMITER ;
