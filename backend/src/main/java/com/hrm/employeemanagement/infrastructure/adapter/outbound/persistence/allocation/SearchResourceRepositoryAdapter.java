package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.dto.allocation.ResourceCandidate;
import com.hrm.employeemanagement.application.port.outbound.allocation.SearchResourcePort;
import com.hrm.employeemanagement.domain.exception.skill.SkillNotFoundException;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.projection.ActiveEmployeeSkillProjection;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataEmployeeSkillRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataSkillRepository;

@Component
public class SearchResourceRepositoryAdapter implements SearchResourcePort {

    private final SpringDataEmployeeSkillRepository employeeSkillRepository;
    private final SpringDataSkillRepository skillRepository;

    public SearchResourceRepositoryAdapter(
            SpringDataEmployeeSkillRepository employeeSkillRepository,
            SpringDataSkillRepository skillRepository
    ) {
        this.employeeSkillRepository = employeeSkillRepository;
        this.skillRepository = skillRepository;
    }

    @Override
    public List<ResourceCandidate> findActiveEmployeesBySkill(Long skillId, int minProficiencyLevel) {
        // 1. Kiểm tra kỹ năng có tồn tại trong hệ thống hay không
        if (!skillRepository.existsById(skillId)) {
            throw new SkillNotFoundException("Không tìm thấy kỹ năng với ID: " + skillId);
        }

        // 2. Thực thi 1 truy vấn JOIN duy nhất giữa employee_skills, employees và skills
        List<ActiveEmployeeSkillProjection> projections = employeeSkillRepository
                .findActiveEmployeesBySkillAndMinLevel(skillId, minProficiencyLevel);

        if (projections == null || projections.isEmpty()) {
            return List.of();
        }

        List<ResourceCandidate> candidates = new ArrayList<>();
        Set<Long> seenEmployeeIds = new HashSet<>();

        for (ActiveEmployeeSkillProjection p : projections) {
            if (!seenEmployeeIds.add(p.getEmployeeId())) {
                continue; // Tránh nhân bản nếu có nhiều bản ghi skill cùng employee
            }
            candidates.add(new ResourceCandidate(
                    p.getEmployeeId(),
                    p.getUserId(),
                    p.getEmployeeCode(),
                    p.getFullName(),
                    p.getOrgUnitId(),
                    p.getProfessionalRole(),
                    p.getStandardHoursPerWeek(),
                    p.getContractEndDate(),
                    p.getSkillId(),
                    p.getSkillName(),
                    p.getProficiencyLevel(),
                    p.getYearsOfExperience()
            ));
        }

        return candidates;
    }
}
