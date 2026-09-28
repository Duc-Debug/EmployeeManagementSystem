package com.hrm.employeemanagement.application.service.skill;

import java.util.Objects;

import com.hrm.employeemanagement.application.dto.skill.DeleteEmployeeSkillCommand;
import com.hrm.employeemanagement.application.port.inbound.skill.DeleteEmployeeSkillUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.skill.EmployeeSkillNotFoundException;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.user.UserId;

public class DeleteEmployeeSkillService implements DeleteEmployeeSkillUseCase {

    private final EmployeeSkillRepository employeeSkillRepository;
    private final SaveAuditLogInNewTransactionPort auditLogRepository;
    private final LoadEmployeePort loadEmployeePort;
    private final AuthorizationService authorizationService;

    public DeleteEmployeeSkillService(
            EmployeeSkillRepository employeeSkillRepository,
            SaveAuditLogInNewTransactionPort auditLogRepository,
            LoadEmployeePort loadEmployeePort,
            AuthorizationService authorizationService
    ) {
        this.employeeSkillRepository = Objects.requireNonNull(employeeSkillRepository, "EmployeeSkillRepository must not be null");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "SaveAuditLogInNewTransactionPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public void execute(DeleteEmployeeSkillCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.EMPLOYEE_SKILL_DECLARE);

        Employee currentEmployee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự của tài khoản đang đăng nhập"));

        if (command.employeeId() != null && !currentEmployee.getIdValue().equals(command.employeeId())) {
            throw new PermissionDeniedException(PermissionCode.EMPLOYEE_SKILL_DECLARE);
        }

        // Kiểm tra xem kỹ năng có tồn tại trong hồ sơ của nhân viên này không trước khi xóa
        EmployeeSkill employeeSkill = employeeSkillRepository
                .findByEmployeeIdAndSkillId(currentEmployee.getIdValue(), command.skillId())
                .orElseThrow(() -> new EmployeeSkillNotFoundException("Kỹ năng này chưa có trong hồ sơ của bạn"));

        employeeSkillRepository.deleteByEmployeeIdAndSkillId(currentEmployee.getIdValue(), command.skillId());

        auditLogRepository.save(AuditLog.createChange(
                currentUserId,
                "DELETE_SKILL",
                "employee_skills",
                employeeSkill.getId(),
                "skillId=" + command.skillId(),
                "deleted"
        ));
    }
}
