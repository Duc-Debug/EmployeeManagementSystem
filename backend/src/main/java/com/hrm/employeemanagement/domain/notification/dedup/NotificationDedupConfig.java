package com.hrm.employeemanagement.domain.notification.dedup;

import java.time.LocalDateTime;

/**
 * Aggregate Root lưu trữ cấu hình chống gửi trùng thông báo (NCL-11-CN-003).
 */
public class NotificationDedupConfig {

    public static final int MIN_WINDOW_DAYS = 1;
    public static final int MAX_WINDOW_DAYS = 90;
    public static final int MIN_SCAN_INTERVAL_MINUTES = 5;
    public static final int MAX_SCAN_INTERVAL_MINUTES = 1440;

    private Long id;
    private boolean isEnabled;
    private int dedupWindowDays;
    private int scanIntervalMinutes;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Long version;

    public NotificationDedupConfig(
            Long id,
            boolean isEnabled,
            int dedupWindowDays,
            int scanIntervalMinutes,
            Long updatedBy,
            LocalDateTime updatedAt,
            Long version
    ) {
        validateWindowDays(dedupWindowDays);
        validateScanInterval(scanIntervalMinutes);
        this.id = id;
        this.isEnabled = isEnabled;
        this.dedupWindowDays = dedupWindowDays;
        this.scanIntervalMinutes = scanIntervalMinutes;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;
    }

    public static NotificationDedupConfig defaultConfig() {
        return new NotificationDedupConfig(1L, true, 7, 60, null, LocalDateTime.now(), 0L);
    }

    public void update(boolean isEnabled, int dedupWindowDays, int scanIntervalMinutes, Long updatedBy, LocalDateTime updatedAt) {
        validateWindowDays(dedupWindowDays);
        validateScanInterval(scanIntervalMinutes);
        this.isEnabled = isEnabled;
        this.dedupWindowDays = dedupWindowDays;
        this.scanIntervalMinutes = scanIntervalMinutes;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    private void validateWindowDays(int days) {
        if (days < MIN_WINDOW_DAYS || days > MAX_WINDOW_DAYS) {
            throw new IllegalArgumentException(
                    String.format("Cửa sổ chống trùng phải nằm trong khoảng %d đến %d ngày", MIN_WINDOW_DAYS, MAX_WINDOW_DAYS)
            );
        }
    }

    private void validateScanInterval(int minutes) {
        if (minutes < MIN_SCAN_INTERVAL_MINUTES || minutes > MAX_SCAN_INTERVAL_MINUTES) {
            throw new IllegalArgumentException(
                    String.format("Chu kỳ quét tác vụ nền phải nằm trong khoảng %d đến %d phút", MIN_SCAN_INTERVAL_MINUTES, MAX_SCAN_INTERVAL_MINUTES)
            );
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public int getDedupWindowDays() {
        return dedupWindowDays;
    }

    public int getScanIntervalMinutes() {
        return scanIntervalMinutes;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
