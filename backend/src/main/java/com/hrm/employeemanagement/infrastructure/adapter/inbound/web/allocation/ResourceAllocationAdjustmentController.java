package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import java.util.List;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.allocation.AdjustAllocationCommand;
import com.hrm.employeemanagement.application.dto.allocation.AllocationChangeLogResult;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.AdjustResourceAllocationUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.dto.AdjustAllocationRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.dto.VarianceNoteRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

/**
 * Controller xử lý các API điều chỉnh phân bổ nguồn lực (NCL-06-CN-004 / QTN-15).
 */
@RestController
@RequestMapping("/api/v1/allocations")
@Validated
public class ResourceAllocationAdjustmentController {

    private final AdjustResourceAllocationUseCase adjustResourceAllocationUseCase;

    public ResourceAllocationAdjustmentController(AdjustResourceAllocationUseCase adjustResourceAllocationUseCase) {
        this.adjustResourceAllocationUseCase = Objects.requireNonNull(
                adjustResourceAllocationUseCase,
                "AdjustResourceAllocationUseCase must not be null"
        );
    }

    /**
     * TC-01: Sửa giờ hoặc chuyển tuần cho dòng phân bổ nguồn lực.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<WeeklyCapacityResult>> adjustAllocation(
            @PathVariable Long id,
            @Valid @RequestBody AdjustAllocationRequest request
    ) {
        AdjustAllocationCommand command = new AdjustAllocationCommand(
                request.action(),
                request.newHours(),
                request.allocationPercentage(),
                request.targetYear(),
                request.targetWeek(),
                request.varianceReason(),
                request.overloadReason()
        );
        WeeklyCapacityResult result = adjustResourceAllocationUseCase.adjustAllocation(id, command);
        return ResponseEntity.ok(ApiResponse.success("Điều chỉnh phân bổ nguồn lực thành công", result));
    }

    /**
     * TC-02: Gỡ dòng phân bổ nguồn lực (chặn 409 nếu tuần đã trôi qua và có giờ công thực tế).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeAllocation(@PathVariable Long id) {
        adjustResourceAllocationUseCase.removeAllocation(id);
        return ResponseEntity.ok(ApiResponse.success("Gỡ dòng phân bổ nguồn lực thành công", null));
    }

    /**
     * TC-02: Ghi chú lý do chênh lệch khi không được phép gỡ dòng phân bổ.
     */
    @PostMapping("/{id}/variance-note")
    public ResponseEntity<ApiResponse<WeeklyCapacityResult>> noteVariance(
            @PathVariable Long id,
            @Valid @RequestBody VarianceNoteRequest request
    ) {
        WeeklyCapacityResult result = adjustResourceAllocationUseCase.noteVariance(id, request.varianceReason());
        return ResponseEntity.ok(ApiResponse.success("Ghi chú lý do chênh lệch thành công", result));
    }

    /**
     * TC-04: Lấy lịch sử thay đổi của dòng phân bổ (Audit trail).
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<AllocationChangeLogResult>>> getHistory(@PathVariable Long id) {
        List<AllocationChangeLogResult> history = adjustResourceAllocationUseCase.getHistory(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử thay đổi phân bổ thành công", history));
    }
}
