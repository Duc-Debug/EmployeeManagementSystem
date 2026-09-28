package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;
import com.hrm.employeemanagement.domain.exception.project.DuplicateProjectMemberException;
import com.hrm.employeemanagement.domain.project.ProjectMemberRole;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectMemberJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectMemberRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.repository.SpringDataTaskRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;

@ExtendWith(MockitoExtension.class)
class ProjectMemberRepositoryAdapterTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long PM_ID = 50L;
    private static final Long MEMBER_ID = 60L;
    private static final Long ORG_UNIT_ID = 100L;

    @Mock
    private SpringDataProjectMemberRepository projectMemberRepository;
    @Mock
    private SpringDataProjectRepository projectRepository;
    @Mock
    private SpringDataEmployeeRepository employeeRepository;
    @Mock
    private SpringDataUserRepository userRepository;
    @Mock
    private SpringDataOrgUnitRepository orgUnitRepository;
    @Mock
    private SpringDataTaskRepository taskRepository;

    private ProjectMemberRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProjectMemberRepositoryAdapter(
                projectMemberRepository,
                projectRepository,
                employeeRepository,
                userRepository,
                orgUnitRepository,
                taskRepository
        );
    }

    @Test
    @DisplayName("addMember gọi saveAndFlush thành công")
    void testAddMember_Success() {
        adapter.addMember(PROJECT_ID, MEMBER_ID);
        verify(projectMemberRepository).saveAndFlush(any(ProjectMemberJpaEntity.class));
    }

    @Test
    @DisplayName("addMember bắt DataIntegrityViolationException và ném DuplicateProjectMemberException")
    void testAddMember_ThrowsDuplicateExceptionOnDataIntegrityViolation() {
        when(projectMemberRepository.saveAndFlush(any(ProjectMemberJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        assertThatThrownBy(() -> adapter.addMember(PROJECT_ID, MEMBER_ID))
                .isInstanceOf(DuplicateProjectMemberException.class);
    }

    @Test
    @DisplayName("findMembersByProjectId nạp và gán orgUnitName cho cả PM và Member")
    void testFindMembersByProjectId_ResolvesOrgUnitNames() {
        ProjectJpaEntity project = new ProjectJpaEntity();
        project.setId(PROJECT_ID);
        project.setManagerId(PM_ID);

        EmployeeJpaEntity pm = new EmployeeJpaEntity();
        pm.setId(PM_ID);
        pm.setEmployeeCode("EMP-PM");
        pm.setFullName("PM User");
        pm.setOrgUnitId(ORG_UNIT_ID);
        pm.setUserId(201L);
        pm.setStatus("ACTIVE");

        EmployeeJpaEntity member = new EmployeeJpaEntity();
        member.setId(MEMBER_ID);
        member.setEmployeeCode("EMP-MEM");
        member.setFullName("Member User");
        member.setOrgUnitId(ORG_UNIT_ID);
        member.setUserId(202L);
        member.setStatus("ACTIVE");

        UserJpaEntity pmUser = new UserJpaEntity();
        pmUser.setId(201L);
        pmUser.setEmail("pm@hrm.com");
        UserJpaEntity memberUser = new UserJpaEntity();
        memberUser.setId(202L);
        memberUser.setEmail("member@hrm.com");

        OrgUnitJpaEntity orgUnit = new OrgUnitJpaEntity();
        orgUnit.setId(ORG_UNIT_ID);
        orgUnit.setUnitName("Phòng Công nghệ");

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(employeeRepository.findById(PM_ID)).thenReturn(Optional.of(pm));
        when(projectMemberRepository.findByProjectId(PROJECT_ID))
                .thenReturn(List.of(new ProjectMemberJpaEntity(PROJECT_ID, MEMBER_ID)));
        when(employeeRepository.findAllById(List.of(MEMBER_ID))).thenReturn(List.of(member));
        when(orgUnitRepository.findAllById(any())).thenReturn(List.of(orgUnit));
        when(userRepository.findAllById(any())).thenReturn(List.of(pmUser, memberUser));

        List<ProjectMemberResult> results = adapter.findMembersByProjectId(PROJECT_ID);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).roleInProject()).isEqualTo(ProjectMemberRole.PROJECT_MANAGER);
        assertThat(results.get(0).email()).isEqualTo("pm@hrm.com");
        assertThat(results.get(0).orgUnitName()).isEqualTo("Phòng Công nghệ");
        assertThat(results.get(1).roleInProject()).isEqualTo(ProjectMemberRole.MEMBER);
        assertThat(results.get(1).email()).isEqualTo("member@hrm.com");
        assertThat(results.get(1).orgUnitName()).isEqualTo("Phòng Công nghệ");
    }

    @Test
    @DisplayName("hasActiveTasks returns true when member has TODO or IN_PROGRESS tasks")
    void testHasActiveTasks() {
        when(taskRepository.existsByProjectIdAndAssigneeIdAndStatusIn(any(), any(), any())).thenReturn(true);
        boolean active = adapter.hasActiveTasks(PROJECT_ID, MEMBER_ID);
        assertThat(active).isTrue();
    }
}
