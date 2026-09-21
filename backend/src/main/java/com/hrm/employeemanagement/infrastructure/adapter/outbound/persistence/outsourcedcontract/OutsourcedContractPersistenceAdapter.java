package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.outsourcedcontract;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadNotificationRecipientUserPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedContractPort;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.UserPersistenceMapper;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;

@Component
public class OutsourcedContractPersistenceAdapter implements
        LoadOutsourcedContractPort,
        LoadOutsourcedAllocationPort,
        LoadNotificationRecipientUserPort {

    private final SpringDataOutsourcedContractEmployeeRepository employeeRepository;
    private final SpringDataOutsourcedContractAllocationRepository allocationRepository;
    private final SpringDataOutsourcedContractRecipientUserRepository recipientUserRepository;
    private final SpringDataProjectRepository projectRepository;
    private final SpringDataOrgUnitRepository orgUnitRepository;
    private final UserPersistenceMapper userPersistenceMapper;

    public OutsourcedContractPersistenceAdapter(
            SpringDataOutsourcedContractEmployeeRepository employeeRepository,
            SpringDataOutsourcedContractAllocationRepository allocationRepository,
            SpringDataOutsourcedContractRecipientUserRepository recipientUserRepository,
            SpringDataProjectRepository projectRepository,
            SpringDataOrgUnitRepository orgUnitRepository,
            UserPersistenceMapper userPersistenceMapper
    ) {
        this.employeeRepository = Objects.requireNonNull(employeeRepository, "employeeRepository must not be null");
        this.allocationRepository = Objects.requireNonNull(allocationRepository, "allocationRepository must not be null");
        this.recipientUserRepository = Objects.requireNonNull(recipientUserRepository, "recipientUserRepository must not be null");
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository must not be null");
        this.orgUnitRepository = Objects.requireNonNull(orgUnitRepository, "orgUnitRepository must not be null");
        this.userPersistenceMapper = Objects.requireNonNull(userPersistenceMapper, "userPersistenceMapper must not be null");
    }

    // ==================== LoadOutsourcedContractPort ====================

    @Override
    public List<Employee> findAllOutsourcedEmployeesWithContract() {
        List<EmployeeJpaEntity> entities = employeeRepository.findAllOutsourcedWithContract();
        return entities.stream()
                .map(userPersistenceMapper::toDomain)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Optional<Employee> findById(Long employeeId) {
        if (employeeId == null) {
            return Optional.empty();
        }
        return employeeRepository.findById(employeeId)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public Map<Long, String> findOrgUnitNamesByIds(List<Long> orgUnitIds) {
        if (orgUnitIds == null || orgUnitIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OrgUnitJpaEntity> orgUnits = orgUnitRepository.findAllById(orgUnitIds);
        Map<Long, String> result = new HashMap<>();
        for (OrgUnitJpaEntity ou : orgUnits) {
            result.put(ou.getId(), ou.getUnitName());
        }
        return result;
    }

    // ==================== LoadOutsourcedAllocationPort ====================

    @Override
    public List<OutsourcedAllocationRecord> findAllocationsByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return Collections.emptyList();
        }
        List<WeeklyProjectAllocationJpaEntity> entities = allocationRepository.findByEmployeeId(employeeId);
        return mapAllocations(entities);
    }

    @Override
    public List<OutsourcedAllocationRecord> findAllocationsByEmployeeIds(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Collections.emptyList();
        }
        int minYear = java.time.LocalDate.now().getYear() - 1;
        return findAllocationsByEmployeeIds(employeeIds, minYear);
    }

    @Override
    public List<OutsourcedAllocationRecord> findAllocationsByEmployeeIds(List<Long> employeeIds, Integer minYear) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<WeeklyProjectAllocationJpaEntity> entities;
        if (minYear != null) {
            entities = allocationRepository.findByEmployeeIdInAndYearGreaterThanEqual(employeeIds, minYear);
        } else {
            entities = allocationRepository.findByEmployeeIdIn(employeeIds);
        }
        return mapAllocations(entities);
    }

    private List<OutsourcedAllocationRecord> mapAllocations(List<WeeklyProjectAllocationJpaEntity> entities) {
        return entities.stream()
                .map(e -> new OutsourcedAllocationRecord(
                        e.getId(),
                        e.getEmployeeId(),
                        e.getProjectId(),
                        YearWeek.of(e.getYear(), e.getWeekNumber()),
                        e.getAllocatedHours()
                ))
                .toList();
    }

    @Override
    public Map<Long, String> findProjectNamesByIds(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProjectJpaEntity> projects = projectRepository.findAllById(projectIds);
        return projects.stream()
                .collect(Collectors.toMap(ProjectJpaEntity::getId, ProjectJpaEntity::getProjectName, (existing, replacement) -> existing));
    }

    // ==================== LoadNotificationRecipientUserPort ====================

    @Override
    public List<Long> findResourceManagersAndHrUserIds() {
        return recipientUserRepository.findResourceManagersAndHrUserIds();
    }
}
