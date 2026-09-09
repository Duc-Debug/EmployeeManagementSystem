package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.milestone;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.milestone.DeleteMilestonePort;
import com.hrm.employeemanagement.application.port.outbound.milestone.LoadMilestonePort;
import com.hrm.employeemanagement.application.port.outbound.milestone.SaveMilestonePort;
import com.hrm.employeemanagement.domain.milestone.Milestone;
import com.hrm.employeemanagement.domain.milestone.MilestoneId;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.milestone.entity.ProjectMilestoneJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.milestone.repository.SpringDataProjectMilestoneRepository;

@Component
public class MilestoneRepositoryAdapter implements LoadMilestonePort, SaveMilestonePort, DeleteMilestonePort {

    private final SpringDataProjectMilestoneRepository repository;

    public MilestoneRepositoryAdapter(SpringDataProjectMilestoneRepository repository) {
        this.repository = Objects.requireNonNull(repository, "SpringDataProjectMilestoneRepository must not be null");
    }

    @Override
    public Optional<Milestone> findById(MilestoneId id) {
        if (id == null || id.value() == null) {
            return Optional.empty();
        }
        return repository.findById(id.value())
                .map(MilestonePersistenceMapper::toDomain);
    }

    @Override
    public List<Milestone> findAllByProjectId(ProjectId projectId) {
        if (projectId == null || projectId.value() == null) {
            return List.of();
        }
        return repository.findAllByProjectIdOrderByPlannedDateAsc(projectId.value()).stream()
                .map(MilestonePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByProjectIdAndName(ProjectId projectId, String name) {
        if (projectId == null || projectId.value() == null || name == null) {
            return false;
        }
        return repository.existsByProjectIdAndNameIgnoreCase(projectId.value(), name.trim());
    }

    @Override
    public Milestone save(Milestone milestone) {
        ProjectMilestoneJpaEntity entity = MilestonePersistenceMapper.toEntity(milestone);
        ProjectMilestoneJpaEntity saved = repository.save(entity);
        return MilestonePersistenceMapper.toDomain(saved);
    }

    @Override
    public void deleteById(MilestoneId id) {
        if (id != null && id.value() != null) {
            repository.deleteById(id.value());
        }
    }
}
