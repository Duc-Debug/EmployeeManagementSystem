package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * FLYWAY MIGRATION V128: Fix duplicate schedule conflict index.
 * Safely removes redundant unique index uk_conflict_emp_year_week_type across
 * MySQL, H2, and other database engines if present, while preserving canonical
 * uk_schedule_conflict_existing.
 */
public class V128__fix_duplicate_schedule_conflict_index extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        DatabaseMetaData metaData = connection.getMetaData();
        String dbProduct = metaData.getDatabaseProductName() != null 
                ? metaData.getDatabaseProductName().toLowerCase() 
                : "";

        try (Statement statement = connection.createStatement()) {
            if (dbProduct.contains("h2")) {
                try {
                    statement.execute("ALTER TABLE schedule_conflict_warnings DROP CONSTRAINT IF EXISTS uk_conflict_emp_year_week_type");
                } catch (Exception ignored) {}
                try {
                    statement.execute("DROP INDEX IF EXISTS uk_conflict_emp_year_week_type");
                } catch (Exception ignored) {}
            } else {
                try {
                    statement.execute("ALTER TABLE schedule_conflict_warnings DROP INDEX uk_conflict_emp_year_week_type");
                } catch (SQLException e) {
                    // MySQL error 1091: Can't DROP 'uk_conflict_emp_year_week_type'; check that column/key exists
                    if (e.getErrorCode() != 1091 && !e.getMessage().toLowerCase().contains("can't drop")) {
                        throw e;
                    }
                }
            }
        }
    }
}
