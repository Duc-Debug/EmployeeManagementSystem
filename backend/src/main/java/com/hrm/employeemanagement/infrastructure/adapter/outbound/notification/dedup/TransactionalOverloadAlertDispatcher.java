package com.hrm.employeemanagement.infrastructure.adapter.outbound.notification.dedup;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.hrm.employeemanagement.application.dto.notification.CreateNotificationEventCommand;
import com.hrm.employeemanagement.application.dto.notification.dedup.EmployeeWeeklyOverloadCandidate;
import com.hrm.employeemanagement.application.port.inbound.notification.CreateNotificationEventUseCase;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.OverloadAlertDispatchResult;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.OverloadAlertDispatcherPort;
import com.hrm.employeemanagement.domain.notification.NotificationLevel;
import com.hrm.employeemanagement.domain.notification.dedup.DedupRecordStatus;
import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupKeyPolicy;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationDedupRecordJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationDedupRecordRepository;

@Component
public class TransactionalOverloadAlertDispatcher implements OverloadAlertDispatcherPort {

    private static final Logger log = LoggerFactory.getLogger(TransactionalOverloadAlertDispatcher.class);

    private static final String EVENT_TYPE = "OVERLOAD_WARNING";
    private static final String TARGET_ENTITY_TYPE = "EMPLOYEE";

    private final SpringDataNotificationDedupRecordRepository recordRepository;
    private final CreateNotificationEventUseCase createNotificationEventUseCase;

    public TransactionalOverloadAlertDispatcher(
            SpringDataNotificationDedupRecordRepository recordRepository,
            CreateNotificationEventUseCase createNotificationEventUseCase
    ) {
        this.recordRepository = Objects.requireNonNull(recordRepository, "recordRepository must not be null");
        this.createNotificationEventUseCase = Objects.requireNonNull(createNotificationEventUseCase, "createNotificationEventUseCase must not be null");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OverloadAlertDispatchResult dispatchOverloadAlert(
            EmployeeWeeklyOverloadCandidate candidate,
            Long recipientUserId,
            String yearWeek,
            LocalDateTime now,
            LocalDateTime expiresAt
    ) {
        String targetEntityId = candidate.employeeId().toString();
        String dedupKey = NotificationDedupKeyPolicy.buildKey(
                EVENT_TYPE,
                TARGET_ENTITY_TYPE,
                targetEntityId,
                yearWeek,
                recipientUserId
        );

        // 1. Kiểm tra nhanh sự tồn tại của active dedup key trong DB
        if (recordRepository.findByActiveDedupKey(dedupKey).isPresent()) {
            log.debug("Khóa chống trùng {} đã tồn tại và active. Bỏ qua cảnh báo lặp lại.", dedupKey);
            return OverloadAlertDispatchResult.SKIPPED_DEDUP;
        }

        // 2. Dự lưu dedup record với trạng thái ACTIVE trong transaction độc lập
        NotificationDedupRecordJpaEntity entity = new NotificationDedupRecordJpaEntity();
        entity.setDedupKey(dedupKey);
        entity.setActiveDedupKey(dedupKey);
        entity.setEventType(EVENT_TYPE);
        entity.setTargetEntityType(TARGET_ENTITY_TYPE);
        entity.setTargetEntityId(targetEntityId);
        entity.setYearWeek(yearWeek);
        entity.setRecipientUserId(recipientUserId);
        entity.setStatus(DedupRecordStatus.ACTIVE.name());
        entity.setCreatedAt(now);
        entity.setExpiresAt(expiresAt);

        try {
            recordRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException dive) {
            // Race condition: Phiên quét đồng thời đã insert trước và chiếm unique constraint uk_notif_dedup_active_key
            log.info("Phát hiện xung đột đồng thời trên khóa {} (chặn bởi unique constraint). Bỏ qua cảnh báo trùng: {}",
                    dedupKey, dive.getMessage());
            markRollbackOnly();
            return OverloadAlertDispatchResult.SKIPPED_DEDUP;
        }

        // 3. Tạo thông báo (Atomic cùng transaction của dedup record)
        try {
            String title = "Cảnh báo quá tải nhân sự: " + candidate.employeeName();
            String message = String.format(
                    "Nhân sự %s (%s) bị phân bổ vượt năng lực khả dụng trong tuần %s: %.1fh / %.1fh. Vui lòng rà soát và điều chỉnh kế hoạch phân bổ.",
                    candidate.employeeName(),
                    candidate.employeeCode(),
                    yearWeek,
                    candidate.allocatedHours() != null ? candidate.allocatedHours() : BigDecimal.ZERO,
                    candidate.availableHours() != null ? candidate.availableHours() : BigDecimal.ZERO
            );

            CreateNotificationEventCommand command = new CreateNotificationEventCommand(
                    EVENT_TYPE,
                    NotificationLevel.CAO,
                    title,
                    message,
                    TARGET_ENTITY_TYPE,
                    targetEntityId,
                    dedupKey,
                    List.of(recipientUserId)
            );

            createNotificationEventUseCase.execute(command);
            log.info("Đã tạo và gửi cảnh báo quá tải mới (key={}) cho người nhận {}", dedupKey, recipientUserId);
            return OverloadAlertDispatchResult.ALERTED;
        } catch (Exception ex) {
            // Nếu việc tạo notification thất bại, rollback toàn bộ transaction
            // Nhờ đó bản ghi dedup không bị lưu lại, cho phép lần quét sau thử gửi lại
            log.error("Lỗi khi tạo thông báo quá tải cho nhân sự {} (người nhận {}): {}. Rollback bản ghi dedup.",
                    candidate.employeeId(), recipientUserId, ex.getMessage(), ex);
            markRollbackOnly();
            return OverloadAlertDispatchResult.FAILED;
        }
    }

    private void markRollbackOnly() {
        try {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (NoTransactionException ignored) {
            // Bỏ qua khi chạy trong môi trường unit test không có proxy transaction của Spring
        }
    }
}
