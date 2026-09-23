package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Older installations recorded V114 as the schedule table creation migration.
 * Repairing that history did not execute the feedback SQL now assigned to V114.
 * Reconcile both the original schema and databases where V114 already ran.
 */
public class V123__repair_schedule_confirmation_feedback extends BaseJavaMigration {
    private static final String TABLE = "employee_schedule_confirmation";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        Map<String, Integer> columns = readColumns(connection);
        if (!columns.containsKey("confirmed_at")) {
            throw new SQLException("Missing employee_schedule_confirmation.confirmed_at; V113 must run first");
        }

        try (Statement statement = connection.createStatement()) {
            if (columns.get("confirmed_at") == DatabaseMetaData.columnNoNulls) {
                statement.execute("ALTER TABLE employee_schedule_confirmation MODIFY COLUMN confirmed_at DATETIME(6) NULL");
            }
            if (!columns.containsKey("feedback_note")) {
                statement.execute("ALTER TABLE employee_schedule_confirmation ADD COLUMN feedback_note TEXT NULL");
            }
            if (!columns.containsKey("feedback_at")) {
                statement.execute("ALTER TABLE employee_schedule_confirmation ADD COLUMN feedback_at DATETIME(6) NULL");
            }
            if (!columns.containsKey("confirmation_status")) {
                statement.execute("ALTER TABLE employee_schedule_confirmation ADD COLUMN confirmation_status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED'");
            }

            statement.executeUpdate("""
                    INSERT INTO permissions (code, name, description)
                    SELECT 'MY_ALLOCATION_READ', 'Xem phan bo ca nhan', 'Quyen xem lich va phan bo du an ca nhan'
                    WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'MY_ALLOCATION_READ')
                    """);
            statement.executeUpdate("""
                    INSERT INTO permissions (code, name, description)
                    SELECT 'MY_ALLOCATION_CONFIRM', 'Xac nhan phan bo ca nhan', 'Quyen xac nhan hoac phan hoi lich phan bo ca nhan'
                    WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'MY_ALLOCATION_CONFIRM')
                    """);
            statement.executeUpdate("""
                    INSERT INTO role_permissions (role_id, permission_id)
                    SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
                    WHERE r.code IN ('VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05', 'VT-06', 'VT-07')
                      AND p.code IN ('MY_ALLOCATION_READ', 'MY_ALLOCATION_CONFIRM')
                      AND NOT EXISTS (
                        SELECT 1 FROM role_permissions rp
                        WHERE rp.role_id = r.id AND rp.permission_id = p.id
                      )
                    """);
        }
    }

    private Map<String, Integer> readColumns(Connection connection) throws SQLException {
        Map<String, Integer> columns = new HashMap<>();
        // MySQL preserves lowercase names; H2 stores unquoted identifiers uppercase.
        for (String tableName : new String[]{TABLE, TABLE.toUpperCase(Locale.ROOT)}) {
            try (ResultSet metadata = connection.getMetaData().getColumns(
                    connection.getCatalog(), null, tableName, null)) {
                while (metadata.next()) {
                    if (TABLE.equalsIgnoreCase(metadata.getString("TABLE_NAME"))) {
                        columns.put(metadata.getString("COLUMN_NAME").toLowerCase(Locale.ROOT), metadata.getInt("NULLABLE"));
                    }
                }
            }
            if (!columns.isEmpty()) break;
        }
        return columns;
    }
}
