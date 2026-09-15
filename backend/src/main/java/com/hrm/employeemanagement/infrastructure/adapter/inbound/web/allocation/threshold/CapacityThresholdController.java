package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.threshold;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.allocation.threshold.CapacityThresholdHistoryResult;
import com.hrm.employeemanagement.application.dto.allocation.threshold.CapacityThresholdResult;
import com.hrm.employeemanagement.application.dto.allocation.threshold.ConfigureCapacityThresholdCommand;
import com.hrm.employeemanagement.application.port.inbound.allocation.threshold.ConfigureCapacityThresholdUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.threshold.GetCapacityThresholdHistoryUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.threshold.GetCapacityThresholdUseCase;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.threshold.dto.ConfigureCapacityThresholdRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

/**
 * REST Controller cho Use Case NCL-07-CN-004:
 * Cấu hình ngưỡng cảnh báo quá tải và nhàn rỗi (QTN-23).
 */
@RestController
@RequestMapping("/api/v1/capacity-thresholds")
public class CapacityThresholdController {

    private final ConfigureCapacityThresholdUseCase configureUseCase;
    private final GetCapacityThresholdUseCase getUseCase;
    private final GetCapacityThresholdHistoryUseCase historyUseCase;

    public CapacityThresholdController(
            ConfigureCapacityThresholdUseCase configureUseCase,
            GetCapacityThresholdUseCase getUseCase,
            GetCapacityThresholdHistoryUseCase historyUseCase
    ) {
        this.configureUseCase = configureUseCase;
        this.getUseCase = getUseCase;
        this.historyUseCase = historyUseCase;
    }

    /**
     * Lấy cấu hình ngưỡng cảnh báo hiệu lực hiện hành.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CapacityThresholdResult>> getThreshold(
            @RequestParam(required = false, defaultValue = "COMPANY") CapacityThresholdScope scopeType,
            @RequestParam(required = false) Long orgUnitId
    ) {
        CapacityThresholdResult result = getUseCase.getEffectiveThreshold(scopeType, orgUnitId);
        return ResponseEntity.ok(ApiResponse.success("Lấy cấu hình ngưỡng cảnh báo năng lực thành công", result));
    }

    /**
     * Cấu hình / cập nhật ngưỡng quá tải và nhàn rỗi (Dành riêng cho Ban giám đốc VT-01).
     */
    @PutMapping
    public ResponseEntity<ApiResponse<CapacityThresholdResult>> configureThreshold(
            @Valid @RequestBody ConfigureCapacityThresholdRequest request
    ) {
        ConfigureCapacityThresholdCommand command = new ConfigureCapacityThresholdCommand(
                request.scopeType() != null ? request.scopeType() : CapacityThresholdScope.COMPANY,
                request.orgUnitId(),
                request.overloadThreshold(),
                request.idleThreshold(),
                request.version()
        );

        CapacityThresholdResult result = configureUseCase.configureThreshold(command);
        return ResponseEntity.ok(ApiResponse.success("Cấu hình ngưỡng cảnh báo năng lực thành công", result));
    }

    /**
     * Lấy lịch sử thay đổi cấu hình ngưỡng cảnh báo (TC-04).
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<CapacityThresholdHistoryResult>>> getHistory(
            @RequestParam(required = false, defaultValue = "COMPANY") CapacityThresholdScope scopeType,
            @RequestParam(required = false) Long orgUnitId
    ) {
        List<CapacityThresholdHistoryResult> results = historyUseCase.getHistory(scopeType, orgUnitId);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử thay đổi cấu hình ngưỡng thành công", results));
    }
}
