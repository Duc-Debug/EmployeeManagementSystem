package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveImpactResult;
import com.hrm.employeemanagement.application.port.inbound.leave.GetLeaveImpactUseCase;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadProjectAllocationForLeavePort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.leave.LeaveRequestNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.*;

/**
 * NCL-05-CN-003: Đánh giá ảnh hưởng của việc nghỉ phép tới các dự án và phân bổ hiện có (TC-02).
 */
public class GetLeaveImpactService implements GetLeaveImpactUseCase {

    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadProjectAllocationForLeavePort loadProjectAllocationPort;
    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;

    public GetLeaveImpactService(
            LoadLeaveRequestPort loadLeaveRequestPort,
            LoadEmployeePort loadEmployeePort,
            LoadProjectAllocationForLeavePort loadProjectAllocationPort,
            AuthorizationService authorizationService
    ) {
        this(loadLeaveRequestPort, loadEmployeePort, loadProjectAllocationPort, authorizationService, null, null);
    }

    public GetLeaveImpactService(
            LoadLeaveRequestPort loadLeaveRequestPort,
            LoadEmployeePort loadEmployeePort,
            LoadProjectAllocationForLeavePort loadProjectAllocationPort,
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort
    ) {
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadProjectAllocationPort = Objects.requireNonNull(loadProjectAllocationPort, "loadProjectAllocationPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
        this.loadUserPort = loadUserPort;
        this.loadOrgUnitPort = loadOrgUnitPort;
    }

    @Override
    public LeaveImpactResult getLeaveImpact(Long leaveRequestId) {
        // 1. TC-03: Kiểm tra quyền truy cập
        Long currentUserId = authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE);

        // 2. Lấy đơn xin nghỉ
        LeaveRequest leaveRequest = loadLeaveRequestPort.findById(leaveRequestId)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Không tìm thấy đơn xin nghỉ phép với mã: " + leaveRequestId));

        // 3. Lấy thông tin nhân viên & kiểm tra Data Scope chống IDOR
        Optional<Employee> employeeOpt = loadEmployeePort.findById(new EmployeeId(leaveRequest.getEmployeeId()));
        String employeeName = employeeOpt.map(Employee::getFullName).orElse("Nhân viên #" + leaveRequest.getEmployeeId());

        if (employeeOpt.isPresent() && loadUserPort != null) {
            User currentUser = loadUserPort.findById(new UserId(currentUserId))
                    .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));
            requireEmployeeInScope(currentUser, employeeOpt.get(), PermissionCode.LEAVE_REQUEST_APPROVE);
        }

        // 4. Xác định danh sách các tuần ISO-8601 nằm trong khoảng ngày xin nghỉ
        Map<Integer, Set<Integer>> yearToWeeks = extractYearAndWeeks(leaveRequest.getStartDate(), leaveRequest.getEndDate());

        List<LeaveImpactResult.ProjectAllocationImpact> affectedProjects = new ArrayList<>();
        BigDecimal totalAllocatedHours = BigDecimal.ZERO;

        for (Map.Entry<Integer, Set<Integer>> entry : yearToWeeks.entrySet()) {
            Integer year = entry.getKey();
            List<Integer> weekNumbers = new ArrayList<>(entry.getValue());

            List<LoadProjectAllocationForLeavePort.ProjectAllocationInfo> allocations =
                    loadProjectAllocationPort.findAllocations(leaveRequest.getEmployeeId(), year, weekNumbers);

            for (LoadProjectAllocationForLeavePort.ProjectAllocationInfo alloc : allocations) {
                if (alloc.allocatedHours() != null && alloc.allocatedHours().compareTo(BigDecimal.ZERO) > 0) {
                    totalAllocatedHours = totalAllocatedHours.add(alloc.allocatedHours());
                    affectedProjects.add(new LeaveImpactResult.ProjectAllocationImpact(
                            alloc.projectId(),
                            alloc.projectName(),
                            alloc.year(),
                            alloc.weekNumber(),
                            alloc.allocatedHours()
                    ));
                }
            }
        }

        // 5. TC-02: Có xung đột nếu nhân viên đang được phân bổ giờ trong tuần nghỉ
        boolean hasConflict = totalAllocatedHours.compareTo(BigDecimal.ZERO) > 0;

        return new LeaveImpactResult(
                leaveRequest.getId(),
                leaveRequest.getEmployeeId(),
                employeeName,
                leaveRequest.getStartDate().toString(),
                leaveRequest.getEndDate().toString(),
                leaveRequest.getDaysCount(),
                leaveRequest.getHoursDeducted(),
                totalAllocatedHours,
                hasConflict,
                affectedProjects
        );
    }

    private Map<Integer, Set<Integer>> extractYearAndWeeks(LocalDate startDate, LocalDate endDate) {
        Map<Integer, Set<Integer>> map = new HashMap<>();
        LocalDate curr = startDate;
        while (!curr.isAfter(endDate)) {
            int year = curr.get(IsoFields.WEEK_BASED_YEAR);
            int week = curr.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            map.computeIfAbsent(year, k -> new HashSet<>()).add(week);
            curr = curr.plusDays(1);
        }
        return map;
    }

    private void requireEmployeeInScope(User currentUser, Employee employee, PermissionCode permission) {
        if (!isEmployeeInScope(currentUser, employee)) {
            throw new PermissionDeniedException(permission);
        }
    }

    private boolean isEmployeeInScope(User currentUser, Employee employee) {
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> currentUser.getIdValue() != null && currentUser.getIdValue().equals(employee.getUserIdValue());
            case ORGANIZATION_BRANCH -> employee.getOrgUnitId() != null
                    && currentUser.getScopeOrgUnitId() != null
                    && loadOrgUnitPort != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(
                            employee.getOrgUnitId(), currentUser.getScopeOrgUnitId());
        };
    }
}
