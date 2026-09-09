package com.hrm.employeemanagement.application.port.outbound.allocation;

import java.util.List;

import com.hrm.employeemanagement.application.dto.allocation.ResourceCandidate;

/**
 * Output port để tầng Application lấy danh sách nhân sự thỏa mãn điều kiện kỹ
 * năng.
 */
public interface SearchResourcePort {

    /**
     * Tìm các nhân sự đang ACTIVE có kỹ năng skillId đã được APPROVED với
     * proficiencyLevel >= minProficiencyLevel.
     */
    List<ResourceCandidate> findActiveEmployeesBySkill(Long skillId, int minProficiencyLevel);
}

