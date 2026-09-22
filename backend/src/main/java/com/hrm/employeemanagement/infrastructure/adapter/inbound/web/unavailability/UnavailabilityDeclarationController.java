package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.ApproveUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.RejectUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.SubmitUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityConflictCheckResult;
import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;
import com.hrm.employeemanagement.application.port.inbound.unavailability.ApproveUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.CancelUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.CheckUnavailabilityConflictUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.GetDepartmentUnavailabilityDeclarationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.GetMyUnavailabilityDeclarationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.RejectUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.SubmitUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.unavailability.dto.ApproveUnavailabilityRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.unavailability.dto.RejectUnavailabilityRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.unavailability.dto.SubmitUnavailabilityRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityPreviewResult;
import com.hrm.employeemanagement.application.port.inbound.unavailability.PreviewUnavailabilityUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/unavailability-declarations")
@Validated
public class UnavailabilityDeclarationController {

    private final SubmitUnavailabilityDeclarationUseCase submitUseCase;
    private final ApproveUnavailabilityDeclarationUseCase approveUseCase;
    private final RejectUnavailabilityDeclarationUseCase rejectUseCase;
    private final CancelUnavailabilityDeclarationUseCase cancelUseCase;
    private final GetMyUnavailabilityDeclarationsUseCase getMyDeclarationsUseCase;
    private final GetDepartmentUnavailabilityDeclarationsUseCase getDepartmentDeclarationsUseCase;
    private final CheckUnavailabilityConflictUseCase checkConflictUseCase;
    private final PreviewUnavailabilityUseCase previewUseCase;

    public UnavailabilityDeclarationController(
            SubmitUnavailabilityDeclarationUseCase submitUseCase,
            ApproveUnavailabilityDeclarationUseCase approveUseCase,
            RejectUnavailabilityDeclarationUseCase rejectUseCase,
            CancelUnavailabilityDeclarationUseCase cancelUseCase,
            GetMyUnavailabilityDeclarationsUseCase getMyDeclarationsUseCase,
            GetDepartmentUnavailabilityDeclarationsUseCase getDepartmentDeclarationsUseCase,
            CheckUnavailabilityConflictUseCase checkConflictUseCase,
            PreviewUnavailabilityUseCase previewUseCase
    ) {
        this.submitUseCase = Objects.requireNonNull(submitUseCase, "submitUseCase must not be null");
        this.approveUseCase = Objects.requireNonNull(approveUseCase, "approveUseCase must not be null");
        this.rejectUseCase = Objects.requireNonNull(rejectUseCase, "rejectUseCase must not be null");
        this.cancelUseCase = Objects.requireNonNull(cancelUseCase, "cancelUseCase must not be null");
        this.getMyDeclarationsUseCase = Objects.requireNonNull(getMyDeclarationsUseCase, "getMyDeclarationsUseCase must not be null");
        this.getDepartmentDeclarationsUseCase = Objects.requireNonNull(getDepartmentDeclarationsUseCase, "getDepartmentDeclarationsUseCase must not be null");
        this.checkConflictUseCase = Objects.requireNonNull(checkConflictUseCase, "checkConflictUseCase must not be null");
        this.previewUseCase = Objects.requireNonNull(previewUseCase, "previewUseCase must not be null");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UnavailabilityDeclarationResult>> submit(
            @Valid @RequestBody SubmitUnavailabilityRequest request
    ) {
        SubmitUnavailabilityCommand command = new SubmitUnavailabilityCommand(
                request.employeeId(),
                request.startDate(),
                request.endDate(),
                request.reasonType(),
                request.reasonDetail()
        );
        UnavailabilityDeclarationResult result = submitUseCase.submit(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khai báo thời gian không sẵn sàng thành công", result));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<UnavailabilityDeclarationResult>>> getMyDeclarations() {
        List<UnavailabilityDeclarationResult> list = getMyDeclarationsUseCase.getMyDeclarations();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khai báo cá nhân thành công", list));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<UnavailabilityDeclarationResult>>> getPendingDeclarations(
            @RequestParam(name = "orgUnitId", required = false) Long orgUnitId
    ) {
        List<UnavailabilityDeclarationResult> list = getDepartmentDeclarationsUseCase.getPendingDeclarations(orgUnitId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khai báo chờ duyệt thành công", list));
    }

    @GetMapping("/{id}/check-conflict")
    public ResponseEntity<ApiResponse<UnavailabilityConflictCheckResult>> checkConflict(
            @PathVariable("id") Long id
    ) {
        UnavailabilityConflictCheckResult result = checkConflictUseCase.checkConflict(id);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra xung đột phân bổ thành công", result));
    }

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<UnavailabilityPreviewResult>> preview(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        UnavailabilityPreviewResult result = previewUseCase.preview(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Tính toán thời gian không sẵn sàng thành công", result));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<UnavailabilityDeclarationResult>> approve(
            @PathVariable("id") Long id,
            @Valid @RequestBody(required = false) ApproveUnavailabilityRequest request
    ) {
        String comment = request != null ? request.approverComment() : null;
        boolean confirmConflict = request != null && Boolean.TRUE.equals(request.confirmConflictWarning());
        ApproveUnavailabilityCommand command = new ApproveUnavailabilityCommand(id, comment, confirmConflict);
        UnavailabilityDeclarationResult result = approveUseCase.approve(command);
        return ResponseEntity.ok(ApiResponse.success("Phê duyệt thời gian không sẵn sàng thành công", result));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<UnavailabilityDeclarationResult>> reject(
            @PathVariable("id") Long id,
            @Valid @RequestBody(required = false) RejectUnavailabilityRequest request
    ) {
        String rejectReason = request != null ? request.rejectReason() : null;
        RejectUnavailabilityCommand command = new RejectUnavailabilityCommand(id, rejectReason);
        UnavailabilityDeclarationResult result = rejectUseCase.reject(command);
        return ResponseEntity.ok(ApiResponse.success("Từ chối thời gian không sẵn sàng thành công", result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<UnavailabilityDeclarationResult>> cancel(
            @PathVariable("id") Long id
    ) {
        UnavailabilityDeclarationResult result = cancelUseCase.cancel(id);
        return ResponseEntity.ok(ApiResponse.success("Hủy khai báo thời gian không sẵn sàng thành công", result));
    }
}
