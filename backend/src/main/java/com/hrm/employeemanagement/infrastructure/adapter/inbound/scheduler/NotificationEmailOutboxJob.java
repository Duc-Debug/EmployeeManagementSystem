package com.hrm.employeemanagement.infrastructure.adapter.inbound.scheduler;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationEmailOutboxRepository;

@Component
public class NotificationEmailOutboxJob {
    private static final Logger log = LoggerFactory.getLogger(NotificationEmailOutboxJob.class);
    private final SpringDataNotificationEmailOutboxRepository repository;
    private final Clock clock;

    public NotificationEmailOutboxJob(SpringDataNotificationEmailOutboxRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.notification.email-outbox-delay-ms:60000}")
    @Transactional
    public void deliverDueEmails() {
        LocalDateTime now = LocalDateTime.now(clock);
        for (var email : repository
                .findTop100ByDeliveredAtIsNullAndAvailableAtLessThanEqualOrderByAvailableAtAsc(now)) {
            log.info("[SIMULATED NOTIFICATION EMAIL] To: {} ({}) | Subject: {} | Body: {}",
                    email.getRecipientName(), email.getRecipientEmail(), email.getSubject(), email.getBody());
            email.markDelivered(now);
            repository.save(email);
        }
    }
}
