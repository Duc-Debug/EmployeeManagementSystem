package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.unavailability;

import com.hrm.employeemanagement.application.port.outbound.unavailability.LoadUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.SaveUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityDeclaration;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.unavailability.entity.UnavailabilityDeclarationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.unavailability.repository.SpringDataUnavailabilityDeclarationRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class UnavailabilityDeclarationRepositoryAdapter implements
        SaveUnavailabilityDeclarationPort,
        LoadUnavailabilityDeclarationPort {

    private final SpringDataUnavailabilityDeclarationRepository repository;

    public UnavailabilityDeclarationRepositoryAdapter(SpringDataUnavailabilityDeclarationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public UnavailabilityDeclaration save(UnavailabilityDeclaration declaration) {
        UnavailabilityDeclarationJpaEntity entity = UnavailabilityDeclarationPersistenceMapper.toJpaEntity(declaration);
        UnavailabilityDeclarationJpaEntity saved = repository.save(entity);
        return UnavailabilityDeclarationPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<UnavailabilityDeclaration> findById(Long id) {
        return repository.findById(id)
                .map(UnavailabilityDeclarationPersistenceMapper::toDomain);
    }

    @Override
    public Optional<UnavailabilityDeclaration> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id)
                .map(UnavailabilityDeclarationPersistenceMapper::toDomain);
    }

    @Override
    public List<UnavailabilityDeclaration> findByEmployeeId(Long employeeId) {
        return repository.findByEmployeeIdOrderByStartDateDesc(employeeId).stream()
                .map(UnavailabilityDeclarationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<UnavailabilityDeclaration> findAllPending() {
        return repository.findAllPending().stream()
                .map(UnavailabilityDeclarationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<UnavailabilityDeclaration> findPendingByOrgUnitIds(List<Long> orgUnitIds) {
        if (orgUnitIds == null || orgUnitIds.isEmpty()) {
            return List.of();
        }
        return repository.findPendingByOrgUnitIds(orgUnitIds).stream()
                .map(UnavailabilityDeclarationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<UnavailabilityDeclaration> findActiveOverlapping(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return repository.findActiveOverlapping(employeeId, startDate, endDate).stream()
                .map(UnavailabilityDeclarationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<UnavailabilityDeclaration> findApprovedByEmployeeIdAndDateRange(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return repository.findApprovedByEmployeeIdAndDateRange(employeeId, startDate, endDate).stream()
                .map(UnavailabilityDeclarationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public BigDecimal getTotalApprovedUnavailabilityHoursBetween(Long employeeId, LocalDate startDate, LocalDate endDate) {
        BigDecimal sum = repository.sumApprovedHoursBetween(employeeId, startDate, endDate);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
