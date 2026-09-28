package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave;

import com.hrm.employeemanagement.application.port.outbound.leave.LoadDepartmentMonthlyLeavePort;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.leave.LeaveCalendarItem;
import com.hrm.employeemanagement.domain.leave.LeaveStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity.LeaveRequestJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository.SpringDataLeaveRequestRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Persistence Adapter triển khai LoadDepartmentMonthlyLeavePort (NCL-05-CN-006).
 */
@Component
public class DepartmentLeaveCalendarRepositoryAdapter implements LoadDepartmentMonthlyLeavePort {

    private final SpringDataLeaveRequestRepository springDataLeaveRequestRepository;

    public DepartmentLeaveCalendarRepositoryAdapter(SpringDataLeaveRequestRepository springDataLeaveRequestRepository) {
        this.springDataLeaveRequestRepository = Objects.requireNonNull(springDataLeaveRequestRepository, "SpringDataLeaveRequestRepository must not be null");
    }

    @Override
    public List<LeaveCalendarItem> findLeavesForEmployees(
            List<Long> employeeIds,
            LocalDate startDate,
            LocalDate endDate,
            Map<Long, Employee> employeeMap
    ) {
        if (employeeIds == null || employeeIds.isEmpty() || startDate == null || endDate == null) {
            return Collections.emptyList();
        }

        List<LeaveRequestJpaEntity> entities = springDataLeaveRequestRepository.findDepartmentLeavesBetween(
                employeeIds,
                startDate,
                endDate
        );

        List<LeaveCalendarItem> result = new ArrayList<>(entities.size());
        for (LeaveRequestJpaEntity entity : entities) {
            Employee emp = (employeeMap != null) ? employeeMap.get(entity.getEmployeeId()) : null;
            String employeeCode = emp != null ? emp.getEmployeeCode() : "EMP-" + entity.getEmployeeId();
            String fullName = emp != null ? emp.getFullName() : "Nhân viên";

            LeaveStatus leaveStatus;
            try {
                leaveStatus = LeaveStatus.valueOf(entity.getStatus());
            } catch (Exception e) {
                leaveStatus = LeaveStatus.PENDING;
            }

            result.add(new LeaveCalendarItem(
                    entity.getId(),
                    entity.getEmployeeId(),
                    employeeCode,
                    fullName,
                    entity.getStartDate(),
                    entity.getEndDate(),
                    leaveStatus,
                    entity.getHoursDeducted(),
                    entity.getLeaveType(),
                    entity.getReason()
            ));
        }

        return result;
    }
}
