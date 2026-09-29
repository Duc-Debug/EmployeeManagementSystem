package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common;

import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.MyAllocationsController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Class-level validation: ObjectError must not throw ClassCastException and must return HTTP 400 VALIDATION_ERROR")
    void testHandleValidationExceptions_ClassLevelObjectError() {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "changePasswordRequest");
        // Add class-level ObjectError (NOT a FieldError)
        bindingResult.addError(new ObjectError("changePasswordRequest", "Mật khẩu xác nhận không khớp với mật khẩu mới"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        // Act & Assert: Must not throw ClassCastException
        ResponseEntity<ErrorResponse> response = assertDoesNotThrow(() -> exceptionHandler.handleValidationExceptions(ex));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("VALIDATION_ERROR", body.getCode());
        assertEquals("VALIDATION_ERROR", body.getErrorCode());
        assertEquals(400, body.getStatus());
        assertTrue(body.getMessage().contains("changePasswordRequest: Mật khẩu xác nhận không khớp"));

        assertNotNull(body.getDetails());
        assertTrue(body.getDetails() instanceof Map);
        Map<?, ?> detailsMap = (Map<?, ?>) body.getDetails();
        assertEquals("Mật khẩu xác nhận không khớp với mật khẩu mới", detailsMap.get("changePasswordRequest"));
    }

    @Test
    @DisplayName("Mixed validation: FieldError and ObjectError combined are safely handled")
    void testHandleValidationExceptions_MixedFieldAndObjectErrors() {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "userRegistrationRequest");
        bindingResult.addError(new FieldError("userRegistrationRequest", "email", "Email không đúng định dạng"));
        bindingResult.addError(new ObjectError("userRegistrationRequest", "Thời gian bắt đầu phải trước thời gian kết thúc"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = assertDoesNotThrow(() -> exceptionHandler.handleValidationExceptions(ex));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("VALIDATION_ERROR", body.getCode());
        assertTrue(body.getMessage().contains("email: Email không đúng định dạng"));
        assertTrue(body.getMessage().contains("userRegistrationRequest: Thời gian bắt đầu phải trước thời gian kết thúc"));

        Map<?, ?> detailsMap = (Map<?, ?>) body.getDetails();
        assertEquals("Email không đúng định dạng", detailsMap.get("email"));
        assertEquals("Thời gian bắt đầu phải trước thời gian kết thúc", detailsMap.get("userRegistrationRequest"));
    }

    @Test
    @DisplayName("MyAllocations feedback validation: ProvideScheduleFeedbackRequest maps to INVALID_FEEDBACK_REASON")
    void testHandleValidationExceptions_ProvideScheduleFeedbackRequest() {
        MyAllocationsController.ProvideScheduleFeedbackRequest target =
                new MyAllocationsController.ProvideScheduleFeedbackRequest("   ");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "provideScheduleFeedbackRequest");
        bindingResult.addError(new FieldError(
                "provideScheduleFeedbackRequest",
                "reason",
                "Lý do hoặc ý kiến phản hồi không được để trống"
        ));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("INVALID_FEEDBACK_REASON", body.getCode());
        assertEquals("INVALID_FEEDBACK_REASON", body.getErrorCode());
        assertEquals("Lý do hoặc ý kiến phản hồi không được để trống", body.getMessage());
    }

    @Test
    @DisplayName("IllegalArgumentException with QTN-24 policy message maps to INVALID_FEEDBACK_REASON")
    void testHandleIllegalArgumentException_FeedbackPolicy() {
        IllegalArgumentException ex = new IllegalArgumentException("Lý do hoặc ý kiến phản hồi không được để trống theo quy định QTN-24");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("INVALID_FEEDBACK_REASON", body.getCode());
        assertEquals("INVALID_FEEDBACK_REASON", body.getErrorCode());
        assertEquals(400, body.getStatus());
    }

    @Test
    @DisplayName("Standard IllegalArgumentException maps to INVALID_ARGUMENT")
    void testHandleIllegalArgumentException_Standard() {
        IllegalArgumentException ex = new IllegalArgumentException("Tham số không hợp lệ");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("INVALID_ARGUMENT", body.getCode());
        assertEquals("INVALID_ARGUMENT", body.getErrorCode());
        assertEquals(400, body.getStatus());
    }
}
