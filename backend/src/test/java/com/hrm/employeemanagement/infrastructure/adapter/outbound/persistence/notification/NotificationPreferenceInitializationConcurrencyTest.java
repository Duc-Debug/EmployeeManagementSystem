package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.hrm.employeemanagement.application.dto.notification.UpdateNotificationPreferenceCommand;
import com.hrm.employeemanagement.application.port.inbound.notification.GetNotificationPreferenceUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.UpdateNotificationPreferenceUseCase;

@SpringBootTest
@ActiveProfiles("test")
class NotificationPreferenceInitializationConcurrencyTest {

    @Autowired
    private GetNotificationPreferenceUseCase getPreferenceUseCase;

    @Autowired
    private UpdateNotificationPreferenceUseCase updatePreferenceUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;
    private Long userId;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        userId = jdbcTemplate.queryForObject("SELECT id FROM users ORDER BY id LIMIT 1", Long.class);
        jdbcTemplate.update("DELETE FROM notification_preferences WHERE user_id = ?", userId);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void concurrentFirstGetsCreateOnePreferenceWithoutFailure() throws Exception {
        runConcurrently(
                () -> getPreferenceUseCase.getMyPreference(userId),
                () -> getPreferenceUseCase.getMyPreference(userId)
        );

        assertEquals(1, preferenceCount());
    }

    @Test
    void concurrentFirstUpdatesCreateOnePreferenceWithoutFailure() throws Exception {
        UpdateNotificationPreferenceCommand enableInApp = command(true, null);
        UpdateNotificationPreferenceCommand enableEmail = command(null, true);

        runConcurrently(
                () -> updatePreferenceUseCase.updateMyPreference(userId, enableInApp),
                () -> updatePreferenceUseCase.updateMyPreference(userId, enableEmail)
        );

        assertEquals(1, preferenceCount());
    }

    private void runConcurrently(Runnable first, Runnable second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<?>> futures = List.of(
                executor.submit(awaitStart(ready, start, first)),
                executor.submit(awaitStart(ready, start, second))
        );

        ready.await();
        start.countDown();
        for (Future<?> future : futures) {
            assertDoesNotThrow(() -> future.get());
        }
    }

    private Runnable awaitStart(CountDownLatch ready, CountDownLatch start, Runnable action) {
        return () -> {
            ready.countDown();
            try {
                start.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            action.run();
        };
    }

    private int preferenceCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_preferences WHERE user_id = ?",
                Integer.class,
                userId
        );
        return count != null ? count : 0;
    }

    private UpdateNotificationPreferenceCommand command(Boolean inAppEnabled, Boolean emailEnabled) {
        return new UpdateNotificationPreferenceCommand(
                inAppEnabled, emailEnabled, null, null, null, null, null,
                null, null, null, null, null, null
        );
    }
}
