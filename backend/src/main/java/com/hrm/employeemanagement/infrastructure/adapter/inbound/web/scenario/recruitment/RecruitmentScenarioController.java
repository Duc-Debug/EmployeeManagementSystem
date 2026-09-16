package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.recruitment;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.scenario.recruitment.InvalidSimulatedEmployeeException;
import com.hrm.employeemanagement.domain.exception.scenario.recruitment.ScenarioNotFoundException;
import com.hrm.employeemanagement.domain.exception.scenario.recruitment.SimulatedEmployeeNotFoundException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.recruitment.dto.AddSimulatedEmployeeRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.recruitment.dto.RecruitmentScenarioEvaluationResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.recruitment.dto.SimulatedEmployeeResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.recruitment.dto.UpdateSimulatedEmployeeRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.recruitment.mapper.RecruitmentScenarioWebMapper;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/scenarios/{scenarioId}")
@Validated
public class RecruitmentScenarioController {

    private final AddSimulatedEmployeeUseCase addSimulatedEmployeeUseCase;
    private final UpdateSimulatedEmployeeUseCase updateSimulatedEmployeeUseCase;
    private final RemoveSimulatedEmployeeUseCase removeSimulatedEmployeeUseCase;
    private final GetScenarioSimulatedEmployeesUseCase getScenarioSimulatedEmployeesUseCase;
    private final RerunRecruitmentScenarioUseCase rerunRecruitmentScenarioUseCase;
    private final GetRecruitmentEvaluationUseCase getRecruitmentEvaluationUseCase;

    public RecruitmentScenarioController(
            AddSimulatedEmployeeUseCase addSimulatedEmployeeUseCase,
            UpdateSimulatedEmployeeUseCase updateSimulatedEmployeeUseCase,
            RemoveSimulatedEmployeeUseCase removeSimulatedEmployeeUseCase,
            GetScenarioSimulatedEmployeesUseCase getScenarioSimulatedEmployeesUseCase,
            RerunRecruitmentScenarioUseCase rerunRecruitmentScenarioUseCase,
            GetRecruitmentEvaluationUseCase getRecruitmentEvaluationUseCase
    ) {
        this.addSimulatedEmployeeUseCase = Objects.requireNonNull(addSimulatedEmployeeUseCase);
        this.updateSimulatedEmployeeUseCase = Objects.requireNonNull(updateSimulatedEmployeeUseCase);
        this.removeSimulatedEmployeeUseCase = Objects.requireNonNull(removeSimulatedEmployeeUseCase);
        this.getScenarioSimulatedEmployeesUseCase = Objects.requireNonNull(getScenarioSimulatedEmployeesUseCase);
        this.rerunRecruitmentScenarioUseCase = Objects.requireNonNull(rerunRecruitmentScenarioUseCase);
        this.getRecruitmentEvaluationUseCase = Objects.requireNonNull(getRecruitmentEvaluationUseCase);
    }

    @PostMapping("/simulated-employees")
    public ResponseEntity<ApiResponse<RecruitmentScenarioEvaluationResponse>> addSimulatedEmployee(
            @PathVariable Long scenarioId,
            @Valid @RequestBody AddSimulatedEmployeeRequest request
    ) {
        AddSimulatedEmployeeCommand command = new AddSimulatedEmployeeCommand(
                scenarioId,
                request.candidateName(),
                request.projectRoleId(),
                request.primarySkillId(),
                request.standardHoursPerWeek(),
                request.weeksCount(),
                request.notes()
        );
        RecruitmentScenarioEvaluationResult result = addSimulatedEmployeeUseCase.addSimulatedEmployee(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm nhân sự giả định vào kịch bản thành công",
                        RecruitmentScenarioWebMapper.toResponse(result)));
    }

    @DeleteMapping("/simulated-employees/{employeeId}")
    public ResponseEntity<ApiResponse<RecruitmentScenarioEvaluationResponse>> removeSimulatedEmployee(
            @PathVariable Long scenarioId,
            @PathVariable Long employeeId
    ) {
        RemoveSimulatedEmployeeCommand command = new RemoveSimulatedEmployeeCommand(scenarioId, employeeId);
        RecruitmentScenarioEvaluationResult result = removeSimulatedEmployeeUseCase.removeSimulatedEmployee(command);
        return ResponseEntity.ok(ApiResponse.success("Xóa nhân sự giả định khỏi kịch bản thành công",
                RecruitmentScenarioWebMapper.toResponse(result)));
    }

    @GetMapping("/simulated-employees")
    public ResponseEntity<ApiResponse<List<SimulatedEmployeeResponse>>> getSimulatedEmployees(
            @PathVariable Long scenarioId
    ) {
        List<SimulatedEmployeeResult> results = getScenarioSimulatedEmployeesUseCase.getSimulatedEmployees(scenarioId);
        List<SimulatedEmployeeResponse> responses = results.stream()
                .map(RecruitmentScenarioWebMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách nhân sự giả định thành công", responses));
    }

    @GetMapping("/recruitment-evaluation")
    public ResponseEntity<ApiResponse<RecruitmentScenarioEvaluationResponse>> getRecruitmentEvaluation(
            @PathVariable Long scenarioId
    ) {
        RecruitmentScenarioEvaluationResult result = getRecruitmentEvaluationUseCase.getRecruitmentEvaluation(scenarioId);
        return ResponseEntity.ok(ApiResponse.success("Lấy đánh giá kịch bản tuyển dụng thành công",
                RecruitmentScenarioWebMapper.toResponse(result)));
    }

    @PostMapping("/recruitment-evaluation/rerun")
    public ResponseEntity<ApiResponse<RecruitmentScenarioEvaluationResponse>> rerunRecruitmentScenario(
            @PathVariable Long scenarioId
    ) {
        RecruitmentScenarioEvaluationResult result = rerunRecruitmentScenarioUseCase.rerunRecruitmentScenario(scenarioId);
        return ResponseEntity.ok(ApiResponse.success("Chạy lại đánh giá kịch bản tuyển dụng thành công",
                RecruitmentScenarioWebMapper.toResponse(result)));
    }

    @PutMapping("/simulated-employees/{employeeId}")
    public ResponseEntity<ApiResponse<RecruitmentScenarioEvaluationResponse>> updateSimulatedEmployee(
            @PathVariable Long scenarioId,
            @PathVariable Long employeeId,
            @Valid @RequestBody UpdateSimulatedEmployeeRequest request
    ) {
        UpdateSimulatedEmployeeCommand command = new UpdateSimulatedEmployeeCommand(
                scenarioId,
                employeeId,
                request.candidateName(),
                request.projectRoleId(),
                request.primarySkillId(),
                request.standardHoursPerWeek(),
                request.weeksCount(),
                request.notes()
        );
        RecruitmentScenarioEvaluationResult result = updateSimulatedEmployeeUseCase.updateSimulatedEmployee(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin nhân sự giả định thành công",
                RecruitmentScenarioWebMapper.toResponse(result)));
    }

    // =========================================================================
    // Local Exception Handlers cho CN-005
    // =========================================================================

    @ExceptionHandler(ScenarioNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleScenarioNotFound(ScenarioNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(SimulatedEmployeeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSimulatedEmployeeNotFound(SimulatedEmployeeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidSimulatedEmployeeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidSimulatedEmployee(InvalidSimulatedEmployeeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePermissionDenied(PermissionDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", ex.getMessage()));
    }
}
