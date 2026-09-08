package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult;
import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult.SkillMatrixCellResult;
import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult.SkillMatrixRowResult;
import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult.SkillMatrixSkillHeaderResult;
import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult.SkillMatrixSummaryResult;

public record DepartmentSkillMatrixResponse(
        Long orgUnitId,
        String orgUnitCode,
        String orgUnitName,
        List<SkillMatrixSkillHeaderResponse> skills,
        List<SkillMatrixRowResponse> rows,
        SkillMatrixSummaryResponse summary
) {

    public record SkillMatrixSkillHeaderResponse(
            Long id,
            String code,
            String name,
            String category,
            int employeeCount,
            boolean singlePersonRisk
    ) {
        public static SkillMatrixSkillHeaderResponse fromResult(SkillMatrixSkillHeaderResult result) {
            return new SkillMatrixSkillHeaderResponse(
                    result.id(),
                    result.code(),
                    result.name(),
                    result.category(),
                    result.employeeCount(),
                    result.singlePersonRisk()
            );
        }
    }

    public record SkillMatrixCellResponse(
            Long skillId,
            Integer proficiencyLevel,
            BigDecimal yearsOfExperience,
            String reviewNotes
    ) {
        public static SkillMatrixCellResponse fromResult(SkillMatrixCellResult result) {
            return new SkillMatrixCellResponse(
                    result.skillId(),
                    result.proficiencyLevel(),
                    result.yearsOfExperience(),
                    result.reviewNotes()
            );
        }
    }

    public record SkillMatrixRowResponse(
            Long employeeId,
            String employeeCode,
            String fullName,
            String professionalRole,
            Map<Long, SkillMatrixCellResponse> skills
    ) {
        public static SkillMatrixRowResponse fromResult(SkillMatrixRowResult result) {
            Map<Long, SkillMatrixCellResponse> cells = new HashMap<>();
            if (result.skills() != null) {
                for (Map.Entry<Long, SkillMatrixCellResult> entry : result.skills().entrySet()) {
                    cells.put(entry.getKey(), SkillMatrixCellResponse.fromResult(entry.getValue()));
                }
            }
            return new SkillMatrixRowResponse(
                    result.employeeId(),
                    result.employeeCode(),
                    result.fullName(),
                    result.professionalRole(),
                    cells
            );
        }
    }

    public record SkillMatrixSummaryResponse(
            int totalEmployees,
            int totalSkills,
            int singlePersonRiskSkillCount,
            int unstaffedSkillCount
    ) {
        public static SkillMatrixSummaryResponse fromResult(SkillMatrixSummaryResult result) {
            return new SkillMatrixSummaryResponse(
                    result.totalEmployees(),
                    result.totalSkills(),
                    result.singlePersonRiskSkillCount(),
                    result.unstaffedSkillCount()
            );
        }
    }

    public static DepartmentSkillMatrixResponse fromResult(DepartmentSkillMatrixResult result) {
        if (result == null) {
            return null;
        }

        List<SkillMatrixSkillHeaderResponse> headers = result.skills() != null
                ? result.skills().stream().map(SkillMatrixSkillHeaderResponse::fromResult).toList()
                : List.of();

        List<SkillMatrixRowResponse> rows = result.rows() != null
                ? result.rows().stream().map(SkillMatrixRowResponse::fromResult).toList()
                : List.of();

        SkillMatrixSummaryResponse summary = result.summary() != null
                ? SkillMatrixSummaryResponse.fromResult(result.summary())
                : null;

        return new DepartmentSkillMatrixResponse(
                result.orgUnitId(),
                result.orgUnitCode(),
                result.orgUnitName(),
                headers,
                rows,
                summary
        );
    }
}