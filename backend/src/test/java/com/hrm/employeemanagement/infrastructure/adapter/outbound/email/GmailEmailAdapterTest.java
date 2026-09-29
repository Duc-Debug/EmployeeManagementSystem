package com.hrm.employeemanagement.infrastructure.adapter.outbound.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

@DisplayName("GmailEmailAdapter Unit Tests")
class GmailEmailAdapterTest {

    @Test
    @DisplayName("Cấu hình adapter nạp đúng các tham số")
    void testConfigurationProperties() {
        GmailEmailAdapter adapter = new GmailEmailAdapter(
                "backend/credentials/credentials.json",
                "custom-tokens",
                9090,
                "http://localhost:5173/reset-password"
        );

        assertThat(adapter.getCredentialsPath()).isEqualTo("backend/credentials/credentials.json");
        assertThat(adapter.getTokensDirectoryPath()).isEqualTo("custom-tokens");
        assertThat(adapter.getOauthPort()).isEqualTo(9090);
        assertThat(adapter.getResetPasswordBaseUrl()).isEqualTo("http://localhost:5173/reset-password");
    }

    @Test
    @DisplayName("Ném IllegalStateException khi file credentials không tồn tại")
    void testSendEmailThrowsWhenCredentialsNotFound() {
        GmailEmailAdapter adapter = new GmailEmailAdapter(
                "non-existent-folder/credentials.json",
                "tokens",
                8888,
                "http://localhost:5173/reset-password"
        );

        assertThatThrownBy(() -> adapter.sendPasswordResetEmail("user@example.com", "testuser", "rawToken123", 15))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy file credentials");
    }

    @Test
    @DisplayName("Gửi email thành công qua Gmail API Client mock")
    void testSendEmailWithMockedGmailClient() throws Exception {
        Gmail gmailMock = mock(Gmail.class);
        Gmail.Users usersMock = mock(Gmail.Users.class);
        Gmail.Users.Messages messagesMock = mock(Gmail.Users.Messages.class);
        Gmail.Users.Messages.Send sendMock = mock(Gmail.Users.Messages.Send.class);

        Message sentResponse = new Message();
        sentResponse.setId("gmail-message-id-999");

        when(gmailMock.users()).thenReturn(usersMock);
        when(usersMock.messages()).thenReturn(messagesMock);
        when(messagesMock.send(eq("me"), any(Message.class))).thenReturn(sendMock);
        when(sendMock.execute()).thenReturn(sentResponse);

        GmailEmailAdapter adapter = new GmailEmailAdapter(gmailMock, "http://localhost:5173/reset-password");
        adapter.sendPasswordResetEmail("recipient@test.com", "nhanvien1", "test-token-abc", 30);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messagesMock).send(eq("me"), messageCaptor.capture());
        assertThat(messageCaptor.getValue().getRaw()).isNotBlank();
    }
}
