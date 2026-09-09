package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectMemberPort;
import com.hrm.employeemanagement.domain.project.ProjectMemberRole;
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
    private final SpringDataTaskRepository taskRepository;

    public ProjectMemberRepositoryAdapter(
            SpringDataProjectMemberRepository projectMemberRepository,
            SpringDataProjectRepository projectRepository,
            SpringDataEmployeeRepository employeeRepository,
            SpringDataUserRepository userRepository,
            SpringDataTaskRepository taskRepository) {
        this.projectMemberRepository = Objects.requireNonNull(projectMemberRepository, "SpringDataProjectMemberRepository must not be null");
        this.projectRepository = Objects.requireNonNull(projectRepository, "SpringDataProjectRepository must not be null");
        this.employeeRepository = Objects.requireNonNull(employeeRepository, "SpringDataEmployeeRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "SpringDataUserRepository must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "SpringDataTaskRepository must not be null");
    }

    @Override
    public List<ProjectMemberResult> findMembersByProjectId(Long projectId) {
        Map<Long, ProjectMemberResult> resultMap = new LinkedHashMap<>();

        // 1. Kiểm tra PM của dự án
        Optional<ProjectJpaEntity> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isPresent() && projectOpt.get().getManagerId() != null) {
            Long pmId = projectOpt.get().getManagerId();
            employeeRepository.findById(pmId).ifPresent(pm -> {
                String email = pm.getUserId() != null
                        ? userRepository.findById(pm.getUserId()).map(UserJpaEntity::getEmail).orElse(null)
                        : null;
                resultMap.put(pmId, new ProjectMemberResult(
                        pm.getId(),
                        pm.getEmployeeCode(),
                        pm.getFullName(),
                        email,
                        pm.getOrgUnitId(),
                        null,
                        ProjectMemberRole.PROJECT_MANAGER,
                        pm.getStatus()
                ));
            });
        }

        // 2. Lấy các thành viên từ project_members
        List<ProjectMemberJpaEntity> memberEntities = projectMemberRepository.findByProjectId(projectId);
        List<Long> employeeIds = memberEntities.stream()
                .map(ProjectMemberJpaEntity::getEmployeeId)
                .filter(id -> !resultMap.containsKey(id))
                .toList();

        if (!employeeIds.isEmpty()) {
            List<EmployeeJpaEntity> employees = employeeRepository.findAllById(employeeIds);
            for (EmployeeJpaEntity emp : employees) {
                String email = emp.getUserId() != null
                        ? userRepository.findById(emp.getUserId()).map(UserJpaEntity::getEmail).orElse(null)
                        : null;
                resultMap.put(emp.getId(), new ProjectMemberResult(
                        emp.getId(),
                        emp.getEmployeeCode(),
                        emp.getFullName(),
                        email,
                        emp.getOrgUnitId(),
                        null,
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
        return taskRepository.existsByProjectIdAndAssigneeIdAndStatusNot(projectId, employeeId, "DONE");
    }

    @Override
    public void addMember(Long projectId, Long employeeId) {
        projectMemberRepository.save(new ProjectMemberJpaEntity(projectId, employeeId));
    }

    @Override
    public void removeMember(Long projectId, Long employeeId) {
        projectMemberRepository.deleteByProjectIdAndEmployeeId(projectId, employeeId);
    }
}
