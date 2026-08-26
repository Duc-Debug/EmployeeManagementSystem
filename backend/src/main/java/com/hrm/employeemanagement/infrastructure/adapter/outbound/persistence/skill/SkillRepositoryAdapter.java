package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.skill.*;
import com.hrm.employeemanagement.domain.exception.skill.DuplicateSkillNameException;
import com.hrm.employeemanagement.domain.skill.*;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.*;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.mapper.SkillPersistenceMapper;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.*;

@Component
public class SkillRepositoryAdapter implements
        LoadSkillPort,
        SaveSkillPort,
        LoadSkillGroupPort,
        SaveSkillGroupPort {

    private final SpringDataSkillRepository skillRepository;
    private final SpringDataSkillGroupRepository skillGroupRepository;
    private final SpringDataEmployeeSkillRepository employeeSkillRepository;

    public SkillRepositoryAdapter(SpringDataSkillRepository skillRepository,
                                  SpringDataSkillGroupRepository skillGroupRepository,
                                  SpringDataEmployeeSkillRepository employeeSkillRepository) {
        this.skillRepository = skillRepository;
        this.skillGroupRepository = skillGroupRepository;
        this.employeeSkillRepository = employeeSkillRepository;
    }

    // =========================================================================
    // LOAD SKILL PORT
    // =========================================================================

    @Override
    public Optional<Skill> findById(SkillId id) {
        if (id == null || id.value() == null) return Optional.empty();
        return skillRepository.findById(id.value())
                .map(SkillPersistenceMapper::toDomain);
    }

    @Override
    public List<Skill> findAllByIdIn(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return skillRepository.findAllById(ids).stream()
                .map(SkillPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Skill> findAll(Long groupId, String status, String keyword) {
        return skillRepository.searchSkills(groupId, status, keyword).stream()
                .map(SkillPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        return skillRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByNameIgnoreCaseAndIdNot(String name, Long id) {
        return skillRepository.existsByNameIgnoreCaseAndIdNot(name, id);
    }

    @Override
    public List<Long> findEmployeeIdsWithSkill(Long skillId) {
        return employeeSkillRepository.findEmployeeIdsBySkillId(skillId);
    }

    @Override
    public boolean hasEmployeeSkill(Long employeeId, Long skillId) {
        return employeeSkillRepository.existsByEmployeeIdAndSkillId(employeeId, skillId);
    }

    @Override
    public long countByGroupIdAndStatus(Long groupId, SkillStatus status) {
        return skillRepository.countByGroupIdAndStatus(groupId, status != null ? status.name() : null);
    }

    // =========================================================================
    // SAVE SKILL PORT
    // =========================================================================

    @Override
    public Skill save(Skill skill) {
        try {
            SkillJpaEntity jpaEntity = SkillPersistenceMapper.toJpaEntity(skill);
            SkillJpaEntity saved = skillRepository.save(jpaEntity);
            return SkillPersistenceMapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
            String lowerMsg = rootMsg != null ? rootMsg.toLowerCase() : "";
            if (lowerMsg.contains("uk_skills_name") || lowerMsg.contains("duplicate") || lowerMsg.contains("unique")) {
                throw new DuplicateSkillNameException("Tên kỹ năng '" + skill.getName() + "' đã tồn tại trong hệ thống.");
            }
            throw ex;
        }
    }

    @Override
    public void reassignEmployeeSkills(Long sourceSkillId, Long targetSkillId) {
        employeeSkillRepository.reassignEmployeeSkills(sourceSkillId, targetSkillId);
    }

    @Override
    public void removeEmployeeSkill(Long employeeId, Long skillId) {
        employeeSkillRepository.deleteByEmployeeIdAndSkillId(employeeId, skillId);
    }

    // =========================================================================
    // LOAD SKILL GROUP PORT
    // =========================================================================

    @Override
    public Optional<SkillGroup> findById(SkillGroupId id) {
        if (id == null || id.value() == null) return Optional.empty();
        return skillGroupRepository.findById(id.value())
                .map(SkillPersistenceMapper::toDomain);
    }

    @Override
    public List<SkillGroup> findAll() {
        return skillGroupRepository.findAll().stream()
                .map(SkillPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(SkillGroupId id) {
        if (id == null || id.value() == null) return false;
        return skillGroupRepository.existsById(id.value());
    }

    @Override
    public boolean existsGroupByNameIgnoreCase(String name) {
        return skillGroupRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsGroupByNameIgnoreCaseAndIdNot(String name, Long id) {
        return skillGroupRepository.existsByNameIgnoreCaseAndIdNot(name, id);
    }


    // =========================================================================
    // SAVE SKILL GROUP PORT
    // =========================================================================

    @Override
    public SkillGroup save(SkillGroup skillGroup) {
        try {
            SkillGroupJpaEntity jpaEntity = SkillPersistenceMapper.toJpaEntity(skillGroup);
            SkillGroupJpaEntity saved = skillGroupRepository.save(jpaEntity);
            return SkillPersistenceMapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
            String lowerMsg = rootMsg != null ? rootMsg.toLowerCase() : "";
            if (lowerMsg.contains("uk_skill_groups_name") || lowerMsg.contains("duplicate") || lowerMsg.contains("unique")) {
                throw new DuplicateSkillNameException("Tên nhóm kỹ năng '" + skillGroup.getName() + "' đã tồn tại trong hệ thống.");
            }
            throw ex;
        }
    }
}
