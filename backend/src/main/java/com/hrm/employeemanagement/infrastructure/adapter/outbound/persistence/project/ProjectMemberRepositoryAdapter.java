package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectMemberPort;
import com.hrm.employeemanagement.domain.exception.project.DuplicateProjectMemberException;
import com.hrm.employeemanagement.domain.project.ProjectMemberRole;
import com.hrm.employeemanagement.domain.task.TaskStatus;
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

@Component
public class ProjectMemberRepositoryAdapter implements LoadProjectMemberPort, SaveProjectMemberPort {

    private final SpringDataProjectMemberRepository projectMemberRepository;
    private final SpringDataProjectRepository projectRepository;
    private final SpringDataEmployeeRepository employeeRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataOrgUnitRepository orgUnitRepository;
    private final SpringDataTaskRepository taskRepository;

    public ProjectMemberRepositoryAdapter(
            SpringDataProjectMemberRepository projectMemberRepository,
            SpringDataProjectRepository projectRepository,
            SpringDataEmployeeRepository employeeRepository,
            SpringDataUserRepository userRepository,
            SpringDataOrgUnitRepository orgUnitRepository,
            SpringDataTaskRepository taskRepository) {
        this.projectMemberRepository = Objects.requireNonNull(projectMemberRepository, "SpringDataProjectMemberRepository must not be null");
        this.projectRepository = Objects.requireNonNull(projectRepository, "SpringDataProjectRepository must not be null");
        this.employeeRepository = Objects.requireNonNull(employeeRepository, "SpringDataEmployeeRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "SpringDataUserRepository must not be null");
        this.orgUnitRepository = Objects.requireNonNull(orgUnitRepository, "SpringDataOrgUnitRepository must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "SpringDataTaskRepository must not be null");
    }

    @Override
    public List<ProjectMemberResult> findMembersByProjectId(Long projectId) {
        // 1. Kiểm tra PM của dự án
        Optional<ProjectJpaEntity> projectOpt = projectRepository.findById(projectId);
        EmployeeJpaEntity pmEntity = null;
        if (projectOpt.isPresent() && projectOpt.get().getManagerId() != null) {
            pmEntity = employeeRepository.findById(projectOpt.get().getManagerId()).orElse(null);
        }

        // 2. Lấy các thành viên từ project_members
        List<ProjectMemberJpaEntity> memberEntities = projectMemberRepository.findByProjectId(projectId);
        List<Long> memberEmployeeIds = memberEntities.stream()
                .map(ProjectMemberJpaEntity::getEmployeeId)
                .toList();

        List<EmployeeJpaEntity> memberEmployees = memberEmployeeIds.isEmpty()
                ? Collections.emptyList()
                : employeeRepository.findAllById(memberEmployeeIds);

        // 3. Gom orgUnitId để truy vấn tên phòng ban hàng loạt
        Set<Long> orgUnitIds = new HashSet<>();
        if (pmEntity != null && pmEntity.getOrgUnitId() != null) {
            orgUnitIds.add(pmEntity.getOrgUnitId());
        }
        for (EmployeeJpaEntity emp : memberEmployees) {
            if (emp.getOrgUnitId() != null) {
                orgUnitIds.add(emp.getOrgUnitId());
            }
        }

        Map<Long, String> orgUnitNames = orgUnitIds.isEmpty()
                ? Collections.emptyMap()
                : orgUnitRepository.findAllById(orgUnitIds).stream()
                        .collect(Collectors.toMap(OrgUnitJpaEntity::getId, OrgUnitJpaEntity::getUnitName, (a, b) -> a));

        // 4. Gom userIds để truy vấn email hàng loạt (tránh N+1)
        Set<Long> userIds = new HashSet<>();
        if (pmEntity != null && pmEntity.getUserId() != null) {
            userIds.add(pmEntity.getUserId());
        }
        for (EmployeeJpaEntity emp : memberEmployees) {
            if (emp.getUserId() != null) {
                userIds.add(emp.getUserId());
            }
        }

        Map<Long, String> userEmails = userIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(UserJpaEntity::getId, UserJpaEntity::getEmail, (a, b) -> a));

        Map<Long, ProjectMemberResult> resultMap = new LinkedHashMap<>();

        // Thêm PM vào kết quả
        if (pmEntity != null) {
            String email = pmEntity.getUserId() != null ? userEmails.get(pmEntity.getUserId()) : null;
            String orgName = pmEntity.getOrgUnitId() != null ? orgUnitNames.get(pmEntity.getOrgUnitId()) : null;
            resultMap.put(pmEntity.getId(), new ProjectMemberResult(
                    pmEntity.getId(),
                    pmEntity.getEmployeeCode(),
                    pmEntity.getFullName(),
                    email,
                    pmEntity.getOrgUnitId(),
                    orgName,
                    ProjectMemberRole.PROJECT_MANAGER,
                    pmEntity.getStatus()
            ));
        }

        // Thêm các Member vào kết quả
        for (EmployeeJpaEntity emp : memberEmployees) {
            if (!resultMap.containsKey(emp.getId())) {
                String email = emp.getUserId() != null ? userEmails.get(emp.getUserId()) : null;
                String orgName = emp.getOrgUnitId() != null ? orgUnitNames.get(emp.getOrgUnitId()) : null;
                resultMap.put(emp.getId(), new ProjectMemberResult(
                        emp.getId(),
                        emp.getEmployeeCode(),
                        emp.getFullName(),
                        email,
                        emp.getOrgUnitId(),
                        orgName,
                        ProjectMemberRole.MEMBER,
                        emp.getStatus()
                ));
            }
        }

        return new ArrayList<>(resultMap.values());
    }

    @Override
    public boolean existsMember(Long projectId, Long employeeId) {
        if (projectMemberRepository.existsByProjectIdAndEmployeeId(projectId, employeeId)) {
            return true;
        }
        return projectRepository.findById(projectId)
                .map(p -> Objects.equals(p.getManagerId(), employeeId))
                .orElse(false);
    }

    @Override
    public boolean hasActiveTasks(Long projectId, Long employeeId) {
        return taskRepository.existsByProjectIdAndAssigneeIdAndStatusIn(
                projectId, employeeId, List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS));
    }

    @Override
    public void addMember(Long projectId, Long employeeId) {
        try {
            projectMemberRepository.saveAndFlush(new ProjectMemberJpaEntity(projectId, employeeId));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateProjectMemberException(employeeId, projectId);
        }
    }

    @Override
    public void removeMember(Long projectId, Long employeeId) {
        projectMemberRepository.deleteByProjectIdAndEmployeeId(projectId, employeeId);
    }
}
