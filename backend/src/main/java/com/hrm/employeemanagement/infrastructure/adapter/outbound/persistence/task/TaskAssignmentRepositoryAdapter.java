package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskAssignmentPort;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.entity.TaskAssignmentJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.repository.SpringDataTaskAssignmentRepository;

@Component
public class TaskAssignmentRepositoryAdapter implements LoadTaskAssignmentPort, SaveTaskAssignmentPort {

    private final SpringDataTaskAssignmentRepository repository;
    private final TaskAssignmentPersistenceMapper mapper;

    public TaskAssignmentRepositoryAdapter(
            SpringDataTaskAssignmentRepository repository,
            TaskAssignmentPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<TaskAssignment> findByTaskId(TaskId taskId) {
        if (taskId == null || taskId.value() == null) {
            return List.of();
        }
        return repository.findByTaskId(taskId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<TaskAssignment> findByTaskIdIn(List<TaskId> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = taskIds.stream().map(TaskId::value).toList();
        return repository.findByTaskIdIn(ids).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<TaskAssignment> findByEmployeeId(EmployeeId employeeId) {
        if (employeeId == null || employeeId.value() == null) {
            return List.of();
        }
        return repository.findByEmployeeId(employeeId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<TaskAssignment> findByTaskIdAndEmployeeId(TaskId taskId, EmployeeId employeeId) {
        if (taskId == null || taskId.value() == null || employeeId == null || employeeId.value() == null) {
            return Optional.empty();
        }
        return repository.findByTaskIdAndEmployeeId(taskId.value(), employeeId.value())
                .map(mapper::toDomain);
    }

    @Override
    public TaskAssignment save(TaskAssignment taskAssignment) {
        TaskAssignmentJpaEntity entity = mapper.toJpaEntity(taskAssignment);
        TaskAssignmentJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<TaskAssignment> saveAll(List<TaskAssignment> taskAssignments) {
        if (taskAssignments == null || taskAssignments.isEmpty()) {
            return List.of();
        }
        List<TaskAssignmentJpaEntity> entities = taskAssignments.stream()
                .map(mapper::toJpaEntity)
                .toList();
        return repository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteByTaskId(TaskId taskId) {
        if (taskId != null && taskId.value() != null) {
            repository.deleteByTaskId(taskId.value());
        }
    }

    @Override
    public void deleteByTaskIdAndEmployeeIdNotIn(TaskId taskId, List<EmployeeId> employeeIds) {
        if (taskId != null && taskId.value() != null && employeeIds != null && !employeeIds.isEmpty()) {
            List<Long> ids = employeeIds.stream().map(EmployeeId::value).toList();
            repository.deleteByTaskIdAndEmployeeIdNotIn(taskId.value(), ids);
        }
    }
}

