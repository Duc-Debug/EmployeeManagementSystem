package com.hrm.employeemanagement.infrastructure.adapter.outbound.serializer.notification;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationJsonSerializerPort;

/**
 * Adapter triển khai NotificationJsonSerializerPort sử dụng Jackson ObjectMapper đã được Spring Boot cấu hình.
 */
@Component
public class JacksonNotificationJsonSerializerAdapter implements NotificationJsonSerializerPort {

    private final ObjectMapper objectMapper;

    public JacksonNotificationJsonSerializerAdapter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public String toJson(Object payload) {
        if (payload == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Lỗi khi tuần tự hóa payload thành JSON", e);
        }
    }
}
