package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.application.dto.reservation.CancelReservationCommand;
import com.hrm.employeemanagement.application.dto.reservation.CreateReservationCommand;
import com.hrm.employeemanagement.application.dto.reservation.ResourceReservationResult;
import com.hrm.employeemanagement.application.port.inbound.reservation.AutoProcessProjectReservationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.reservation.CancelResourceReservationUseCase;
import com.hrm.employeemanagement.application.port.inbound.reservation.CreateResourceReservationUseCase;
import com.hrm.employeemanagement.application.port.inbound.reservation.GetResourceReservationsUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.reservation.InvalidReservationDataException;
import com.hrm.employeemanagement.domain.exception.reservation.ReservationNotFoundException;
import com.hrm.employeemanagement.domain.reservation.ReservationStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.reservation.dto.CancelReservationRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.reservation.dto.CreateReservationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceReservationController API Tests")
class ResourceReservationControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CreateResourceReservationUseCase createUseCase;
    @Mock
    private CancelResourceReservationUseCase cancelUseCase;
    @Mock
    private GetResourceReservationsUseCase getUseCase;
    @Mock
    private AutoProcessProjectReservationsUseCase autoProcessUseCase;

    @BeforeEach
    void setUp() {
        ResourceReservationController controller = new ResourceReservationController(
                createUseCase, cancelUseCase, getUseCase, autoProcessUseCase
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ResourceReservationExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/resource-reservations -> 201 Created khi request hợp lệ")
    void shouldReturn201WhenCreateReservationSucceeds() throws Exception {
        CreateReservationRequest request = new CreateReservationRequest(
                100L, 50L, 2026, 40, BigDecimal.valueOf(20.0), "Giữ chỗ test"
        );

        ResourceReservationResult result = new ResourceReservationResult(
                1L, 100L, "PRJ-01", "ERP", 50L, "EMP050", "Nguyen Van A",
                2026, 40, BigDecimal.valueOf(20.0), ReservationStatus.ACTIVE,
                null, null, "Giữ chỗ test", 1L, LocalDateTime.now()
        );

        when(createUseCase.createReservation(any(CreateReservationCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/resource-reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.reservedHours", is(20.0)))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("POST /api/v1/resource-reservations -> 400 Bad Request khi reservedHours <= 0")
    void shouldReturn400WhenReservedHoursIsInvalid() throws Exception {
        CreateReservationRequest request = new CreateReservationRequest(
                100L, 50L, 2026, 40, BigDecimal.ZERO, "Note"
        );

        mockMvc.perform(post("/api/v1/resource-reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/resource-reservations -> 403 Forbidden khi thiếu quyền hạn (TC-04)")
    void shouldReturn403WhenPermissionDenied() throws Exception {
        CreateReservationRequest request = new CreateReservationRequest(
                100L, 50L, 2026, 40, BigDecimal.valueOf(20.0), "Note"
        );

        when(createUseCase.createReservation(any(CreateReservationCommand.class)))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_RESERVATION_CREATE));

        mockMvc.perform(post("/api/v1/resource-reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("PATCH /api/v1/resource-reservations/{id}/cancel -> 200 OK khi hủy thành công")
    void shouldReturn200WhenCancelSucceeds() throws Exception {
        CancelReservationRequest request = new CancelReservationRequest("Thay đổi kế hoạch");

        ResourceReservationResult result = new ResourceReservationResult(
                1L, 100L, "PRJ-01", "ERP", 50L, "EMP050", "Nguyen Van A",
                2026, 40, BigDecimal.valueOf(20.0), ReservationStatus.CANCELLED,
                null, "Thay đổi kế hoạch", "Giữ chỗ test", 1L, LocalDateTime.now()
        );

        when(cancelUseCase.cancelReservation(any(CancelReservationCommand.class))).thenReturn(result);

        mockMvc.perform(patch("/api/v1/resource-reservations/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("PATCH /api/v1/resource-reservations/{id}/cancel -> 404 Not Found khi ID không tồn tại")
    void shouldReturn404WhenReservationNotFound() throws Exception {
        when(cancelUseCase.cancelReservation(any(CancelReservationCommand.class)))
                .thenThrow(new ReservationNotFoundException("Không tìm thấy"));

        mockMvc.perform(patch("/api/v1/resource-reservations/999/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/resource-reservations -> 200 OK trả về danh sách giữ chỗ")
    void shouldReturnReservationsList() throws Exception {
        ResourceReservationResult r = new ResourceReservationResult(
                1L, 100L, "PRJ-01", "ERP", 50L, "EMP050", "Nguyen Van A",
                2026, 40, BigDecimal.valueOf(20.0), ReservationStatus.ACTIVE,
                null, null, "Giữ chỗ", 1L, LocalDateTime.now()
        );

        when(getUseCase.getReservations(100L, null, null, null, null)).thenReturn(List.of(r));

        mockMvc.perform(get("/api/v1/resource-reservations?projectId=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].id", is(1)))
                .andExpect(jsonPath("$.data[0].reservedHours", is(20.0)));
    }

    @Test
    @DisplayName("POST /api/v1/resource-reservations/projects/{id}/auto-cancel -> 200 OK (TC-02)")
    void shouldAutoCancelForProject() throws Exception {
        when(autoProcessUseCase.autoCancelForProject(100L, "Dự án dự kiến bị hủy", 1L)).thenReturn(3);

        mockMvc.perform(post("/api/v1/resource-reservations/projects/100/auto-cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(3)));
    }

    @Test
    @DisplayName("POST /api/v1/resource-reservations/projects/{id}/auto-convert -> 200 OK (TC-03)")
    void shouldAutoConvertForProject() throws Exception {
        when(autoProcessUseCase.autoConvertForProject(100L, 1L)).thenReturn(2);

        mockMvc.perform(post("/api/v1/resource-reservations/projects/100/auto-convert"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(2)));
    }
}
