package com.hrm.employeemanagement.infrastructure.transaction.allocation;

import com.hrm.employeemanagement.application.dto.allocation.AllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.AllocateResourceUseCase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import jakarta.persistence.OptimisticLockException;
import java.util.List;
import java.util.Objects;

public class RetryableAllocateResourceUseCaseDecorator implements AllocateResourceUseCase {

    private final AllocateResourceUseCase transactionalDelegate;
    private final int maxRetries;

    public RetryableAllocateResourceUseCaseDecorator(AllocateResourceUseCase transactionalDelegate) {
        this(transactionalDelegate, 3);
    }

    public RetryableAllocateResourceUseCaseDecorator(AllocateResourceUseCase transactionalDelegate, int maxRetries) {
        this.transactionalDelegate = Objects.requireNonNull(transactionalDelegate, "AllocateResourceUseCase delegate must not be null");
        if (maxRetries < 1) {
            throw new IllegalArgumentException("maxRetries must be at least 1");
        }
        this.maxRetries = maxRetries;
    }

    @Override
    public WeeklyCapacityResult allocateResource(AllocateResourceCommand command) {
        RuntimeException lastException = null;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return transactionalDelegate.allocateResource(command);
            } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
                lastException = ex;
            }
        }
        throw lastException != null ? lastException : new DataIntegrityViolationException("Không thể hoàn tất phân bổ tài nguyên sau nhiều lần thử lại");
    }

    @Override
    public List<WeeklyCapacityResult> getWeeklyCapacities(List<Long> employeeIds, Integer year, Integer weekNumber) {
        return transactionalDelegate.getWeeklyCapacities(employeeIds, year, weekNumber);
    }
}
