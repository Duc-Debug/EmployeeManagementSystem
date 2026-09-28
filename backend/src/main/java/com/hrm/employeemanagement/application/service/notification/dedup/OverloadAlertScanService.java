package com.hrm.employeemanagement.application.service.notification.dedup;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hrm.employeemanagement.application.dto.notification.dedup.EmployeeWeeklyOverloadCandidate;
import com.hrm.employeemanagement.application.dto.notification.dedup.OverloadScanResult;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.ScanOverloadAndAlertUseCase;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.LoadWeeklyOverloadCandidatesPort;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.NotificationDedupConfigRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.NotificationDedupRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.OverloadAlertDispatchResult;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.OverloadAlertDispatcherPort;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupConfig;
import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupRecord;

/**
 * Application Service thực thi tác vụ nền rà soát quá tải và gửi cảnh báo chống trùng lặp (NCL-11-CN-003 / QTN-19).
 * Đáp ứng:
 * - TC-01: Chạy lại sau 1 giờ -> không gửi lại cho cùng tuần và cùng nhân sự nếu khóa active đã tồn tại (SKIP).
 * - TC-02: Thoát quá tải rồi quá tải lại trong cùng tuần -> resolve khóa cũ, kích hoạt sự kiện mới và gửi cảnh báo lại.
 * - Concurrency & Atomicity: Cấp phát khóa dedup và phát sinh notification được thực hiện nguyên tử qua OverloadAlertDispatcherPort.
 */
public class OverloadAlertScanService implements ScanOverloadAndAlertUseCase {

    private static final Logger log = LoggerFactory.getLogger(OverloadAlertScanService.class);
    public static final String TARGET_ENTITY_TYPE = "EMPLOYEE";

    private final NotificationDedupConfigRepositoryPort configRepositoryPort;
    private final NotificationDedupRepositoryPort dedupRepositoryPort;
    private final LoadWeeklyOverloadCandidatesPort loadCandidatesPort;
    private final OverloadAlertDispatcherPort overloadAlertDispatcherPort;
    private final Clock clock;

    public OverloadAlertScanService(
            NotificationDedupConfigRepositoryPort configRepositoryPort,
            NotificationDedupRepositoryPort dedupRepositoryPort,
            LoadWeeklyOverloadCandidatesPort loadCandidatesPort,
            OverloadAlertDispatcherPort overloadAlertDispatcherPort,
            Clock clock
    ) {
        this.configRepositoryPort = Objects.requireNonNull(configRepositoryPort, "configRepositoryPort must not be null");
        this.dedupRepositoryPort = Objects.requireNonNull(dedupRepositoryPort, "dedupRepositoryPort must not be null");
        this.loadCandidatesPort = Objects.requireNonNull(loadCandidatesPort, "loadCandidatesPort must not be null");
        this.overloadAlertDispatcherPort = Objects.requireNonNull(overloadAlertDispatcherPort, "overloadAlertDispatcherPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public OverloadScanResult scanCurrentWeek() {
        YearWeek currentYearWeek = YearWeek.from(LocalDate.now(clock));
        return scanAndAlert(currentYearWeek.year(), currentYearWeek.weekNumber());
    }

    @Override
    public OverloadScanResult scanAndAlert(int year, int weekNumber) {
        NotificationDedupConfig config = configRepositoryPort.loadConfig();
        if (!config.isEnabled()) {
            log.info("Cơ chế chống gửi trùng thông báo đang tắt. Bỏ qua lượt quét quá tải tuần {}-W{}", year, weekNumber);
            return OverloadScanResult.empty();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plusDays(config.getDedupWindowDays());
        String yearWeek = String.format("%d-W%02d", year, weekNumber);

        List<EmployeeWeeklyOverloadCandidate> candidates = loadCandidatesPort.loadOverloadCandidates(year, weekNumber);
        int totalScanned = candidates.size();
        int overloadedCount = 0;
        int newlyAlertedCount = 0;
        int skippedDedupCount = 0;
        int resolvedCount = 0;

        for (EmployeeWeeklyOverloadCandidate candidate : candidates) {
            String targetEntityId = candidate.employeeId().toString();

            if (!candidate.isOverloaded()) {
                // Thoát quá tải: Đánh dấu các active dedup records trước đó của nhân sự trong tuần này thành INACTIVE (TC-02)
                List<NotificationDedupRecord> activeRecords = dedupRepositoryPort.findActiveByTargetAndWeek(
                        TARGET_ENTITY_TYPE,
                        targetEntityId,
                        yearWeek
                );
                if (!activeRecords.isEmpty()) {
                    dedupRepositoryPort.resolveActiveRecords(TARGET_ENTITY_TYPE, targetEntityId, yearWeek, now);
                    resolvedCount += activeRecords.size();
                    log.info("Nhân sự {} ({}) đã thoát quá tải trong tuần {}. Đã resolve {} bản ghi dedup.",
                            candidate.employeeName(), candidate.employeeCode(), yearWeek, activeRecords.size());
                }
                continue;
            }

            // Nhân sự bị quá tải
            overloadedCount++;
            for (Long recipientUserId : candidate.recipientUserIds()) {
                if (recipientUserId == null) {
                    continue;
                }

                OverloadAlertDispatchResult dispatchResult = overloadAlertDispatcherPort.dispatchOverloadAlert(
                        candidate,
                        recipientUserId,
                        yearWeek,
                        now,
                        expiresAt
                );

                if (dispatchResult == OverloadAlertDispatchResult.ALERTED) {
                    newlyAlertedCount++;
                } else if (dispatchResult == OverloadAlertDispatchResult.SKIPPED_DEDUP) {
                    skippedDedupCount++;
                } else {
                    log.warn("Gửi cảnh báo quá tải thất bại cho nhân sự {} (người nhận {}). Lần quét sau sẽ thử lại.",
                            candidate.employeeId(), recipientUserId);
                }
            }
        }

        return new OverloadScanResult(totalScanned, overloadedCount, newlyAlertedCount, skippedDedupCount, resolvedCount);
    }
}
