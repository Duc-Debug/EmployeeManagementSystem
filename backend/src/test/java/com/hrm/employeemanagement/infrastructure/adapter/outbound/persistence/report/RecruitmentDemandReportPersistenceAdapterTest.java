package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.availability.YearWeek;
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

    @Mock
    private LoadApprovedLeavesPort loadApprovedLeavesPort;

    @Mock
    private LoadHolidaysPort loadHolidaysPort;

    @Mock
    private LoadWeeklyProjectAllocationPort loadAllocationPort;

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
        when(projectRoleRepository.findAllById(anyList())).thenReturn(List.of(role));

        ProjectResourceDemandJpaEntity demand = new ProjectResourceDemandJpaEntity(1L, 50L, 100L, 2026, 1, BigDecimal.valueOf(120), 0L);
        when(projectResourceDemandRepository.findDemandsFiltered(10L, 2026, 1, 2026, 4))
                .thenReturn(List.of(demand));

        Map<Long, BigDecimal> demandMap = adapter.loadProjectDemandHoursGroupedBySkill(2026, 1, 2026, 4, 10L);

        assertEquals(1, demandMap.size());
        assertEquals(BigDecimal.valueOf(120), demandMap.get(1L));

        verify(projectResourceDemandRepository).findDemandsFiltered(10L, 2026, 1, 2026, 4);
    }

    @Test
    @DisplayName("HIGH-02: Single Employee with 1 Skill - 100% Capacity Assigned")
    void testLoadAvailableCapacity_SingleSkill() {
        EmployeeSkillCapacityProjection proj1 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 1L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
        };

        when(employeeSkillRepository.findApprovedCapacityByOrgUnit(5L))
                .thenReturn(List.of(proj1));

        // 4 tuần => 40h * 4 = 160h
        Map<Long, BigDecimal> capacityMap = adapter.loadAvailableCapacityHoursGroupedBySkill(2026, 1, 2026, 4, 5L);

        assertEquals(1, capacityMap.size());
        assertEquals(0, BigDecimal.valueOf(160.00).compareTo(capacityMap.get(1L)));
    }

    @Test
    @DisplayName("HIGH-02: Multi-Skill Capacity Attribution - Full Net Available Capacity per Skill Pool")
    void testLoadAvailableCapacity_MultipleSkills_FullNetCapacityPerSkill() {
        // Employee 101 có 2 kỹ năng: Skill 1 (Java) và Skill 2 (React)
        EmployeeSkillCapacityProjection emp1Skill1 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 1L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
        };
        EmployeeSkillCapacityProjection emp1Skill2 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 2L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
        };

        // Employee 102 chỉ có 1 kỹ năng: Skill 1 (Java)
        EmployeeSkillCapacityProjection emp2Skill1 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 1L; }
            @Override public Long getEmployeeId() { return 102L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
        };

        when(employeeSkillRepository.findApprovedCapacityByOrgUnit(null))
                .thenReturn(List.of(emp1Skill1, emp1Skill2, emp2Skill1));

        // 4 tuần: Emp 101 = 160h, Emp 102 = 160h
        // Skill 1 (Java): Emp 101 (160h) + Emp 102 (160h) = 320h
        // Skill 2 (React): Emp 101 (160h) = 160h
        Map<Long, BigDecimal> capacityMap = adapter.loadAvailableCapacityHoursGroupedBySkill(2026, 1, 2026, 4, null);

        assertEquals(2, capacityMap.size());
        assertEquals(0, BigDecimal.valueOf(320.00).compareTo(capacityMap.get(1L)));
        assertEquals(0, BigDecimal.valueOf(160.00).compareTo(capacityMap.get(2L)));
    }

    @Test
    @DisplayName("Capacity Calculation - Deducts Approved Leave and Project Allocations")
    void testLoadAvailableCapacity_DeductsLeaveAndAllocations() {
        EmployeeSkillCapacityProjection empProj = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 1L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
        };

        when(employeeSkillRepository.findApprovedCapacityByOrgUnit(null))
                .thenReturn(List.of(empProj));

        // Mock 1 tuần nghỉ phép (16h)
        YearWeek yw = YearWeek.of(2026, 1);
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList()))
                .thenReturn(Map.of(101L, Map.of(yw, BigDecimal.valueOf(16))));

        // Mock 1 tuần phân bổ dự án (10h)
        WeeklyProjectAllocation alloc = new WeeklyProjectAllocation(1L, 101L, 10L, yw, BigDecimal.valueOf(10));
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList()))
                .thenReturn(List.of(alloc));

        // 1 tuần: 40h standard - 16h leave - 10h allocation = 14h net available
        Map<Long, BigDecimal> capacityMap = adapter.loadAvailableCapacityHoursGroupedBySkill(2026, 1, 2026, 1, null);

        assertEquals(1, capacityMap.size());
        assertEquals(0, BigDecimal.valueOf(14.00).compareTo(capacityMap.get(1L)));
    }

    @Test
    @DisplayName("HIGH-03: Role không khớp skill nào sẽ trả về unmapped (null)")
    void testUnmappedRole_ReturnsNull() {
        SkillJpaEntity javaSkill = new SkillJpaEntity(1L, "JAVA", "Java Skill", "Backend", "Desc", LocalDateTime.now());
        javaSkill.setStatus("ACTIVE");
        javaSkill.setGroupId(10L);

        when(skillRepository.findAllActiveSkills()).thenReturn(List.of(javaSkill));

        ProjectRoleJpaEntity unknownRole = new ProjectRoleJpaEntity(200L, "UNKNOWN", "Unknown Role", "Desc", 99L, "ACTIVE");
        when(projectRoleRepository.findAllById(anyList())).thenReturn(List.of(unknownRole));

        ProjectResourceDemandJpaEntity demand = new ProjectResourceDemandJpaEntity(2L, 50L, 200L, 2026, 1, BigDecimal.valueOf(100), 0L);
        when(projectResourceDemandRepository.findDemandsFiltered(null, 2026, 1, 2026, 4))
                .thenReturn(List.of(demand));

        Map<Long, BigDecimal> demandMap = adapter.loadProjectDemandHoursGroupedBySkill(2026, 1, 2026, 4, null);

        assertEquals(0, demandMap.size());
    }
}
