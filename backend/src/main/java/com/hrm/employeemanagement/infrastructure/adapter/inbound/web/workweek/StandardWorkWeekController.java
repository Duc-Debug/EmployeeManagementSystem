package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek;

import com.hrm.employeemanagement.application.dto.workweek.CapacityConversionCommand;
import com.hrm.employeemanagement.application.dto.workweek.CapacityConversionResult;
import com.hrm.employeemanagement.application.dto.workweek.StandardWorkWeekConfigResult;
import com.hrm.employeemanagement.application.dto.workweek.StandardWorkWeekDayDto;
import com.hrm.employeemanagement.application.dto.workweek.UpdateStandardWorkWeekCommand;
import com.hrm.employeemanagement.application.port.inbound.workweek.ConvertCapacityUnitUseCase;
import com.hrm.employeemanagement.application.port.inbound.workweek.GetStandardWorkWeekConfigUseCase;
import com.hrm.employeemanagement.application.port.inbound.workweek.UpdateStandardWorkWeekConfigUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek.dto.CapacityConversionRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek.dto.CapacityConversionResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek.dto.StandardWorkWeekConfigResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek.dto.StandardWorkWeekDayResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek.dto.UpdateStandardWorkWeekRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/work-week-configs")
@Validated
public class StandardWorkWeekController {

    private final GetStandardWorkWeekConfigUseCase getConfigUseCase;
    private final UpdateStandardWorkWeekConfigUseCase updateConfigUseCase;
    private final ConvertCapacityUnitUseCase convertUnitUseCase;

    public StandardWorkWeekController(
            GetStandardWorkWeekConfigUseCase getConfigUseCase,
            UpdateStandardWorkWeekConfigUseCase updateConfigUseCase,
            ConvertCapacityUnitUseCase convertUnitUseCase) {
        this.getConfigUseCase = Objects.requireNonNull(getConfigUseCase, "getConfigUseCase must not be null");
        this.updateConfigUseCase = Objects.requireNonNull(updateConfigUseCase, "updateConfigUseCase must not be null");
        this.convertUnitUseCase = Objects.requireNonNull(convertUnitUseCase, "convertUnitUseCase must not be null");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<StandardWorkWeekConfigResponse>> getConfig(
            @RequestParam(required = false, defaultValue = "COMPANY") String scopeType,
            @RequestParam(required = false) Long orgUnitId) {

        StandardWorkWeekConfigResult result = getConfigUseCase.execute(scopeType, orgUnitId);
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy cấu hình tuần làm việc chuẩn thành công",
                toResponse(result)
        ));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<StandardWorkWeekConfigResponse>> updateConfig(
            @Valid @RequestBody UpdateStandardWorkWeekRequest request) {

        List<StandardWorkWeekDayDto> dayDtos = request.days().stream()
                .map(d -> new StandardWorkWeekDayDto(d.dayOfWeek(), Boolean.TRUE.equals(d.isWorkingDay()), d.workingHours()))
                .toList();

        UpdateStandardWorkWeekCommand command = new UpdateStandardWorkWeekCommand(
                request.scopeType(),
                request.orgUnitId(),
                request.capacityUnit(),
                request.weekStartDay(),
                request.standardHoursPerDay(),
                dayDtos,
                request.version()
        );

        StandardWorkWeekConfigResult result = updateConfigUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật cấu hình tuần làm việc chuẩn thành công",
                toResponse(result)
        ));
    }

    @PostMapping("/convert")
    public ResponseEntity<ApiResponse<CapacityConversionResponse>> convertCapacity(
            @Valid @RequestBody CapacityConversionRequest request) {

        CapacityConversionCommand command = new CapacityConversionCommand(
                request.value(),
                request.fromUnit(),
                request.toUnit(),
                request.scopeType(),
                request.orgUnitId()
        );

        CapacityConversionResult result = convertUnitUseCase.execute(command);
        CapacityConversionResponse response = new CapacityConversionResponse(
                result.originalValue(),
                result.fromUnit(),
                result.convertedValue(),
                result.toUnit(),
                result.formulaDescription()
        );

        return ResponseEntity.ok(ApiResponse.success(
                "Quy đổi đơn vị năng lực thành công",
                response
        ));
    }

    private StandardWorkWeekConfigResponse toResponse(StandardWorkWeekConfigResult result) {
        List<StandardWorkWeekDayResponse> dayResponses = result.days().stream()
                .map(d -> new StandardWorkWeekDayResponse(d.dayOfWeek(), d.isWorkingDay(), d.workingHours()))
                .toList();

        return new StandardWorkWeekConfigResponse(
                result.id(),
                result.scopeType(),
                result.orgUnitId(),
                result.scopeKey(),
                result.capacityUnit(),
                result.weekStartDay(),
                result.standardHoursPerDay(),
                result.standardHoursPerWeek(),
                dayResponses,
                result.createdBy(),
                result.updatedBy(),
                result.updatedAt(),
                result.version(),
                result.isInherited()
        );
    }
}

