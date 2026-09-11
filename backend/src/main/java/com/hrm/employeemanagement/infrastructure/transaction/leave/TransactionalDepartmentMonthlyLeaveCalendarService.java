package com.hrm.employeemanagement.infrastructure.transaction.leave;

import com.hrm.employeemanagement.application.dto.leave.DepartmentMonthlyLeaveCalendarResult;
import com.hrm.employeemanagement.application.dto.leave.GetDepartmentMonthlyLeaveCalendarQuery;
import com.hrm.employeemanagement.application.port.inbound.leave.GetDepartmentMonthlyLeaveCalendarUseCase;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Transactional Decorator quản lý transaction boundary cho Use Case xem lịch nghỉ bộ phận (NCL-05-CN-006).
 */
public class TransactionalDepartmentMonthlyLeaveCalendarService implements GetDepartmentMonthlyLeaveCalendarUseCase {

    private final GetDepartmentMonthlyLeaveCalendarUseCase delegate;

    public TransactionalDepartmentMonthlyLeaveCalendarService(GetDepartmentMonthlyLeaveCalendarUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate UseCase must not be null");
    }

    @Override
    @Transactional(readOnly = false) // readOnly = false vì cần lưu audit log vào CSDL
    public DepartmentMonthlyLeaveCalendarResult execute(GetDepartmentMonthlyLeaveCalendarQuery query) {
        return delegate.execute(query);
    }
}
