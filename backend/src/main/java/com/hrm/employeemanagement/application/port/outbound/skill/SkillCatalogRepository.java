package com.hrm.employeemanagement.application.port.outbound.skill;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.skill.Skill;

public interface SkillCatalogRepository {

    Optional<Skill> findById(Long id);

    Optional<Skill> findByCode(String code);

    List<Skill> findAll();

    boolean existsById(Long id);
}
