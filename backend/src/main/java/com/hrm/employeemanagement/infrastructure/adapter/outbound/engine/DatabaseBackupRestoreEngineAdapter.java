package com.hrm.employeemanagement.infrastructure.adapter.outbound.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hrm.employeemanagement.application.port.outbound.DatabaseBackupRestoreEnginePort;
import com.hrm.employeemanagement.domain.backup.BackupType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.ResultSetMetaData;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DatabaseBackupRestoreEngineAdapter implements DatabaseBackupRestoreEnginePort {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupRestoreEngineAdapter.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private static final java.util.regex.Pattern IDENTIFIER_PATTERN = java.util.regex.Pattern.compile("^[a-zA-Z0-9_]+$");

    // Ordered list of tables to backup/restore (dependencies handled)
    public static final List<String> FULL_BACKUP_TABLES = List.of(
            // Core Identity & Organization
            "roles",
            "permissions",
            "role_permissions",
            "org_units",
            "departments",
            "users",
            "employees",
            "audit_logs",
            "password_reset_tokens",
            "password_reset_email_outbox",
            // Skills & Qualifications
            "skill_groups",
            "skills",
            "employee_skills",
            // Calendar, Availability & Leaves
            "working_calendar_configs",
            "holidays",
            "employee_leave_balances",
            "leave_requests",
            "employee_weekly_availabilities",
            "unavailability_declarations",
            // Projects, Templates & WBS
            "project_templates",
            "project_template_tasks",
            "projects",
            "project_members",
            "project_roles",
            "project_role_skills",
            "project_milestones",
            "tasks",
            "task_assignments",
            "task_dependencies",
            "milestone_tasks",
            "task_comments",
            "task_attachments",
            "task_comment_mentions",
            // Resource Demands & Allocations
            "project_resource_demands",
            "weekly_project_allocations",
            "allocation_planning_periods",
            "allocation_plan_snapshots",
            "allocation_plan_snapshot_items",
            "allocation_change_logs",
            "capacity_threshold_configs",
            "project_role_allocation_templates",
            "project_role_allocation_template_items",
            // Timesheets & Work Logs
            "timesheets",
            "timesheet_entries",
            "timesheet_histories",
            "timesheet_audit_logs",
            // Work Week & Schedule Rules
            "standard_work_week_configs",
            "standard_work_week_days",
            "employee_schedule_confirmation",
            "resource_reservations",
            "schedule_conflict_warnings",
            "schedule_conflict_replacements",
            "prolonged_idleness_acknowledgements",
            // Simulation Scenarios
            "simulation_scenarios",
            "resource_scenarios",
            "scenario_demands",
            "scenario_simulated_employees",
            "scenario_allocation_snapshot",
            "scenario_shares",
            // Notification Center
            "notification_preferences",
            "notification_dedup_configs",
            "notification_dedup_config_histories",
            "notification_dedup_records",
            "notification_events",
            "notification_recipients",
            "notifications",
            "notification_audit_logs",
            "notification_digest_items",
            "notification_email_outbox",
            "notification_email_digest_items"
    );

    public static final List<String> RESOURCE_PLAN_TABLES = List.of(
            "project_templates",
            "project_template_tasks",
            "projects",
            "project_members",
            "project_roles",
            "project_role_skills",
            "project_milestones",
            "tasks",
            "task_assignments",
            "task_dependencies",
            "milestone_tasks",
            "task_comments",
            "task_attachments",
            "task_comment_mentions",
            "project_resource_demands",
            "weekly_project_allocations",
            "allocation_planning_periods",
            "allocation_plan_snapshots",
            "allocation_plan_snapshot_items",
            "allocation_change_logs",
            "capacity_threshold_configs",
            "project_role_allocation_templates",
            "project_role_allocation_template_items",
            "timesheets",
            "timesheet_entries",
            "timesheet_histories",
            "timesheet_audit_logs",
            "standard_work_week_configs",
            "standard_work_week_days",
            "employee_schedule_confirmation",
            "resource_reservations",
            "schedule_conflict_warnings",
            "schedule_conflict_replacements",
            "prolonged_idleness_acknowledgements",
            "simulation_scenarios",
            "resource_scenarios",
            "scenario_demands",
            "scenario_simulated_employees",
            "scenario_allocation_snapshot",
            "scenario_shares"
    );

    public DatabaseBackupRestoreEngineAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public List<String> getSupportedTables() {
        return FULL_BACKUP_TABLES;
    }

    @Override
    public File performBackup(File targetFile, BackupType backupType) throws Exception {
        List<String> targetTables = (backupType == BackupType.RESOURCE_PLAN) ? RESOURCE_PLAN_TABLES : FULL_BACKUP_TABLES;

        Map<String, Object> backupData = new LinkedHashMap<>();
        backupData.put("version", "1.0");
        backupData.put("backupType", backupType.name());
        backupData.put("createdAt", LocalDateTime.now().toString());

        Map<String, List<Map<String, Object>>> tablesData = new LinkedHashMap<>();

        List<String> failedTables = new ArrayList<>();
        for (String table : targetTables) {
            try {
                if (tableExists(table)) {
                    List<Map<String, Object>> rows = extractTableData(table);
                    tablesData.put(table, rows);
                }
            } catch (Exception e) {
                log.error("Không thể trích xuất dữ liệu bảng {}: {}", table, e.getMessage(), e);
                failedTables.add(table + " (" + e.getMessage() + ")");
            }
        }

        if (!failedTables.isEmpty()) {
            throw new IllegalStateException("Sao lưu dữ liệu thất bại do lỗi trích xuất các bảng: " + failedTables);
        }

        backupData.put("tables", tablesData);

        if (targetFile.getParentFile() != null) {
            targetFile.getParentFile().mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            objectMapper.writeValue(fos, backupData);
        }

        return targetFile;
    }

    @Override
    @Transactional
    public void performRestore(File backupFile, BackupType backupType) throws Exception {
        if (!backupFile.exists()) {
            throw new IllegalArgumentException("File sao lưu không tồn tại: " + backupFile.getAbsolutePath());
        }

        Map<String, Object> backupData;
        try (FileInputStream fis = new FileInputStream(backupFile)) {
            backupData = objectMapper.readValue(fis, Map.class);
        }

        Object rawType = backupData.get("backupType");
        if (!(rawType instanceof String typeValue)) {
            throw new IllegalArgumentException("Backup file thiếu metadata backupType");
        }

        if (!backupType.name().equalsIgnoreCase(typeValue.trim())) {
            throw new IllegalArgumentException("backupType trong file (" + typeValue + ") không khớp với backupType metadata (" + backupType.name() + ")");
        }

        Object rawTables = backupData.get("tables");
        if (!(rawTables instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Metadata 'tables' trong tệp sao lưu phải là một đối tượng JSON");
        }

        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, Object>>> tablesData = (Map<String, List<Map<String, Object>>>) rawTables;
        if (tablesData.isEmpty()) {
            throw new IllegalStateException("Tệp sao lưu không chứa dữ liệu bảng hợp lệ.");
        }

        // Tạm tắt kiểm tra khóa ngoại (Foreign Key Checks)
        setForeignKeyChecks(false);

        try {
            List<String> restoreOrder = (backupType == BackupType.RESOURCE_PLAN) ? RESOURCE_PLAN_TABLES : FULL_BACKUP_TABLES;

            // Xóa dữ liệu các bảng cần phục hồi theo thứ tự ngược
            for (int i = restoreOrder.size() - 1; i >= 0; i--) {
                String table = restoreOrder.get(i);
                if (tablesData.containsKey(table) && tableExists(table)) {
                    truncateTable(table);
                }
            }

            // Nạp dữ liệu các bảng theo thứ tự xuôi
            for (String table : restoreOrder) {
                List<Map<String, Object>> rows = tablesData.get(table);
                if (rows != null && !rows.isEmpty() && tableExists(table)) {
                    insertTableData(table, rows);
                }
            }
        } finally {
            // Luôn bật lại kiểm tra khóa ngoại
            setForeignKeyChecks(true);
        }
    }

    private boolean tableExists(String tableName) {
        if (!IDENTIFIER_PATTERN.matcher(tableName).matches()) {
            return false;
        }
        try {
            jdbcTemplate.execute("SELECT 1 FROM " + tableName + " LIMIT 1");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Set<String> getTableColumns(String tableName) {
        if (!IDENTIFIER_PATTERN.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Tên bảng không hợp lệ: " + tableName);
        }
        Set<String> columns = new HashSet<>();
        try {
            jdbcTemplate.query("SELECT * FROM " + tableName + " WHERE 1=0", rs -> {
                ResultSetMetaData meta = rs.getMetaData();
                int count = meta.getColumnCount();
                for (int i = 1; i <= count; i++) {
                    columns.add(meta.getColumnLabel(i).toLowerCase(Locale.ROOT));
                }
                return null;
            });
        } catch (Exception e) {
            log.warn("Không thể lấy siêu dữ liệu cột cho bảng {}: {}", tableName, e.getMessage());
        }
        return columns;
    }

    private List<Map<String, Object>> extractTableData(String tableName) {
        if (!IDENTIFIER_PATTERN.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Tên bảng không hợp lệ: " + tableName);
        }
        String sql = "SELECT * FROM " + tableName;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ResultSetMetaData meta = rs.getMetaData();
            int count = meta.getColumnCount();
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= count; i++) {
                String colName = meta.getColumnLabel(i);
                Object val = rs.getObject(i);
                row.put(colName, val);
            }
            return row;
        });
    }

    private void truncateTable(String tableName) {
        if (!IDENTIFIER_PATTERN.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Tên bảng không hợp lệ: " + tableName);
        }
        jdbcTemplate.execute("DELETE FROM " + tableName);
    }

    private void insertTableData(String tableName, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return;
        if (!IDENTIFIER_PATTERN.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Tên bảng không hợp lệ: " + tableName);
        }

        Set<String> allowedColumns = getTableColumns(tableName);
        if (allowedColumns.isEmpty()) {
            log.warn("Bảng {} không có cột hợp lệ hoặc không tồn tại trong DB schema, bỏ qua chèn dữ liệu.", tableName);
            return;
        }

        Map<String, Object> firstRow = rows.get(0);
        List<String> validColumns = new ArrayList<>();
        for (String col : firstRow.keySet()) {
            if (col != null && IDENTIFIER_PATTERN.matcher(col).matches() && allowedColumns.contains(col.toLowerCase(Locale.ROOT))) {
                validColumns.add(col);
            } else {
                log.warn("Cột '{}' bị loại bỏ do không hợp lệ hoặc không nằm trong schema bảng '{}'", col, tableName);
            }
        }

        if (validColumns.isEmpty()) {
            log.warn("Không tìm thấy cột hợp lệ nào cho bảng {}, bỏ qua bản ghi.", tableName);
            return;
        }

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < validColumns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
                placeholders.append(", ");
            }
            sql.append(validColumns.get(i));
            placeholders.append("?");
        }
        sql.append(") VALUES (").append(placeholders).append(")");

        String insertSql = sql.toString();

        List<Object[]> batchArgs = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object[] args = new Object[validColumns.size()];
            for (int i = 0; i < validColumns.size(); i++) {
                args[i] = row.get(validColumns.get(i));
            }
            batchArgs.add(args);
        }

        jdbcTemplate.batchUpdate(insertSql, batchArgs);
    }

    private void setForeignKeyChecks(boolean enabled) {
        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = " + (enabled ? "1" : "0"));
        } catch (Exception e) {
            try {
                jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY " + (enabled ? "TRUE" : "FALSE"));
            } catch (Exception ex) {
                // Ignore for DBs without these commands
            }
        }
    }
}
