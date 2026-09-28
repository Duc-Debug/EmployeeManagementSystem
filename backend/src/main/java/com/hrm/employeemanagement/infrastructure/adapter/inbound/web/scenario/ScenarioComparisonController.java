package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.scenario.CompareScenariosCommand;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioComparisonResult;
import com.hrm.employeemanagement.application.port.inbound.scenario.CompareSimulationScenariosUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto.CompareScenariosRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/resource-scenarios")
@Validated
public class ScenarioComparisonController {

    private final CompareSimulationScenariosUseCase compareScenariosUseCase;

    public ScenarioComparisonController(CompareSimulationScenariosUseCase compareScenariosUseCase) {
        this.compareScenariosUseCase = Objects.requireNonNull(compareScenariosUseCase, "CompareSimulationScenariosUseCase must not be null");
    }

    @PostMapping("/compare")
    @PreAuthorize("hasAuthority('RESOURCE_SCENARIO_COMPARE')")
    public ResponseEntity<ApiResponse<ScenarioComparisonResult>> compareScenarios(
            @Valid @RequestBody CompareScenariosRequest request
    ) {
        CompareScenariosCommand command = new CompareScenariosCommand(request.scenarioIds());
        ScenarioComparisonResult result = compareScenariosUseCase.compareScenarios(command);
        return ResponseEntity.ok(ApiResponse.success("So sánh kịch bản mô phỏng thành công", result));
    }
}
