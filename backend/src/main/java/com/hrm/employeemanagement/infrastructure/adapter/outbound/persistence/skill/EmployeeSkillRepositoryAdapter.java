package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.domain.exception.skill.DuplicateEmployeeSkillException;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.EmployeeSkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataEmployeeSkillRepository;

@Component
public class EmployeeSkillRepositoryAdapter implements EmployeeSkillRepository {

    private final SpringDataEmployeeSkillRepository repository;

    public EmployeeSkillRepositoryAdapter(SpringDataEmployeeSkillRepository repository) {
        this.repository = repository;
    }

    @Override
    public EmployeeSkill save(EmployeeSkill employeeSkill) {
        try {
            EmployeeSkillJpaEntity jpaEntity = SkillPersistenceMapper.toJpaEntity(employeeSkill);
            EmployeeSkillJpaEntity saved = repository.save(jpaEntity);
            return SkillPersistenceMapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            String msg = ex.getRootCause() != null ? ex.getRootCause().getMessage().toLowerCase() : "";
            if (msg.contains("uq_employee_skill") || msg.contains("duplicate")) {
                throw new DuplicateEmployeeSkillException("Kỹ năng này đã có trong hồ sơ của nhân viên.");
            }
            throw ex;
        }
    }

    @Override
    public Optional<EmployeeSkill> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id).map(SkillPersistenceMapper::toDomain);
    }

    @Override
    public Optional<EmployeeSkill> findByEmployeeIdAndSkillId(Long employeeId, Long skillId) {
        if (employeeId == null || skillId == null) {
            return Optional.empty();
        }
        return repository.findByEmployeeIdAndSkillId(employeeId, skillId).map(SkillPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmployeeIdAndSkillId(Long employeeId, Long skillId) {
        if (employeeId == null || skillId == null) {
            return false;
        }
        return repository.existsByEmployeeIdAndSkillId(employeeId, skillId);
    }

    @Override
    public List<EmployeeSkill> findByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return List.of();
        }
        return repository.findByEmployeeId(employeeId).stream()
                .map(SkillPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }
}
