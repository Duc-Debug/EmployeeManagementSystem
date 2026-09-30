package com.hrm.employeemanagement.infrastructure.adapter.outbound.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

@DisplayName("GmailEmailAdapter Unit Tests")
class GmailEmailAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Cấu hình adapter nạp đúng các tham số")
    void testConfigurationProperties() {
        GmailEmailAdapter adapter = new GmailEmailAdapter(
                "backend/credentials/credentials.json",
                "custom-tokens",
                9090,
                "custom-refresh-token",
                false,
                "http://localhost:5173/reset-password"
        );

        assertThat(adapter.getCredentialsPath()).isEqualTo("backend/credentials/credentials.json");
        assertThat(adapter.getTokensDirectoryPath()).isEqualTo("custom-tokens");
        assertThat(adapter.getOauthPort()).isEqualTo(9090);
        assertThat(adapter.getConfiguredRefreshToken()).isEqualTo("custom-refresh-token");
        assertThat(adapter.isAllowBrowserAuth()).isFalse();
        assertThat(adapter.getResetPasswordBaseUrl()).isEqualTo("http://localhost:5173/reset-password");
    }

    @Test
    @DisplayName("P2-4: resolveCredentialsFile với đường dẫn tuyệt đối hợp lệ")
    void testResolveCredentialsFile_absolutePath() throws IOException {
        Path credFile = Files.createFile(tempDir.resolve("my-creds.json"));
        GmailEmailAdapter adapter = new GmailEmailAdapter(
                credFile.toAbsolutePath().toString(),
                tempDir.resolve("tokens").toString(),
                8889,
                "http://localhost:5173/reset-password"
        );

        File resolved = adapter.resolveCredentialsFile(credFile.toAbsolutePath().toString());
        assertThat(resolved).exists().isFile();
        assertThat(resolved.getAbsolutePath()).isEqualTo(credFile.toAbsolutePath().toString());
    }

    @Test
    @DisplayName("P2-4: resolveCredentialsFile với đường dẫn tương đối hợp lệ")
    void testResolveCredentialsFile_relativePath() throws IOException {
        Path relativeCred = tempDir.resolve("relative-credentials.json");
        Files.createFile(relativeCred);

        GmailEmailAdapter adapter = new GmailEmailAdapter(
                relativeCred.toString(),
                tempDir.resolve("tokens").toString(),
                8889,
                "http://localhost:5173/reset-password"
        );

        File resolved = adapter.resolveCredentialsFile(relativeCred.toString());
        assertThat(resolved).exists();
        assertThat(resolved.getName()).isEqualTo("relative-credentials.json");
    }

    @Test
    @DisplayName("P2-4: resolveCredentialsFile ném ngoại lệ khi file không tồn tại")
    void testResolveCredentialsFile_missingFile() {
        GmailEmailAdapter adapter = new GmailEmailAdapter(
                "non-existent-dir/missing.json",
                "tokens",
                8889,
                "http://localhost:5173/reset-password"
        );

        assertThatThrownBy(() -> adapter.resolveCredentialsFile("non-existent-dir/missing.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Không tìm thấy file credentials");
    }

    @Test
    @DisplayName("P2-4: resolveTokensDirectory tự động tạo thư mục khi chưa tồn tại (đường dẫn tuyệt đối)")
    void testResolveTokensDirectory_createsDirectoryWhenMissing() {
        Path tokensPath = tempDir.resolve("nested/subfolder/tokens");
        assertThat(Files.exists(tokensPath)).isFalse();

        GmailEmailAdapter adapter = new GmailEmailAdapter(
                "backend/credentials/credentials.json",
                tokensPath.toAbsolutePath().toString(),
                8889,
                "http://localhost:5173/reset-password"
        );

        File resolved = adapter.resolveTokensDirectory(tokensPath.toAbsolutePath().toString());
        assertThat(resolved).exists().isDirectory();
        assertThat(resolved.canWrite()).isTrue();
    }

    @Test
    @DisplayName("P2-4: resolveTokensDirectory ném ngoại lệ khi thư mục không có quyền ghi")
    void testResolveTokensDirectory_throwsWhenNotWritable() throws IOException {
        Path readOnlyDir = Files.createDirectory(tempDir.resolve("readonly-tokens"));
        File dirFile = readOnlyDir.toFile();
        boolean setReadOnly = dirFile.setReadOnly();

        if (setReadOnly && !dirFile.canWrite()) {
            GmailEmailAdapter adapter = new GmailEmailAdapter(
                    "backend/credentials/credentials.json",
                    readOnlyDir.toAbsolutePath().toString(),
                    8889,
                    "http://localhost:5173/reset-password"
            );

            try {
                assertThatThrownBy(() -> adapter.resolveTokensDirectory(readOnlyDir.toAbsolutePath().toString()))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("không có quyền ghi");
            } finally {
                dirFile.setWritable(true);
            }
        }
    }

    @Test
    @DisplayName("P1-2: Chế độ Headless: Ném IllegalStateException khi thiếu token và không mở trình duyệt")
    void testHeadlessMode_throwsIllegalStateExceptionWhenTokensMissing() throws IOException {
        // Tạo file credentials JSON tối thiểu hợp lệ
        String minimalClientSecrets = """
                {
                  "installed": {
                    "client_id": "test-client-id.apps.googleusercontent.com",
                    "client_secret": "test-client-secret",
                    "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                    "token_uri": "https://oauth2.googleapis.com/token"
                  }
                }
                """;
        Path credFile = Files.createFile(tempDir.resolve("credentials.json"));
        Files.writeString(credFile, minimalClientSecrets);
        Path emptyTokensDir = Files.createDirectory(tempDir.resolve("empty-tokens"));

        // allowBrowserAuth = false (mặc định cho backend chạy tự động)
        GmailEmailAdapter adapter = new GmailEmailAdapter(
                credFile.toAbsolutePath().toString(),
                emptyTokensDir.toAbsolutePath().toString(),
                8889,
                null,
                false,
                "http://localhost:5173/reset-password"
        );

        assertThatThrownBy(() -> adapter.sendPasswordResetEmail("user@example.com", "user1", "token123", 15))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("refresh token")
                .hasMessageContaining("OAuth browser flow");
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

    @Test
    @DisplayName("P1-2: Bắt và xử lý riêng biệt khi token bị thu hồi hoặc hết hạn (401 Unauthorized)")
    void testSendEmail_handlesRevokedOrExpiredToken() throws Exception {
        Gmail gmailMock = mock(Gmail.class);
        Gmail.Users usersMock = mock(Gmail.Users.class);
        Gmail.Users.Messages messagesMock = mock(Gmail.Users.Messages.class);
        Gmail.Users.Messages.Send sendMock = mock(Gmail.Users.Messages.Send.class);

        GoogleJsonResponseException unauthorizedEx = mock(GoogleJsonResponseException.class);
        when(unauthorizedEx.getStatusCode()).thenReturn(401);
        when(unauthorizedEx.getMessage()).thenReturn("401 Unauthorized - Invalid Credentials");

        when(gmailMock.users()).thenReturn(usersMock);
        when(usersMock.messages()).thenReturn(messagesMock);
        when(messagesMock.send(eq("me"), any(Message.class))).thenReturn(sendMock);
        when(sendMock.execute()).thenThrow(unauthorizedEx);

        GmailEmailAdapter adapter = new GmailEmailAdapter(gmailMock, "http://localhost:5173/reset-password");

        assertThatThrownBy(() -> adapter.sendPasswordResetEmail("recipient@test.com", "nhanvien1", "test-token-abc", 30))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Gmail OAuth xác thực thất bại (401 Unauthorized)");
    }
}
