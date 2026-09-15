package com.hrm.employeemanagement.infrastructure.transaction.task;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.task.tracking.ProjectTaskTrackingResult;
import com.hrm.employeemanagement.application.dto.task.tracking.TaskTrackingQuery;
import com.hrm.employeemanagement.application.port.inbound.task.GetProjectTaskTrackingUseCase;

/**
 * Transactional Decorator cho Use Case xem bảng theo dõi công việc dự án (NCL-04-CN-003).
 * Áp dụng read-only transaction để tối ưu hóa hiệu năng Connection Pool và tắt dirty checking Hibernate.
 */
public class TransactionalGetProjectTaskTrackingUseCase implements GetProjectTaskTrackingUseCase {

    private final GetProjectTaskTrackingUseCase delegate;

    public TransactionalGetProjectTaskTrackingUseCase(GetProjectTaskTrackingUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate GetProjectTaskTrackingUseCase must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectTaskTrackingResult getTaskTracking(TaskTrackingQuery query) {
        return delegate.getTaskTracking(query);
    }
}
