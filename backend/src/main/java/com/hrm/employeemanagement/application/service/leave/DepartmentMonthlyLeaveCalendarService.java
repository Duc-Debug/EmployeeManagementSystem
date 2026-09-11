package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.DepartmentMonthlyLeaveCalendarResult;
import com.hrm.employeemanagement.application.dto.leave.GetDepartmentMonthlyLeaveCalendarQuery;
import com.hrm.employeemanagement.application.port.inbound.leave.GetDepartmentMonthlyLeaveCalendarUseCase;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadDepartmentMonthlyLeavePort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.leave.DepartmentMonthlyLeaveCalendar;
import com.hrm.employeemanagement.domain.leave.LeaveCalendarItem;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import com.hrm.employeemanagement.application.port.outbound.calendar.HolidayQueryPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.HolidayRecord;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application Service triển khai Use Case xem lịch nghỉ của bộ phận theo tháng (NCL-05-CN-006).
 * Tuân thủ chuẩn Hexagonal Architecture (Pure Java, không phụ thuộc Spring Framework).
 */
public class DepartmentMonthlyLeaveCalendarService implements GetDepartmentMonthlyLeaveCalendarUseCase {

    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final LoadDepartmentMonthlyLeavePort loadDepartmentMonthlyLeavePort;
    private final AuthorizationService authorizationService;
    private final SaveAuditLogPort saveAuditLogPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;
    private final HolidayQueryPort holidayQueryPort;

    public DepartmentMonthlyLeaveCalendarService(
            LoadOrgUnitPort loadOrgUnitPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadDepartmentMonthlyLeavePort loadDepartmentMonthlyLeavePort,
            AuthorizationService authorizationService,
            SaveAuditLogPort saveAuditLogPort
    ) {
        this(loadOrgUnitPort, loadEmployeePort, loadUserPort, loadDepartmentMonthlyLeavePort, authorizationService, saveAuditLogPort, null, null);
    }

    public DepartmentMonthlyLeaveCalendarService(
            LoadOrgUnitPort loadOrgUnitPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadDepartmentMonthlyLeavePort loadDepartmentMonthlyLeavePort,
            AuthorizationService authorizationService,
            SaveAuditLogPort saveAuditLogPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            HolidayQueryPort holidayQueryPort
    ) {
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadDepartmentMonthlyLeavePort = Objects.requireNonNull(loadDepartmentMonthlyLeavePort, "LoadDepartmentMonthlyLeavePort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
        this.holidayQueryPort = holidayQueryPort;
    }

    @Override
    public DepartmentMonthlyLeaveCalendarResult execute(GetDepartmentMonthlyLeaveCalendarQuery query) {
        Objects.requireNonNull(query, "Query must not be null");

        // 1. Kiểm tra quyền hạn bắt buộc: DEPARTMENT_LEAVE_READ (TC-03)
        Long currentUserId = authorizationService.require(PermissionCode.DEPARTMENT_LEAVE_READ);

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        // 2. Kiểm tra bộ phận/đơn vị tồn tại
        OrgUnit orgUnit = loadOrgUnitPort.findById(new OrgUnitId(query.orgUnitId()))
                .orElseThrow(() -> new OrgUnitNotFoundException("Không tìm thấy đơn vị/bộ phận với ID: " + query.orgUnitId()));

        // 3. Kiểm tra Data Scope RBAC (VT-03 quản lý ORGANIZATION_BRANCH; VT-01/VT-06 COMPANY)
        requireOrgUnitInScope(currentUser, query.orgUnitId());

        // 4. Xác định tháng và năm truy vấn
        LocalDate now = LocalDate.now();
        int targetYear = query.year() != null ? query.year() : now.getYear();
        int targetMonth = query.month() != null ? query.month() : now.getMonthValue();
        YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        // 5. Lấy danh sách nhân viên đang hoạt động trong bộ phận (hoặc toàn bộ nhánh con nếu includeSubUnits = true)
        List<Employee> activeEmployees;
        if (Boolean.TRUE.equals(query.includeSubUnits())) {
            List<Employee> branchEmployees = new ArrayList<>();
            int pageSize = 500;
            int offset = 0;
            while (true) {
                List<Employee> page = loadEmployeePort.findByOrgUnitBranch(query.orgUnitId(), pageSize, offset);
                if (page == null || page.isEmpty()) {
                    break;
                }
                branchEmployees.addAll(page);
                if (page.size() < pageSize) {
                    break;
                }
                offset += pageSize;
            }
            activeEmployees = branchEmployees.stream()
                    .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE)
                    .toList();
        } else {
            activeEmployees = loadEmployeePort.findActiveByOrgUnitId(query.orgUnitId());
        }

        List<LeaveCalendarItem> leaveItems;
        if (activeEmployees.isEmpty()) {
            leaveItems = Collections.emptyList();
        } else {
            Map<Long, Employee> employeeMap = activeEmployees.stream()
                    .filter(e -> e.getIdValue() != null)
                    .collect(Collectors.toMap(Employee::getIdValue, e -> e, (e1, e2) -> e1));

            List<Long> employeeIds = activeEmployees.stream()
                    .map(Employee::getIdValue)
                    .filter(Objects::nonNull)
                    .toList();

            // 6. Lấy danh sách đơn nghỉ trong tháng (cả APPROVED và PENDING theo mô tả User Story)
            leaveItems = loadDepartmentMonthlyLeavePort.findLeavesForEmployees(
                    employeeIds,
                    monthStart,
                    monthEnd,
                    employeeMap
            );
        }

        // Tải lịch làm việc và danh sách ngày lễ toàn công ty
        CompanyWorkingCalendar companyCalendar = loadWorkingCalendarPort != null ? loadWorkingCalendarPort.loadCompanyCalendar() : null;
        Set<LocalDate> holidayDates = (holidayQueryPort != null)
                ? holidayQueryPort.findByYear(targetYear).stream()
                        .map(HolidayRecord::date)
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        // 7. Tạo Domain Aggregate và thực thi chính sách cảnh báo ngưỡng (kèm nhận diện ngày nghỉ/lễ và tổng giờ nghỉ)
        DepartmentMonthlyLeaveCalendar calendar = DepartmentMonthlyLeaveCalendar.calculate(
                query.orgUnitId(),
                orgUnit.getUnitCode(),
                orgUnit.getUnitName(),
                targetYear,
                targetMonth,
                activeEmployees.size(),
                query.warningThresholdRate(),
                leaveItems,
                companyCalendar,
                holidayDates
        );

        // 8. Lưu lịch sử kiểm toán thao tác xem lịch nghỉ bộ phận (TC-04)
        saveAuditLogPort.save(AuditLog.create(
                currentUserId,
                "VIEW_DEPARTMENT_LEAVE_CALENDAR",
                "leave_requests",
                query.orgUnitId()
        ));

        return DepartmentMonthlyLeaveCalendarResult.from(calendar);
    }

    private void requireOrgUnitInScope(User currentUser, Long orgUnitId) {
        boolean inScope = switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case ORGANIZATION_BRANCH -> currentUser.getScopeOrgUnitId() != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, currentUser.getScopeOrgUnitId());
            case SELF -> false;
        };

        if (!inScope) {
            saveAuditLogPort.save(AuditLog.createChange(
                    currentUser.getIdValue(),
                    "ACCESS_DENIED",
                    "org_units",
                    orgUnitId,
                    null,
                    "reason=OUT_OF_DATA_SCOPE;scopeOrgUnitId=" + currentUser.getScopeOrgUnitId()
            ));
            throw new PermissionDeniedException(PermissionCode.DEPARTMENT_LEAVE_READ);
        }
    }
}
