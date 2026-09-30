package com.hrm.employeemanagement.infrastructure.adapter.outbound.email;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.hrm.employeemanagement.application.port.outbound.email.EmailSenderPort;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Infrastructure Adapter sending password reset emails via Gmail API with OAuth 2.0.
 * Active exclusively under the 'gmail' Spring Profile.
 * Supports headless execution by loading pre-authorized credentials or configured refresh token.
 */
@Component
@Profile("gmail")
public class GmailEmailAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(GmailEmailAdapter.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "Employee Management System";
    private static final String USER_ID = "user";

    private final String credentialsPath;
    private final String tokensDirectoryPath;
    private final int oauthPort;
    private final String configuredRefreshToken;
    private final boolean allowBrowserAuth;
    private final String resetPasswordBaseUrl;

    private Gmail gmailService;

    @Autowired
    public GmailEmailAdapter(
            @Value("${app.gmail.credentials-path:backend/credentials/credentials.json}") String credentialsPath,
            @Value("${app.gmail.tokens-directory-path:backend/tokens}") String tokensDirectoryPath,
            @Value("${app.gmail.oauth-port:8889}") int oauthPort,
            @Value("${app.gmail.refresh-token:}") String configuredRefreshToken,
            @Value("${app.gmail.allow-browser-auth:false}") boolean allowBrowserAuth,
            @Value("${app.auth.reset-password-base-url:http://localhost:5173/reset-password}") String resetPasswordBaseUrl) {
        this.credentialsPath = credentialsPath;
        this.tokensDirectoryPath = tokensDirectoryPath;
        this.oauthPort = oauthPort;
        this.configuredRefreshToken = configuredRefreshToken;
        this.allowBrowserAuth = allowBrowserAuth;
        this.resetPasswordBaseUrl = resetPasswordBaseUrl;
    }

    public GmailEmailAdapter(
            String credentialsPath,
            String tokensDirectoryPath,
            int oauthPort,
            String resetPasswordBaseUrl) {
        this(credentialsPath, tokensDirectoryPath, oauthPort, null, false, resetPasswordBaseUrl);
    }

    /**
     * Package-private constructor for unit testing with mocked Gmail client.
     */
    GmailEmailAdapter(Gmail gmailService, String resetPasswordBaseUrl) {
        this.credentialsPath = null;
        this.tokensDirectoryPath = null;
        this.oauthPort = 8888;
        this.configuredRefreshToken = null;
        this.allowBrowserAuth = false;
        this.resetPasswordBaseUrl = resetPasswordBaseUrl;
        this.gmailService = gmailService;
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String username, String resetToken, long validityMinutes) {
        try {
            Gmail service = getOrCreateGmailService();
            MimeMessage mimeMessage = createMimeMessage(recipientEmail, username, resetToken, validityMinutes);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            mimeMessage.writeTo(buffer);
            byte[] rawBytes = buffer.toByteArray();
            String encodedEmail = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);

            Message gmailMessage = new Message();
            gmailMessage.setRaw(encodedEmail);

            Message response = service.users().messages().send("me", gmailMessage).execute();
            log.info("Gmail API successfully sent password reset email to {} (Message ID: {})", recipientEmail, response.getId());
        } catch (TokenResponseException ex) {
            log.error("Gmail OAuth token không hợp lệ hoặc đã bị thu hồi: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Gmail OAuth token đã hết hạn hoặc bị thu hồi (invalid_grant). Cần cấp quyền lại: " + ex.getMessage(), ex);
        } catch (GoogleJsonResponseException ex) {
            if (ex.getStatusCode() == 401) {
                log.error("Gmail API trả về 401 Unauthorized - OAuth token không hợp lệ hoặc đã bị thu hồi.", ex);
                throw new IllegalStateException("Gmail OAuth xác thực thất bại (401 Unauthorized). Vui lòng cấp quyền lại: " + ex.getMessage(), ex);
            }
            log.error("Lỗi Google API khi gửi email tới {}: {}", recipientEmail, ex.getMessage(), ex);
            throw new RuntimeException("Lỗi gửi email qua Gmail API: " + ex.getMessage(), ex);
        } catch (MessagingException | IOException | GeneralSecurityException ex) {
            log.error("Failed to send password reset email via Gmail API to recipient: {}", recipientEmail, ex);
            throw new RuntimeException("Lỗi gửi email qua Gmail API: " + ex.getMessage(), ex);
        }
    }

    private synchronized Gmail getOrCreateGmailService() throws GeneralSecurityException, IOException {
        if (this.gmailService != null) {
            return this.gmailService;
        }

        File credentialsFile = resolveCredentialsFile(this.credentialsPath);
        File tokensDir = resolveTokensDirectory(this.tokensDirectoryPath);

        log.info("Loading Gmail API OAuth 2.0 credentials from: {}", credentialsFile.getAbsolutePath());

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleClientSecrets clientSecrets;
        try (InputStream in = new FileInputStream(credentialsFile)) {
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in, StandardCharsets.UTF_8));
        }

        Credential credential = null;

        // 1. Cấu hình refresh token trực tiếp qua biến môi trường / Secret Manager
        if (this.configuredRefreshToken != null && !this.configuredRefreshToken.isBlank()) {
            log.info("Using configured refresh token from environment/properties.");
            credential = new GoogleCredential.Builder()
                    .setTransport(httpTransport)
                    .setJsonFactory(JSON_FACTORY)
                    .setClientSecrets(clientSecrets)
                    .build()
                    .setRefreshToken(this.configuredRefreshToken);
        } else {
            // 2. Nạp thông tin xác thực đã được cấp quyền trước từ DataStore trong thư mục tokens
            FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(tokensDir);
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport,
                    JSON_FACTORY,
                    clientSecrets,
                    Collections.singletonList(GmailScopes.GMAIL_SEND))
                    .setDataStoreFactory(dataStoreFactory)
                    .setAccessType("offline")
                    .build();

            credential = flow.loadCredential(USER_ID);

            // 3. Nếu chưa có thông tin xác thực trong tokens directory
            if (credential == null || (credential.getRefreshToken() == null && credential.getAccessToken() == null)) {
                if (this.allowBrowserAuth) {
                    LocalServerReceiver receiver = this.oauthPort > 0
                            ? new LocalServerReceiver.Builder().setPort(this.oauthPort).build()
                            : new LocalServerReceiver.Builder().build();
                    log.info("Interactive OAuth mode enabled: Starting local authorization server on port {}...",
                            this.oauthPort > 0 ? this.oauthPort : "dynamic");
                    credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize(USER_ID);
                } else {
                    throw new IllegalStateException(
                            "Không tìm thấy thông tin xác thực Gmail OAuth 2.0 đã được cấp quyền (refresh token) tại: "
                                    + tokensDir.getAbsolutePath()
                                    + ". Backend đang chạy trong môi trường tự động/không có trình duyệt và không tự mở OAuth browser flow. "
                                    + "Vui lòng hoàn tất cấp quyền trước (pre-authorize) hoặc cấu hình GMAIL_REFRESH_TOKEN."
                    );
                }
            }
        }

        // 4. Kiểm tra làm mới token và phát hiện token hết hạn / bị thu hồi
        try {
            Long expiresIn = credential.getExpiresInSeconds();
            if (credential.getAccessToken() == null || (expiresIn != null && expiresIn <= 60)) {
                boolean refreshed = credential.refreshToken();
                if (!refreshed && credential.getAccessToken() == null) {
                    throw new IllegalStateException("Không thể làm mới access token Gmail OAuth: Refresh token không hợp lệ.");
                }
            }
        } catch (TokenResponseException ex) {
            if (ex.getDetails() != null && "invalid_grant".equals(ex.getDetails().getError())) {
                log.error("Gmail OAuth refresh token đã bị thu hồi hoặc hết hạn (invalid_grant). Cần cấp quyền lại OAuth.", ex);
                throw new IllegalStateException("Gmail OAuth refresh token đã bị thu hồi hoặc hết hạn. Vui lòng cấp quyền lại: " + ex.getMessage(), ex);
            }
            throw new RuntimeException("Lỗi xác thực Gmail OAuth khi làm mới token: " + ex.getMessage(), ex);
        }

        this.gmailService = new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        log.info("Gmail API service successfully initialized.");
        return this.gmailService;
    }

    public File resolveCredentialsFile(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Đường dẫn credentials không được để trống.");
        }
        File file = new File(path);
        if (!file.isAbsolute()) {
            if (!file.exists()) {
                File backendPrefixed = new File("backend", path);
                if (backendPrefixed.exists()) {
                    file = backendPrefixed;
                } else if (path.startsWith("backend/") || path.startsWith("backend\\")) {
                    File stripped = new File(path.substring(8));
                    if (stripped.exists()) {
                        file = stripped;
                    }
                }
            }
        }
        if (!file.exists()) {
            throw new IllegalStateException("Không tìm thấy file credentials tại: " + path
                    + " (Đường dẫn tuyệt đối: " + file.getAbsolutePath() + "). Vui lòng kiểm tra lại cấu hình credentials.");
        }
        if (!file.isFile()) {
            throw new IllegalStateException("Đường dẫn credentials không phải là file: " + file.getAbsolutePath());
        }
        return file;
    }

    public File resolveTokensDirectory(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Đường dẫn thư mục tokens không được để trống.");
        }
        File dir = new File(path);
        if (!dir.isAbsolute()) {
            if (!dir.exists()) {
                File backendPrefixed = new File("backend", path);
                if (backendPrefixed.exists()) {
                    dir = backendPrefixed;
                } else if (path.startsWith("backend/") || path.startsWith("backend\\")) {
                    File stripped = new File(path.substring(8));
                    if (stripped.exists()) {
                        dir = stripped;
                    }
                }
            }
        }
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created && !dir.exists()) {
                throw new IllegalStateException("Không thể tạo thư mục lưu trữ tokens tại: " + dir.getAbsolutePath());
            }
        }
        if (!dir.isDirectory()) {
            throw new IllegalStateException("Đường dẫn tokens không phải là thư mục: " + dir.getAbsolutePath());
        }
        if (!dir.canWrite()) {
            throw new IllegalStateException("Thư mục tokens không có quyền ghi: " + dir.getAbsolutePath());
        }
        return dir;
    }

    private MimeMessage createMimeMessage(String recipientEmail, String username, String resetToken, long validityMinutes)
            throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage message = new MimeMessage(session);
        message.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(recipientEmail));
        message.setSubject("[Employee Management System] Yêu cầu khôi phục mật khẩu", StandardCharsets.UTF_8.name());

        String resetUrl = resetPasswordBaseUrl + (resetPasswordBaseUrl.contains("?") ? "&token=" : "?token=") + resetToken;
        String content = String.format("""
                Xin chào %s,

                Hệ thống nhận được yêu cầu khôi phục mật khẩu cho tài khoản của bạn.
                Vui lòng truy cập đường dẫn sau hoặc sử dụng mã Token bên dưới để hoàn tất:

                Mã Token khôi phục: %s
                Đường dẫn khôi phục: %s

                Lưu ý: Mã này chỉ có hiệu lực trong vòng %d phút và chỉ được sử dụng 01 lần.
                Nếu bạn không gửi yêu cầu này, vui lòng bỏ qua email này.
                """, username, resetToken, resetUrl, validityMinutes);

        message.setText(content, StandardCharsets.UTF_8.name());
        return message;
    }

    public String getCredentialsPath() {
        return credentialsPath;
    }

    public String getTokensDirectoryPath() {
        return tokensDirectoryPath;
    }

    public int getOauthPort() {
        return oauthPort;
    }

    public String getConfiguredRefreshToken() {
        return configuredRefreshToken;
    }

    public boolean isAllowBrowserAuth() {
        return allowBrowserAuth;
    }

    public String getResetPasswordBaseUrl() {
        return resetPasswordBaseUrl;
    }
}
