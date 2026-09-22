package com.hrm.employeemanagement.application.service.employee;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.hrm.employeemanagement.application.dto.employee.DeclareOutsourcedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.employee.OutsourcedEmployeeResult;
import com.hrm.employeemanagement.application.port.inbound.employee.DeclareOutsourcedEmployeeUseCase;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.skill.SkillCatalogRepository;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.InvalidEmployeeDataException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.ProficiencyLevel;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class DeclareOutsourcedEmployeeService implements DeclareOutsourcedEmployeeUseCase {

    private final LoadEmployeePort loadEmployeePort;
    private final SaveEmployeePort saveEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadUserPort loadUserPort;
    private final AuthorizationService authorizationService;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SkillCatalogRepository skillCatalogRepository;
    private final EmployeeSkillRepository employeeSkillRepository;

    public DeclareOutsourcedEmployeeService(
            LoadEmployeePort loadEmployeePort,
            SaveEmployeePort saveEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadUserPort loadUserPort,
            AuthorizationService authorizationService,
            SaveAuditLogPort saveAuditLogPort,
            SkillCatalogRepository skillCatalogRepository,
            EmployeeSkillRepository employeeSkillRepository
    ) {
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.saveEmployeePort = Objects.requireNonNull(saveEmployeePort, "SaveEmployeePort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.skillCatalogRepository = skillCatalogRepository;
        this.employeeSkillRepository = employeeSkillRepository;
    }

    @Override
    public OutsourcedEmployeeResult execute(DeclareOutsourcedEmployeeCommand command) {
        // 1. Phân quyền: Yêu cầu quyền EMPLOYEE_UPDATE (VT-05)
        Long currentUserId = authorizationService.require(PermissionCode.EMPLOYEE_UPDATE);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        // 2. Kiểm tra Đơn vị tổ chức tiếp nhận (OrgUnit)
        if (command.orgUnitId() == null) {
            throw new InvalidEmployeeDataException("Đơn vị phòng ban tiếp nhận không được để trống");
        }
        OrgUnit orgUnit = loadOrgUnitPort.findById(new OrgUnitId(command.orgUnitId()))
                .orElseThrow(() -> new OrgUnitNotFoundException("Không tìm thấy đơn vị phòng ban với ID: " + command.orgUnitId()));
        if (orgUnit.getStatus() != OrgUnitStatus.ACTIVE) {
            throw new InvalidEmployeeDataException("Đơn vị phòng ban đã bị vô hiệu hóa hoặc không hoạt động");
        }

        // Kiểm tra DataScope của user
        if (currentUser.getDataScope() == DataScope.ORGANIZATION_BRANCH) {
            Long scopeOrgUnitId = currentUser.getScopeOrgUnitId();
            if (scopeOrgUnitId == null || !loadOrgUnitPort.existsInOrgUnitBranch(command.orgUnitId(), scopeOrgUnitId)) {
                throw new PermissionDeniedException(PermissionCode.EMPLOYEE_UPDATE);
            }
        }

        // 3. Kiểm tra mã nhân viên (Employee Code)
        String employeeCode = command.employeeCode();
        if (employeeCode != null && !employeeCode.isBlank()) {
            employeeCode = employeeCode.trim();
            if (loadEmployeePort.existsByEmployeeCode(employeeCode)) {
                throw new InvalidEmployeeDataException("Mã nhân viên '" + employeeCode + "' đã tồn tại trong hệ thống");
            }
        } else {
            employeeCode = generateUniqueEmployeeCode();
        }

        // 4. Kiểm tra các trường bắt buộc khác
        if (command.fullName() == null || command.fullName().isBlank()) {
            throw new InvalidEmployeeDataException("Họ tên nhân sự không được để trống");
        }
        if (command.providerName() == null || command.providerName().isBlank()) {
            throw new InvalidEmployeeDataException("Đơn vị cung cấp nhân sự thuê ngoài không được để trống");
        }
        if (command.startDate() == null || command.contractEndDate() == null) {
            throw new InvalidEmployeeDataException("Thời hạn hợp đồng thuê (ngày bắt đầu và ngày kết thúc) không được để trống");
        }
        // BR-02: Ngày kết thúc không được sớm hơn ngày bắt đầu
        if (command.contractEndDate().isBefore(command.startDate())) {
            throw new InvalidEmployeeDataException("Ngày kết thúc hợp đồng thuê không được sớm hơn ngày bắt đầu");
        }

        int standardHours = (command.standardHoursPerWeek() != null) ? command.standardHoursPerWeek() : 40;

        // 5. Khởi tạo Domain Entity
        Employee outsourcedEmployee = Employee.createOutsourced(
                command.orgUnitId(),
                employeeCode,
                command.fullName().trim(),
                command.providerName().trim(),
                command.professionalRole() != null ? command.professionalRole().trim() : null,
                command.startDate(),
                command.contractEndDate(),
                standardHours
        );

        // 6. Lưu vào cơ sở dữ liệu
        Employee saved = saveEmployeePort.save(outsourcedEmployee);

        // 7. Gán kỹ năng nếu có
        List<String> skillNames = new ArrayList<>();
        if (command.skillIds() != null && !command.skillIds().isEmpty()
                && skillCatalogRepository != null && employeeSkillRepository != null) {
            List<Skill> skills = skillCatalogRepository.findAllByIdIn(command.skillIds());
            LocalDateTime now = LocalDateTime.now();
            for (Skill skill : skills) {
                EmployeeSkill employeeSkill = new EmployeeSkill(
                        null,
                        saved.getIdValue(),
                        skill.getId(),
                        ProficiencyLevel.ADVANCED,
                        BigDecimal.ONE,
                        SkillStatus.APPROVED,
                        currentUserId,
                        now,
                        null,
                        "Khai báo cùng hồ sơ nhân sự thuê ngoài",
                        now,
                        now,
                        0L
                );
                employeeSkillRepository.save(employeeSkill);
                skillNames.add(skill.getName());
            }
        }

        // 8. Ghi nhận Business Audit Log (BR-05 / TC-04)
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "DECLARE_OUTSOURCED_EMPLOYEE",
                "employees",
                saved.getIdValue(),
                null,
                "fullName=" + saved.getFullName()
                        + ";providerName=" + saved.getProviderName()
                        + ";contractStart=" + saved.getStartDate()
                        + ";contractEnd=" + saved.getContractEndDate()
                        + ";standardHours=" + saved.getStandardHoursPerWeek()
                        + ";orgUnitId=" + saved.getOrgUnitId()
        ));

        return OutsourcedEmployeeResult.fromDomain(saved, orgUnit.getUnitName(), skillNames);
    }

    private String generateUniqueEmployeeCode() {
        long timestamp = System.currentTimeMillis() % 1000000;
        String code = "EXT-" + timestamp;
        int attempt = 1;
        while (loadEmployeePort.existsByEmployeeCode(code)) {
            code = "EXT-" + (timestamp + attempt);
            attempt++;
        }
        return code;
    }
}
