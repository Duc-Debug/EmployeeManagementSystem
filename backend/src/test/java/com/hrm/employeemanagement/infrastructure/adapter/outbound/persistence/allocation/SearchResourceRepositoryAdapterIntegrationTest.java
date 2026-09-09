package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.allocation.ResourceCandidate;
import com.hrm.employeemanagement.domain.exception.skill.SkillNotFoundException;
import com.hrm.employeemanagement.domain.skill.SkillStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.EmployeeSkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataEmployeeSkillRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataSkillRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchResourceRepositoryAdapterIntegrationTest {

    @Autowired
    private SearchResourceRepositoryAdapter adapter;

    @Autowired
    private SpringDataSkillRepository skillRepository;

    @Autowired
    private SpringDataEmployeeRepository employeeRepository;

    @Autowired
    private SpringDataEmployeeSkillRepository employeeSkillRepository;

    @Autowired
    private SpringDataOrgUnitRepository orgUnitRepository;

    @Test
    @DisplayName("findActiveEmployeesBySkill: Skill không tồn tại ném SkillNotFoundException")
    void testFindActiveEmployeesBySkill_SkillNotFound_ThrowsSkillNotFoundException() {
        assertThrows(SkillNotFoundException.class, () ->
                adapter.findActiveEmployeesBySkill(999_999L, 1)
        );
    }

    @Test
    @DisplayName("findActiveEmployeesBySkill: Truy vấn thực tế JOIN giữa employee_skills, employees và skills")
    void testFindActiveEmployeesBySkill_ActualDatabaseQuery_Success() {
        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();
        String suffix = String.valueOf(System.nanoTime());

        // 1. Tạo 1 kỹ năng
        SkillJpaEntity skill = new SkillJpaEntity();
        skill.setCode("SKILL-TEST-" + suffix);
        skill.setName("Integration Skill " + suffix);
        skill.setCategory("Backend");
        skill.setDescription("Skill for integration test");
        skill.setGroupId(1L);
        skill.setStatus("ACTIVE");
        skill.setCreatedAt(LocalDateTime.now());
        skill.setUpdatedAt(LocalDateTime.now());
        skill = skillRepository.save(skill);

        // 2. Tạo 3 nhân viên:
        // - emp1: ACTIVE, có skill APPROVED level 4
        // - emp2: ACTIVE, có skill APPROVED level 2 (nhỏ hơn minLevel 3)
        // - emp3: INACTIVE, có skill APPROVED level 5 (không được trả về do không ACTIVE)
        EmployeeJpaEntity emp1 = createEmployee("EMP-1-" + suffix, "Nguyễn Văn Active 1", root.getId(), "ACTIVE", 40);
        EmployeeJpaEntity emp2 = createEmployee("EMP-2-" + suffix, "Nguyễn Văn Active 2", root.getId(), "ACTIVE", 35);
        EmployeeJpaEntity emp3 = createEmployee("EMP-3-" + suffix, "Trần Văn Inactive", root.getId(), "INACTIVE", 40);

        // Tạo employee_skills
        employeeSkillRepository.save(new EmployeeSkillJpaEntity(
                null, emp1.getId(), skill.getId(), 4, new BigDecimal("3.5"),
                SkillStatus.APPROVED, null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        ));
        employeeSkillRepository.save(new EmployeeSkillJpaEntity(
                null, emp2.getId(), skill.getId(), 2, new BigDecimal("1.0"),
                SkillStatus.APPROVED, null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        ));
        employeeSkillRepository.save(new EmployeeSkillJpaEntity(
                null, emp3.getId(), skill.getId(), 5, new BigDecimal("5.0"),
                SkillStatus.APPROVED, null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        ));

        // 3. Thực thi adapter với minProficiencyLevel = 3
        List<ResourceCandidate> results = adapter.findActiveEmployeesBySkill(skill.getId(), 3);

        // 4. Kiểm chứng:
        // Chỉ emp1 thỏa mãn (ACTIVE, APPROVED, level 4 >= 3)
        // emp2 bị loại do level 2 < 3
        // emp3 bị loại do status INACTIVE
        assertNotNull(results);
        assertEquals(1, results.size());

        ResourceCandidate candidate = results.get(0);
        assertEquals(emp1.getId(), candidate.employeeId());
        assertEquals("EMP-1-" + suffix, candidate.employeeCode());
        assertEquals("Nguyễn Văn Active 1", candidate.fullName());
        assertEquals(root.getId(), candidate.orgUnitId());
        assertEquals(40, candidate.standardHoursPerWeek());
        assertEquals(skill.getId(), candidate.skillId());
        assertEquals(skill.getName(), candidate.skillName());
        assertEquals(4, candidate.proficiencyLevel());
        assertEquals(0, new BigDecimal("3.5").compareTo(candidate.yearsOfExperience()));
    }

    private EmployeeJpaEntity createEmployee(String code, String name, Long orgUnitId, String status, int hours) {
        EmployeeJpaEntity emp = new EmployeeJpaEntity();
        emp.setEmployeeCode(code);
        emp.setFullName(name);
        emp.setOrgUnitId(orgUnitId);
        emp.setStandardHoursPerWeek(hours);
        emp.setStatus(status);
        emp.setContractEndDate(LocalDate.of(2028, 12, 31));
        return employeeRepository.save(emp);
    }
}
