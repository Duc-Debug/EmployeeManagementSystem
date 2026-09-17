package com.hrm.employeemanagement.infrastructure.adapter.outbound.serializer.notification;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationJsonSerializerPort;

/**
 * Adapter triển khai NotificationJsonSerializerPort sử dụng Jackson ObjectMapper.
 * Tự động cấu hình JavaTimeModule và fallback an toàn nếu Spring container chưa đăng ký ObjectMapper bean.
 */
@Component
public class JacksonNotificationJsonSerializerAdapter implements NotificationJsonSerializerPort {

    private final ObjectMapper objectMapper;

    public JacksonNotificationJsonSerializerAdapter(@Autowired(required = false) ObjectMapper objectMapper) {
        if (objectMapper != null) {
            this.objectMapper = objectMapper;
        } else {
            ObjectMapper defaultMapper = new ObjectMapper();
            defaultMapper.registerModule(new JavaTimeModule());
            this.objectMapper = defaultMapper;
        }
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
