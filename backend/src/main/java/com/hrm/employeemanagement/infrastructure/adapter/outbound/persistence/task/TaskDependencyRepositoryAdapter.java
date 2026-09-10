package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.task.DeleteTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskDependencyPort;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.dependency.TaskDependency;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.entity.TaskDependencyJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.repository.SpringDataTaskDependencyRepository;

@Component
public class TaskDependencyRepositoryAdapter implements
        LoadTaskDependencyPort,
        SaveTaskDependencyPort,
        DeleteTaskDependencyPort {

    private final SpringDataTaskDependencyRepository repository;

    public TaskDependencyRepositoryAdapter(SpringDataTaskDependencyRepository repository) {
        this.repository = Objects.requireNonNull(repository, "SpringDataTaskDependencyRepository must not be null");
    }

    @Override
    public List<TaskDependency> findByProjectId(ProjectId projectId) {
        if (projectId == null || projectId.value() == null) {
            return List.of();
        }
        return repository.findByProjectId(projectId.value()).stream()
                .map(TaskDependencyPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<TaskDependency> findByProjectIdAndPredecessorId(ProjectId projectId, TaskId predecessorId) {
        if (projectId == null || predecessorId == null) {
            return List.of();
        }
        return repository.findByProjectIdAndPredecessorId(projectId.value(), predecessorId.value()).stream()
                .map(TaskDependencyPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<TaskDependency> findByProjectIdAndSuccessorId(ProjectId projectId, TaskId successorId) {
        if (projectId == null || successorId == null) {
            return List.of();
        }
        return repository.findByProjectIdAndSuccessorId(projectId.value(), successorId.value()).stream()
                .map(TaskDependencyPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<TaskDependency> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id).map(TaskDependencyPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByPredecessorIdAndSuccessorId(TaskId predecessorId, TaskId successorId) {
        if (predecessorId == null || successorId == null) {
            return false;
        }
        return repository.existsByPredecessorIdAndSuccessorId(predecessorId.value(), successorId.value());
    }

    @Override
    public TaskDependency save(TaskDependency dependency) {
        TaskDependencyJpaEntity entity = TaskDependencyPersistenceMapper.toJpaEntity(dependency);
        TaskDependencyJpaEntity saved = repository.save(entity);
        return TaskDependencyPersistenceMapper.toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        if (id != null) {
            repository.deleteById(id);
        }
    }
}
