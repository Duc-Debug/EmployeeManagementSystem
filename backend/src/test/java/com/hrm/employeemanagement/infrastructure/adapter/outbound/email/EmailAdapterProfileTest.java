package com.hrm.employeemanagement.infrastructure.adapter.outbound.email;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.hrm.employeemanagement.application.port.outbound.email.EmailSenderPort;

@DisplayName("Email Adapter Profile & Bean Selection Tests")
class EmailAdapterProfileTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ConsoleSimulatedEmailAdapter.class, GmailEmailAdapter.class);

    @Test
    @DisplayName("Profile 'local' không có 'gmail': Kích hoạt ConsoleSimulatedEmailAdapter duy nhất")
    void whenProfileLocal_thenOnlyConsoleAdapterIsPresent() {
        contextRunner
                .withPropertyValues("spring.profiles.active=local")
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailSenderPort.class);
                    assertThat(context).hasSingleBean(ConsoleSimulatedEmailAdapter.class);
                    assertThat(context).doesNotHaveBean(GmailEmailAdapter.class);
                });
    }

    @Test
    @DisplayName("Profile 'gmail': Kích hoạt GmailEmailAdapter duy nhất, không duplicate bean")
    void whenProfileGmail_thenOnlyGmailAdapterIsPresent() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=gmail",
                        "app.gmail.credentials-path=backend/credentials/credentials.json"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailSenderPort.class);
                    assertThat(context).hasSingleBean(GmailEmailAdapter.class);
                    assertThat(context).doesNotHaveBean(ConsoleSimulatedEmailAdapter.class);
                });
    }

    @Test
    @DisplayName("Profile 'local,gmail': Kích hoạt GmailEmailAdapter duy nhất, ghi đè simulated console")
    void whenProfileLocalAndGmail_thenOnlyGmailAdapterIsPresent() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=local,gmail",
                        "app.gmail.credentials-path=backend/credentials/credentials.json"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailSenderPort.class);
                    assertThat(context).hasSingleBean(GmailEmailAdapter.class);
                    assertThat(context).doesNotHaveBean(ConsoleSimulatedEmailAdapter.class);
                });
    }

    @Test
    @DisplayName("Profile 'prod': Không kích hoạt ConsoleSimulatedEmailAdapter lẫn GmailEmailAdapter")
    void whenProfileProd_thenSimulatedAndGmailAdaptersAreAbsent() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ConsoleSimulatedEmailAdapter.class);
                    assertThat(context).doesNotHaveBean(GmailEmailAdapter.class);
                });
    }
}
