package com.hrm.employeemanagement.infrastructure.adapter.outbound.email;

import com.hrm.employeemanagement.application.port.outbound.email.SimulatedEmailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Infrastructure Adapter for Simulated Email Service.
 * Restricted to dev/local/test profiles to prevent credential logging in production.
 */
@Component
@Profile({"dev", "local", "test", "default"})
public class ConsoleSimulatedEmailAdapter implements SimulatedEmailPort {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSimulatedEmailAdapter.class);

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String username, String resetToken, long validityMinutes) {
        String resetUrl = "http://localhost:8080/api/v1/auth/reset-password?token=" + resetToken;

        String emailBody = String.format("""
                ======================= [SIMULATED EMAIL SENDER] =======================
                Thời gian gửi: %s
                Gửi tới      : %s (Tài khoản: %s)
                Tiêu đề      : [Employee Management System] Yêu cầu khôi phục mật khẩu
                ------------------------------------------------------------------------
                Xin chào %s,
                
                Hệ thống nhận được yêu cầu khôi phục mật khẩu cho tài khoản của bạn.
                Vui lòng sử dụng đường dẫn bên dưới hoặc gửi mã Token sau để hoàn tất:
                
                Mã Token khôi phục: %s
                Đường dẫn khôi phục: %s
                
                Lưu ý: Mã này chỉ có hiệu lực trong vòng %d phút và chỉ được sử dụng 01 lần.
                Nếu bạn không gửi yêu cầu này, vui lòng bỏ qua email này.
                ========================================================================
                """, LocalDateTime.now(), recipientEmail, username, username, resetToken, resetUrl, validityMinutes);

        log.info("\n{}", emailBody);
        System.out.println(emailBody);
    }
}
