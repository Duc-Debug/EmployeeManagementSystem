package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmployeeRepositoryAdapterIntegrationTest {

    @Autowired
    private EmployeeRepositoryAdapter repositoryAdapter;

    @Autowired
    private SpringDataEmployeeRepository springDataEmployeeRepository;

    @Autowired
    private com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository orgUnitRepository;

    private EmployeeJpaEntity createEmployee(String code, String name, String role, Long orgUnitId, String status) {
        EmployeeJpaEntity entity = new EmployeeJpaEntity();
        entity.setEmployeeCode(code);
        entity.setFullName(name);
        entity.setProfessionalRole(role);
        entity.setOrgUnitId(orgUnitId);
        entity.setStatus(status);
        entity.setStandardHoursPerWeek(40);
        entity.setStartDate(LocalDate.of(2025, 1, 1));
        entity.setIsOutsourced(false);
        return springDataEmployeeRepository.save(entity);
    }

    @Test
    @DisplayName("findActivePaged va countActive: Loc dung ACTIVE, sap xep fullName ASC, phan trang LIMIT/OFFSET va loc search")
    void testFindActivePagedAndCountActive() {
        var root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();
        Long orgUnitId = root.getId();

        String suffix = String.valueOf(System.nanoTime()).substring(8);
        createEmployee("EMP-B" + suffix, "Binh Tran", "QA", orgUnitId, "ACTIVE");
        createEmployee("EMP-A" + suffix, "An Nguyen", "Developer", orgUnitId, "ACTIVE");
        createEmployee("EMP-C" + suffix, "Cuong Le", "DevOps", orgUnitId, "ACTIVE");
        createEmployee("EMP-D" + suffix, "Dung Pham", "Developer", orgUnitId, "INACTIVE");

        // 1. Company scope, search theo "EMP-" + suffix
        long totalSearch = repositoryAdapter.countActive(null, suffix);
        assertThat(totalSearch).isEqualTo(3); // Chi lay 3 ACTIVE, bo qua INACTIVE

        List<Employee> page1 = repositoryAdapter.findActivePaged(null, suffix, 2, 0);
        assertThat(page1).hasSize(2);
        // Sap xep fullName ASC: An Nguyen truoc, Binh Tran sau
        assertThat(page1.get(0).getFullName()).isEqualTo("An Nguyen");
        assertThat(page1.get(1).getFullName()).isEqualTo("Binh Tran");

        List<Employee> page2 = repositoryAdapter.findActivePaged(null, suffix, 2, 2);
        assertThat(page2).hasSize(1);
        assertThat(page2.get(0).getFullName()).isEqualTo("Cuong Le");

        // 2. OrgUnit scope: chi orgUnitId
        long countOrg = repositoryAdapter.countActive(List.of(orgUnitId), suffix);
        assertThat(countOrg).isEqualTo(3);

        List<Employee> listOrg = repositoryAdapter.findActivePaged(List.of(orgUnitId), suffix, 10, 0);
        assertThat(listOrg).hasSize(3);

        // 3. Search role: "QA"
        long countQa = repositoryAdapter.countActive(List.of(orgUnitId), "QA");
        assertThat(countQa).isGreaterThanOrEqualTo(1);

        List<Employee> listQa = repositoryAdapter.findActivePaged(List.of(orgUnitId), "QA", 10, 0);
        assertThat(listQa.stream().anyMatch(e -> e.getEmployeeCode().equals("EMP-B" + suffix))).isTrue();
    }
}
