package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit.dto;

import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitMemberResult;
import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitNodeResult;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;

import java.util.List;

public record OrgUnitNodeResponse(
        Long id,
        String unitCode,
        String unitName,
        OrgUnitType unitType,
        Long parentId,
        String treePath,
        Integer level,
        OrgUnitStatus status,
        String description,
        Long managerId,
        String managerName,
        Integer employeeCount,
        List<OrgUnitMemberResponse> members,
        List<OrgUnitNodeResponse> children) {

    public record OrgUnitMemberResponse(
            Long id,
            String employeeCode,
            String fullName,
            String professionalRole
    ) {
        public static OrgUnitMemberResponse fromResult(OrgUnitMemberResult result) {
            if (result == null) return null;
            return new OrgUnitMemberResponse(
                    result.id(),
                    result.employeeCode(),
                    result.fullName(),
                    result.professionalRole()
            );
        }
    }

    public static OrgUnitNodeResponse fromResult(OrgUnitNodeResult result) {
        if (result == null)
            return null;
        List<OrgUnitNodeResponse> childResponses = result.children() != null
                ? result.children().stream().map(OrgUnitNodeResponse::fromResult).toList()
                : List.of();

        List<OrgUnitMemberResponse> memberResponses = result.members() != null
                ? result.members().stream().map(OrgUnitMemberResponse::fromResult).toList()
                : List.of();

        return new OrgUnitNodeResponse(
                result.id(),
                result.unitCode(),
                result.unitName(),
                result.unitType(),
                result.parentId(),
                result.treePath(),
                result.level(),
                result.status(),
                result.description(),
                result.managerId(),
                result.managerName(),
                result.employeeCount(),
                memberResponses,
                childResponses);
    }
}
