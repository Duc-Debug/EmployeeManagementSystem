package com.hrm.employeemanagement.infrastructure.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;

class FlywayEmployeeOrgUnitMigrationTest {

    private static final String MIGRATION_LOCATION =
            "classpath:db/migration";

    @Test
    void migrationsRunOnPlainH2LocalProfileUrl()
            throws Exception {
        String url = plainH2JdbcUrl("local_profile");

        try (Connection keepAlive = connect(url)) {
            migrateToLatest(url);

            try (Connection connection = connect(url)) {
                long companyRootId =
                        queryLong(
                                connection,
                                "SELECT id FROM org_units WHERE unit_code = ?",
                                "COMPANY_ROOT"
                        );
                OrgUnitRow pb01 =
                        queryOrgUnit(connection, "PB-01");

                assertNotEquals(companyRootId, pb01.id());
                assertEquals(
                        "/" + companyRootId + "/" + pb01.id() + "/",
                        pb01.treePath()
                );
            }
        }
    }

    @Test
    void v7BackfillsPb01ByBusinessKeyAndNormalizesSystemAdminScope()
            throws Exception {
        String url = jdbcUrl("v7_pb01");

        try (Connection keepAlive = connect(url)) {
            migrateTo(url, "4");

            try (Connection connection = connect(url)) {
                insertLegacyEmployee(
                        connection,
                        "legacy-pb01-user",
                        "VT-04",
                        "EMP-PB01",
                        "PB-01"
                );
                insertUser(connection, "legacy-admin", "VT-06");
            }

            migrateToLatest(url);

            try (Connection connection = connect(url)) {
                long companyRootId =
                        queryLong(
                                connection,
                                "SELECT id FROM org_units WHERE unit_code = ?",
                                "COMPANY_ROOT"
                        );
                OrgUnitRow pb01 =
                        queryOrgUnit(connection, "PB-01");

                assertEquals("Ban giám đốc", pb01.unitName());
                assertEquals("DEPARTMENT", pb01.unitType());
                assertEquals(companyRootId, pb01.parentId());
                assertEquals(2, pb01.level());
                assertEquals(
                        "/" + companyRootId + "/" + pb01.id() + "/",
                        pb01.treePath()
                );

                long employeeOrgUnitId =
                        queryLong(
                                connection,
                                """
                                        SELECT org_unit_id
                                        FROM employees
                                        WHERE employee_code = ?
                                        """,
                                "EMP-PB01"
                        );

                assertEquals(pb01.id(), employeeOrgUnitId);
                assertNotEquals(companyRootId, employeeOrgUnitId);

                long branchMatchCount =
                        queryLong(
                                connection,
                                """
                                        SELECT COUNT(*)
                                        FROM org_units ou
                                        JOIN org_units scope
                                            ON scope.unit_code = 'COMPANY_ROOT'
                                        WHERE ou.unit_code = 'PB-01'
                                          AND ou.tree_path LIKE CONCAT(scope.tree_path, '%')
                                        """
                        );

                assertEquals(1L, branchMatchCount);
                assertEquals(
                        "COMPANY",
                        queryString(
                                connection,
                                """
                                        SELECT data_scope
                                        FROM users
                                        WHERE username = ?
                                        """,
                                "legacy-admin"
                        )
                );
                assertNull(
                        queryNullableLong(
                                connection,
                                """
                                        SELECT scope_org_unit_id
                                        FROM users
                                        WHERE username = ?
                                        """,
                                "legacy-admin"
                        )
                );
            }
        }
    }

    @Test
    void v7FailsWhenLegacyDepartmentHasNoOrgUnitMapping()
            throws Exception {
        String url = jdbcUrl("v7_missing_mapping");

        try (Connection keepAlive = connect(url)) {
            migrateTo(url, "4");

            try (Connection connection = connect(url)) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(
                            """
                                    INSERT INTO departments (code, name, parent_id)
                                    VALUES ('PB-99', 'Unknown Department', NULL)
                                    """
                    );
                }
                insertLegacyEmployee(
                        connection,
                        "legacy-unknown-user",
                        "VT-04",
                        "EMP-UNKNOWN",
                        "PB-99"
                );
            }

