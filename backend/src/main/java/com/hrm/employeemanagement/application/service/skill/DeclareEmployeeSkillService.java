package com.hrm.employeemanagement.application.service.skill;

import java.util.Objects;

import com.hrm.employeemanagement.application.dto.skill.DeclareEmployeeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.port.inbound.skill.DeclareEmployeeSkillUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.skill.SkillCatalogRepository;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.skill.DuplicateEmployeeSkillException;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.Skill;

public class DeclareEmployeeSkillService implements DeclareEmployeeSkillUseCase {

    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillCatalogRepository skillCatalogRepository;
    private final SaveAuditLogInNewTransactionPort auditLogRepository;
    private final AuthorizationService authorizationService;

    public DeclareEmployeeSkillService(
            EmployeeSkillRepository employeeSkillRepository,
            SkillCatalogRepository skillCatalogRepository,
            SaveAuditLogInNewTransactionPort auditLogRepository,
            AuthorizationService authorizationService
    ) {
        this.employeeSkillRepository = Objects.requireNonNull(employeeSkillRepository, "EmployeeSkillRepository must not be null");
        this.skillCatalogRepository = Objects.requireNonNull(skillCatalogRepository, "SkillCatalogRepository must not be null");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public EmployeeSkillResult execute(DeclareEmployeeSkillCommand command) {
        // 1. Kiểm tra quyền khai báo kỹ năng (Yêu cầu EMPLOYEE_SKILL_DECLARE)
        Long currentUserId = authorizationService.require(PermissionCode.EMPLOYEE_SKILL_DECLARE);

        // 2. Kiểm tra kỹ năng có tồn tại trong danh mục không
        Skill skill = skillCatalogRepository.findById(command.skillId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kỹ năng trong danh mục với ID: " + command.skillId()));

        // 3. Kiểm tra dữ liệu trùng lặp (TC-02)
        if (employeeSkillRepository.existsByEmployeeIdAndSkillId(command.employeeId(), command.skillId())) {
            throw new DuplicateEmployeeSkillException("Kỹ năng '" + skill.getName() + "' đã có trong hồ sơ. Vui lòng chọn cập nhật mức thành thạo thay vì thêm mới.");
        }

        // 4. Khởi tạo bản ghi kỹ năng mới ở trạng thái PENDING (TC-01)
        EmployeeSkill newSkill = EmployeeSkill.declare(
                command.employeeId(),
                command.skillId(),
                command.proficiencyLevel(),
                command.yearsOfExperience()
        );

        EmployeeSkill savedSkill = employeeSkillRepository.save(newSkill);

        // 5. Ghi lại nhật ký Audit Log (TC-04)
        auditLogRepository.save(AuditLog.createChange(
                currentUserId,
                "DECLARE_SKILL",
                "employee_skills",
                savedSkill.getId(),
                null,
                "Khai báo kỹ năng: " + skill.getName() + " - Mức thành thạo: " + command.proficiencyLevel() + "/5"
        ));

        return EmployeeSkillResult.fromDomain(savedSkill, skill);
    }
}
