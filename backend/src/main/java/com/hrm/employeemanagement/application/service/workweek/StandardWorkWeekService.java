package com.hrm.employeemanagement.application.service.workweek;

import com.hrm.employeemanagement.application.dto.workweek.CapacityConversionCommand;
import com.hrm.employeemanagement.application.dto.workweek.CapacityConversionResult;
import com.hrm.employeemanagement.application.dto.workweek.StandardWorkWeekConfigResult;
import com.hrm.employeemanagement.application.dto.workweek.StandardWorkWeekDayDto;
import com.hrm.employeemanagement.application.dto.workweek.UpdateStandardWorkWeekCommand;
import com.hrm.employeemanagement.application.port.inbound.workweek.ConvertCapacityUnitUseCase;
import com.hrm.employeemanagement.application.port.inbound.workweek.GetStandardWorkWeekConfigUseCase;
import com.hrm.employeemanagement.application.port.inbound.workweek.UpdateStandardWorkWeekConfigUseCase;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.workweek.LoadStandardWorkWeekPort;
import com.hrm.employeemanagement.application.port.outbound.workweek.SaveStandardWorkWeekPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.workweek.InvalidStandardWorkWeekException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.workweek.CapacityUnit;
import com.hrm.employeemanagement.domain.workweek.StandardWorkWeekConfig;
import com.hrm.employeemanagement.domain.workweek.StandardWorkWeekDay;
import com.hrm.employeemanagement.domain.workweek.StandardWorkWeekPolicy;
import com.hrm.employeemanagement.domain.workweek.WeekStartDay;
import com.hrm.employeemanagement.domain.workweek.WorkWeekScope;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StandardWorkWeekService implements
        GetStandardWorkWeekConfigUseCase,
        UpdateStandardWorkWeekConfigUseCase,
        ConvertCapacityUnitUseCase {

    private final AuthorizationService authorizationService;
    private final LoadStandardWorkWeekPort loadStandardWorkWeekPort;
    private final SaveStandardWorkWeekPort saveStandardWorkWeekPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final SaveAuditLogPort saveAuditLogPort;

    public StandardWorkWeekService(
            AuthorizationService authorizationService,
            LoadStandardWorkWeekPort loadStandardWorkWeekPort,
            SaveStandardWorkWeekPort saveStandardWorkWeekPort,
            LoadOrgUnitPort loadOrgUnitPort,
            SaveAuditLogPort saveAuditLogPort) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadStandardWorkWeekPort = Objects.requireNonNull(loadStandardWorkWeekPort, "LoadStandardWorkWeekPort must not be null");
        this.saveStandardWorkWeekPort = Objects.requireNonNull(saveStandardWorkWeekPort, "SaveStandardWorkWeekPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
    }

    private void requireReadPermission() {
        try {
            authorizationService.require(PermissionCode.STANDARD_WORK_WEEK_READ);
        } catch (Exception e) {
            authorizationService.require(PermissionCode.WORKING_CALENDAR_READ);
        }
    }

    private Long requireManagePermission() {
        try {
            return authorizationService.require(PermissionCode.STANDARD_WORK_WEEK_MANAGE);
        } catch (Exception e) {
            return authorizationService.require(PermissionCode.WORKING_CALENDAR_MANAGE);
        }
    }

    @Override
    public StandardWorkWeekConfigResult execute(String scopeTypeStr, Long orgUnitId) {
        requireReadPermission();

        WorkWeekScope targetScope = parseScope(scopeTypeStr, orgUnitId);

        if (targetScope.isOrgUnit()) {
            Optional<StandardWorkWeekConfig> specificConfig = loadStandardWorkWeekPort.findByScope(targetScope);
            if (specificConfig.isPresent()) {
                return mapToResult(specificConfig.get(), false);
            }
            // Fallback sang cấu hình cấp COMPANY
            StandardWorkWeekConfig companyConfig = loadCompanyOrFallback();
            return mapToResult(companyConfig, true);
        }

        StandardWorkWeekConfig companyConfig = loadCompanyOrFallback();
        return mapToResult(companyConfig, false);
    }

    @Override
    public StandardWorkWeekConfigResult execute(UpdateStandardWorkWeekCommand command) {
        Long currentUserId = requireManagePermission();

        Objects.requireNonNull(command, "Command không được null");
        WorkWeekScope scope = parseScope(command.scopeType(), command.orgUnitId());

        if (scope.isOrgUnit()) {
            loadOrgUnitPort.findById(new OrgUnitId(scope.orgUnitId()))
                    .orElseThrow(() -> new OrgUnitNotFoundException("Không tìm thấy đơn vị với ID: " + scope.orgUnitId()));
        }

        CapacityUnit capacityUnit = parseCapacityUnit(command.capacityUnit());
        WeekStartDay weekStartDay = parseWeekStartDay(command.weekStartDay());

        List<StandardWorkWeekDay> domainDays = mapDaysToDomain(command.days());

        Optional<StandardWorkWeekConfig> existingOpt = loadStandardWorkWeekPort.findByScope(scope);
        StandardWorkWeekConfig configToSave;

        if (existingOpt.isPresent()) {
            configToSave = existingOpt.get();
            configToSave.update(
                    capacityUnit,
                    weekStartDay,
                    command.standardHoursPerDay(),
                    domainDays,
                    currentUserId
            );
        } else {
            configToSave = new StandardWorkWeekConfig(
                    null,
                    scope,
                    capacityUnit,
                    weekStartDay,
                    command.standardHoursPerDay(),
                    domainDays,
                    currentUserId,
                    null,
                    LocalDateTime.now(),
                    null,
                    0L
            );
        }

        StandardWorkWeekConfig saved = saveStandardWorkWeekPort.save(configToSave);

        String oldValue = existingOpt
                .map(c -> "totalHours=" + c.getStandardHoursPerWeek() + ", unit=" + c.getCapacityUnit())
                .orElse("NONE");
        String newValue = "scope=" + scope.toScopeKey() + ", totalHours=" + saved.getStandardHoursPerWeek() + ", unit=" + saved.getCapacityUnit();

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "UPDATE_STANDARD_WORK_WEEK",
                "standard_work_week_configs",
                saved.getId(),
                oldValue,
                newValue
        ));

        return mapToResult(saved, false);
    }

    @Override
    public CapacityConversionResult execute(CapacityConversionCommand command) {
        requireReadPermission();

        Objects.requireNonNull(command, "CapacityConversionCommand không được null");
        WorkWeekScope scope = parseScope(command.scopeType(), command.orgUnitId());

        StandardWorkWeekConfig config = scope.isOrgUnit()
                ? loadStandardWorkWeekPort.findByScope(scope).orElseGet(this::loadCompanyOrFallback)
                : loadCompanyOrFallback();

        CapacityUnit fromUnit = parseCapacityUnit(command.fromUnit());
        CapacityUnit toUnit = parseCapacityUnit(command.toUnit());

        BigDecimal converted = StandardWorkWeekPolicy.convertCapacity(
                command.value(),
                fromUnit,
                toUnit,
                config.getStandardHoursPerDay(),
                config.getStandardHoursPerWeek()
        );

        String formula = String.format("Quy đổi từ %s sang %s (chuẩn: %.1fh/ngày, %.1fh/tuần)",
                fromUnit.getDisplayName(),
                toUnit.getDisplayName(),
                config.getStandardHoursPerDay(),
                config.getStandardHoursPerWeek());

        return new CapacityConversionResult(
                command.value(),
                fromUnit.name(),
                converted,
                toUnit.name(),
                formula
        );
    }

    private StandardWorkWeekConfig loadCompanyOrFallback() {
        return loadStandardWorkWeekPort.findCompanyDefault()
                .orElseGet(() -> StandardWorkWeekConfig.createDefaultCompany(1L));
    }

    private WorkWeekScope parseScope(String scopeTypeStr, Long orgUnitId) {
        if (scopeTypeStr == null || scopeTypeStr.equalsIgnoreCase("COMPANY")) {
            return WorkWeekScope.company();
        }
        if (scopeTypeStr.equalsIgnoreCase("ORG_UNIT")) {
            if (orgUnitId == null || orgUnitId <= 0) {
                throw new InvalidStandardWorkWeekException("Vui lòng chỉ định orgUnitId hợp lệ cho cấu hình cấp ORG_UNIT");
            }
            return WorkWeekScope.orgUnit(orgUnitId);
        }
        throw new InvalidStandardWorkWeekException("scopeType không hợp lệ: " + scopeTypeStr);
    }

    private CapacityUnit parseCapacityUnit(String unitStr) {
        if (unitStr == null || unitStr.isBlank()) {
            return CapacityUnit.HOURS;
        }
        try {
            return CapacityUnit.valueOf(unitStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidStandardWorkWeekException("Đơn vị tính năng lực không hợp lệ: " + unitStr);
        }
    }

    private WeekStartDay parseWeekStartDay(String dayStr) {
        if (dayStr == null || dayStr.isBlank()) {
            return WeekStartDay.MONDAY;
        }
        try {
            return WeekStartDay.valueOf(dayStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidStandardWorkWeekException("Ngày bắt đầu tuần không hợp lệ: " + dayStr);
        }
    }

    private List<StandardWorkWeekDay> mapDaysToDomain(List<StandardWorkWeekDayDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new InvalidStandardWorkWeekException("Danh sách ngày làm việc trong tuần không được rỗng");
        }
        return dtos.stream()
                .map(dto -> new StandardWorkWeekDay(dto.dayOfWeek(), dto.isWorkingDay(), dto.workingHours()))
                .toList();
    }

    private StandardWorkWeekConfigResult mapToResult(StandardWorkWeekConfig config, boolean isInherited) {
        List<StandardWorkWeekDayDto> dayDtos = config.getDays().stream()
                .map(d -> new StandardWorkWeekDayDto(d.getDayOfWeek(), d.isWorkingDay(), d.getWorkingHours()))
                .toList();

        return new StandardWorkWeekConfigResult(
                config.getId(),
                config.getScope().scopeType().name(),
                config.getScope().orgUnitId(),
                config.getScope().toScopeKey(),
                config.getCapacityUnit().name(),
                config.getWeekStartDay().name(),
                config.getStandardHoursPerDay(),
                config.getStandardHoursPerWeek(),
                dayDtos,
                config.getCreatedBy(),
                config.getUpdatedBy(),
                config.getUpdatedAt(),
                isInherited
        );
    }
}