            FlywayException exception =
                    assertThrows(
                            FlywayException.class,
                            () -> migrateToLatest(url)
                    );

            assertTrue(
                    containsMessage(
                            exception,
                            "chk_employees_org_unit_backfilled"
                    )
            );
        }
    }

    private static void migrateTo(String url, String target) {
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations(MIGRATION_LOCATION)
                .target(target)
                .load()
                .migrate();
    }

    private static void migrateToLatest(String url) {
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations(MIGRATION_LOCATION)
                .load()
                .migrate();
    }

    private static String jdbcUrl(String name) {
        return "jdbc:h2:mem:"
                + name
                + "_"
                + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
    }

    private static String plainH2JdbcUrl(String name) {
        return "jdbc:h2:mem:"
                + name
                + "_"
                + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1";
    }

    private static Connection connect(String url)
            throws SQLException {
        return DriverManager.getConnection(url, "sa", "");
    }

    private static void insertUser(
            Connection connection,
            String username,
            String roleCode
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                        INSERT INTO users (
                            username,
                            password_hash,
                            role_id,
                            is_active
                        )
                        SELECT ?, 'hash', id, TRUE
                        FROM roles
                        WHERE code = ?
                        """
        )) {
            statement.setString(1, username);
            statement.setString(2, roleCode);

            assertEquals(1, statement.executeUpdate());
        }
    }

    private static void insertLegacyEmployee(
            Connection connection,
            String username,
            String roleCode,
            String employeeCode,
            String departmentCode
    ) throws SQLException {
        insertUser(connection, username, roleCode);

        try (PreparedStatement statement = connection.prepareStatement(
                """
                        INSERT INTO employees (
                            user_id,
                            department_id,
                            employee_code,
                            full_name
                        )
                        SELECT u.id, d.id, ?, ?
                        FROM users u
                        JOIN departments d
                            ON d.code = ?
                        WHERE u.username = ?
                        """
        )) {
            statement.setString(1, employeeCode);
            statement.setString(2, username);
            statement.setString(3, departmentCode);
            statement.setString(4, username);

            assertEquals(1, statement.executeUpdate());
        }
    }

    private static OrgUnitRow queryOrgUnit(
            Connection connection,
            String unitCode
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                        SELECT id,
                               unit_name,
                               unit_type,
                               parent_id,
                               tree_path,
                               level
                        FROM org_units
                        WHERE unit_code = ?
                        """
        )) {
            statement.setString(1, unitCode);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new AssertionError(
                            "Missing org unit: " + unitCode
                    );
                }

                return new OrgUnitRow(
                        resultSet.getLong("id"),
                        resultSet.getString("unit_name"),
                        resultSet.getString("unit_type"),
                        resultSet.getLong("parent_id"),
                        resultSet.getString("tree_path"),
                        resultSet.getInt("level")
                );
            }
        }
    }

    private static long queryLong(
            Connection connection,
            String sql,
            Object... parameters
    ) throws SQLException {
        Object value = querySingleValue(connection, sql, parameters);

        if (value instanceof Number number) {
            return number.longValue();
        }

        throw new AssertionError("Expected numeric value but got: " + value);
    }

    private static Long queryNullableLong(
            Connection connection,
            String sql,
            Object... parameters
    ) throws SQLException {
        Object value = querySingleValue(connection, sql, parameters);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        throw new AssertionError("Expected numeric value but got: " + value);
    }

    private static String queryString(
            Connection connection,
            String sql,
            Object... parameters
    ) throws SQLException {
        Object value = querySingleValue(connection, sql, parameters);

        return value != null
                ? value.toString()
                : null;
    }

    private static Object querySingleValue(
            Connection connection,
            String sql,
            Object... parameters
    ) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new AssertionError(
                            "Query returned no rows: " + sql
                    );
                }

                return resultSet.getObject(1);
            }
        }
    }

    private static boolean containsMessage(
            Throwable throwable,
            String expectedText
    ) {
        String normalizedExpected =
                expectedText.toLowerCase();

        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && message.toLowerCase().contains(normalizedExpected)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private record OrgUnitRow(
            long id,
            String unitName,
            String unitType,
            long parentId,
            String treePath,
            int level
    ) {
    }
}
