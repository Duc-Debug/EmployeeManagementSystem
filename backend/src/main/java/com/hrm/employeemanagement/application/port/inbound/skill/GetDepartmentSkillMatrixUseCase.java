package com.hrm.employeemanagement.application.port.inbound.skill;

import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult;

public interface GetDepartmentSkillMatrixUseCase {
    /**
     * Thực thi tính toán và trả về ma trận kỹ năng của bộ phận theo orgUnitId.
     *
     * @param orgUnitId ID của đơn vị/bộ phận cần xem ma trận
     * @return Ma trận kỹ năng đầy đủ gồm danh sách nhân sự, kỹ năng và tổng kết
     */
    DepartmentSkillMatrixResult execute(Long orgUnitId);
}
