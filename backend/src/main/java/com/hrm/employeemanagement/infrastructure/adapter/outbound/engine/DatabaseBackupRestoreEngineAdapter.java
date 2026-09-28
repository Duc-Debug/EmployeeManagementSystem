package com.hrm.employeemanagement.infrastructure.adapter.outbound.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hrm.employeemanagement.application.port.outbound.DatabaseBackupRestoreEnginePort;
import com.hrm.employeemanagement.domain.backup.BackupType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.ResultSetMetaData;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DatabaseBackupRestoreEngineAdapter implements DatabaseBackupRestoreEnginePort {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupRestoreEngineAdapter.class);

    private static final byte[] MAGIC_HEADER = "ENC_BACKUP_GCM_V1:".getBytes(StandardCharsets.UTF_8);
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String encryptionKey;

    private static final java.util.regex.Pattern IDENTIFIER_PATTERN = java.util.regex.Pattern.compile("^[a-zA-Z0-9_]+$");

    // Ordered list of tables to backup/restore (dependencies handled)
    // Sensitive token and outbox tables are excluded
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
            "notification_digest_items"
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

    public DatabaseBackupRestoreEngineAdapter(
            JdbcTemplate jdbcTemplate,
            @Value("${app.backup.encryption-key:${APP_BACKUP_ENCRYPTION_KEY:${BACKUP_ENCRYPTION_KEY:${jwt.secret:${JWT_SECRET:local-development-backup-aes-key-32-chars-minimum}}}}}") String encryptionKey
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.encryptionKey = encryptionKey;
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

        byte[] jsonBytes = objectMapper.writeValueAsBytes(backupData);
        byte[] encryptedBytes = encrypt(jsonBytes);

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(encryptedBytes);
        }

        return targetFile;
    }

    @Override
    @Transactional
    public void performRestore(File backupFile, BackupType backupType) throws Exception {
        if (!backupFile.exists()) {
            throw new IllegalArgumentException("File sao lưu không tồn tại: " + backupFile.getAbsolutePath());
        }

        byte[] fileBytes;
        try (FileInputStream fis = new FileInputStream(backupFile)) {
            fileBytes = fis.readAllBytes();
        }

        byte[] decryptedBytes = decrypt(fileBytes);

        @SuppressWarnings("unchecked")
        Map<String, Object> backupData = objectMapper.readValue(decryptedBytes, Map.class);

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

    private byte[] getDerivedKey() {
        String key = (encryptionKey != null && !encryptionKey.isBlank())
                ? encryptionKey
                : "default-fallback-backup-encryption-key-32b";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private byte[] encrypt(byte[] plaintext) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(getDerivedKey(), "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

        byte[] ciphertext = cipher.doFinal(plaintext);

        ByteBuffer byteBuffer = ByteBuffer.allocate(MAGIC_HEADER.length + iv.length + ciphertext.length);
        byteBuffer.put(MAGIC_HEADER);
        byteBuffer.put(iv);
        byteBuffer.put(ciphertext);
        return byteBuffer.array();
    }

    private byte[] decrypt(byte[] encryptedData) throws Exception {
        if (isEncrypted(encryptedData)) {
            int ivStart = MAGIC_HEADER.length;
            byte[] iv = Arrays.copyOfRange(encryptedData, ivStart, ivStart + GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(encryptedData, ivStart + GCM_IV_LENGTH, encryptedData.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(getDerivedKey(), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            return cipher.doFinal(ciphertext);
        }
        // Fallback for unencrypted legacy JSON data
        return encryptedData;
    }

    private boolean isEncrypted(byte[] data) {
        if (data == null || data.length < MAGIC_HEADER.length + GCM_IV_LENGTH) {
            return false;
        }
        for (int i = 0; i < MAGIC_HEADER.length; i++) {
            if (data[i] != MAGIC_HEADER[i]) {
                return false;
            }
        }
        return true;
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
