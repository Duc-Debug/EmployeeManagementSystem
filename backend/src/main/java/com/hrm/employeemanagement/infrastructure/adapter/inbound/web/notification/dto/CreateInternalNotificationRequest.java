package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInternalNotificationRequest(
        @NotBlank(message = "eventType không được để trống")
        String eventType,

        String level, // CAO, TRUNG_BINH, THAP

        @NotBlank(message = "title không được để trống")
        String title,

        String message,

        String relatedEntityType,

        String relatedEntityId,

        @NotBlank(message = "sourceEventKey không được để trống")
        String sourceEventKey,

        @NotNull(message = "recipientUserIds không được null")
        List<Long> recipientUserIds
) {}
