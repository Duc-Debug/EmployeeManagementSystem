package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.entity.TaskJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.repository.SpringDataTaskRepository;

@Component
public class TaskRepositoryAdapter implements LoadTaskPort, SaveTaskPort {

    private final SpringDataTaskRepository taskRepository;
    private final TaskPersistenceMapper mapper;

    public TaskRepositoryAdapter(SpringDataTaskRepository taskRepository, TaskPersistenceMapper mapper) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "SpringDataTaskRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "TaskPersistenceMapper must not be null");
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        if (id == null || id.value() == null) {
            return Optional.empty();
        }
        return taskRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Task> findAllById(List<TaskId> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> rawIds = ids.stream().map(TaskId::value).filter(Objects::nonNull).toList();
        if (rawIds.isEmpty()) {
            return List.of();
        }
        return taskRepository.findAllById(rawIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Task> findAllByProjectId(ProjectId projectId) {
        if (projectId == null || projectId.value() == null) {
            return List.of();
        }
        return taskRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId.value())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByIdAndProjectId(TaskId id, ProjectId projectId) {
        if (id == null || id.value() == null || projectId == null || projectId.value() == null) {
            return false;
        }
        return taskRepository.existsByIdAndProjectId(id.value(), projectId.value());
    }

    @Override
    public boolean hasChildren(TaskId parentId) {
        if (parentId == null || parentId.value() == null) {
            return false;
        }
        return taskRepository.existsByParentId(parentId.value());
    }

    @Override
    public Task save(Task task) {
        TaskJpaEntity entity = mapper.toJpaEntity(task);
        TaskJpaEntity saved = taskRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(TaskId id) {
        if (id != null && id.value() != null) {
            taskRepository.deleteById(id.value());
        }
    }
}
