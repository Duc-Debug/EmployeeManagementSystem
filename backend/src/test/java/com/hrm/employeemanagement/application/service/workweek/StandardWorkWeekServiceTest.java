package com.hrm.employeemanagement.application.service.workweek;

import com.hrm.employeemanagement.application.dto.workweek.CapacityConversionCommand;
import com.hrm.employeemanagement.application.dto.workweek.CapacityConversionResult;
import com.hrm.employeemanagement.application.dto.workweek.StandardWorkWeekConfigResult;
import com.hrm.employeemanagement.application.dto.workweek.StandardWorkWeekDayDto;
import com.hrm.employeemanagement.application.dto.workweek.UpdateStandardWorkWeekCommand;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.workweek.LoadStandardWorkWeekPort;
import com.hrm.employeemanagement.application.port.outbound.workweek.SaveStandardWorkWeekPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.workweek.StandardWorkWeekVersionConflictException;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.workweek.StandardWorkWeekConfig;
import com.hrm.employeemanagement.domain.workweek.WorkWeekScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class StandardWorkWeekServiceTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private LoadStandardWorkWeekPort loadStandardWorkWeekPort;

    @Mock
    private SaveStandardWorkWeekPort saveStandardWorkWeekPort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    private StandardWorkWeekService service;

    @BeforeEach
    void setUp() {
        service = new StandardWorkWeekService(
                authorizationService,
                loadStandardWorkWeekPort,
                saveStandardWorkWeekPort,
                loadOrgUnitPort,
                saveAuditLogPort
        );
    }

    private List<StandardWorkWeekDayDto> createStandardDayDtos(BigDecimal weekdayHours, BigDecimal saturdayHours) {
        return List.of(
                new StandardWorkWeekDayDto(DayOfWeek.MONDAY, true, weekdayHours),
                new StandardWorkWeekDayDto(DayOfWeek.TUESDAY, true, weekdayHours),
                new StandardWorkWeekDayDto(DayOfWeek.WEDNESDAY, true, weekdayHours),
                new StandardWorkWeekDayDto(DayOfWeek.THURSDAY, true, weekdayHours),
                new StandardWorkWeekDayDto(DayOfWeek.FRIDAY, true, weekdayHours),
                new StandardWorkWeekDayDto(DayOfWeek.SATURDAY, saturdayHours.compareTo(BigDecimal.ZERO) > 0, saturdayHours),
                new StandardWorkWeekDayDto(DayOfWeek.SUNDAY, false, BigDecimal.ZERO)
        );
    }

    @Test
    @DisplayName("Lấy cấu hình cấp công ty thành công")
    void getConfig_company_success() {
        StandardWorkWeekConfig companyConfig = StandardWorkWeekConfig.createDefaultCompany(1L);
        when(loadStandardWorkWeekPort.findCompanyDefault()).thenReturn(Optional.of(companyConfig));

        StandardWorkWeekConfigResult result = service.execute("COMPANY", null);

        assertThat(result.scopeType()).isEqualTo("COMPANY");
        assertThat(result.isInherited()).isFalse();
        assertThat(result.standardHoursPerWeek()).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("Lấy cấu hình cấp ORG_UNIT tự động fallback sang COMPANY nếu đơn vị chưa cấu hình riêng")
    void getConfig_orgUnit_fallbackToCompany() {
        when(loadStandardWorkWeekPort.findByScope(WorkWeekScope.orgUnit(10L))).thenReturn(Optional.empty());
        StandardWorkWeekConfig companyConfig = StandardWorkWeekConfig.createDefaultCompany(1L);
        when(loadStandardWorkWeekPort.findCompanyDefault()).thenReturn(Optional.of(companyConfig));

        StandardWorkWeekConfigResult result = service.execute("ORG_UNIT", 10L);

        assertThat(result.isInherited()).isTrue();
        assertThat(result.scopeKey()).isEqualTo("COMPANY:DEFAULT");
        assertThat(result.standardHoursPerWeek()).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("Cập nhật cấu hình tuần chuẩn thành công và ghi Audit Log")
    void updateConfig_success() {
        when(authorizationService.require(any(PermissionCode.class))).thenReturn(100L);

        StandardWorkWeekConfig existing = StandardWorkWeekConfig.createDefaultCompany(1L);
        existing.setId(1L);
        when(loadStandardWorkWeekPort.findByScope(WorkWeekScope.company())).thenReturn(Optional.of(existing));
        when(saveStandardWorkWeekPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // T2-T6: 8h, T7: 4h -> tổng 44h/tuần
        List<StandardWorkWeekDayDto> days = createStandardDayDtos(BigDecimal.valueOf(8), BigDecimal.valueOf(4));
        UpdateStandardWorkWeekCommand command = new UpdateStandardWorkWeekCommand(
                "COMPANY",
                null,
                "HOURS",
                "MONDAY",
                BigDecimal.valueOf(8),
                days,
                0L
        );

        StandardWorkWeekConfigResult result = service.execute(command);

        assertThat(result.standardHoursPerWeek()).isEqualByComparingTo("44.00");
        verify(saveStandardWorkWeekPort).save(any());
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getOldValue()).contains("totalHours=40.00");
        assertThat(auditCaptor.getValue().getNewValue()).contains("totalHours=44.00");
    }

    @Test
    @DisplayName("Cập nhật cấu hình đơn vị ORG_UNIT ném ngoại lệ nếu đơn vị không tồn tại")
    void updateConfig_orgUnitNotFound_throwsException() {
        when(authorizationService.require(any(PermissionCode.class))).thenReturn(100L);
        when(loadOrgUnitPort.findById(new OrgUnitId(999L))).thenReturn(Optional.empty());

        List<StandardWorkWeekDayDto> days = createStandardDayDtos(BigDecimal.valueOf(8), BigDecimal.ZERO);
        UpdateStandardWorkWeekCommand command = new UpdateStandardWorkWeekCommand(
                "ORG_UNIT",
                999L,
                "HOURS",
                "MONDAY",
                BigDecimal.valueOf(8),
                days,
                null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(OrgUnitNotFoundException.class)
                .hasMessageContaining("Không tìm thấy đơn vị");
    }

    @Test
    @DisplayName("Quy đổi đơn vị năng lực thành công")
    void convertCapacity_success() {
        StandardWorkWeekConfig companyConfig = StandardWorkWeekConfig.createDefaultCompany(1L);
        when(loadStandardWorkWeekPort.findCompanyDefault()).thenReturn(Optional.of(companyConfig));

        CapacityConversionCommand command = new CapacityConversionCommand(
                BigDecimal.valueOf(16.0),
                "HOURS",
                "DAYS",
                "COMPANY",
                null
        );

        CapacityConversionResult result = service.execute(command);

        assertThat(result.convertedValue()).isEqualByComparingTo("2.00");
        assertThat(result.toUnit()).isEqualTo("DAYS");
    }

    @Test
    @DisplayName("Từ chối truy cập khi không có quyền")
    void permissionDenied_throwsException() {
        when(authorizationService.require(any(PermissionCode.class)))
                .thenThrow(new PermissionDeniedException(PermissionCode.STANDARD_WORK_WEEK_READ));

        assertThatThrownBy(() -> service.execute("COMPANY", null))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("Không fallback authorization khi permission service gặp lỗi hệ thống")
    void authorizationInfrastructureFailure_isPropagated() {
        IllegalStateException failure = new IllegalStateException("authorization store unavailable");
        when(authorizationService.require(PermissionCode.STANDARD_WORK_WEEK_READ)).thenThrow(failure);

        assertThatThrownBy(() -> service.execute("COMPANY", null)).isSameAs(failure);
        verify(authorizationService, never()).require(PermissionCode.WORKING_CALENDAR_READ);
    }

    @Test
    @DisplayName("Từ chối cập nhật bằng version cũ trước khi ghi dữ liệu")
    void updateConfig_staleVersion_throwsConflict() {
        when(authorizationService.require(PermissionCode.STANDARD_WORK_WEEK_MANAGE)).thenReturn(100L);
        StandardWorkWeekConfig existing = StandardWorkWeekConfig.createDefaultCompany(1L);
        existing.setId(1L);
        when(loadStandardWorkWeekPort.findByScope(WorkWeekScope.company())).thenReturn(Optional.of(existing));

        UpdateStandardWorkWeekCommand command = new UpdateStandardWorkWeekCommand(
                "COMPANY", null, "HOURS", "MONDAY", BigDecimal.valueOf(8),
                createStandardDayDtos(BigDecimal.valueOf(8), BigDecimal.ZERO), 99L);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(StandardWorkWeekVersionConflictException.class);
        verify(saveStandardWorkWeekPort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }
}
