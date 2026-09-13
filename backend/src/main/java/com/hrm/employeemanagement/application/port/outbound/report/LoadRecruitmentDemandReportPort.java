package com.hrm.employeemanagement.application.port.outbound.report;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.application.dto.report.RecruitmentCapacityMetrics;
import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandMetrics;

public interface LoadRecruitmentDemandReportPort {

    /**
     * Tải danh sách tất cả kỹ năng đang hoạt động trong hệ thống catalog.
     */
    List<Skill> loadAllActiveSkills();

    /**
     * Tải tổng số giờ nhu cầu nhân sự của các dự án nhóm theo skill/role trong khoảng thời gian.
     * Return Map<SkillId, TotalRequiredHours>
     */
    Map<Long, BigDecimal> loadProjectDemandHoursGroupedBySkill(
            Integer fromYear, Integer fromWeek, Integer toYear, Integer toWeek, Long orgUnitId);

    /**
     * Tải tổng số giờ năng lực khả dụng hiện có của nhân sự theo từng kỹ năng trong khoảng thời gian.
     * Return Map<SkillId, TotalAvailableCapacityHours>
     */
    Map<Long, BigDecimal> loadAvailableCapacityHoursGroupedBySkill(
            Integer fromYear, Integer fromWeek, Integer toYear, Integer toWeek, Long orgUnitId);

    RecruitmentDemandMetrics loadProjectDemandMetrics(
            Integer fromYear, Integer fromWeek, Integer toYear, Integer toWeek, Long orgUnitId);

    RecruitmentCapacityMetrics loadAvailableCapacityMetrics(
            Integer fromYear, Integer fromWeek, Integer toYear, Integer toWeek, Long orgUnitId);
}
