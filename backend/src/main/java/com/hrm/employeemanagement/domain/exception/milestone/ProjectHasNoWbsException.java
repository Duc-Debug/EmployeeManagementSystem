package com.hrm.employeemanagement.domain.exception.milestone;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ProjectHasNoWbsException extends DomainException {
    public ProjectHasNoWbsException(Long projectId) {
        super("Dự án với ID " + projectId + " chưa có cây công việc (WBS). Vui lòng tạo cây công việc trước khi khai báo mốc tiến độ.");
    }
}
