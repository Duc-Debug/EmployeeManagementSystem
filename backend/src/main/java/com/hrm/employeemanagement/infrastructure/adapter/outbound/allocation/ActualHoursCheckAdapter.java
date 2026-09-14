package com.hrm.employeemanagement.infrastructure.adapter.outbound.allocation;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.allocation.CheckActualHoursPort;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.repository.SpringDataTaskRepository;

/**
 * Adapter kiểm tra giờ công thực tế phát sinh (TC-02, K3).
 * Kiểm tra các công việc dự án được giao cho nhân sự đã có actualHours > 0 hay chưa.
 * Thiết kế mở sẵn sàng kết nối trực tiếp với module Bảng chấm công (Timesheet / NCL-04) khi merge.
 */
@Component
public class ActualHoursCheckAdapter implements CheckActualHoursPort {

    private final SpringDataTaskRepository taskRepository;

    public ActualHoursCheckAdapter(Optional<SpringDataTaskRepository> taskRepository) {
        this.taskRepository = taskRepository.orElse(null);
    }

    @Override
    public boolean hasActualHours(Long employeeId, Long projectId, YearWeek yearWeek) {
        if (employeeId == null || projectId == null) {
            return false;
        }
        if (taskRepository != null) {
            return taskRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId).stream()
                    .anyMatch(t -> employeeId.equals(t.getAssigneeId())
                            && t.getActualHours() != null
                            && t.getActualHours().compareTo(BigDecimal.ZERO) > 0);
        }
        return false;
    }
}
