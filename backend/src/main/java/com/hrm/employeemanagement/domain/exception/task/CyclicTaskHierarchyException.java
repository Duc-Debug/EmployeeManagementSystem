package com.hrm.employeemanagement.domain.exception.task;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class CyclicTaskHierarchyException extends DomainException {
    public CyclicTaskHierarchyException(String message) {
        super(message);
    }

    public static CyclicTaskHierarchyException forCycle(Long taskId, Long parentId) {
        return new CyclicTaskHierarchyException(
                "Phát hiện chu trình phân cấp: công việc ID " + taskId + " không thể chọn công việc ID " + parentId
                        + " làm cha");
    }
}
