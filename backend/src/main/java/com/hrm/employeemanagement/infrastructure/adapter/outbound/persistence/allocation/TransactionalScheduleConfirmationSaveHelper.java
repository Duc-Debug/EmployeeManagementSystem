package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.ScheduleConfirmationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataScheduleConfirmationRepository;

/**
 * Helper thực hiện lưu ScheduleConfirmation trong một Transaction cô lập (REQUIRES_NEW).
 * Mục đích: Khi xảy ra xung đột DuplicateKey (race condition concurrent insert),
 * chỉ inner transaction bị rollback, outer transaction vẫn sạch và không bị đánh dấu 'rollback-only'.
 */
@Component
public class TransactionalScheduleConfirmationSaveHelper {

    private final SpringDataScheduleConfirmationRepository repository;

    public TransactionalScheduleConfirmationSaveHelper(SpringDataScheduleConfirmationRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScheduleConfirmationJpaEntity saveInIsolatedTransaction(ScheduleConfirmationJpaEntity entity) {
        return repository.saveAndFlush(entity);
    }
}