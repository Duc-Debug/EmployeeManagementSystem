package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario;

import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.hrm.employeemanagement.application.dto.scenario.*;
import com.hrm.employeemanagement.application.port.inbound.scenario.*;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto.AddScenarioDemandRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto.CreateScenarioRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto.UpdateScenarioDemandRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/resource-scenarios")
@Validated
public class ResourceScenarioController {

    private final CreateSimulationScenarioUseCase createScenarioUseCase;
    private final GetSimulationScenarioUseCase getScenarioUseCase;
    private final ListSimulationScenariosUseCase listScenariosUseCase;
    private final AddScenarioDemandUseCase addDemandUseCase;
    private final UpdateScenarioDemandUseCase updateDemandUseCase;
    private final DeleteScenarioDemandUseCase deleteDemandUseCase;
    private final GetScenarioSimulationResultUseCase simulationResultUseCase;

    public ResourceScenarioController(
            CreateSimulationScenarioUseCase createScenarioUseCase,
            GetSimulationScenarioUseCase getScenarioUseCase,
            ListSimulationScenariosUseCase listScenariosUseCase,
            AddScenarioDemandUseCase addDemandUseCase,
            UpdateScenarioDemandUseCase updateDemandUseCase,
            DeleteScenarioDemandUseCase deleteDemandUseCase,
            GetScenarioSimulationResultUseCase simulationResultUseCase
    ) {
        this.createScenarioUseCase = Objects.requireNonNull(createScenarioUseCase, "CreateSimulationScenarioUseCase must not be null");
        this.getScenarioUseCase = Objects.requireNonNull(getScenarioUseCase, "GetSimulationScenarioUseCase must not be null");
        this.listScenariosUseCase = Objects.requireNonNull(listScenariosUseCase, "ListSimulationScenariosUseCase must not be null");
        this.addDemandUseCase = Objects.requireNonNull(addDemandUseCase, "AddScenarioDemandUseCase must not be null");
        this.updateDemandUseCase = Objects.requireNonNull(updateDemandUseCase, "UpdateScenarioDemandUseCase must not be null");
        this.deleteDemandUseCase = Objects.requireNonNull(deleteDemandUseCase, "DeleteScenarioDemandUseCase must not be null");
        this.simulationResultUseCase = Objects.requireNonNull(simulationResultUseCase, "GetScenarioSimulationResultUseCase must not be null");
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RESOURCE_SCENARIO_MANAGE')")
    public ResponseEntity<ApiResponse<ScenarioResult>> createScenario(
            @Valid @RequestBody CreateScenarioRequest request
    ) {
        CreateScenarioCommand command = new CreateScenarioCommand(
                request.code(),
                request.name(),
                request.description(),
                request.orgUnitId(),
                request.fromYear(),
                request.fromWeek(),
                request.durationWeeks()
        );
        ScenarioResult result = createScenarioUseCase.createScenario(command);
        return ResponseEntity.ok(ApiResponse.success("Tạo kịch bản mô phỏng thành công", result));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RESOURCE_SCENARIO_READ')")
    public ResponseEntity<ApiResponse<List<ScenarioResult>>> listScenarios(
            @RequestParam(required = false) Long orgUnitId
    ) {
        List<ScenarioResult> results = listScenariosUseCase.listScenarios(orgUnitId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách kịch bản mô phỏng thành công", results));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RESOURCE_SCENARIO_READ')")
    public ResponseEntity<ApiResponse<ScenarioDetailResult>> getScenarioById(
            @PathVariable("id") Long id
    ) {
        ScenarioDetailResult result = getScenarioUseCase.getScenarioById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin kịch bản mô phỏng thành công", result));
    }

    @PostMapping("/{id}/demands")
    @PreAuthorize("hasAuthority('RESOURCE_SCENARIO_MANAGE')")
    public ResponseEntity<ApiResponse<ScenarioDemandResult>> addDemand(
            @PathVariable("id") Long id,
            @Valid @RequestBody AddScenarioDemandRequest request
    ) {
        AddScenarioDemandCommand command = new AddScenarioDemandCommand(
                id,
                request.demandName(),
                request.headcount(),
                request.startYear(),
                request.startWeek(),
                request.endYear(),
                request.endWeek(),
                request.hoursPerWeekPerPerson(),
                request.skillRequirement()
        );
        ScenarioDemandResult result = addDemandUseCase.addDemand(command);
        return ResponseEntity.ok(ApiResponse.success("Thêm nhu cầu nhân sự giả định thành công", result));
    }

    @PutMapping("/{id}/demands/{demandId}")
    @PreAuthorize("hasAuthority('RESOURCE_SCENARIO_MANAGE')")
    public ResponseEntity<ApiResponse<ScenarioDemandResult>> updateDemand(
            @PathVariable("id") Long id,
            @PathVariable("demandId") Long demandId,
            @Valid @RequestBody UpdateScenarioDemandRequest request
    ) {
        UpdateScenarioDemandCommand command = new UpdateScenarioDemandCommand(
                id,
                demandId,
                request.demandName(),
                request.headcount(),
                request.startYear(),
                request.startWeek(),
                request.endYear(),
                request.endWeek(),
                request.hoursPerWeekPerPerson(),
                request.skillRequirement()
        );
        ScenarioDemandResult result = updateDemandUseCase.updateDemand(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật nhu cầu nhân sự giả định thành công", result));
    }

    @DeleteMapping("/{id}/demands/{demandId}")
    @PreAuthorize("hasAuthority('RESOURCE_SCENARIO_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteDemand(
            @PathVariable("id") Long id,
            @PathVariable("demandId") Long demandId
    ) {
        deleteDemandUseCase.deleteDemand(id, demandId);
        return ResponseEntity.ok(ApiResponse.success("Xóa nhu cầu nhân sự giả định thành công", null));
    }

    @GetMapping("/{id}/simulation")
    @PreAuthorize("hasAuthority('RESOURCE_SCENARIO_READ')")
    public ResponseEntity<ApiResponse<ScenarioSimulationResult>> getSimulationResult(
            @PathVariable("id") Long id
    ) {
        ScenarioSimulationResult result = simulationResultUseCase.getSimulationResult(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy kết quả mô phỏng năng lực thành công", result));
    }
}
