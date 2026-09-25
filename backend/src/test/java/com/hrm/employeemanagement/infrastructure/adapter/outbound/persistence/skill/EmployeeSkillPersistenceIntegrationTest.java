package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.ProficiencyLevel;
import com.hrm.employeemanagement.domain.skill.SkillStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataSkillRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmployeeSkillPersistenceIntegrationTest {

    @Autowired
    private EmployeeSkillRepository employeeSkillRepository;

    @Autowired
    private SpringDataSkillRepository skillRepository;

    @Autowired
    private SpringDataEmployeeRepository employeeRepository;

    @Autowired
    private SpringDataOrgUnitRepository orgUnitRepository;

    @Test
    @DisplayName("APPROVED -> update -> save -> reload -> approve workflow persists and updates pending fields correctly")
    void testApprovedUpdateSaveReloadApproveWorkflow() {
        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();
        String suffix = String.valueOf(System.nanoTime());
        EmployeeJpaEntity emp = createEmployee("EMP-" + suffix, "Developer " + suffix, root.getId());
        SkillJpaEntity skill = createSkill("SKILL-" + suffix, "ReactJS " + suffix);

        // 1. Initial State: APPROVED skill (proficiency=3, years=2.0)
        EmployeeSkill initialSkill = new EmployeeSkill(
                null,
                emp.getId(),
                skill.getId(),
                ProficiencyLevel.fromValue(3),
                new BigDecimal("2.0"),
                SkillStatus.APPROVED,
                1L,
                LocalDateTime.now(),
                null,
                "Initial approval",
                3,
                new BigDecimal("2.0"),
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
        EmployeeSkill savedInitial = employeeSkillRepository.save(initialSkill);
        assertNotNull(savedInitial.getId());

        // 2. Employee updates skill (proficiency=4, years=3.5) -> creates pending request
        savedInitial.updateProficiency(ProficiencyLevel.fromValue(4), new BigDecimal("3.5"));
        assertTrue(savedInitial.isPendingReview());
        assertEquals(Integer.valueOf(4), savedInitial.getPendingProficiencyLevel());
        assertEquals(new BigDecimal("3.5"), savedInitial.getPendingYearsOfExperience());
        assertEquals(SkillStatus.APPROVED, savedInitial.getStatus()); // Status remains APPROVED while pending update

        // 3. Save to database
        employeeSkillRepository.save(savedInitial);

        // 4. Reload from database and verify pending fields were preserved
        EmployeeSkill reloaded = employeeSkillRepository.findByEmployeeIdAndSkillId(emp.getId(), skill.getId()).orElseThrow();
        assertEquals(SkillStatus.APPROVED, reloaded.getStatus());
        assertEquals(3, reloaded.getProficiencyLevelValue());
        assertEquals(new BigDecimal("2.0"), reloaded.getYearsOfExperience());
        assertEquals(Integer.valueOf(3), reloaded.getLastApprovedProficiencyLevel());
        assertEquals(new BigDecimal("2.0"), reloaded.getLastApprovedYearsOfExperience());
        assertEquals(Integer.valueOf(4), reloaded.getPendingProficiencyLevel(), "Pending proficiency level must be persisted and reloaded");
        assertEquals(new BigDecimal("3.5"), reloaded.getPendingYearsOfExperience(), "Pending years of experience must be persisted and reloaded");
        assertTrue(reloaded.isPendingReview());

        // 5. Reviewer approves the pending update
        reloaded.approve(1L, "Approved upgrade to Level 4");
        assertEquals(SkillStatus.APPROVED, reloaded.getStatus());
        assertEquals(4, reloaded.getProficiencyLevelValue());
        assertEquals(new BigDecimal("3.5"), reloaded.getYearsOfExperience());
        assertNull(reloaded.getPendingProficiencyLevel());
        assertNull(reloaded.getPendingYearsOfExperience());

        // 6. Save and reload to verify final state
        employeeSkillRepository.save(reloaded);
        EmployeeSkill finalReloaded = employeeSkillRepository.findByEmployeeIdAndSkillId(emp.getId(), skill.getId()).orElseThrow();
        assertEquals(SkillStatus.APPROVED, finalReloaded.getStatus());
        assertEquals(4, finalReloaded.getProficiencyLevelValue());
        assertEquals(new BigDecimal("3.5"), finalReloaded.getYearsOfExperience());
        assertNull(finalReloaded.getPendingProficiencyLevel());
        assertNull(finalReloaded.getPendingYearsOfExperience());
    }

    @Test
    @DisplayName("APPROVED -> update -> save -> reload -> reject workflow preserves approved skill and clears pending fields")
    void testApprovedUpdateSaveReloadRejectWorkflow() {
        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();
        String suffix = String.valueOf(System.nanoTime());
        EmployeeJpaEntity emp = createEmployee("EMP-" + suffix, "Developer " + suffix, root.getId());
        SkillJpaEntity skill = createSkill("SKILL-" + suffix, "Java " + suffix);

        // 1. Initial State: APPROVED skill (proficiency=2, years=1.5)
        EmployeeSkill initialSkill = new EmployeeSkill(
                null,
                emp.getId(),
                skill.getId(),
                ProficiencyLevel.fromValue(2),
                new BigDecimal("1.5"),
                SkillStatus.APPROVED,
                1L,
                LocalDateTime.now(),
                null,
                "Initial approval",
                2,
                new BigDecimal("1.5"),
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
        EmployeeSkill savedInitial = employeeSkillRepository.save(initialSkill);

        // 2. Employee updates skill (proficiency=4, years=4.0) -> creates pending request
        savedInitial.updateProficiency(ProficiencyLevel.fromValue(4), new BigDecimal("4.0"));
        employeeSkillRepository.save(savedInitial);

        // 3. Reload from database
        EmployeeSkill reloaded = employeeSkillRepository.findByEmployeeIdAndSkillId(emp.getId(), skill.getId()).orElseThrow();
        assertEquals(Integer.valueOf(4), reloaded.getPendingProficiencyLevel());
        assertEquals(new BigDecimal("4.0"), reloaded.getPendingYearsOfExperience());

        // 4. Reviewer rejects the update
        reloaded.reject(1L, "Experience proof insufficient for Level 4");
        assertEquals(SkillStatus.APPROVED, reloaded.getStatus(), "Status remains APPROVED with previous level");
        assertEquals(2, reloaded.getProficiencyLevelValue(), "Proficiency level remains at previous approved level 2");
        assertEquals(new BigDecimal("1.5"), reloaded.getYearsOfExperience(), "Years of experience remains at 1.5");
        assertNull(reloaded.getPendingProficiencyLevel());
        assertNull(reloaded.getPendingYearsOfExperience());
        assertEquals("Experience proof insufficient for Level 4", reloaded.getRejectionReason());

        // 5. Save and reload to verify rejected state
        employeeSkillRepository.save(reloaded);
        EmployeeSkill finalReloaded = employeeSkillRepository.findByEmployeeIdAndSkillId(emp.getId(), skill.getId()).orElseThrow();
        assertEquals(SkillStatus.APPROVED, finalReloaded.getStatus());
        assertEquals(2, finalReloaded.getProficiencyLevelValue());
        assertEquals(new BigDecimal("1.5"), finalReloaded.getYearsOfExperience());
        assertNull(finalReloaded.getPendingProficiencyLevel());
        assertNull(finalReloaded.getPendingYearsOfExperience());
    }

    private SkillJpaEntity createSkill(String code, String name) {
        SkillJpaEntity skill = new SkillJpaEntity();
        skill.setCode(code);
        skill.setName(name);
        skill.setCategory("Programming");
        skill.setDescription("Skill description");
        skill.setGroupId(1L);
        skill.setStatus("ACTIVE");
        skill.setCreatedAt(LocalDateTime.now());
        skill.setUpdatedAt(LocalDateTime.now());
        return skillRepository.save(skill);
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
