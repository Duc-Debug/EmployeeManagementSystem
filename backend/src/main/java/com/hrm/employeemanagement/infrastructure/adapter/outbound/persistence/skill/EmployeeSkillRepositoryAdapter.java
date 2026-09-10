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
            if (isDuplicateSkillConstraintViolation(ex)) {
                throw new DuplicateEmployeeSkillException("Kỹ năng này đã có trong hồ sơ của nhân viên.");
            }
            throw ex;
        }
    }

    private boolean isDuplicateSkillConstraintViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String message = cause != null && cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";

        // 1. Kiểm tra chính xác tên constraint unique "uq_employee_skill"
        if (message.contains("uq_employee_skill")) {
            return true;
        }

        // 2. Kiểm tra mã lỗi SQL / SQLState dành cho lỗi trùng lặp dữ liệu (MySQL: 1062, ANSI SQLState: 23000 / 23505)
        if (cause instanceof java.sql.SQLException sqlEx) {
            int errorCode = sqlEx.getErrorCode();
            String sqlState = sqlEx.getSQLState();
            if (errorCode == 1062 || "23000".equals(sqlState) || "23505".equals(sqlState)) {
                return message.contains("employee_skills") || message.contains("employee_id");
            }
        }
        return false;
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

    @Override
    public List<EmployeeSkill> findByStatus(com.hrm.employeemanagement.domain.skill.SkillStatus status) {
        if (status == null) {
            return List.of();
        }
        return repository.findByStatus(status).stream()
                .map(SkillPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmployeeSkill> findByStatusAndEmployeeIdIn(com.hrm.employeemanagement.domain.skill.SkillStatus status, List<Long> employeeIds) {
        if (status == null || employeeIds == null || employeeIds.isEmpty()) {
            return List.of();
        }
        return repository.findByStatusAndEmployeeIdIn(status, employeeIds).stream()
                .map(SkillPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<PendingEmployeeSkillItemResult> findPendingSkills(
            DataScope dataScope,
            Long scopeOrgUnitId,
            Long currentUserId,
            String keyword,
            int page,
            int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = safePage * safeSize;
        String normalizedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        DataScope scope = dataScope != null ? dataScope : DataScope.COMPANY;

        List<PendingEmployeeSkillProjection> contentProjections;
        long totalElements;

        if (scope == DataScope.SELF) {
            contentProjections = repository.findPendingSkillsForSelf(currentUserId, normalizedKeyword, offset, safeSize);
            totalElements = repository.countPendingSkillsForSelf(currentUserId, normalizedKeyword);
        } else if (scope == DataScope.DEPARTMENT) {
            contentProjections = repository.findPendingSkillsForDepartment(scopeOrgUnitId, normalizedKeyword, offset, safeSize);
            totalElements = repository.countPendingSkillsForDepartment(scopeOrgUnitId, normalizedKeyword);
        } else {
            contentProjections = repository.findPendingSkillsForCompany(normalizedKeyword, offset, safeSize);
            totalElements = repository.countPendingSkillsForCompany(normalizedKeyword);
        }

        List<PendingEmployeeSkillItemResult> content = contentProjections.stream()
                .map(this::toItemResult)
                .collect(Collectors.toList());

        return PageResult.of(content, safePage, safeSize, totalElements);
    }

    private PendingEmployeeSkillItemResult toItemResult(PendingEmployeeSkillProjection p) {
        return new PendingEmployeeSkillItemResult(
                p.getId(),
                p.getEmployeeId(),
                p.getEmployeeCode(),
                p.getEmployeeName(),
                p.getOrgUnitId(),
                p.getOrgUnitName(),
                p.getSkillId(),
                p.getSkillCode(),
                p.getSkillName(),
                p.getSkillCategory(),
                p.getProficiencyLevel(),
                p.getYearsOfExperience(),
                p.getStatus(),
                p.getCreatedAt()
        );
    }

    @Override
    public void deleteByEmployeeIdAndSkillId(Long employeeId, Long skillId) {
        if (employeeId != null && skillId != null) {
            repository.deleteByEmployeeIdAndSkillId(employeeId, skillId);
        }
    }
}
