package com.hrm.employeemanagement.infrastructure.adapter.outbound.email;

import com.hrm.employeemanagement.application.port.outbound.email.SimulatedEmailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Production Email Adapter implementing SimulatedEmailPort/EmailPort.
 * Dispatches password reset emails securely via production JavaMailSender without logging plaintext reset tokens.
 * Fails fast during Spring startup if JavaMailSender or reset base URL configuration is missing in production.
 */
@Component
@Profile("prod")
public class ProductionEmailAdapter implements SimulatedEmailPort {

    private static final Logger log = LoggerFactory.getLogger(ProductionEmailAdapter.class);

    private final JavaMailSender mailSender;
    private final String resetPasswordBaseUrl;

    public ProductionEmailAdapter(JavaMailSender mailSender,
                                  @Value("${app.auth.reset-password-base-url}") String resetPasswordBaseUrl) {
        this.mailSender = mailSender;
        this.resetPasswordBaseUrl = resetPasswordBaseUrl;
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String username, String resetToken, long validityMinutes) {
        log.info("Dispatching password reset email to recipient: {} (User: {})", recipientEmail, username);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject("[Employee Management System] Yêu cầu khôi phục mật khẩu");
        
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

        message.setText(content);

        try {
            mailSender.send(message);
            log.info("Successfully dispatched password reset email to recipient: {}", recipientEmail);
        } catch (Exception ex) {
            log.error("Failed to send email to recipient: {}", recipientEmail, ex);
            throw new IllegalStateException("Gửi email khôi phục mật khẩu thất bại. Vui lòng thử lại sau.", ex);
        }
    }
}
