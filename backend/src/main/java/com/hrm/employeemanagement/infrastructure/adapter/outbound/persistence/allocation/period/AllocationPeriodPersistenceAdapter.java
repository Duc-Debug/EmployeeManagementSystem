package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.allocation.period.LoadAllocationPlanSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.LoadAllocationPlanningPeriodPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.LoadAllocationsForPeriodPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.SaveAllocationPlanSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.SaveAllocationPlanningPeriodPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodStatus;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPlanSnapshot;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPlanningPeriod;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.entity.AllocationPlanSnapshotItemJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.entity.AllocationPlanSnapshotJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.entity.AllocationPlanningPeriodJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.repository.SpringDataAllocationPlanSnapshotItemRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.repository.SpringDataAllocationPlanSnapshotRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.repository.SpringDataAllocationPlanningPeriodRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class AllocationPeriodPersistenceAdapter implements
        SaveAllocationPlanningPeriodPort,
        LoadAllocationPlanningPeriodPort,
        SaveAllocationPlanSnapshotPort,
        LoadAllocationPlanSnapshotPort,
        LoadAllocationsForPeriodPort {

    private final SpringDataAllocationPlanningPeriodRepository periodRepository;
    private final SpringDataAllocationPlanSnapshotRepository snapshotRepository;
    private final SpringDataAllocationPlanSnapshotItemRepository itemRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AllocationPeriodPersistenceAdapter(
            SpringDataAllocationPlanningPeriodRepository periodRepository,
            SpringDataAllocationPlanSnapshotRepository snapshotRepository,
            SpringDataAllocationPlanSnapshotItemRepository itemRepository
    ) {
        this.periodRepository = periodRepository;
        this.snapshotRepository = snapshotRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    public AllocationPlanningPeriod save(AllocationPlanningPeriod period) {
        AllocationPlanningPeriodJpaEntity entity = AllocationPeriodPersistenceMapper.toEntityPeriod(period);
        AllocationPlanningPeriodJpaEntity saved = periodRepository.save(entity);
        return AllocationPeriodPersistenceMapper.toDomainPeriod(saved);
    }

    @Override
    public Optional<AllocationPlanningPeriod> findById(Long id) {
        return periodRepository.findById(id)
                .map(AllocationPeriodPersistenceMapper::toDomainPeriod);
    }

    @Override
    public List<AllocationPlanningPeriod> findAll(Integer year, AllocationPeriodStatus status) {
        List<AllocationPlanningPeriodJpaEntity> entities;
        if (year != null && status != null) {
            entities = periodRepository.findByYearAndStatus(year, status.name());
        } else if (year != null) {
            entities = periodRepository.findByYear(year);
        } else if (status != null) {
            entities = periodRepository.findByStatus(status.name());
        } else {
            entities = periodRepository.findAll();
        }
        return entities.stream()
                .map(AllocationPeriodPersistenceMapper::toDomainPeriod)
                .toList();
    }

    @Override
    public List<AllocationPlanningPeriod> findLockedPeriodsCoveringWeek(int year, int weekNumber) {
        return periodRepository.findLockedPeriodsCoveringWeek(year, weekNumber).stream()
                .map(AllocationPeriodPersistenceMapper::toDomainPeriod)
                .toList();
    }

    @Override
    public boolean existsOverlapping(Integer year, int startWeek, int endWeek, Long excludeId) {
        return periodRepository.existsOverlapping(year, startWeek, endWeek, excludeId);
    }

    @Override
    public AllocationPlanSnapshot save(AllocationPlanSnapshot snapshot) {
        AllocationPlanSnapshotJpaEntity snapshotEntity = new AllocationPlanSnapshotJpaEntity(
                snapshot.getId(),
                snapshot.getPeriodId(),
                snapshot.getSnapshotVersion(),
                snapshot.getTotalAllocations(),
                snapshot.getTotalAllocatedHours(),
                snapshot.getCreatedBy()
        );
        AllocationPlanSnapshotJpaEntity savedSnapshot = snapshotRepository.save(snapshotEntity);

        if (snapshot.getItems() != null && !snapshot.getItems().isEmpty()) {
            List<AllocationPlanSnapshotItemJpaEntity> itemEntities = snapshot.getItems().stream()
                    .map(item -> new AllocationPlanSnapshotItemJpaEntity(
                            item.getId(),
                            savedSnapshot.getId(),
                            item.getOriginalAllocationId(),
                            item.getEmployeeId(),
                            item.getProjectId(),
                            item.getYear(),
                            item.getWeekNumber(),
                            item.getAllocatedHours(),
                            item.getAllocationPercentage(),
                            item.isOverloaded(),
                            item.getOverloadReason()
                    ))
                    .toList();
            List<AllocationPlanSnapshotItemJpaEntity> savedItems = itemRepository.saveAll(itemEntities);
            return AllocationPeriodPersistenceMapper.toDomainSnapshot(savedSnapshot, savedItems);
        }

        return AllocationPeriodPersistenceMapper.toDomainSnapshot(savedSnapshot, List.of());
    }

    @Override
    public Optional<AllocationPlanSnapshot> findSnapshotById(Long id) {
        return snapshotRepository.findById(id)
                .map(entity -> {
                    List<AllocationPlanSnapshotItemJpaEntity> items = itemRepository.findBySnapshotId(entity.getId());
                    return AllocationPeriodPersistenceMapper.toDomainSnapshot(entity, items);
                });
    }

    @Override
    public Optional<AllocationPlanSnapshot> findLatestByPeriodId(Long periodId) {
        return snapshotRepository.findFirstByPeriodIdOrderBySnapshotVersionDesc(periodId)
                .map(entity -> {
                    List<AllocationPlanSnapshotItemJpaEntity> items = itemRepository.findBySnapshotId(entity.getId());
                    return AllocationPeriodPersistenceMapper.toDomainSnapshot(entity, items);
                });
    }

    @Override
    public List<AllocationPlanSnapshot> findByPeriodId(Long periodId) {
        return snapshotRepository.findByPeriodIdOrderBySnapshotVersionDesc(periodId).stream()
                .map(entity -> {
                    List<AllocationPlanSnapshotItemJpaEntity> items = itemRepository.findBySnapshotId(entity.getId());
                    return AllocationPeriodPersistenceMapper.toDomainSnapshot(entity, items);
                })
                .toList();
    }

    @Override
    public int countSnapshotsByPeriodId(Long periodId) {
        return snapshotRepository.countByPeriodId(periodId);
    }

    @Override
    public List<WeeklyProjectAllocation> loadAllocationsInWeekRange(int year, int startWeek, int endWeek) {
        List<WeeklyProjectAllocationJpaEntity> entities = entityManager.createQuery(
                        "SELECT a FROM WeeklyProjectAllocationJpaEntity a WHERE a.year = :year AND a.weekNumber BETWEEN :startWeek AND :endWeek",
                        WeeklyProjectAllocationJpaEntity.class)
                .setParameter("year", year)
                .setParameter("startWeek", startWeek)
                .setParameter("endWeek", endWeek)
                .getResultList();

        return entities.stream()
                .map(AllocationPeriodPersistenceMapper::toDomainAllocation)
                .toList();
    }
}
