package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectResourceDemandJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectRoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectResourceDemandRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.projection.EmployeeSkillCapacityProjection;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataEmployeeSkillRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataSkillRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecruitmentDemandReportPersistenceAdapter Tests")
class RecruitmentDemandReportPersistenceAdapterTest {

    @Mock
    private SpringDataSkillRepository skillRepository;

    @Mock
    private SpringDataProjectRoleRepository projectRoleRepository;

    @Mock
    private SpringDataProjectResourceDemandRepository projectResourceDemandRepository;

    @Mock
    private SpringDataEmployeeSkillRepository employeeSkillRepository;

    @InjectMocks
    private RecruitmentDemandReportPersistenceAdapter adapter;

    @Test
    @DisplayName("HIGH-04 & HIGH-01: Tải danh sách skill active và filter demand theo orgUnitId")
    void testLoadAllActiveSkills_And_DemandFiltering() {
        SkillJpaEntity activeSkill = new SkillJpaEntity(1L, "JAVA", "Java Skill", "Backend", "Desc", LocalDateTime.now());
        activeSkill.setStatus("ACTIVE");
        activeSkill.setGroupId(10L);

        when(skillRepository.findAllActiveSkills()).thenReturn(List.of(activeSkill));

        List<Skill> skills = adapter.loadAllActiveSkills();
        assertEquals(1, skills.size());
        assertEquals("JAVA", skills.get(0).getCode());

        ProjectRoleJpaEntity role = new ProjectRoleJpaEntity(100L, "DEV", "Developer", "Desc", 10L, "ACTIVE");
        when(projectRoleRepository.findAll()).thenReturn(List.of(role));

        ProjectResourceDemandJpaEntity demand = new ProjectResourceDemandJpaEntity(1L, 50L, 100L, 2026, 1, BigDecimal.valueOf(120), 0L);
        when(projectResourceDemandRepository.findDemandsFiltered(10L, 2026, 1, 2026, 4))
                .thenReturn(List.of(demand));

        Map<Long, BigDecimal> demandMap = adapter.loadProjectDemandHoursGroupedBySkill(2026, 1, 2026, 4, 10L);

        assertEquals(1, demandMap.size());
        assertEquals(BigDecimal.valueOf(120), demandMap.get(1L));

        verify(projectResourceDemandRepository).findDemandsFiltered(10L, 2026, 1, 2026, 4);
    }

    @Test
    @DisplayName("HIGH-02: Tính toán capacity dựa trên standardHoursPerWeek thực tế của nhân sự")
    void testLoadAvailableCapacityHoursGroupedBySkill_ActualStandardHours() {
        EmployeeSkillCapacityProjection proj1 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 1L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
        };
        EmployeeSkillCapacityProjection proj2 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 1L; }
            @Override public Long getEmployeeId() { return 102L; }
            @Override public Integer getStandardHoursPerWeek() { return 20; }
        };

        when(employeeSkillRepository.findApprovedCapacityByOrgUnit(5L))
                .thenReturn(List.of(proj1, proj2));

        // 4 tuần
        Map<Long, BigDecimal> capacityMap = adapter.loadAvailableCapacityHoursGroupedBySkill(2026, 1, 2026, 4, 5L);

        // 40h * 4 = 160, 20h * 4 = 80 => Tổng: 240h
        assertEquals(1, capacityMap.size());
        assertEquals(BigDecimal.valueOf(240L), capacityMap.get(1L));
    }

    @Test
    @DisplayName("HIGH-03: Role không khớp skill nào sẽ trả về unmapped (null) mà không dùng heuristic hay fallback")
    void testUnmappedRole_ReturnsNull() {
        SkillJpaEntity javaSkill = new SkillJpaEntity(1L, "JAVA", "Java Skill", "Backend", "Desc", LocalDateTime.now());
        javaSkill.setStatus("ACTIVE");
        javaSkill.setGroupId(10L);

        when(skillRepository.findAllActiveSkills()).thenReturn(List.of(javaSkill));

        // Role có skillGroupId không khớp và code/name khác hẳn
        ProjectRoleJpaEntity unknownRole = new ProjectRoleJpaEntity(200L, "UNKNOWN", "Unknown Role", "Desc", 99L, "ACTIVE");
        when(projectRoleRepository.findAll()).thenReturn(List.of(unknownRole));

        ProjectResourceDemandJpaEntity demand = new ProjectResourceDemandJpaEntity(2L, 50L, 200L, 2026, 1, BigDecimal.valueOf(100), 0L);
        when(projectResourceDemandRepository.findDemandsFiltered(null, 2026, 1, 2026, 4))
                .thenReturn(List.of(demand));

        Map<Long, BigDecimal> demandMap = adapter.loadProjectDemandHoursGroupedBySkill(2026, 1, 2026, 4, null);

        // Không có skill nào được map
        assertEquals(0, demandMap.size());
    }
}
