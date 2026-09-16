package com.hrm.employeemanagement.application.service.scenario;

import java.util.Objects;

import com.hrm.employeemanagement.application.dto.scenario.AddScenarioDemandCommand;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioDemandResult;
import com.hrm.employeemanagement.application.dto.scenario.UpdateScenarioDemandCommand;
import com.hrm.employeemanagement.application.port.inbound.scenario.AddScenarioDemandUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.DeleteScenarioDemandUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.UpdateScenarioDemandUseCase;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.DeleteScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.scenario.InvalidScenarioDemandException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioDemandNotFoundException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.scenario.ResourceScenario;
import com.hrm.employeemanagement.domain.scenario.ScenarioDemand;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class ScenarioDemandService implements
        AddScenarioDemandUseCase,
        UpdateScenarioDemandUseCase,
        DeleteScenarioDemandUseCase {

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadResourceScenarioPort loadScenarioPort;
    private final SaveScenarioDemandPort saveDemandPort;
    private final LoadScenarioDemandPort loadDemandPort;
    private final DeleteScenarioDemandPort deleteDemandPort;
    private final SaveAuditLogPort saveAuditLogPort;

    public ScenarioDemandService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadResourceScenarioPort loadScenarioPort,
            SaveScenarioDemandPort saveDemandPort,
            LoadScenarioDemandPort loadDemandPort,
            DeleteScenarioDemandPort deleteDemandPort,
            SaveAuditLogPort saveAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.loadScenarioPort = Objects.requireNonNull(loadScenarioPort, "LoadResourceScenarioPort must not be null");
        this.saveDemandPort = Objects.requireNonNull(saveDemandPort, "SaveScenarioDemandPort must not be null");
        this.loadDemandPort = Objects.requireNonNull(loadDemandPort, "LoadScenarioDemandPort must not be null");
        this.deleteDemandPort = Objects.requireNonNull(deleteDemandPort, "DeleteScenarioDemandPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
    }

    @Override
    public ScenarioDemandResult addDemand(AddScenarioDemandCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        ResourceScenario scenario = loadScenarioPort.findById(command.scenarioId())
                .orElseThrow(() -> new ScenarioNotFoundException(command.scenarioId()));

        // Kiểm tra DataScope
        verifyScenarioInUserScope(currentUser, scenario);

        // Kiểm tra trạng thái kịch bản phải là draft
        scenario.assertModifiable();

        // Kiểm tra phạm vi tuần của nhu cầu phải nằm trong phạm vi kịch bản
        YearWeek demandStart = YearWeek.of(command.startYear(), command.startWeek());
        YearWeek demandEnd = YearWeek.of(command.endYear(), command.endWeek());
        if (demandStart.isBefore(scenario.getStartYearWeek()) || demandEnd.isAfter(scenario.getEndYearWeek())) {
            throw new InvalidScenarioDemandException(
                    "Thời gian nhu cầu (" + demandStart + " đến " + demandEnd +
                    ") phải nằm trong phạm vi kịch bản (" + scenario.getStartYearWeek() + " đến " + scenario.getEndYearWeek() + ")"
            );
        }

        // Tạo nhu cầu mới (tự động validate headcount, week, hours)
        ScenarioDemand demand = ScenarioDemand.create(
                scenario.getId(),
                command.demandName(),
                command.headcount(),
                command.startYear(),
                command.startWeek(),
                command.endYear(),
                command.endWeek(),
                command.hoursPerWeekPerPerson(),
                command.skillRequirement()
        );

        ScenarioDemand saved = saveDemandPort.save(demand);

        // Ghi audit log
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "ADD_SCENARIO_DEMAND",
                "scenario_demands",
                saved.getId(),
                null,
                "scenarioId=" + scenario.getId() + ";demandName=" + saved.getDemandName() +
                        ";headcount=" + saved.getHeadcount() + ";start=" + saved.getStartYear() + "-W" + saved.getStartWeek() +
                        ";end=" + saved.getEndYear() + "-W" + saved.getEndWeek() +
                        ";hours=" + saved.getHoursPerWeekPerPerson()
        ));

        return toDemandResult(saved);
    }

    @Override
    public ScenarioDemandResult updateDemand(UpdateScenarioDemandCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        ResourceScenario scenario = loadScenarioPort.findById(command.scenarioId())
                .orElseThrow(() -> new ScenarioNotFoundException(command.scenarioId()));

        verifyScenarioInUserScope(currentUser, scenario);
        scenario.assertModifiable();

        // Kiểm tra phạm vi tuần của nhu cầu phải nằm trong phạm vi kịch bản
        YearWeek demandStart = YearWeek.of(command.startYear(), command.startWeek());
        YearWeek demandEnd = YearWeek.of(command.endYear(), command.endWeek());
        if (demandStart.isBefore(scenario.getStartYearWeek()) || demandEnd.isAfter(scenario.getEndYearWeek())) {
            throw new InvalidScenarioDemandException(
                    "Thời gian nhu cầu (" + demandStart + " đến " + demandEnd +
                    ") phải nằm trong phạm vi kịch bản (" + scenario.getStartYearWeek() + " đến " + scenario.getEndYearWeek() + ")"
            );
        }

        ScenarioDemand demand = loadDemandPort.findById(command.demandId())
                .orElseThrow(() -> new ScenarioDemandNotFoundException(command.demandId()));

        if (!demand.getScenarioId().equals(scenario.getId())) {
            throw new ScenarioDemandNotFoundException(command.demandId());
        }

        String oldValue = "demandName=" + demand.getDemandName() + ";headcount=" + demand.getHeadcount() +
                ";start=" + demand.getStartYear() + "-W" + demand.getStartWeek() +
                ";end=" + demand.getEndYear() + "-W" + demand.getEndWeek() +
                ";hours=" + demand.getHoursPerWeekPerPerson();

        demand.update(
                command.demandName(),
                command.headcount(),
                command.startYear(),
                command.startWeek(),
                command.endYear(),
                command.endWeek(),
                command.hoursPerWeekPerPerson(),
                command.skillRequirement()
        );

        ScenarioDemand updated = saveDemandPort.save(demand);

        String newValue = "demandName=" + updated.getDemandName() + ";headcount=" + updated.getHeadcount() +
                ";start=" + updated.getStartYear() + "-W" + updated.getStartWeek() +
                ";end=" + updated.getEndYear() + "-W" + updated.getEndWeek() +
                ";hours=" + updated.getHoursPerWeekPerPerson();

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "UPDATE_SCENARIO_DEMAND",
                "scenario_demands",
                updated.getId(),
                oldValue,
                newValue
        ));

        return toDemandResult(updated);
    }

    @Override
    public void deleteDemand(Long scenarioId, Long demandId) {
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        ResourceScenario scenario = loadScenarioPort.findById(scenarioId)
                .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));

        verifyScenarioInUserScope(currentUser, scenario);
        scenario.assertModifiable();

        ScenarioDemand demand = loadDemandPort.findById(demandId)
                .orElseThrow(() -> new ScenarioDemandNotFoundException(demandId));

        if (!demand.getScenarioId().equals(scenario.getId())) {
            throw new ScenarioDemandNotFoundException(demandId);
        }

        String oldValue = "demandName=" + demand.getDemandName() + ";headcount=" + demand.getHeadcount() +
                ";start=" + demand.getStartYear() + "-W" + demand.getStartWeek() +
                ";end=" + demand.getEndYear() + "-W" + demand.getEndWeek();

        deleteDemandPort.deleteById(demandId);

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "DELETE_SCENARIO_DEMAND",
                "scenario_demands",
                demandId,
                oldValue,
                null
        ));
    }

    private void verifyScenarioInUserScope(User currentUser, ResourceScenario scenario) {
        if (isOrgUnitInUserScope(currentUser, scenario.getOrgUnitId())) {
            return;
        }
        throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_MANAGE);
    }

    private boolean isOrgUnitInUserScope(User currentUser, Long targetOrgUnitId) {
        if (currentUser.getDataScope() == DataScope.COMPANY) {
            return true;
        }
        Long userScopeOrgUnitId = currentUser.getScopeOrgUnitId();
        if (userScopeOrgUnitId == null) return false;
        if (userScopeOrgUnitId.equals(targetOrgUnitId)) return true;
        return loadOrgUnitPort.existsInOrgUnitBranch(targetOrgUnitId, userScopeOrgUnitId)
                || loadOrgUnitPort.existsInOrgUnitBranch(userScopeOrgUnitId, targetOrgUnitId);
    }

    private ScenarioDemandResult toDemandResult(ScenarioDemand demand) {
        return new ScenarioDemandResult(
                demand.getId(),
                demand.getScenarioId(),
                demand.getDemandName(),
                demand.getHeadcount(),
                demand.getStartYear(),
                demand.getStartWeek(),
                demand.getEndYear(),
                demand.getEndWeek(),
                demand.getHoursPerWeekPerPerson(),
                demand.getTotalHoursPerWeek(),
                demand.getSkillRequirement(),
                demand.getCreatedAt(),
                demand.getUpdatedAt()
        );
    }
}
