package com.hrm.employeemanagement.application.service.skill;

import java.util.Objects;

import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.dto.skill.UpdateEmployeeSkillCommand;
import com.hrm.employeemanagement.application.port.inbound.skill.UpdateEmployeeSkillUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.skill.SkillCatalogRepository;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.skill.EmployeeSkillNotFoundException;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.user.UserId;

public class UpdateEmployeeSkillService implements UpdateEmployeeSkillUseCase {

    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillCatalogRepository skillCatalogRepository;
    private final SaveAuditLogInNewTransactionPort auditLogRepository;
    private final LoadEmployeePort loadEmployeePort;
    private final AuthorizationService authorizationService;

    public UpdateEmployeeSkillService(
            EmployeeSkillRepository employeeSkillRepository,
            SkillCatalogRepository skillCatalogRepository,
            SaveAuditLogInNewTransactionPort auditLogRepository,
            LoadEmployeePort loadEmployeePort,
            AuthorizationService authorizationService
    ) {
        this.employeeSkillRepository = Objects.requireNonNull(employeeSkillRepository, "EmployeeSkillRepository must not be null");
        this.skillCatalogRepository = Objects.requireNonNull(skillCatalogRepository, "SkillCatalogRepository must not be null");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "SaveAuditLogInNewTransactionPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public EmployeeSkillResult execute(UpdateEmployeeSkillCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.EMPLOYEE_SKILL_DECLARE);

        Employee currentEmployee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự của tài khoản đang đăng nhập"));

        if (command.employeeId() != null && !currentEmployee.getIdValue().equals(command.employeeId())) {
            throw new PermissionDeniedException(PermissionCode.EMPLOYEE_SKILL_DECLARE);
        }

        EmployeeSkill employeeSkill = employeeSkillRepository
                .findByEmployeeIdAndSkillId(currentEmployee.getIdValue(), command.skillId())
                .orElseThrow(() -> new EmployeeSkillNotFoundException("Kỹ năng này chưa có trong hồ sơ của bạn"));

        String oldValue = "proficiency=" + employeeSkill.getProficiencyLevelValue()
                + ";years=" + employeeSkill.getYearsOfExperience()
                + ";status=" + employeeSkill.getStatus();

        // Cập nhật thông tin trong Domain (Domain Rule: Tự động chuyển về PENDING chờ duyệt lại nếu có thay đổi)
        employeeSkill.updateProficiency(command.proficiencyLevel(), command.yearsOfExperience());
        EmployeeSkill savedSkill = employeeSkillRepository.save(employeeSkill);

        Skill skill = skillCatalogRepository.findById(savedSkill.getSkillId()).orElse(null);

        String newValue = "proficiency=" + savedSkill.getProficiencyLevelValue()
                + ";years=" + savedSkill.getYearsOfExperience()
                + ";status=" + savedSkill.getStatus();

        auditLogRepository.save(AuditLog.createChange(
                currentUserId,
                "UPDATE_SKILL",
                "employee_skills",
                savedSkill.getId(),
                oldValue,
                newValue
        ));

        return EmployeeSkillResult.fromDomain(savedSkill, skill);
    }
}
