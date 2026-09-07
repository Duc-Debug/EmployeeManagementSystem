package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import com.hrm.employeemanagement.domain.skill.SkillStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.EmployeeSkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.projection.PendingEmployeeSkillProjection;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SpringDataEmployeeSkillRepositoryScopeTest {

    @Autowired
    private SpringDataEmployeeSkillRepository employeeSkillRepository;

    @Autowired
    private SpringDataSkillRepository skillRepository;

    @Autowired
    private SpringDataEmployeeRepository employeeRepository;

    @Autowired
    private SpringDataOrgUnitRepository orgUnitRepository;

    @Test
    @DisplayName("Branch query filters tree path correctly and ignores other branches")
    void testFindPendingBranchScope() {
        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();
        String suffix = String.valueOf(System.nanoTime());

        OrgUnitJpaEntity tech = childOrgUnit("TECH-" + suffix, "Tech Dept", root);
        OrgUnitJpaEntity hr = childOrgUnit("HR-" + suffix, "HR Dept", root);

        EmployeeJpaEntity devEmp = createEmployee("EMP-D-" + suffix, "Developer A", tech.getId());
        EmployeeJpaEntity hrEmp = createEmployee("EMP-H-" + suffix, "HR Officer B", hr.getId());

        SkillJpaEntity javaSkill = createSkill("JAVA-" + suffix, "Java Core");

        EmployeeSkillJpaEntity pendingDevSkill = employeeSkillRepository.save(new EmployeeSkillJpaEntity(
                null, devEmp.getId(), javaSkill.getId(), 3, new BigDecimal("2.0"), SkillStatus.PENDING, null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        ));
        EmployeeSkillJpaEntity pendingHrSkill = employeeSkillRepository.save(new EmployeeSkillJpaEntity(
                null, hrEmp.getId(), javaSkill.getId(), 2, new BigDecimal("1.0"), SkillStatus.PENDING, null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        ));

        List<PendingEmployeeSkillProjection> techScopeSkills = employeeSkillRepository.findPendingBranchScope(tech.getId(), null, 10, 0);
        assertEquals(1, techScopeSkills.size());
        assertEquals(pendingDevSkill.getId(), techScopeSkills.get(0).getId());
        assertEquals(devEmp.getFullName(), techScopeSkills.get(0).getEmployeeName());

        long count = employeeSkillRepository.countPendingBranchScope(tech.getId(), null);
        assertEquals(1, count);

        List<PendingEmployeeSkillProjection> companyScopeSkills = employeeSkillRepository.findPendingCompanyScope("Developer", 10, 0);
        assertTrue(companyScopeSkills.stream().anyMatch(s -> s.getId().equals(pendingDevSkill.getId())));
        assertFalse(companyScopeSkills.stream().anyMatch(s -> s.getId().equals(pendingHrSkill.getId())));
    }

    @Test
    @DisplayName("Optimistic locking version field is managed and incremented on save")
    void testOptimisticLockingVersionIncrement() {
        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();
        String suffix = String.valueOf(System.nanoTime());
        EmployeeJpaEntity emp = createEmployee("EMP-V-" + suffix, "Employee V", root.getId());
        SkillJpaEntity skill = createSkill("SKILL-V-" + suffix, "Skill V");

        EmployeeSkillJpaEntity entity = new EmployeeSkillJpaEntity(
                null, emp.getId(), skill.getId(), 3, new BigDecimal("1.5"), SkillStatus.PENDING, null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
        EmployeeSkillJpaEntity saved = employeeSkillRepository.saveAndFlush(entity);
        assertNotNull(saved.getVersion());
        assertEquals(0L, saved.getVersion());

        saved.setStatus(SkillStatus.APPROVED);
        EmployeeSkillJpaEntity updated = employeeSkillRepository.saveAndFlush(saved);
        assertEquals(1L, updated.getVersion());
    }

    private SkillJpaEntity createSkill(String code, String name) {
        SkillJpaEntity skill = new SkillJpaEntity();
        skill.setCode(code);
        skill.setName(name);
        skill.setCategory("Backend");
        skill.setDescription("Description");
        skill.setGroupId(1L);
        skill.setStatus("ACTIVE");
        skill.setCreatedAt(LocalDateTime.now());
        skill.setUpdatedAt(LocalDateTime.now());
        return skillRepository.save(skill);
    }

    private OrgUnitJpaEntity childOrgUnit(String code, String name, OrgUnitJpaEntity parent) {
        OrgUnitJpaEntity unit = new OrgUnitJpaEntity();
        unit.setUnitCode(code);
        unit.setUnitName(name);
        unit.setUnitType(OrgUnitType.DEPARTMENT);
        unit.setStatus(OrgUnitStatus.ACTIVE);
        unit.setParentId(parent.getId());
        unit.setTreePath(parent.getTreePath() + code + "/");
        unit.setLevel(parent.getLevel() + 1);
        unit.setCreatedAt(LocalDateTime.now());
        unit.setUpdatedAt(LocalDateTime.now());
        return orgUnitRepository.save(unit);
    }

    private EmployeeJpaEntity createEmployee(String code, String name, Long orgUnitId) {
        EmployeeJpaEntity emp = new EmployeeJpaEntity();
        emp.setEmployeeCode(code);
        emp.setFullName(name);
        emp.setOrgUnitId(orgUnitId);
        emp.setStandardHoursPerWeek(40);
        emp.setStatus("ACTIVE");
        return employeeRepository.save(emp);
    }
}