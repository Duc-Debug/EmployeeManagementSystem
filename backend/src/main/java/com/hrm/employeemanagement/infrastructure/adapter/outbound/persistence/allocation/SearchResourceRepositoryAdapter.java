package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.dto.allocation.ResourceCandidate;
import com.hrm.employeemanagement.application.port.outbound.allocation.SearchResourcePort;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.EmployeeSkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataEmployeeSkillRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataSkillRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;

@Component
public class SearchResourceRepositoryAdapter implements SearchResourcePort {

    private final SpringDataEmployeeSkillRepository employeeSkillRepository;
    private final SpringDataSkillRepository skillRepository;
    private final SpringDataEmployeeRepository employeeRepository;

    public SearchResourceRepositoryAdapter(
            SpringDataEmployeeSkillRepository employeeSkillRepository,
            SpringDataSkillRepository skillRepository,
            SpringDataEmployeeRepository employeeRepository
    ) {
        this.employeeSkillRepository = employeeSkillRepository;
        this.skillRepository = skillRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<ResourceCandidate> findActiveEmployeesBySkill(Long skillId, int minProficiencyLevel) {
        // 1. Tải thông tin kỹ năng để lấy tên
        Optional<SkillJpaEntity> skillOpt = skillRepository.findById(skillId);
        if (skillOpt.isEmpty()) {
            return List.of();
        }
        String skillName = skillOpt.get().getName();

        // 2. Tìm tất cả nhân viên có kỹ năng này đã được duyệt và đạt minLevel
        List<EmployeeSkillJpaEntity> skillRecords = employeeSkillRepository.findApprovedBySkillAndMinLevel(skillId, minProficiencyLevel);
        if (skillRecords.isEmpty()) {
            return List.of();
        }

        // 3. Batch query danh sách Employee thay vì gọi N queries trong vòng lặp
        List<Long> employeeIds = skillRecords.stream()
                .map(EmployeeSkillJpaEntity::getEmployeeId)
                .distinct()
                .toList();

        Map<Long, EmployeeJpaEntity> employeeMap = employeeRepository.findAllById(employeeIds).stream()
                .filter(emp -> "ACTIVE".equalsIgnoreCase(emp.getStatus()))
                .collect(Collectors.toMap(EmployeeJpaEntity::getId, Function.identity(), (a, b) -> a));

        List<ResourceCandidate> candidates = new ArrayList<>();
        for (EmployeeSkillJpaEntity record : skillRecords) {
            EmployeeJpaEntity empEntity = employeeMap.get(record.getEmployeeId());
            if (empEntity != null) {
                candidates.add(new ResourceCandidate(
                        empEntity.getId(),
                        empEntity.getUserId(),
                        empEntity.getEmployeeCode(),
                        empEntity.getFullName(),
                        empEntity.getOrgUnitId(),
                        empEntity.getProfessionalRole(),
                        empEntity.getStandardHoursPerWeek(),
                        empEntity.getContractEndDate(),
                        skillId,
                        skillName,
                        record.getProficiencyLevel(),
                        record.getYearsOfExperience()
                ));
            }
        }

        return candidates;
    }
}
