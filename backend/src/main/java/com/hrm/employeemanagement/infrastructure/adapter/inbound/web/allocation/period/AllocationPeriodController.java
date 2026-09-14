package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.period;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.allocation.period.AllocationPeriodResult;
import com.hrm.employeemanagement.application.dto.allocation.period.AllocationPlanSnapshotResult;
import com.hrm.employeemanagement.application.dto.allocation.period.CreatePeriodCommand;
import com.hrm.employeemanagement.application.dto.allocation.period.LockPeriodCommand;
import com.hrm.employeemanagement.application.dto.allocation.period.PeriodLockCheckResult;
import com.hrm.employeemanagement.application.dto.allocation.period.UnlockPeriodCommand;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.CheckAllocationPeriodLockUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.CreateAllocationPeriodUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.GetAllocationPeriodsUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.LockAllocationPeriodUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.UnlockAllocationPeriodUseCase;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.period.dto.CreatePeriodRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.period.dto.UnlockPeriodRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

/**
 * Controller cung cấp REST API cho chức năng NCL-06-CN-009: Khóa kế hoạch phân bổ của kỳ (QTN-18).
 */
@RestController
@RequestMapping("/api/v1/allocations/periods")
@Validated
public class AllocationPeriodController {

    private final CreateAllocationPeriodUseCase createPeriodUseCase;
    private final LockAllocationPeriodUseCase lockPeriodUseCase;
    private final UnlockAllocationPeriodUseCase unlockPeriodUseCase;
    private final GetAllocationPeriodsUseCase getPeriodsUseCase;
    private final CheckAllocationPeriodLockUseCase checkLockUseCase;

    public AllocationPeriodController(
            CreateAllocationPeriodUseCase createPeriodUseCase,
            LockAllocationPeriodUseCase lockPeriodUseCase,
            UnlockAllocationPeriodUseCase unlockPeriodUseCase,
            GetAllocationPeriodsUseCase getPeriodsUseCase,
            CheckAllocationPeriodLockUseCase checkLockUseCase
    ) {
        this.createPeriodUseCase = createPeriodUseCase;
        this.lockPeriodUseCase = lockPeriodUseCase;
        this.unlockPeriodUseCase = unlockPeriodUseCase;
        this.getPeriodsUseCase = getPeriodsUseCase;
        this.checkLockUseCase = checkLockUseCase;
    }

    /**
     * Tạo kỳ kế hoạch phân bổ mới.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AllocationPeriodResult>> createPeriod(
            @Valid @RequestBody CreatePeriodRequest request
    ) {
        CreatePeriodCommand command = new CreatePeriodCommand(
                request.name(),
                request.periodType(),
                request.year(),
                request.startWeek(),
                request.endWeek()
        );
        AllocationPeriodResult result = createPeriodUseCase.createPeriod(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo kỳ kế hoạch phân bổ thành công", result));
    }

    /**
     * Lấy danh sách các kỳ kế hoạch phân bổ (lọc theo năm và trạng thái).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AllocationPeriodResult>>> getPeriods(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) AllocationPeriodStatus status
    ) {
        List<AllocationPeriodResult> results = getPeriodsUseCase.getPeriods(year, status);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách kỳ kế hoạch phân bổ thành công", results));
    }

    /**
     * Lấy thông tin chi tiết của 1 kỳ kế hoạch phân bổ.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AllocationPeriodResult>> getPeriodById(@PathVariable Long id) {
        AllocationPeriodResult result = getPeriodsUseCase.getPeriodById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin kỳ kế hoạch phân bổ thành công", result));
    }

    /**
     * [TC-01, TC-03, TC-04]: Khóa kỳ kế hoạch phân bổ và lưu bản chụp kế hoạch.
     */
    @PostMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<AllocationPeriodResult>> lockPeriod(@PathVariable Long id) {
        LockPeriodCommand command = new LockPeriodCommand(id);
        AllocationPeriodResult result = lockPeriodUseCase.lockPeriod(command);
        return ResponseEntity.ok(ApiResponse.success("Khóa kế hoạch phân bổ của kỳ thành công", result));
    }

    /**
     * [TC-04]: Mở lại kỳ kế hoạch phân bổ kèm theo lý do bắt buộc.
     */
    @PostMapping("/{id}/unlock")
    public ResponseEntity<ApiResponse<AllocationPeriodResult>> unlockPeriod(
            @PathVariable Long id,
            @Valid @RequestBody UnlockPeriodRequest request
    ) {
        UnlockPeriodCommand command = new UnlockPeriodCommand(id, request.reason());
        AllocationPeriodResult result = unlockPeriodUseCase.unlockPeriod(command);
        return ResponseEntity.ok(ApiResponse.success("Mở lại kỳ kế hoạch phân bổ thành công", result));
    }

    /**
     * Lấy danh sách các bản chụp kế hoạch (Snapshots / Baselines) của kỳ.
     */
    @GetMapping("/{id}/snapshots")
    public ResponseEntity<ApiResponse<List<AllocationPlanSnapshotResult>>> getPeriodSnapshots(@PathVariable Long id) {
        List<AllocationPlanSnapshotResult> results = getPeriodsUseCase.getPeriodSnapshots(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bản chụp kế hoạch của kỳ thành công", results));
    }

    /**
     * Lấy chi tiết nội dung phân bổ trong một bản chụp kế hoạch cụ thể.
     */
    @GetMapping("/{id}/snapshots/{snapshotId}")
    public ResponseEntity<ApiResponse<AllocationPlanSnapshotResult>> getSnapshotDetail(
            @PathVariable Long id,
            @PathVariable Long snapshotId
    ) {
        AllocationPlanSnapshotResult result = getPeriodsUseCase.getSnapshotDetail(id, snapshotId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết bản chụp kế hoạch thành công", result));
    }

    /**
     * Tiện ích phục vụ Frontend: Kiểm tra nhanh tuần cụ thể có đang bị khóa bởi kỳ nào hay không.
     */
    @GetMapping("/check-lock")
    public ResponseEntity<ApiResponse<PeriodLockCheckResult>> checkWeekLock(
            @RequestParam int year,
            @RequestParam int weekNumber
    ) {
        PeriodLockCheckResult result = checkLockUseCase.checkWeekLock(year, weekNumber);
        return ResponseEntity.ok(ApiResponse.success(result.message(), result));
    }
}
