package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.reservation;

import com.hrm.employeemanagement.application.dto.reservation.CancelReservationCommand;
import com.hrm.employeemanagement.application.dto.reservation.CreateReservationCommand;
import com.hrm.employeemanagement.application.dto.reservation.ResourceReservationResult;
import com.hrm.employeemanagement.application.port.inbound.reservation.AutoProcessProjectReservationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.reservation.CancelResourceReservationUseCase;
import com.hrm.employeemanagement.application.port.inbound.reservation.CreateResourceReservationUseCase;
import com.hrm.employeemanagement.application.port.inbound.reservation.GetResourceReservationsUseCase;
import com.hrm.employeemanagement.domain.reservation.ReservationStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.reservation.dto.CancelReservationRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.reservation.dto.CreateReservationRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping({"/api/v1/resource-reservations", "/api/v1/reservations"})
public class ResourceReservationController {

    private final CreateResourceReservationUseCase createUseCase;
    private final CancelResourceReservationUseCase cancelUseCase;
    private final GetResourceReservationsUseCase getUseCase;
    private final AutoProcessProjectReservationsUseCase autoProcessUseCase;

    public ResourceReservationController(
            CreateResourceReservationUseCase createUseCase,
            CancelResourceReservationUseCase cancelUseCase,
            GetResourceReservationsUseCase getUseCase,
            AutoProcessProjectReservationsUseCase autoProcessUseCase
    ) {
        this.createUseCase = Objects.requireNonNull(createUseCase, "CreateResourceReservationUseCase must not be null");
        this.cancelUseCase = Objects.requireNonNull(cancelUseCase, "CancelResourceReservationUseCase must not be null");
        this.getUseCase = Objects.requireNonNull(getUseCase, "GetResourceReservationsUseCase must not be null");
        this.autoProcessUseCase = Objects.requireNonNull(autoProcessUseCase, "AutoProcessProjectReservationsUseCase must not be null");
    }

    /**
     * TC-01: Tạo giữ chỗ nguồn lực cho dự án dự kiến.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ResourceReservationResult>> createReservation(
            @Valid @RequestBody CreateReservationRequest request
    ) {
        CreateReservationCommand command = new CreateReservationCommand(
                request.projectId(),
                request.employeeId(),
                request.year(),
                request.weekNumber(),
                request.reservedHours(),
                request.note()
        );

        ResourceReservationResult result = createUseCase.createReservation(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Giữ chỗ nguồn lực thành công", result));
    }

    /**
     * Hủy giữ chỗ nguồn lực thủ công.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ResourceReservationResult>> cancelReservation(
            @PathVariable Long id,
            @RequestBody(required = false) CancelReservationRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        CancelReservationCommand command = new CancelReservationCommand(id, reason);
        ResourceReservationResult result = cancelUseCase.cancelReservation(command);
        return ResponseEntity.ok(ApiResponse.success("Hủy giữ chỗ thành công", result));
    }

    /**
     * Lấy danh sách giữ chỗ nguồn lực theo bộ lọc.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResourceReservationResult>>> getReservations(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) ReservationStatus status
    ) {
        List<ResourceReservationResult> result = getUseCase.getReservations(projectId, employeeId, year, weekNumber, status);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách giữ chỗ thành công", result));
    }

    /**
     * TC-02: Tự động hủy các bản ghi giữ chỗ khi dự án bị hủy.
     */
    @PostMapping("/projects/{projectId}/auto-cancel")
    public ResponseEntity<ApiResponse<Integer>> autoCancelForProject(
            @PathVariable Long projectId,
            @RequestParam(required = false, defaultValue = "Dự án dự kiến bị hủy") String cancelReason
    ) {
        int count = autoProcessUseCase.autoCancelForProject(projectId, cancelReason);
        return ResponseEntity.ok(ApiResponse.success("Đã tự hủy " + count + " dòng giữ chỗ của dự án", count));
    }

    /**
     * TC-03: Tự động chuyển đổi các bản ghi giữ chỗ thành phân bổ chính thức khi dự án được duyệt.
     */
    @PostMapping("/projects/{projectId}/auto-convert")
    public ResponseEntity<ApiResponse<Integer>> autoConvertForProject(
            @PathVariable Long projectId
    ) {
        int count = autoProcessUseCase.autoConvertForProject(projectId);
        return ResponseEntity.ok(ApiResponse.success("Đã chuyển đổi " + count + " dòng giữ chỗ thành phân bổ chính thức", count));
    }
}
