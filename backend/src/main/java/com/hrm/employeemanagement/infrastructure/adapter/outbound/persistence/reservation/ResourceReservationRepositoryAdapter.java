package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.reservation;

import com.hrm.employeemanagement.application.port.outbound.reservation.LoadResourceReservationPort;
import com.hrm.employeemanagement.application.port.outbound.reservation.SaveResourceReservationPort;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.reservation.ReservationStatus;
import com.hrm.employeemanagement.domain.reservation.ResourceReservation;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.reservation.entity.ResourceReservationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.reservation.repository.SpringDataResourceReservationRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ResourceReservationRepositoryAdapter implements LoadResourceReservationPort, SaveResourceReservationPort {

    private final SpringDataResourceReservationRepository repository;

    public ResourceReservationRepositoryAdapter(SpringDataResourceReservationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "SpringDataResourceReservationRepository must not be null");
    }

    @Override
    public ResourceReservation save(ResourceReservation reservation) {
        ResourceReservationJpaEntity entity = ResourceReservationPersistenceMapper.toEntity(reservation);
        ResourceReservationJpaEntity saved = repository.save(entity);
        return ResourceReservationPersistenceMapper.toDomain(saved);
    }

    @Override
    public List<ResourceReservation> saveAll(List<ResourceReservation> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            return List.of();
        }
        List<ResourceReservationJpaEntity> entities = reservations.stream()
                .map(ResourceReservationPersistenceMapper::toEntity)
                .toList();
        return repository.saveAll(entities).stream()
                .map(ResourceReservationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ResourceReservation> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id).map(ResourceReservationPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ResourceReservation> findActiveByProjectAndEmployeeAndYearWeek(Long projectId, Long employeeId, YearWeek yearWeek) {
        if (projectId == null || employeeId == null || yearWeek == null) {
            return Optional.empty();
        }
        return repository.findFirstByProjectIdAndEmployeeIdAndYearNumberAndWeekNumberAndStatus(
                projectId, employeeId, yearWeek.year(), yearWeek.weekNumber(), ReservationStatus.ACTIVE
        ).map(ResourceReservationPersistenceMapper::toDomain);
    }

    @Override
    public List<ResourceReservation> findActiveByEmployeeIdAndYearWeek(Long employeeId, YearWeek yearWeek) {
        if (employeeId == null || yearWeek == null) {
            return List.of();
        }
        return repository.findAllByEmployeeIdAndYearNumberAndWeekNumberAndStatus(
                employeeId, yearWeek.year(), yearWeek.weekNumber(), ReservationStatus.ACTIVE
        ).stream().map(ResourceReservationPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<ResourceReservation> findActiveByEmployeeIdsAndYearWeeks(List<Long> employeeIds, List<YearWeek> yearWeeks) {
        if (employeeIds == null || employeeIds.isEmpty() || yearWeeks == null || yearWeeks.isEmpty()) {
            return List.of();
        }
        Set<String> weekKeys = yearWeeks.stream()
                .map(yw -> yw.year() + "_" + yw.weekNumber())
                .collect(Collectors.toSet());

        List<ResourceReservationJpaEntity> entities = repository.findAllByEmployeeIdInAndStatus(
                employeeIds, ReservationStatus.ACTIVE
        );

        return entities.stream()
                .filter(e -> weekKeys.contains(e.getYearNumber() + "_" + e.getWeekNumber()))
                .map(ResourceReservationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ResourceReservation> findByProjectId(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        return repository.findAllByProjectId(projectId).stream()
                .map(ResourceReservationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ResourceReservation> findActiveByProjectId(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        return repository.findAllByProjectIdAndStatus(projectId, ReservationStatus.ACTIVE).stream()
                .map(ResourceReservationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ResourceReservation> findReservations(
            Long projectId, Long employeeId, Integer year, Integer weekNumber, ReservationStatus status
    ) {
        return repository.findReservations(projectId, employeeId, year, weekNumber, status).stream()
                .map(ResourceReservationPersistenceMapper::toDomain)
                .toList();
    }
}
