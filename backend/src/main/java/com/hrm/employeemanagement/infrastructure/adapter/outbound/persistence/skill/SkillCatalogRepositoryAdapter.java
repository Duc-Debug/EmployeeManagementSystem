package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.skill.SkillCatalogRepository;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataSkillRepository;

@Component
public class SkillCatalogRepositoryAdapter implements SkillCatalogRepository {

    private final SpringDataSkillRepository repository;

    public SkillCatalogRepositoryAdapter(SpringDataSkillRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Skill> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id).map(SkillPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Skill> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return repository.findByCode(code.trim().toUpperCase()).map(SkillPersistenceMapper::toDomain);
    }

    @Override
    public List<Skill> findAll() {
        return repository.findAll().stream()
                .map(SkillPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) {
            return false;
        }
        return repository.existsById(id);
    }
}
