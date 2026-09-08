package com.hrm.employeemanagement.application.service.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.hrm.employeemanagement.application.dto.skill.ApproveEmployeeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.dto.skill.PendingEmployeeSkillItemResult;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.port.inbound.skill.ApproveEmployeeSkillUseCase;
import com.hrm.employeemanagement.application.port.inbound.skill.GetPendingEmployeeSkillsUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.skill.SkillCatalogRepository;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.skill.EmployeeSkillNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.ProficiencyLevel;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class ApproveEmployeeSkillService implements ApproveEmployeeSkillUseCase, GetPendingEmployeeSkillsUseCase {

    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillCatalogRepository skillCatalogRepository;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final SaveAuditLogInNewTransactionPort saveAuditLogPort;
    private final AuthorizationService authorizationService;

    public ApproveEmployeeSkillService(
            EmployeeSkillRepository employeeSkillRepository,
            SkillCatalogRepository skillCatalogRepository,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort,
            AuthorizationService authorizationService
    ) {
        this.employeeSkillRepository = Objects.requireNonNull(employeeSkillRepository, "EmployeeSkillRepository must not be null");
        this.skillCatalogRepository = Objects.requireNonNull(skillCatalogRepository, "SkillCatalogRepository must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public EmployeeSkillResult execute(ApproveEmployeeSkillCommand command) {
        if (command == null || command.employeeSkillId() == null) {
            throw new IllegalArgumentException("ID kỹ năng nhân sự không được để trống");
        }

        // 1. Kiểm tra quyền phê duyệt (TC-03: EMPLOYEE_SKILL_APPROVE dành cho VT-03)
        Long currentUserId = authorizationService.require(PermissionCode.EMPLOYEE_SKILL_APPROVE);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        // 2. Tìm bản ghi kỹ năng của nhân viên
        EmployeeSkill employeeSkill = employeeSkillRepository.findById(command.employeeSkillId())
                .orElseThrow(() -> new EmployeeSkillNotFoundException("Không tìm thấy bản ghi kỹ năng nhân sự với ID: " + command.employeeSkillId()));

        // 3. Tìm thông tin nhân viên sở hữu kỹ năng
        Employee employee = loadEmployeePort.findById(new EmployeeId(employeeSkill.getEmployeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy thông tin nhân sự với ID: " + employeeSkill.getEmployeeId()));

        // 4. Kiểm tra phạm vi dữ liệu Data Scope (TC-03)
        requireEmployeeInScope(currentUser, employee, PermissionCode.EMPLOYEE_SKILL_APPROVE);

        // 5. Thực hiện nghiệp vụ duyệt hoặc điều chỉnh
        int oldLevel = employeeSkill.getProficiencyLevelValue();
        String actionName;

        if (command.adjustedProficiencyLevel() != null && command.adjustedProficiencyLevel() != oldLevel) {
            // TC-02: Điều chỉnh mức thành thạo kèm ghi chú
            employeeSkill.adjustAndApprove(
                    currentUserId,
                    ProficiencyLevel.fromValue(command.adjustedProficiencyLevel()),
                    command.reviewNotes()
            );
            actionName = "ADJUST_SKILL_PROFICIENCY";
        } else {
            // TC-01: Xác nhận giữ nguyên mức tự khai kèm ghi chú
            employeeSkill.approve(currentUserId, command.reviewNotes());
            actionName = "APPROVE_SKILL";
        }

        EmployeeSkill saved = employeeSkillRepository.save(employeeSkill);

        // 6. Ghi nhật ký kiểm toán (TC-04)
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                actionName,
                "employee_skills",
                saved.getId(),
                String.valueOf(oldLevel),
                String.valueOf(saved.getProficiencyLevelValue())
        ));

        Skill skill = skillCatalogRepository.findById(saved.getSkillId()).orElse(null);
        return EmployeeSkillResult.fromDomain(saved, skill);
    }

    @Override
    public List<PendingEmployeeSkillItemResult> execute(String keyword) {
        return execute(keyword, 0, 1000).getContent();
    }

    @Override
    public PageResult<PendingEmployeeSkillItemResult> execute(String keyword, int page, int size) {
        // 1. Kiểm tra quyền truy cập của Quản lý nguồn lực (TC-03)
        Long currentUserId = authorizationService.require(PermissionCode.EMPLOYEE_SKILL_APPROVE);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        // 2. Đưa toàn bộ việc lọc Data Scope, tìm kiếm từ khóa và phân trang xuống Persistence Layer
        return employeeSkillRepository.findPendingSkills(
                currentUser.getDataScope(),
                currentUser.getScopeOrgUnitId(),
                currentUser.getIdValue(),
                keyword,
                page,
                size
        );
    }

    private void requireEmployeeInScope(User currentUser, Employee employee, PermissionCode permission) {
        if (!isEmployeeInScope(currentUser, employee)) {
            throw new PermissionDeniedException(permission);
        }
    }

    private boolean isEmployeeInScope(User currentUser, Employee employee) {
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> currentUser.getIdValue() != null && currentUser.getIdValue().equals(employee.getUserIdValue());
            case ORGANIZATION_BRANCH -> employee.getOrgUnitId() != null
                    && currentUser.getScopeOrgUnitId() != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(
                            employee.getOrgUnitId(), currentUser.getScopeOrgUnitId());
        };
    }
}
