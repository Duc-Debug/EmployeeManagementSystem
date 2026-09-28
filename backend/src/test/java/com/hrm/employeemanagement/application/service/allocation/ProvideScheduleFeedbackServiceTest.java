package com.hrm.employeemanagement.application.service.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.application.dto.allocation.ProvideScheduleFeedbackResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.ScheduleConfirmationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.ScheduleConfirmationPort.SaveConfirmationResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.ScheduleConfirmationPort.ScheduleConfirmationRecord;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

class ProvideScheduleFeedbackServiceTest {

    private GetAuthenticatedUserPort authenticatedUserPort;
    private ScheduleConfirmationPort scheduleConfirmationPort;
    private ProvideScheduleFeedbackService service;

    private final UserId userId = new UserId(100L);

    @BeforeEach
    void setUp() {
        authenticatedUserPort = mock(GetAuthenticatedUserPort.class);
        scheduleConfirmationPort = mock(ScheduleConfirmationPort.class);

        service = new ProvideScheduleFeedbackService(
                authenticatedUserPort,
                scheduleConfirmationPort
        );

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(mockUser);
    }

    @Test
    @DisplayName("QTN-24: Gui phan hoi thanh cong -> Luu feedbackNote, status HAS_FEEDBACK, khong doi gio phan bo")
    void testProvideFeedback_Success() {
        LocalDate monday = LocalDate.of(2026, 9, 21);
        String reason = "Trung lich du an Alpha va Beta";
        String clientIp = "127.0.0.1";
        LocalDateTime feedbackTime = LocalDateTime.of(2026, 9, 21, 14, 30);

        ScheduleConfirmationRecord savedRecord = new ScheduleConfirmationRecord(
                1L, userId.value(), monday, null, clientIp, reason, feedbackTime, "HAS_FEEDBACK"
        );

        when(scheduleConfirmationPort.saveFeedback(eq(userId.value()), eq(monday), eq(reason), any(LocalDateTime.class), eq(clientIp)))
                .thenReturn(new SaveConfirmationResult(savedRecord, true));

        ProvideScheduleFeedbackResult result = service.provideFeedback(monday, reason, clientIp);

        assertThat(result.weekStartDate()).isEqualTo(monday);
        assertThat(result.confirmationStatus()).isEqualTo("HAS_FEEDBACK");
        assertThat(result.feedbackNote()).isEqualTo(reason);
        assertThat(result.feedbackAt()).isEqualTo(feedbackTime);
        assertThat(result.message()).contains("QTN-24");
    }

    @Test
    @DisplayName("Validation: Ly do phan hoi rong hoac khoang trang -> Bi tu choi")
    void testProvideFeedback_BlankReason_ThrowsException() {
        LocalDate monday = LocalDate.of(2026, 9, 21);

        assertThatThrownBy(() -> service.provideFeedback(monday, "", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lý do");

        assertThatThrownBy(() -> service.provideFeedback(monday, "   ", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lý do");

        assertThatThrownBy(() -> service.provideFeedback(monday, null, "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lý do");

        verify(scheduleConfirmationPort, never()).saveFeedback(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Validation: Ngay khong phai Thu Hai -> Tu dong normalize ve Thu Hai theo domain policy")
    void testProvideFeedback_NonMonday_Normalizes() {
        LocalDate tuesday = LocalDate.of(2026, 9, 22);
        LocalDate monday = LocalDate.of(2026, 9, 21);
        String reason = "Reason";
        LocalDateTime feedbackTime = LocalDateTime.now();

        ScheduleConfirmationRecord savedRecord = new ScheduleConfirmationRecord(
                1L, userId.value(), monday, null, "127.0.0.1", reason, feedbackTime, "HAS_FEEDBACK"
        );
        when(scheduleConfirmationPort.saveFeedback(eq(userId.value()), eq(monday), eq(reason), any(), any()))
                .thenReturn(new SaveConfirmationResult(savedRecord, true));

        ProvideScheduleFeedbackResult res = service.provideFeedback(tuesday, reason, "127.0.0.1");
        assertThat(res.weekStartDate()).isEqualTo(monday);
    }
}
