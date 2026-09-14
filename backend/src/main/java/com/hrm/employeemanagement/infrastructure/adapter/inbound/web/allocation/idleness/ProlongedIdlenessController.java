package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.idleness;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.allocation.idleness.AcknowledgeProlongedIdleStaffCommand;
import com.hrm.employeemanagement.application.dto.allocation.idleness.AcknowledgeProlongedIdleStaffResult;
import com.hrm.employeemanagement.application.dto.allocation.idleness.ProlongedIdlenessQuery;
import com.hrm.employeemanagement.application.dto.allocation.idleness.ProlongedIdlenessReportResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.idleness.AcknowledgeProlongedIdleStaffUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.idleness.GetProlongedIdleStaffUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.idleness.dto.AcknowledgeProlongedIdleStaffRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

/**
 * REST Controller cho tính năng Cảnh báo nhân sự nhàn rỗi kéo dài (NCL-07-CN-006 / QTN-23).
 */
@RestController
@RequestMapping("/api/v1/capacity/prolonged-idleness")
public class ProlongedIdlenessController {

    private final GetProlongedIdleStaffUseCase getProlongedIdleStaffUseCase;
    private final AcknowledgeProlongedIdleStaffUseCase acknowledgeProlongedIdleStaffUseCase;

    public ProlongedIdlenessController(
            GetProlongedIdleStaffUseCase getProlongedIdleStaffUseCase,
            AcknowledgeProlongedIdleStaffUseCase acknowledgeProlongedIdleStaffUseCase
    ) {
        this.getProlongedIdleStaffUseCase = getProlongedIdleStaffUseCase;
        this.acknowledgeProlongedIdleStaffUseCase = acknowledgeProlongedIdleStaffUseCase;
    }

    /**
     * Rà soát và lấy danh sách nhân sự nhàn rỗi kéo dài (NCL-07-CN-006 / QTN-23).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ProlongedIdlenessReportResult>> getProlongedIdleStaff(
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromWeek,
            @RequestParam(required = false, defaultValue = "4") Integer durationWeeks,
            @RequestParam(required = false, defaultValue = "3") Integer consecutiveThreshold,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        ProlongedIdlenessQuery query = new ProlongedIdlenessQuery(
                orgUnitId,
                fromYear,
                fromWeek,
                durationWeeks,
                consecutiveThreshold,
                search,
                page,
                size
        );
        ProlongedIdlenessReportResult result = getProlongedIdleStaffUseCase.getProlongedIdleStaff(query);
        return ResponseEntity.ok(ApiResponse.success("Rà soát danh sách nhân sự nhàn rỗi kéo dài thành công", result));
    }

    /**
     * Xác nhận xử lý cảnh báo nhân sự nhàn rỗi kéo dài (NCL-07-CN-006-TC-04).
     */
    @PostMapping("/acknowledge")
    public ResponseEntity<ApiResponse<AcknowledgeProlongedIdleStaffResult>> acknowledge(
            @Valid @RequestBody AcknowledgeProlongedIdleStaffRequest request
    ) {
        AcknowledgeProlongedIdleStaffCommand command = new AcknowledgeProlongedIdleStaffCommand(
                request.employeeId(),
                request.actionTaken(),
                request.notes()
        );
        AcknowledgeProlongedIdleStaffResult result = acknowledgeProlongedIdleStaffUseCase.acknowledgeProlongedIdleStaff(command);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận xử lý cảnh báo nhàn rỗi thành công", result));
    }
}
