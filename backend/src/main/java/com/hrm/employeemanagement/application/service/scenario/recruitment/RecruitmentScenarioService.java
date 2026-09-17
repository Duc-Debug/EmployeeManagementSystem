package com.hrm.employeemanagement.application.service.scenario.recruitment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.scenario.recruitment.AddSimulatedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.RecruitmentScenarioEvaluationResult;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.RemoveSimulatedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.SimulatedEmployeeResult;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.UpdateSimulatedEmployeeCommand;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.AddSimulatedEmployeeUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.GetRecruitmentEvaluationUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.GetScenarioSimulatedEmployeesUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.RemoveSimulatedEmployeeUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.RerunRecruitmentScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.UpdateSimulatedEmployeeUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.DeleteSimulatedEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.LoadScenarioShortfallPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.LoadSimulatedEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.LoadSimulationScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.SaveSimulatedEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.skill.LoadSkillPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.scenario.recruitment.InvalidSimulatedEmployeeException;
import com.hrm.employeemanagement.domain.exception.scenario.recruitment.ScenarioNotFoundException;
import com.hrm.employeemanagement.domain.exception.scenario.recruitment.SimulatedEmployeeNotFoundException;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.domain.scenario.recruitment.RecruitmentScenarioEvaluation;
import com.hrm.employeemanagement.domain.scenario.recruitment.RecruitmentScenarioPolicy;
import com.hrm.employeemanagement.domain.scenario.recruitment.RoleShortfallDemand;
import com.hrm.employeemanagement.domain.scenario.recruitment.ScenarioSimulatedEmployee;
import com.hrm.employeemanagement.domain.scenario.recruitment.SimulatedEmployeeId;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillId;

