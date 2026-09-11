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
    public List<LeaveRequest> findByEmployeeId(Long employeeId) {
        return springDataLeaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(employeeId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsOverlappingLeave(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return springDataLeaveRequestRepository.existsOverlappingLeave(employeeId, startDate, endDate);
    }
}
