package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.hrm.employeemanagement.application.port.inbound.allocation.AllocateResourceUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.BulkAllocateResourceUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.CheckAllocationPeriodLockUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.LoadAllocationPlanSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.LoadAllocationPlanningPeriodPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.LoadAllocationsForPeriodPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.SaveAllocationPlanSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.SaveAllocationPlanningPeriodPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.service.allocation.period.AllocationPeriodService;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.port.outbound.allocation.DeleteWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataWeeklyProjectAllocationRepository;
import com.hrm.employeemanagement.infrastructure.decorator.allocation.LockGuardedAllocateResourceUseCaseDecorator;
import com.hrm.employeemanagement.infrastructure.decorator.allocation.LockGuardedBulkAllocateResourceUseCaseDecorator;
import com.hrm.employeemanagement.infrastructure.decorator.allocation.LockGuardedDeleteWeeklyProjectAllocationPortDecorator;
import com.hrm.employeemanagement.infrastructure.decorator.allocation.LockGuardedSaveWeeklyProjectAllocationPortDecorator;
import com.hrm.employeemanagement.infrastructure.transaction.allocation.period.TransactionalAllocationPeriodServiceDecorator;

@Configuration
public class AllocationPeriodUseCaseConfig {

    @Bean
    public TransactionalAllocationPeriodServiceDecorator allocationPeriodService(
            AuthorizationService authorizationService,
            SaveAllocationPlanningPeriodPort savePeriodPort,
            LoadAllocationPlanningPeriodPort loadPeriodPort,
            SaveAllocationPlanSnapshotPort saveSnapshotPort,
            LoadAllocationPlanSnapshotPort loadSnapshotPort,
            LoadAllocationsForPeriodPort loadAllocationsPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort
    ) {
        AllocationPeriodService service = new AllocationPeriodService(
                authorizationService,
                savePeriodPort,
                loadPeriodPort,
                saveSnapshotPort,
                loadSnapshotPort,
                loadAllocationsPort,
                saveAuditLogPort
        );
        return new TransactionalAllocationPeriodServiceDecorator(service);
    }

    /**
     * [QTN-18 / TC-02]: Bọc AllocateResourceUseCase bằng Decorator để tự động chặn các phân bổ
     * vào tuần nằm trong kỳ đã bị khóa (LOCKED). Không chỉnh sửa trực tiếp mã nguồn của người khác.
     */
    @Bean
    @Primary
    public AllocateResourceUseCase lockGuardedAllocateResourceUseCase(
            @Qualifier("allocateResourceUseCase") AllocateResourceUseCase baseAllocateResourceUseCase,
            CheckAllocationPeriodLockUseCase checkAllocationPeriodLockUseCase
    ) {
        return new LockGuardedAllocateResourceUseCaseDecorator(
                baseAllocateResourceUseCase,
                checkAllocationPeriodLockUseCase
        );
    }

    /**
     * [QTN-18 / TC-02]: Bọc BulkAllocateResourceUseCase bằng Decorator để tự động chặn phân bổ hàng loạt
     * vào các tuần nằm trong kỳ đã bị khóa (LOCKED).
     */
    @Bean
    @Primary
    public BulkAllocateResourceUseCase lockGuardedBulkAllocateResourceUseCase(
            @Qualifier("bulkAllocateResourceUseCase") BulkAllocateResourceUseCase baseBulkAllocateResourceUseCase,
            CheckAllocationPeriodLockUseCase checkAllocationPeriodLockUseCase
    ) {
        return new LockGuardedBulkAllocateResourceUseCaseDecorator(
                baseBulkAllocateResourceUseCase,
                checkAllocationPeriodLockUseCase
        );
    }

    /**
     * [QTN-18 / TC-02]: Bọc SaveWeeklyProjectAllocationPort bằng Decorator để bảo vệ ở mức Persistence layer:
     * Chặn mọi hành vi thêm mới, cập nhật hoặc gỡ/xóa phân bổ đối với các tuần nằm trong kỳ đã bị khóa (LOCKED).
     */
    @Bean
    @Primary
    public SaveWeeklyProjectAllocationPort lockGuardedSaveWeeklyProjectAllocationPort(
            @Qualifier("weeklyProjectAllocationPersistenceAdapter") SaveWeeklyProjectAllocationPort baseSaveAllocationPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            CheckAllocationPeriodLockUseCase checkAllocationPeriodLockUseCase
    ) {
        return new LockGuardedSaveWeeklyProjectAllocationPortDecorator(
                baseSaveAllocationPort,
                loadAllocationPort,
                checkAllocationPeriodLockUseCase
        );
    }

    /**
     * [QTN-18 / TC-02]: Bọc DeleteWeeklyProjectAllocationPort bằng Decorator để bảo vệ ở mức Persistence layer
     * đối với thao tác DELETE / REMOVE: Chặn mọi hành vi xóa phân bổ đối với các tuần nằm trong kỳ đã bị khóa (LOCKED).
     */
    @Bean
    @Primary
    public DeleteWeeklyProjectAllocationPort lockGuardedDeleteWeeklyProjectAllocationPort(
            @Qualifier("weeklyProjectAllocationPersistenceAdapter") DeleteWeeklyProjectAllocationPort baseDeleteAllocationPort,
            CheckAllocationPeriodLockUseCase checkAllocationPeriodLockUseCase,
            SpringDataWeeklyProjectAllocationRepository repository
    ) {
        return new LockGuardedDeleteWeeklyProjectAllocationPortDecorator(
                baseDeleteAllocationPort,
                checkAllocationPeriodLockUseCase,
                repository
        );
    }
}