public class RecruitmentScenarioService implements
        AddSimulatedEmployeeUseCase,
        UpdateSimulatedEmployeeUseCase,
        RemoveSimulatedEmployeeUseCase,
        GetScenarioSimulatedEmployeesUseCase,
        RerunRecruitmentScenarioUseCase,
        GetRecruitmentEvaluationUseCase {

    private final AuthorizationService authorizationService;
    private final LoadSimulationScenarioPort loadScenarioPort;
    private final LoadScenarioShortfallPort loadShortfallPort;
    private final SaveSimulatedEmployeePort saveEmployeePort;
    private final LoadSimulatedEmployeePort loadEmployeePort;
    private final DeleteSimulatedEmployeePort deleteEmployeePort;
    private final LoadProjectRolePort loadRolePort;
    private final LoadSkillPort loadSkillPort;
    private final SaveAuditLogInNewTransactionPort saveAuditLogPort;

    public RecruitmentScenarioService(
            AuthorizationService authorizationService,
            LoadSimulationScenarioPort loadScenarioPort,
            LoadScenarioShortfallPort loadShortfallPort,
            SaveSimulatedEmployeePort saveEmployeePort,
            LoadSimulatedEmployeePort loadEmployeePort,
            DeleteSimulatedEmployeePort deleteEmployeePort,
            LoadProjectRolePort loadRolePort,
            LoadSkillPort loadSkillPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadScenarioPort = Objects.requireNonNull(loadScenarioPort, "LoadSimulationScenarioPort must not be null");
        this.loadShortfallPort = Objects.requireNonNull(loadShortfallPort, "LoadScenarioShortfallPort must not be null");
        this.saveEmployeePort = Objects.requireNonNull(saveEmployeePort, "SaveSimulatedEmployeePort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadSimulatedEmployeePort must not be null");
        this.deleteEmployeePort = Objects.requireNonNull(deleteEmployeePort, "DeleteSimulatedEmployeePort must not be null");
        this.loadRolePort = Objects.requireNonNull(loadRolePort, "LoadProjectRolePort must not be null");
        this.loadSkillPort = Objects.requireNonNull(loadSkillPort, "LoadSkillPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
    }

    private Long requireManagePermission(Long scenarioId) {
        try {
            return authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE);
        } catch (PermissionDeniedException ex) {
            saveAuditLogPort.save(AuditLog.create(
                    null,
                    "ACCESS_DENIED",
                    "SCENARIO_SIMULATED_EMPLOYEE",
                    scenarioId
            ));
            throw ex;
        }
    }

    private Long requireReadPermission(Long scenarioId) {
        try {
            return authorizationService.requireAny(
                    PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_READ,
                    PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE
            );
        } catch (PermissionDeniedException ex) {
            saveAuditLogPort.save(AuditLog.create(
                    null,
                    "ACCESS_DENIED",
                    "SCENARIO_SIMULATED_EMPLOYEE",
                    scenarioId
            ));
            throw ex;
        }
    }

    private void validateScenarioExists(Long scenarioId) {
        if (!loadScenarioPort.existsById(scenarioId)) {
            throw new ScenarioNotFoundException(scenarioId);
        }
    }

    private void validateScenarioIsDraft(Long scenarioId) {
        LoadSimulationScenarioPort.SimulationScenarioInfo scenario = loadScenarioPort.findById(scenarioId)
                .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));
        if (scenario.status() != null && !"DRAFT".equalsIgnoreCase(scenario.status())) {
            throw new InvalidSimulatedEmployeeException(
                    String.format("Không thể thao tác nhân sự giả định khi kịch bản không ở trạng thái Bản nháp (DRAFT). Trạng thái hiện tại: %s", scenario.status())
            );
        }
    }

    private void validateRoleAndSkill(Long projectRoleId, Long primarySkillId) {
        if (projectRoleId == null) {
            throw new InvalidSimulatedEmployeeException("Vai trò dự án không được để trống");
        }
        if (loadRolePort.findById(new ProjectRoleId(projectRoleId)).isEmpty()) {
            throw new InvalidSimulatedEmployeeException("Vai trò dự án không tồn tại: " + projectRoleId);
        }
        if (primarySkillId != null && loadSkillPort.findById(new SkillId(primarySkillId)).isEmpty()) {
            throw new InvalidSimulatedEmployeeException("Kỹ năng chính không tồn tại: " + primarySkillId);
        }
    }

    @Override
    public RecruitmentScenarioEvaluationResult addSimulatedEmployee(AddSimulatedEmployeeCommand command) {
        Objects.requireNonNull(command, "AddSimulatedEmployeeCommand must not be null");
        Long currentUserId = requireManagePermission(command.scenarioId());
        validateScenarioExists(command.scenarioId());
        validateScenarioIsDraft(command.scenarioId());
        validateRoleAndSkill(command.projectRoleId(), command.primarySkillId());

        ScenarioSimulatedEmployee candidate = ScenarioSimulatedEmployee.create(
                command.scenarioId(),
                command.candidateName(),
                command.projectRoleId(),
                command.primarySkillId(),
                command.standardHoursPerWeek(),
                command.weeksCount(),
                command.notes(),
                currentUserId
        );

        ScenarioSimulatedEmployee saved = saveEmployeePort.save(candidate);

        // [TC-04] Ghi nhận lịch sử kiểm toán thao tác thành công
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "ADD_SIMULATED_EMPLOYEE",
                "scenario_simulated_employees",
                saved.getIdValue(),
                null,
                String.format("Thêm nhân sự giả định '%s' vai trò ID %d cho kịch bản %d (%s h/tuần, %d tuần)",
                        saved.getCandidateName(), saved.getProjectRoleId(), saved.getScenarioId(),
                        saved.getStandardHoursPerWeek(), saved.getWeeksCount())
        ));

        // Chạy lại kịch bản tuyển dụng để cập nhật đánh giá mới nhất
        return evaluateScenarioInternal(command.scenarioId());
    }

    @Override
    public RecruitmentScenarioEvaluationResult updateSimulatedEmployee(UpdateSimulatedEmployeeCommand command) {
        Objects.requireNonNull(command, "UpdateSimulatedEmployeeCommand must not be null");
        Long currentUserId = requireManagePermission(command.scenarioId());
        validateScenarioExists(command.scenarioId());
        validateScenarioIsDraft(command.scenarioId());

        ScenarioSimulatedEmployee employee = loadEmployeePort.findById(new SimulatedEmployeeId(command.employeeId()))
                .orElseThrow(() -> new SimulatedEmployeeNotFoundException(command.employeeId()));

        if (!Objects.equals(employee.getScenarioId(), command.scenarioId())) {
            throw new SimulatedEmployeeNotFoundException("Nhân sự giả định không thuộc kịch bản này");
        }

        validateRoleAndSkill(command.projectRoleId(), command.primarySkillId());

        String oldInfo = String.format("name=%s, roleId=%d, skillId=%s, hours=%s, weeks=%d",
                employee.getCandidateName(), employee.getProjectRoleId(), employee.getPrimarySkillId(),
                employee.getStandardHoursPerWeek(), employee.getWeeksCount());

        employee.updateDetails(
                command.candidateName(),
                command.projectRoleId(),
                command.primarySkillId(),
                command.standardHoursPerWeek(),
                command.weeksCount(),
                command.notes()
        );

        ScenarioSimulatedEmployee updated = saveEmployeePort.save(employee);

        String newInfo = String.format("name=%s, roleId=%d, skillId=%s, hours=%s, weeks=%d",
                updated.getCandidateName(), updated.getProjectRoleId(), updated.getPrimarySkillId(),
                updated.getStandardHoursPerWeek(), updated.getWeeksCount());

        // [TC-04] Ghi nhận lịch sử kiểm toán thao tác cập nhật
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "UPDATE_SIMULATED_EMPLOYEE",
                "scenario_simulated_employees",
                updated.getIdValue(),
                oldInfo,
                newInfo
        ));

        return evaluateScenarioInternal(command.scenarioId());
    }

    @Override
    public RecruitmentScenarioEvaluationResult removeSimulatedEmployee(RemoveSimulatedEmployeeCommand command) {
        Objects.requireNonNull(command, "RemoveSimulatedEmployeeCommand must not be null");
        Long currentUserId = requireManagePermission(command.scenarioId());
        validateScenarioExists(command.scenarioId());
        validateScenarioIsDraft(command.scenarioId());

        ScenarioSimulatedEmployee employee = loadEmployeePort.findById(new SimulatedEmployeeId(command.employeeId()))
                .orElseThrow(() -> new SimulatedEmployeeNotFoundException(command.employeeId()));

        if (!Objects.equals(employee.getScenarioId(), command.scenarioId())) {
            throw new SimulatedEmployeeNotFoundException("Nhân sự giả định không thuộc kịch bản này");
        }

        deleteEmployeePort.deleteById(employee.getId());

        // [TC-04] Ghi nhận lịch sử kiểm toán
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "REMOVE_SIMULATED_EMPLOYEE",
                "scenario_simulated_employees",
                command.employeeId(),
                employee.getCandidateName(),
                null
        ));

        return evaluateScenarioInternal(command.scenarioId());
    }

    @Override
    public List<SimulatedEmployeeResult> getSimulatedEmployees(Long scenarioId) {
        requireReadPermission(scenarioId);
        validateScenarioExists(scenarioId);

        List<ScenarioSimulatedEmployee> employees = loadEmployeePort.findByScenarioId(scenarioId);
        return employees.stream().map(emp -> {
            String roleCode = null;
            String roleName = null;
            if (emp.getProjectRoleId() != null) {
                Optional<ProjectRole> roleOpt = loadRolePort.findById(new ProjectRoleId(emp.getProjectRoleId()));
                if (roleOpt.isPresent()) {
                    roleCode = roleOpt.get().getCode();
                    roleName = roleOpt.get().getName();
                }
            }

            String skillName = null;
            if (emp.getPrimarySkillId() != null) {
                Optional<Skill> skillOpt = loadSkillPort.findById(new SkillId(emp.getPrimarySkillId()));
                if (skillOpt.isPresent()) {
                    skillName = skillOpt.get().getName();
                }
            }

            return new SimulatedEmployeeResult(
                    emp.getIdValue(),
                    emp.getScenarioId(),
                    emp.getCandidateName(),
                    emp.getProjectRoleId(),
                    roleCode,
                    roleName,
                    emp.getPrimarySkillId(),
                    skillName,
                    emp.getStandardHoursPerWeek(),
                    emp.getWeeksCount(),
                    emp.calculateSimulatedCapacityHours(),
                    emp.getNotes(),
                    emp.getCreatedBy(),
                    emp.getCreatedAt()
            );
        }).collect(Collectors.toList());
    }

    @Override
    public RecruitmentScenarioEvaluationResult getRecruitmentEvaluation(Long scenarioId) {
        requireReadPermission(scenarioId);
        validateScenarioExists(scenarioId);
        return evaluateScenarioInternal(scenarioId);
    }

    @Override
    public RecruitmentScenarioEvaluationResult rerunRecruitmentScenario(Long scenarioId) {
        Long currentUserId = requireManagePermission(scenarioId);
        validateScenarioExists(scenarioId);

        RecruitmentScenarioEvaluationResult result = evaluateScenarioInternal(scenarioId);

        // [TC-04] Ghi nhận lịch sử thao tác chạy lại kịch bản
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "RERUN_RECRUITMENT_SCENARIO",
                "simulation_scenarios",
                scenarioId,
                null,
                String.format("Chạy lại kịch bản tuyển dụng %d: tổng giờ thiếu ban đầu=%s, giờ bù=%s, giờ còn thiếu=%s, còn vỡ KH=%b",
                        scenarioId, result.totalOriginalShortfallHours(), result.totalSimulatedCapacityHours(),
                        result.totalRemainingShortfallHours(), result.isPlanBroken())
        ));

        return result;
    }

    private RecruitmentScenarioEvaluationResult evaluateScenarioInternal(Long scenarioId) {
        List<RoleShortfallDemand> demands = loadShortfallPort.loadShortfallDemands(scenarioId);
        List<ScenarioSimulatedEmployee> employees = loadEmployeePort.findByScenarioId(scenarioId);

        RecruitmentScenarioEvaluation evaluation = RecruitmentScenarioPolicy.evaluate(scenarioId, demands, employees);

        List<RecruitmentScenarioEvaluationResult.RoleEvaluationItemResult> roleResults = evaluation.roleEvaluations().stream()
                .map(r -> new RecruitmentScenarioEvaluationResult.RoleEvaluationItemResult(
                        r.roleId(),
                        r.roleCode(),
                        r.roleName(),
                        r.originalShortfallHours(),
                        r.simulatedCapacityHours(),
                        r.remainingShortfallHours(),
                        r.simulatedEmployeesCount(),
                        r.suggestedRecruitsNeeded()
                ))
                .collect(Collectors.toList());

        int totalSuggestedRecruits = roleResults.stream()
                .mapToInt(RecruitmentScenarioEvaluationResult.RoleEvaluationItemResult::suggestedRecruitsNeeded)
                .sum();

        return new RecruitmentScenarioEvaluationResult(
                scenarioId,
                evaluation.totalOriginalShortfallHours(),
                evaluation.totalSimulatedCapacityHours(),
                evaluation.totalRemainingShortfallHours(),
                evaluation.isPlanBroken(),
                evaluation.overloadedRoleCount(),
                employees.size(),
                totalSuggestedRecruits,
                roleResults
        );
    }
}
