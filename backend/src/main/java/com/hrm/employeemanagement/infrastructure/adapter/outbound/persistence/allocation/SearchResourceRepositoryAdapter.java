package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.dto.allocation.EmployeeSkillCandidate;
import com.hrm.employeemanagement.application.port.outbound.allocation.SearchResourcePort;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.EmployeeSkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataEmployeeSkillRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataSkillRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.UserPersistenceMapper;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;

@Component
public class SearchResourceRepositoryAdapter implements SearchResourcePort {

    private final SpringDataEmployeeSkillRepository employeeSkillRepository;
    private final SpringDataSkillRepository skillRepository;
    private final SpringDataEmployeeRepository employeeRepository;
    private final UserPersistenceMapper userMapper;

    public SearchResourceRepositoryAdapter(
            SpringDataEmployeeSkillRepository employeeSkillRepository,
            SpringDataSkillRepository skillRepository,
            SpringDataEmployeeRepository employeeRepository,
            UserPersistenceMapper userMapper
    ) {
        this.employeeSkillRepository = employeeSkillRepository;
        this.skillRepository = skillRepository;
        this.employeeRepository = employeeRepository;
        this.userMapper = userMapper;
    }

    @Override
    public List<EmployeeSkillCandidate> findActiveEmployeesBySkill(Long skillId, int minProficiencyLevel) {
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

        List<EmployeeSkillCandidate> candidates = new ArrayList<>();
        for (EmployeeSkillJpaEntity record : skillRecords) {
            Optional<EmployeeJpaEntity> empEntityOpt = employeeRepository.findById(record.getEmployeeId());
            if (empEntityOpt.isPresent()) {
                EmployeeJpaEntity empEntity = empEntityOpt.get();
                // Chỉ lấy nhân sự đang ACTIVE
                if ("ACTIVE".equalsIgnoreCase(empEntity.getStatus())) {
                    Employee domainEmp = userMapper.toDomain(empEntity);
                    candidates.add(new EmployeeSkillCandidate(
                            domainEmp,
                            skillId,
                            skillName,
                            record.getProficiencyLevel(),
                            record.getYearsOfExperience()
                    ));
                }
            }
        }

        return candidates;
    }
}
