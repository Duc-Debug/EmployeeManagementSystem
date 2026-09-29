package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Flyway Java Migration V128: Fix Duplicate Schedule Conflict Index.
 * Safely removes duplicate index uk_conflict_emp_year_week_type on schedule_conflict_warnings if present.
 */
public class V128__FixDuplicateScheduleConflictIndex extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        DatabaseMetaData metaData = connection.getMetaData();
        String databaseProductName = metaData.getDatabaseProductName().toLowerCase();

        if (databaseProductName.contains("h2")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE schedule_conflict_warnings DROP CONSTRAINT IF EXISTS uk_conflict_emp_year_week_type");
                statement.execute("DROP INDEX IF EXISTS uk_conflict_emp_year_week_type");
            }
            return;
        }

        boolean indexExists = false;
        try (ResultSet rs = metaData.getIndexInfo(null, null, "schedule_conflict_warnings", false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if ("uk_conflict_emp_year_week_type".equalsIgnoreCase(indexName)) {
                    indexExists = true;
                    break;
                }
            }
        }
        if (!indexExists) {
            try (ResultSet rs = metaData.getIndexInfo(null, null, "SCHEDULE_CONFLICT_WARNINGS", false, false)) {
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    if ("uk_conflict_emp_year_week_type".equalsIgnoreCase(indexName)) {
                        indexExists = true;
                        break;
                    }
                }
            }
        }

        if (indexExists) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE schedule_conflict_warnings DROP INDEX uk_conflict_emp_year_week_type");
            }
        }
    }
}
