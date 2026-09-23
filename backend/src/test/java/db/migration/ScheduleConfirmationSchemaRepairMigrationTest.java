package db.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleConfirmationSchemaRepairMigrationTest {
    @Test
    void repairsLegacySchemaPreservesConfirmationsAndAllowsFeedbackWithoutConfirmation() throws Exception {
        try (Connection connection = legacyDatabase()) {
            execute(connection, "INSERT INTO employee_schedule_confirmation (user_id, week_start_date, confirmed_at) VALUES (20, '2026-09-21', '2026-09-22 10:00:00')");
            assertThatThrownBy(() -> count(connection, "SELECT COUNT(feedback_note) FROM employee_schedule_confirmation"))
                    .isInstanceOf(SQLException.class);

            migrate(connection);

            assertThat(count(connection, "SELECT COUNT(*) FROM employee_schedule_confirmation WHERE user_id=20 AND confirmed_at='2026-09-22 10:00:00' AND confirmation_status='CONFIRMED'"))
                    .isEqualTo(1);
            execute(connection, "INSERT INTO employee_schedule_confirmation (user_id, week_start_date, confirmed_at, feedback_note, feedback_at, confirmation_status) VALUES (21, '2026-09-21', NULL, 'Schedule conflict', '2026-09-22 12:00:00', 'HAS_FEEDBACK')");
            assertThat(count(connection, "SELECT COUNT(*) FROM employee_schedule_confirmation WHERE confirmed_at IS NULL AND feedback_note='Schedule conflict' AND feedback_at IS NOT NULL AND confirmation_status='HAS_FEEDBACK'"))
                    .isEqualTo(1);
            assertThat(count(connection, "SELECT COUNT(*) FROM permissions")).isEqualTo(2);
            assertThat(count(connection, "SELECT COUNT(*) FROM role_permissions rp JOIN roles r ON r.id=rp.role_id WHERE r.code='VT-02'"))
                    .isEqualTo(2);
            assertThat(count(connection, "SELECT COUNT(*) FROM role_permissions rp JOIN roles r ON r.id=rp.role_id WHERE r.code='CUSTOM'"))
                    .isZero();
        }
    }

    @Test
    void preservesAlreadyUpgradedSchemaFeedbackAndPermissionsOnRerun() throws Exception {
        try (Connection connection = legacyDatabase()) {
            execute(connection, "ALTER TABLE employee_schedule_confirmation MODIFY COLUMN confirmed_at DATETIME(6) NULL");
            execute(connection, "ALTER TABLE employee_schedule_confirmation ADD COLUMN feedback_note TEXT NULL");
            execute(connection, "ALTER TABLE employee_schedule_confirmation ADD COLUMN feedback_at DATETIME(6) NULL");
            execute(connection, "ALTER TABLE employee_schedule_confirmation ADD COLUMN confirmation_status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED'");
            execute(connection, "INSERT INTO employee_schedule_confirmation (user_id, week_start_date, feedback_note, feedback_at, confirmation_status) VALUES (20, '2026-09-21', 'Existing feedback', '2026-09-22 12:00:00', 'HAS_FEEDBACK')");
            execute(connection, "INSERT INTO permissions (code, name, description) VALUES ('MY_ALLOCATION_READ', 'Existing name', 'Existing description')");
            execute(connection, "INSERT INTO role_permissions SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code='VT-02'");

            migrate(connection);
            migrate(connection);

            assertThat(count(connection, "SELECT COUNT(*) FROM employee_schedule_confirmation WHERE feedback_note='Existing feedback' AND confirmed_at IS NULL AND confirmation_status='HAS_FEEDBACK'"))
                    .isEqualTo(1);
            assertThat(count(connection, "SELECT COUNT(*) FROM permissions WHERE name='Existing name'")).isEqualTo(1);
            assertThat(count(connection, "SELECT COUNT(*) FROM permissions")).isEqualTo(2);
            assertThat(count(connection, "SELECT COUNT(*) FROM role_permissions")).isEqualTo(4);
        }
    }

    private Connection legacyDatabase() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:schedule-repair-" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        execute(connection, "CREATE TABLE employee_schedule_confirmation (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, week_start_date DATE NOT NULL, confirmed_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0, UNIQUE(user_id, week_start_date))");
        execute(connection, "CREATE TABLE permissions (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(100) NOT NULL UNIQUE, name VARCHAR(200), description VARCHAR(500))");
        execute(connection, "CREATE TABLE roles (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(30) NOT NULL UNIQUE)");
        execute(connection, "CREATE TABLE role_permissions (role_id BIGINT NOT NULL REFERENCES roles(id), permission_id BIGINT NOT NULL REFERENCES permissions(id), PRIMARY KEY(role_id, permission_id))");
        execute(connection, "INSERT INTO roles (code) VALUES ('VT-02'), ('VT-04'), ('CUSTOM')");
        return connection;
    }

    private void migrate(Connection connection) throws Exception {
        Context context = mock(Context.class);
        when(context.getConnection()).thenReturn(connection);
        new V123__repair_schedule_confirmation_feedback().migrate(context);
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getLong(1);
        }
    }
}
