package com.hrm.employeemanagement.infrastructure.transaction.allocation;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.allocation.AdjustAllocationCommand;
import com.hrm.employeemanagement.application.dto.allocation.AllocationChangeLogResult;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.AdjustResourceAllocationUseCase;

/**
 * Transaction decorator bọc quanh UseCase điều chỉnh phân bổ nguồn lực.
 * Đảm bảo cập nhật phân bổ và ghi nhật ký kiểm toán là ATOMIC theo ràng buộc cứng QTN-15.
 */
public class TransactionalAdjustResourceAllocationUseCase implements AdjustResourceAllocationUseCase {

    private final AdjustResourceAllocationUseCase pureDelegate;

    public TransactionalAdjustResourceAllocationUseCase(AdjustResourceAllocationUseCase pureDelegate) {
        this.pureDelegate = Objects.requireNonNull(pureDelegate, "AdjustResourceAllocationUseCase pureDelegate must not be null");
    }

    @Override
    @Transactional
    public WeeklyCapacityResult adjustAllocation(Long allocationId, AdjustAllocationCommand command) {
        return pureDelegate.adjustAllocation(allocationId, command);
    }

    @Override
    @Transactional
    public void removeAllocation(Long allocationId) {
        pureDelegate.removeAllocation(allocationId);
    }

    @Override
    @Transactional
    public WeeklyCapacityResult noteVariance(Long allocationId, String varianceReason) {
        return pureDelegate.noteVariance(allocationId, varianceReason);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllocationChangeLogResult> getHistory(Long allocationId) {
        return pureDelegate.getHistory(allocationId);
    }
}
