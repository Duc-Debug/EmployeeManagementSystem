package com.hrm.employeemanagement.application.dto.projecttemplate;

import java.time.LocalDateTime;

public record ProjectTemplateResult(
    Long id,
    String templateCode,
    String name,
    String description,
    boolean active,
    LocalDateTime createdAt
) {
}
