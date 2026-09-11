package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave;

import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveRequestPort;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity.LeaveRequestJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository.SpringDataLeaveRequestRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class LeaveRequestRepositoryAdapter implements SaveLeaveRequestPort, LoadLeaveRequestPort {

    private final SpringDataLeaveRequestRepository springDataLeaveRequestRepository;
    private final LeaveRequestPersistenceMapper mapper;

    public LeaveRequestRepositoryAdapter(
            SpringDataLeaveRequestRepository springDataLeaveRequestRepository,
            LeaveRequestPersistenceMapper mapper) {
        this.springDataLeaveRequestRepository = Objects.requireNonNull(springDataLeaveRequestRepository, "springDataLeaveRequestRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public LeaveRequest save(LeaveRequest leaveRequest) {
        LeaveRequestJpaEntity entity = mapper.toJpaEntity(leaveRequest);
        LeaveRequestJpaEntity saved = springDataLeaveRequestRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<LeaveRequest> findById(Long id) {
        return springDataLeaveRequestRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<LeaveRequest> findByIdForUpdate(Long id) {
        return springDataLeaveRequestRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public List<LeaveRequest> findByEmployeeId(Long employeeId) {
        return springDataLeaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(employeeId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsOverlappingLeave(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return springDataLeaveRequestRepository.existsOverlappingLeave(employeeId, startDate, endDate);
    }

    @Override
    public List<LeaveRequest> findPendingRequests() {
        return springDataLeaveRequestRepository.findByStatusOrderByCreatedAtAsc("PENDING")
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public com.hrm.employeemanagement.application.dto.user.PageResult<LeaveRequest> findPendingRequests(
            com.hrm.employeemanagement.domain.authorization.DataScope dataScope,
            Long scopeOrgUnitId,
            Long currentUserId,
            int page,
            int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 20 : Math.min(size, 1000);
        int offset = safePage * safeSize;

        com.hrm.employeemanagement.domain.authorization.DataScope scope =
                dataScope != null ? dataScope : com.hrm.employeemanagement.domain.authorization.DataScope.COMPANY;

        return switch (scope) {
            case ORGANIZATION_BRANCH -> {
                if (scopeOrgUnitId == null) {
                    yield new com.hrm.employeemanagement.application.dto.user.PageResult<>(List.of(), safePage, safeSize, 0);
                }
                List<LeaveRequestJpaEntity> entities =
                        springDataLeaveRequestRepository.findPendingBranchScope(scopeOrgUnitId, safeSize, offset);
                long totalElements = springDataLeaveRequestRepository.countPendingBranchScope(scopeOrgUnitId);
                yield toPageResult(entities, safePage, safeSize, totalElements);
            }
            case SELF -> {
                if (currentUserId == null) {
                    yield new com.hrm.employeemanagement.application.dto.user.PageResult<>(List.of(), safePage, safeSize, 0);
                }
                List<LeaveRequestJpaEntity> entities =
                        springDataLeaveRequestRepository.findPendingSelfScope(currentUserId, safeSize, offset);
                long totalElements = springDataLeaveRequestRepository.countPendingSelfScope(currentUserId);
                yield toPageResult(entities, safePage, safeSize, totalElements);
            }
            case COMPANY -> {
                List<LeaveRequestJpaEntity> entities =
                        springDataLeaveRequestRepository.findPendingCompanyScope(safeSize, offset);
                long totalElements = springDataLeaveRequestRepository.countPendingCompanyScope();
                yield toPageResult(entities, safePage, safeSize, totalElements);
            }
        };
    }

    private com.hrm.employeemanagement.application.dto.user.PageResult<LeaveRequest> toPageResult(
            List<LeaveRequestJpaEntity> entities,
            int page,
            int size,
            long totalElements
    ) {
        List<LeaveRequest> content = entities.stream()
                .map(mapper::toDomain)
                .toList();
        return new com.hrm.employeemanagement.application.dto.user.PageResult<>(content, page, size, totalElements);
    }
}
