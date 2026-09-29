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
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
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
 */
@Component
@Profile("gmail")
public class GmailEmailAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(GmailEmailAdapter.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "Employee Management System";

    private final String credentialsPath;
    private final String tokensDirectoryPath;
    private final int oauthPort;
    private final String resetPasswordBaseUrl;

    private Gmail gmailService;

    @Autowired
    public GmailEmailAdapter(
            @Value("${app.gmail.credentials-path}") String credentialsPath,
            @Value("${app.gmail.tokens-directory-path:tokens}") String tokensDirectoryPath,
            @Value("${app.gmail.oauth-port:8889}") int oauthPort,
            @Value("${app.auth.reset-password-base-url:http://localhost:5173/reset-password}") String resetPasswordBaseUrl) {
        this.credentialsPath = credentialsPath;
        this.tokensDirectoryPath = tokensDirectoryPath;
        this.oauthPort = oauthPort;
        this.resetPasswordBaseUrl = resetPasswordBaseUrl;
    }

    /**
     * Package-private constructor for unit testing with mocked Gmail client.
     */
    GmailEmailAdapter(Gmail gmailService, String resetPasswordBaseUrl) {
        this.credentialsPath = null;
        this.tokensDirectoryPath = null;
        this.oauthPort = 8888;
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
        if (!credentialsFile.exists()) {
            throw new IllegalStateException("Không tìm thấy file credentials tại: " + this.credentialsPath
                    + " (Đường dẫn tuyệt đối: " + credentialsFile.getAbsolutePath() + "). Vui lòng kiểm tra lại cấu hình GMAIL_CREDENTIALS_PATH.");
        }

        log.info("Initializing Gmail API OAuth 2.0 flow using credentials: {}", credentialsFile.getAbsolutePath());

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleClientSecrets clientSecrets;
        try (InputStream in = new FileInputStream(credentialsFile)) {
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in, StandardCharsets.UTF_8));
        }

        File tokensDir = resolveTokensDirectory(this.tokensDirectoryPath);
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                JSON_FACTORY,
                clientSecrets,
                Collections.singletonList(GmailScopes.GMAIL_SEND))
                .setDataStoreFactory(new FileDataStoreFactory(tokensDir))
                .setAccessType("offline")
                .build();

        Credential credential;
        try {
            LocalServerReceiver receiver = this.oauthPort > 0
                    ? new LocalServerReceiver.Builder().setPort(this.oauthPort).build()
                    : new LocalServerReceiver.Builder().build();
            log.info("Starting local OAuth authorization server on port {}...", this.oauthPort > 0 ? this.oauthPort : "dynamic");
            credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        } catch (IOException ex) {
            if (this.oauthPort > 0 && ex.getMessage() != null && ex.getMessage().contains("already in use")) {
                log.warn("Cổng OAuth {} bị chiếm dụng ({}), tự động thử lại với cổng ngẫu nhiên khả dụng...", this.oauthPort, ex.getMessage());
                LocalServerReceiver dynamicReceiver = new LocalServerReceiver.Builder().build();
                credential = new AuthorizationCodeInstalledApp(flow, dynamicReceiver).authorize("user");
            } else {
                throw ex;
            }
        }

        this.gmailService = new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        log.info("Gmail API service successfully initialized.");
        return this.gmailService;
    }

    private File resolveCredentialsFile(String path) {
        if (path == null || path.isBlank()) {
            path = "backend/credentials/credentials.json";
        }
        File directFile = new File(path);
        if (directFile.exists()) {
            return directFile;
        }
        File backendFile = new File("backend", path);
        if (backendFile.exists()) {
            return backendFile;
        }
        if (path.startsWith("backend/") || path.startsWith("backend\\")) {
            File strippedFile = new File(path.substring(8));
            if (strippedFile.exists()) {
                return strippedFile;
            }
        }
        if ("credentials.json".equals(path) || "credentials/credentials.json".equals(path) || "backend/credentials/credentials.json".equals(path)) {
            File[] candidateDirs = new File[] {
                    new File("credentials"),
                    new File("backend/credentials")
            };
            for (File dir : candidateDirs) {
                if (dir.exists() && dir.isDirectory()) {
                    File[] jsonFiles = dir.listFiles((d, name) -> name.endsWith(".json"));
                    if (jsonFiles != null && jsonFiles.length > 0) {
                        return jsonFiles[0];
                    }
                }
            }
        }
        return directFile;
    }

    private File resolveTokensDirectory(String path) {
        if (path == null || path.isBlank()) {
            path = "tokens";
        }
        File directDir = new File(path);
        if ((path.startsWith("backend/") || path.startsWith("backend\\")) && new File("src").exists()) {
            return new File(path.substring(8));
        }
        if (directDir.isAbsolute()) {
            return directDir;
        }
        if (new File("backend").isDirectory() && !path.startsWith("backend")) {
            return new File("backend", path);
        }
        return directDir;
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

    public String getResetPasswordBaseUrl() {
        return resetPasswordBaseUrl;
    }
}
