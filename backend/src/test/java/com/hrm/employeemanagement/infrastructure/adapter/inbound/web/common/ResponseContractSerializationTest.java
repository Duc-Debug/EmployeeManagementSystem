package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Response Contract Serialization Tests")
class ResponseContractSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("Contract 1: Success response must include success=true, code=SUCCESS, status=200, message, data, timestamp")
    void testSuccessResponseContract_Default() throws Exception {
        ApiResponse<Map<String, Object>> response = ApiResponse.success(Map.of("id", 100, "name", "John"));
        String json = objectMapper.writeValueAsString(response);
        JsonNode root = objectMapper.readTree(json);

        // Required contract fields
        assertTrue(root.get("success").asBoolean(), "success must be true");
        assertEquals("SUCCESS", root.get("code").asText(), "code must be 'SUCCESS'");
        assertEquals(200, root.get("status").asInt(), "status must be 200");
        assertEquals("Thành công", root.get("message").asText());
        assertNotNull(root.get("timestamp"), "timestamp must not be null");
        assertEquals(100, root.get("data").get("id").asInt());
        assertEquals("John", root.get("data").get("name").asText());

        // Error-specific and deprecated fields must NOT be present in success JSON
        assertFalse(root.has("errorCode"), "errorCode must be omitted on success");
        assertFalse(root.has("error_code"), "error_code alias must never be present");
        assertFalse(root.has("details"), "details must be omitted when null");
    }

    @Test
    @DisplayName("Contract 2: Success response with custom message and custom status")
    void testSuccessResponseContract_CustomMessageAndStatus() throws Exception {
        ApiResponse<String> response = ApiResponse.success(201, "Tạo mới thành công", "CREATED_ID_123");
        String json = objectMapper.writeValueAsString(response);
        JsonNode root = objectMapper.readTree(json);

        assertTrue(root.get("success").asBoolean());
        assertEquals("SUCCESS", root.get("code").asText());
        assertEquals(201, root.get("status").asInt());
        assertEquals("Tạo mới thành công", root.get("message").asText());
        assertEquals("CREATED_ID_123", root.get("data").asText());
        assertFalse(root.has("errorCode"));
        assertFalse(root.has("error_code"));
    }

    @Test
    @DisplayName("Contract 3: ErrorResponse must include success=false, code, errorCode, status, message, timestamp without error_code alias")
    void testErrorResponseContract_Standard() throws Exception {
        ErrorResponse errorResponse = ErrorResponse.of("ORG_UNIT_NOT_FOUND", "Không tìm thấy đơn vị tổ chức", 404);
        String json = objectMapper.writeValueAsString(errorResponse);
        JsonNode root = objectMapper.readTree(json);

        // Required contract fields
        assertFalse(root.get("success").asBoolean(), "success must be false");
        assertEquals("ORG_UNIT_NOT_FOUND", root.get("code").asText());
        assertEquals("ORG_UNIT_NOT_FOUND", root.get("errorCode").asText(), "errorCode must match code");
        assertEquals(404, root.get("status").asInt());
        assertEquals("Không tìm thấy đơn vị tổ chức", root.get("message").asText());
        assertNotNull(root.get("timestamp"));

        // LOW feedback verification: snake_case alias error_code must be eliminated
        assertFalse(root.has("error_code"), "API must not expose snake_case error_code alias alongside errorCode");
    }

    @Test
    @DisplayName("Contract 4: ErrorResponse with details")
    void testErrorResponseContract_WithDetails() throws Exception {
        Map<String, String> fieldErrors = Map.of("email", "Email không đúng định dạng", "username", "Tên tài khoản đã tồn tại");
        ErrorResponse errorResponse = ErrorResponse.of("VALIDATION_ERROR", "Dữ liệu không hợp lệ", 400, fieldErrors);
        String json = objectMapper.writeValueAsString(errorResponse);
        JsonNode root = objectMapper.readTree(json);

        assertFalse(root.get("success").asBoolean());
        assertEquals("VALIDATION_ERROR", root.get("code").asText());
        assertEquals("VALIDATION_ERROR", root.get("errorCode").asText());
        assertEquals(400, root.get("status").asInt());
        assertEquals("Email không đúng định dạng", root.get("details").get("email").asText());
        assertEquals("Tên tài khoản đã tồn tại", root.get("details").get("username").asText());
        assertFalse(root.has("error_code"), "error_code alias must not exist");
    }

    @Test
    @DisplayName("Contract 5: ApiResponse.error() must match ErrorResponse contract and omit error_code alias")
    void testApiResponseErrorContract() throws Exception {
        ApiResponse<Void> response = ApiResponse.error("RESOURCE_CONFLICT", "Xung đột lịch làm việc", 409);
        String json = objectMapper.writeValueAsString(response);
        JsonNode root = objectMapper.readTree(json);

        assertFalse(root.get("success").asBoolean());
        assertEquals("RESOURCE_CONFLICT", root.get("code").asText());
        assertEquals("RESOURCE_CONFLICT", root.get("errorCode").asText());
        assertEquals(409, root.get("status").asInt());
        assertEquals("Xung đột lịch làm việc", root.get("message").asText());
        assertNotNull(root.get("timestamp"));
        assertFalse(root.has("error_code"), "error_code alias must not exist");
        assertFalse(root.has("data"), "data must be omitted on error when null");
    }
}
