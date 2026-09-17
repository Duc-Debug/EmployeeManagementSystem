package com.hrm.employeemanagement.infrastructure.transaction.report.timesheetvariance;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceQuery;
import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceResult;
import com.hrm.employeemanagement.application.port.inbound.report.timesheetvariance.GetTimesheetVarianceUseCase;

/**
 * Transactional Decorator cho Use Case Báo cáo đối chiếu giờ phân bổ với giờ thực tế (NCL-09-CN-004).
 */
public class TransactionalTimesheetVarianceUseCaseDecorator implements GetTimesheetVarianceUseCase {

    private final GetTimesheetVarianceUseCase delegate;

    public TransactionalTimesheetVarianceUseCaseDecorator(GetTimesheetVarianceUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "GetTimesheetVarianceUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public TimesheetVarianceResult execute(TimesheetVarianceQuery query) {
        return delegate.execute(query);
    }
}
