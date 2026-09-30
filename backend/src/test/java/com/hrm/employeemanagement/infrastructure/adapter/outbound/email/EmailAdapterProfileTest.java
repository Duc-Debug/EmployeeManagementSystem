package com.hrm.employeemanagement.infrastructure.adapter.outbound.email;

import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

import com.hrm.employeemanagement.application.port.outbound.email.EmailSenderPort;

@DisplayName("Email Adapter Profile & Bean Selection Tests")
class EmailAdapterProfileTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    ConsoleSimulatedEmailAdapter.class,
                    GmailEmailAdapter.class,
                    ProductionEmailAdapter.class
            )
            .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class));

    @Test
    @DisplayName("P1-1: application.properties không được cấu hình spring.profiles.group.local=gmail")
    void verifyApplicationPropertiesDoesNotGroupLocalWithGmail() throws Exception {
        Properties properties = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            assertThat(in).isNotNull();
            properties.load(in);
        }
        assertThat(properties.getProperty("spring.profiles.group.local"))
                .as("Profile local không được tự động gom nhóm hoặc kích hoạt profile gmail")
                .isNull();
    }

    @Test
    @DisplayName("Profile 'local' không có 'gmail': Kích hoạt ConsoleSimulatedEmailAdapter duy nhất")
    void whenProfileLocal_thenOnlyConsoleAdapterIsPresent() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=local",
                        "app.auth.reset-password-base-url=http://localhost:5173/reset-password"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailSenderPort.class);
                    assertThat(context).hasSingleBean(ConsoleSimulatedEmailAdapter.class);
                    assertThat(context).doesNotHaveBean(GmailEmailAdapter.class);
                    assertThat(context).doesNotHaveBean(ProductionEmailAdapter.class);
                });
    }

    @Test
    @DisplayName("Profile 'gmail': Kích hoạt GmailEmailAdapter duy nhất, không duplicate bean")
    void whenProfileGmail_thenOnlyGmailAdapterIsPresent() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=gmail",
                        "app.gmail.credentials-path=backend/credentials/credentials.json",
                        "app.auth.reset-password-base-url=http://localhost:5173/reset-password"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailSenderPort.class);
                    assertThat(context).hasSingleBean(GmailEmailAdapter.class);
                    assertThat(context).doesNotHaveBean(ConsoleSimulatedEmailAdapter.class);
                    assertThat(context).doesNotHaveBean(ProductionEmailAdapter.class);
                });
    }

    @Test
    @DisplayName("Profile 'local,gmail': Kích hoạt GmailEmailAdapter duy nhất, ghi đè simulated console")
    void whenProfileLocalAndGmail_thenOnlyGmailAdapterIsPresent() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=local,gmail",
                        "app.gmail.credentials-path=backend/credentials/credentials.json",
                        "app.auth.reset-password-base-url=http://localhost:5173/reset-password"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailSenderPort.class);
                    assertThat(context).hasSingleBean(GmailEmailAdapter.class);
                    assertThat(context).doesNotHaveBean(ConsoleSimulatedEmailAdapter.class);
                    assertThat(context).doesNotHaveBean(ProductionEmailAdapter.class);
                });
    }

    @Test
    @DisplayName("Profile 'prod': Kích hoạt ProductionEmailAdapter duy nhất")
    void whenProfileProd_thenOnlyProductionAdapterIsPresent() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "app.auth.reset-password-base-url=https://hrm.example.com/reset-password"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailSenderPort.class);
                    assertThat(context).hasSingleBean(ProductionEmailAdapter.class);
                    assertThat(context).doesNotHaveBean(ConsoleSimulatedEmailAdapter.class);
                    assertThat(context).doesNotHaveBean(GmailEmailAdapter.class);
                });
    }
}
