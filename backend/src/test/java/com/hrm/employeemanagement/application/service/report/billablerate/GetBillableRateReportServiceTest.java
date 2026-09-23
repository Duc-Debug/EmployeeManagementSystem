package com.hrm.employeemanagement.application.service.report.billablerate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateExport;
import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateItem;
import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateQuery;
import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateResult;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.report.billablerate.LoadBillableRateTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.report.billablerate.LoadBillableRateTimesheetPort.EmployeeHoursData;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

@DisplayName("GetBillableRateReportService Unit Tests (NCL-10-CN-002)")
class GetBillableRateReportServiceTest {

    private AuthorizationService authorizationService;
    private LoadUserPort loadUserPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private LoadHolidaysPort loadHolidaysPort;
    private LoadApprovedLeavesPort loadApprovedLeavesPort;
    private LoadBillableRateTimesheetPort loadBillableRateTimesheetPort;
    private SaveAuditLogPort saveAuditLogPort;

    private GetBillableRateReportService service;

    private final Long USER_VT01_ID = 100L;
    private final Long ORG_UNIT_ID = 10L;
    private final Long EMPLOYEE_ID = 1L;

    @BeforeEach
    void setUp() {
        authorizationService = mock(AuthorizationService.class);
        loadUserPort = mock(LoadUserPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);
        loadWeeklyAvailabilityPort = mock(LoadWeeklyAvailabilityPort.class);
        loadHolidaysPort = mock(LoadHolidaysPort.class);
        loadApprovedLeavesPort = mock(LoadApprovedLeavesPort.class);
        loadBillableRateTimesheetPort = mock(LoadBillableRateTimesheetPort.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);

        service = new GetBillableRateReportService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadBillableRateTimesheetPort,
                null,
                saveAuditLogPort
        );
    }

    private User createExecutiveUser() {
        User user = mock(User.class);
        when(user.getIdValue()).thenReturn(USER_VT01_ID);
        when(user.getDataScope()).thenReturn(DataScope.COMPANY);
        when(user.getScopeOrgUnitId()).thenReturn(null);
        return user;
    }

    private Employee createEmployee(Long id, String code, String name, Long orgId, int standardHours) {
        Employee emp = mock(Employee.class);
        when(emp.getIdValue()).thenReturn(id);
        when(emp.getEmployeeCode()).thenReturn(code);
        when(emp.getFullName()).thenReturn(name);
        when(emp.getOrgUnitId()).thenReturn(orgId);
        when(emp.getStandardHoursPerWeek()).thenReturn(standardHours);
        return emp;
    }

    @Test
    @DisplayName("NCL-10-CN-002-TC-01: Luồng thành công - 120h tính phí trên 160h khả dụng đạt tỷ lệ 75.0%")
    void testTC01_Success_120hBillableOn160hAvailable_Returns75Percent() {
        // Arrange
        User execUser = createExecutiveUser();
        when(authorizationService.require(PermissionCode.BILLABLE_HOURS_REPORT_READ)).thenReturn(USER_VT01_ID);
        when(loadUserPort.findById(new UserId(USER_VT01_ID))).thenReturn(Optional.of(execUser));

        Employee emp = createEmployee(EMPLOYEE_ID, "EMP001", "Nguyễn Văn A", ORG_UNIT_ID, 40);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));

        OrgUnit unit = mock(OrgUnit.class);
        when(unit.getUnitName()).thenReturn("Phòng Công Nghệ");
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID))).thenReturn(Optional.of(unit));

        // 4 tuần (W36 đến W39 năm 2026) -> 4 * 40h = 160h khả dụng
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any())).thenReturn(Map.of());

        // 120h tính phí, 10h không tính phí
        when(loadBillableRateTimesheetPort.loadApprovedHoursByEmployeesAndDateRange(any(), any(), any()))
                .thenReturn(Map.of(EMPLOYEE_ID, new EmployeeHoursData(BigDecimal.valueOf(120), BigDecimal.valueOf(10))));

        BillableRateQuery query = new BillableRateQuery(null, null, 2026, 36, 2026, 39);

        // Act
        BillableRateResult result = service.execute(query);

        // Assert
        assertNotNull(result);
        assertTrue(result.hasData());
        assertEquals(1, result.employeeBreakdown().size());

        BillableRateItem item = result.employeeBreakdown().getFirst();
        assertEquals(BigDecimal.valueOf(160).setScale(1), item.netAvailableHours());
        assertEquals(BigDecimal.valueOf(120).setScale(1), item.billableHours());
        assertEquals(BigDecimal.valueOf(10).setScale(1), item.nonBillableHours());
        assertEquals(BigDecimal.valueOf(130).setScale(1), item.totalActualHours());
        assertEquals(BigDecimal.valueOf(75.0).setScale(1), item.billableRate());
        assertTrue(item.hasAvailableHours());

        // Toàn công ty
        assertEquals(BigDecimal.valueOf(75.0).setScale(1), result.summary().overallBillableRate());
        assertEquals(BigDecimal.valueOf(160).setScale(1), result.summary().totalAvailableHours());
        assertEquals(BigDecimal.valueOf(120).setScale(1), result.summary().totalBillableHours());

        // Audit Log verification
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("NCL-10-CN-002-TC-02: Ngoại lệ - Nghỉ phép cả tuần (40h) được trừ khỏi mẫu số thay vì tính không hiệu quả")
    void testTC02_Exception_ApprovedLeaveWeek_DeductedFromDenominator() {
        // Arrange
        User execUser = createExecutiveUser();
        when(authorizationService.require(PermissionCode.BILLABLE_HOURS_REPORT_READ)).thenReturn(USER_VT01_ID);
        when(loadUserPort.findById(new UserId(USER_VT01_ID))).thenReturn(Optional.of(execUser));

        Employee emp = createEmployee(EMPLOYEE_ID, "EMP001", "Trần Thị B", ORG_UNIT_ID, 40);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));

        OrgUnit unit = mock(OrgUnit.class);
        when(unit.getUnitName()).thenReturn("Phòng Dự Án");
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID))).thenReturn(Optional.of(unit));

        // 4 tuần (160h chuẩn), nhưng nghỉ phép tuần 37 (40h đã duyệt) -> Mẫu số còn 120h
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());

        YearWeek w36 = YearWeek.of(2026, 36);
        YearWeek w37 = YearWeek.of(2026, 37);
        YearWeek w38 = YearWeek.of(2026, 38);
        YearWeek w39 = YearWeek.of(2026, 39);

        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any()))
                .thenReturn(Map.of(EMPLOYEE_ID, Map.of(
                        w36, BigDecimal.ZERO,
                        w37, BigDecimal.valueOf(40),
                        w38, BigDecimal.ZERO,
                        w39, BigDecimal.ZERO
                )));

        // Nhân viên làm 90h tính phí trong 3 tuần còn lại -> Tỷ lệ = 90 / 120 = 75.0%
        when(loadBillableRateTimesheetPort.loadApprovedHoursByEmployeesAndDateRange(any(), any(), any()))
                .thenReturn(Map.of(EMPLOYEE_ID, new EmployeeHoursData(BigDecimal.valueOf(90), BigDecimal.ZERO)));

        BillableRateQuery query = new BillableRateQuery(null, null, 2026, 36, 2026, 39);

        // Act
        BillableRateResult result = service.execute(query);

        // Assert
        BillableRateItem item = result.employeeBreakdown().getFirst();
        assertEquals(BigDecimal.valueOf(160).setScale(1), item.standardHours());
        assertEquals(BigDecimal.valueOf(40).setScale(1), item.approvedLeaveHours());
        assertEquals(BigDecimal.valueOf(120).setScale(1), item.netAvailableHours());
        assertEquals(BigDecimal.valueOf(90).setScale(1), item.billableHours());
        assertEquals(BigDecimal.valueOf(75.0).setScale(1), item.billableRate());
    }

    @Test
    @DisplayName("NCL-10-CN-002-TC-03: Không có quyền - Vai trò không hợp lệ ném PermissionDeniedException và ghi audit log")
    void testTC03_Unauthorized_ThrowsPermissionDeniedException_AndAudits() {
        // Arrange
        when(authorizationService.require(PermissionCode.BILLABLE_HOURS_REPORT_READ))
                .thenThrow(new PermissionDeniedException(PermissionCode.BILLABLE_HOURS_REPORT_READ));

        BillableRateQuery query = new BillableRateQuery(null, null, 2026, 36, 2026, 39);

        // Act & Assert
        assertThrows(PermissionDeniedException.class, () -> service.execute(query));
    }

    @Test
    @DisplayName("NCL-10-CN-002-TC-04: Xuất báo cáo CSV - Sinh nội dung CSV UTF-8 BOM đầy đủ")
    void testTC04_ExportCsv_GeneratesValidCsv() {
        // Arrange
        User execUser = createExecutiveUser();
        when(authorizationService.require(PermissionCode.BILLABLE_HOURS_REPORT_READ)).thenReturn(USER_VT01_ID);
        when(loadUserPort.findById(new UserId(USER_VT01_ID))).thenReturn(Optional.of(execUser));

        Employee emp = createEmployee(EMPLOYEE_ID, "EMP001", "Lê Văn C", ORG_UNIT_ID, 40);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));

        OrgUnit unit = mock(OrgUnit.class);
        when(unit.getUnitName()).thenReturn("Phòng QA");
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID))).thenReturn(Optional.of(unit));

        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any())).thenReturn(Map.of());
        when(loadBillableRateTimesheetPort.loadApprovedHoursByEmployeesAndDateRange(any(), any(), any()))
                .thenReturn(Map.of(EMPLOYEE_ID, new EmployeeHoursData(BigDecimal.valueOf(120), BigDecimal.ZERO)));

        BillableRateQuery query = new BillableRateQuery(null, null, 2026, 36, 2026, 39);

        // Act
        BillableRateExport export = service.export(query);

        // Assert
        assertNotNull(export);
        assertTrue(export.filename().endsWith(".csv"));
        assertTrue(export.content().length > 0);

        String csvString = new String(export.content(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csvString.contains("BÁO CÁO TỶ LỆ GIỜ TÍNH PHÍ"));
        assertTrue(csvString.contains("EMP001"));
        assertTrue(csvString.contains("Lê Văn C"));
        assertTrue(csvString.contains("75.0%"));
    }

    @Test
    @DisplayName("Nhân viên nghỉ phép toàn kỳ -> Net available hours = 0h -> Billable rate trả về null (N/A)")
    void testFullPeriodLeave_ReturnsNullRate() {
        // Arrange
        User execUser = createExecutiveUser();
        when(authorizationService.require(PermissionCode.BILLABLE_HOURS_REPORT_READ)).thenReturn(USER_VT01_ID);
        when(loadUserPort.findById(new UserId(USER_VT01_ID))).thenReturn(Optional.of(execUser));

        Employee emp = createEmployee(EMPLOYEE_ID, "EMP001", "Hoàng Văn D", ORG_UNIT_ID, 40);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));

        OrgUnit unit = mock(OrgUnit.class);
        when(unit.getUnitName()).thenReturn("Phòng Vận Hành");
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID))).thenReturn(Optional.of(unit));

        YearWeek w36 = YearWeek.of(2026, 36);
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any()))
                .thenReturn(Map.of(EMPLOYEE_ID, Map.of(w36, BigDecimal.valueOf(40))));

        when(loadBillableRateTimesheetPort.loadApprovedHoursByEmployeesAndDateRange(any(), any(), any()))
                .thenReturn(Map.of(EMPLOYEE_ID, new EmployeeHoursData(BigDecimal.ZERO, BigDecimal.ZERO)));

        BillableRateQuery query = new BillableRateQuery(null, null, 2026, 36, 2026, 36);

        // Act
        BillableRateResult result = service.execute(query);

        // Assert
        BillableRateItem item = result.employeeBreakdown().getFirst();
        assertEquals(BigDecimal.ZERO.setScale(1), item.netAvailableHours());
        assertNull(item.billableRate());
        assertFalse(item.hasAvailableHours());
        assertEquals("ON_LEAVE", item.status());
    }
}
