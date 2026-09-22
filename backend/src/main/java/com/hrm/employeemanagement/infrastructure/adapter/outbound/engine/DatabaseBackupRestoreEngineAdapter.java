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

    // Ordered list of tables to backup/restore (dependencies handled)
    private static final List<String> FULL_BACKUP_TABLES = List.of(
            "roles",
            "permissions",
            "role_permissions",
            "org_units",
            "users",
            "user_roles",
            "skills",
            "skill_groups",
            "employee_skills",
            "project_templates",
            "project_template_tasks",
            "projects",
            "project_roles",
            "project_role_skills",
            "tasks",
            "task_dependencies",
            "project_milestones",
            "project_resource_demands",
            "weekly_availabilities",
            "weekly_project_allocations",
            "allocation_change_logs",
            "allocation_planning_periods",
            "allocation_plan_snapshots",
            "allocation_plan_snapshot_items",
            "capacity_threshold_configs",
            "role_allocation_templates",
            "role_allocation_template_items",
            "timesheets",
            "timesheet_entries",
            "timesheet_histories",
            "timesheet_audit_logs",
            "standard_work_week_configs",
            "org_unit_work_week_configs",
            "employee_schedule_confirmations",
            "schedule_conflict_warnings",
            "resource_reservations"
    );

    private static final List<String> RESOURCE_PLAN_TABLES = List.of(
            "projects",
            "project_roles",
            "project_role_skills",
            "tasks",
            "task_dependencies",
            "project_milestones",
            "project_resource_demands",
            "weekly_availabilities",
            "weekly_project_allocations",
            "allocation_change_logs",
            "allocation_planning_periods",
            "allocation_plan_snapshots",
            "allocation_plan_snapshot_items",
            "capacity_threshold_configs",
            "role_allocation_templates",
            "role_allocation_template_items",
            "timesheets",
            "timesheet_entries",
            "timesheet_histories",
            "employee_schedule_confirmations",
            "schedule_conflict_warnings",
            "resource_reservations"
    );

    public DatabaseBackupRestoreEngineAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
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

        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, Object>>> tablesData = (Map<String, List<Map<String, Object>>>) backupData.get("tables");
        if (tablesData == null || tablesData.isEmpty()) {
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
        try {
            jdbcTemplate.execute("SELECT 1 FROM " + tableName + " LIMIT 1");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private List<Map<String, Object>> extractTableData(String tableName) {
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
        jdbcTemplate.execute("DELETE FROM " + tableName);
    }

    private void insertTableData(String tableName, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return;

        Map<String, Object> firstRow = rows.get(0);
        List<String> columns = new ArrayList<>(firstRow.keySet());

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
                placeholders.append(", ");
            }
            sql.append(columns.get(i));
            placeholders.append("?");
        }
        sql.append(") VALUES (").append(placeholders).append(")");

        String insertSql = sql.toString();

        List<Object[]> batchArgs = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object[] args = new Object[columns.size()];
            for (int i = 0; i < columns.size(); i++) {
                args[i] = row.get(columns.get(i));
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
