package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.conflict.SaveScheduleConflictReplacementPort;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictReplacement;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.entity.ScheduleConflictReplacementJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.repository.SpringDataScheduleConflictReplacementRepository;

@Component
public class JpaScheduleConflictReplacementRepositoryAdapter implements SaveScheduleConflictReplacementPort {

    private final SpringDataScheduleConflictReplacementRepository repository;

    public JpaScheduleConflictReplacementRepositoryAdapter(SpringDataScheduleConflictReplacementRepository repository) {
        this.repository = repository;
    }

    @Override
    public ScheduleConflictReplacement save(ScheduleConflictReplacement domain) {
        ScheduleConflictReplacementJpaEntity entity = new ScheduleConflictReplacementJpaEntity();
        entity.setId(domain.getId());
        entity.setConflictId(domain.getConflictId());
        entity.setOriginalEmployeeId(domain.getOriginalEmployeeId());
        entity.setReplacementEmployeeId(domain.getReplacementEmployeeId());
        entity.setSkillId(domain.getSkillId());
        entity.setProficiencyLevel(domain.getProficiencyLevel());
        entity.setFreeHours(domain.getFreeHours());
        entity.setStatus(domain.getStatus());
        entity.setNotes(domain.getNotes());
        entity.setCreatedBy(domain.getCreatedBy());

        ScheduleConflictReplacementJpaEntity saved = repository.save(entity);

        return new ScheduleConflictReplacement(
                saved.getId(),
                saved.getConflictId(),
                saved.getOriginalEmployeeId(),
                saved.getReplacementEmployeeId(),
                saved.getSkillId(),
                saved.getProficiencyLevel(),
                saved.getFreeHours(),
                saved.getStatus(),
                saved.getNotes(),
                saved.getCreatedBy(),
                saved.getCreatedAt()
        );
    }
}
