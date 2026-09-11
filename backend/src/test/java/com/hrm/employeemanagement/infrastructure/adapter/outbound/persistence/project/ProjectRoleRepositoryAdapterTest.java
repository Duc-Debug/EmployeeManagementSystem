package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.hrm.employeemanagement.domain.exception.role.DuplicateProjectRoleCodeException;
import com.hrm.employeemanagement.domain.exception.role.DuplicateProjectRoleNameException;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectRoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRoleRepository;

@ExtendWith(MockitoExtension.class)
class ProjectRoleRepositoryAdapterTest {

    @Mock
    private SpringDataProjectRoleRepository springDataProjectRoleRepository;

    private ProjectRoleRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProjectRoleRepositoryAdapter(springDataProjectRoleRepository);
    }

    @Test
    @DisplayName("save calls saveAndFlush and returns mapped domain object")
    void testSave_Success() {
        ProjectRole domain = new ProjectRole(
                null,
                "DEV",
                "Developer",
                "Desc",
                1L,
                null,
                ProjectRoleStatus.ACTIVE,
                null,
                null
        );

        ProjectRoleJpaEntity savedEntity = new ProjectRoleJpaEntity();
        savedEntity.setId(10L);
        savedEntity.setCode("DEV");
        savedEntity.setName("Developer");
        savedEntity.setStatus("ACTIVE");

        when(springDataProjectRoleRepository.saveAndFlush(any(ProjectRoleJpaEntity.class))).thenReturn(savedEntity);

        ProjectRole result = adapter.save(domain);

        assertThat(result).isNotNull();
        assertThat(result.getIdValue()).isEqualTo(10L);
        assertThat(result.getCode()).isEqualTo("DEV");
        assertThat(result.getName()).isEqualTo("Developer");
        verify(springDataProjectRoleRepository).saveAndFlush(any(ProjectRoleJpaEntity.class));
    }

    @Test
    @DisplayName("save translates duplicate name constraint violation to DuplicateProjectRoleNameException")
    void testSave_TranslatesDuplicateNameException() {
        ProjectRole domain = new ProjectRole(
                null,
                "DEV",
                "Developer",
                "Desc",
                1L,
                null,
                ProjectRoleStatus.ACTIVE,
                null,
                null
        );

        SQLException sqlException = new SQLException("Duplicate entry 'Developer' for key 'uk_project_roles_name'", "23505", 1062);
        ConstraintViolationException cve = new ConstraintViolationException("Duplicate name", sqlException, "uk_project_roles_name");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("Constraint violation", cve);

        when(springDataProjectRoleRepository.saveAndFlush(any(ProjectRoleJpaEntity.class))).thenThrow(dive);

        assertThatThrownBy(() -> adapter.save(domain))
                .isInstanceOf(DuplicateProjectRoleNameException.class);
    }

    @Test
    @DisplayName("save translates duplicate code constraint violation to DuplicateProjectRoleCodeException")
    void testSave_TranslatesDuplicateCodeException() {
        ProjectRole domain = new ProjectRole(
                null,
                "DEV",
                "Developer",
                "Desc",
                1L,
                null,
                ProjectRoleStatus.ACTIVE,
                null,
                null
        );

        SQLException sqlException = new SQLException("Duplicate entry 'DEV' for key 'code'", "23505", 1062);
        ConstraintViolationException cve = new ConstraintViolationException("Duplicate code", sqlException, "code");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("Constraint violation", cve);

        when(springDataProjectRoleRepository.saveAndFlush(any(ProjectRoleJpaEntity.class))).thenThrow(dive);

        assertThatThrownBy(() -> adapter.save(domain))
                .isInstanceOf(DuplicateProjectRoleCodeException.class);
    }

    @Test
    @DisplayName("save rethrows other DataIntegrityViolationException")
    void testSave_RethrowsOtherDataIntegrityViolation() {
        ProjectRole domain = new ProjectRole(
                null,
                "DEV",
                "Developer",
                "Desc",
                1L,
                null,
                ProjectRoleStatus.ACTIVE,
                null,
                null
        );

        DataIntegrityViolationException dive = new DataIntegrityViolationException("Foreign key violation: fk_other");
        when(springDataProjectRoleRepository.saveAndFlush(any(ProjectRoleJpaEntity.class))).thenThrow(dive);

        assertThatThrownBy(() -> adapter.save(domain))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
