package com.hrm.employeemanagement.application.dto.skill;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO kết quả trả về ma trận kỹ năng của một bộ phận/đơn vị (NCL-02-CN-007).
 */
public record DepartmentSkillMatrixResult(
        Long orgUnitId,
        String orgUnitCode,
        String orgUnitName,
        List<SkillMatrixSkillHeaderResult> skills,
        List<SkillMatrixRowResult> rows,
        SkillMatrixSummaryResult summary
) {
    /**
     * Thông tin cột kỹ năng trên ma trận, kèm số lượng người nắm và cờ rủi ro phụ thuộc 1 người (TC-01, TC-02).
     */
    public record SkillMatrixSkillHeaderResult(
            Long id,
            String code,
            String name,
            String category,
            int employeeCount,
            boolean singlePersonRisk
    ) {}
    /**
     * Thông tin một ô (Cell) ma trận: mức thành thạo và kinh nghiệm của một nhân sự đối với một kỹ năng.
     */
    public record SkillMatrixCellResult(
            Long skillId,
            Integer proficiencyLevel,
            BigDecimal yearsOfExperience,
            String reviewNotes
    ) {}
    /**
     * Thông tin một hàng (Row) nhân sự trên ma trận.
     */
    public record SkillMatrixRowResult(
            Long employeeId,
            String employeeCode,
            String fullName,
            String professionalRole,
            Map<Long, SkillMatrixCellResult> skills
    ) {}
    /**
     * Thống kê tổng hợp của ma trận.
     */
    public record SkillMatrixSummaryResult(
            int totalEmployees,
            int totalSkills,
            int singlePersonRiskSkillCount,
            int unstaffedSkillCount
    ) {}
}