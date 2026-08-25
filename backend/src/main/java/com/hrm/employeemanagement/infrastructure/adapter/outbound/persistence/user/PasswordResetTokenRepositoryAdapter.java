package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import com.hrm.employeemanagement.application.port.outbound.user.LoadPasswordResetTokenPort;
import com.hrm.employeemanagement.application.port.outbound.user.SavePasswordResetTokenPort;
import com.hrm.employeemanagement.domain.user.PasswordResetToken;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.PasswordResetTokenJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataPasswordResetTokenRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PasswordResetTokenRepositoryAdapter implements LoadPasswordResetTokenPort, SavePasswordResetTokenPort {

    private final SpringDataPasswordResetTokenRepository repository;

    public PasswordResetTokenRepositoryAdapter(SpringDataPasswordResetTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken domain) {
        PasswordResetTokenJpaEntity entity;
        if (domain.getId() != null) {
            entity = repository.findById(domain.getId())
                    .orElseGet(() -> toJpaEntity(domain));
            entity.setUsed(domain.isUsed());
        } else {
            entity = toJpaEntity(domain);
        }

        PasswordResetTokenJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void invalidateActiveTokensByUserId(UserId userId) {
        if (userId != null && userId.value() != null) {
            repository.invalidateActiveTokensByUserId(userId.value());
        }
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHashWithLock(tokenHash)
                .map(this::toDomain);
    }

    private PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity) {
        if (entity == null) return null;
        return new PasswordResetToken(
                entity.getId(),
                new UserId(entity.getUserId()),
                entity.getTokenHash(),
                entity.getExpiryDate(),
                Boolean.TRUE.equals(entity.getUsed()),
                entity.getCreatedAt()
        );
    }

    private PasswordResetTokenJpaEntity toJpaEntity(PasswordResetToken domain) {
        if (domain == null) return null;
        return new PasswordResetTokenJpaEntity(
                domain.getId(),
                domain.getUserIdValue(),
                domain.getTokenHash(),
                domain.getExpiryDate(),
                domain.isUsed(),
                domain.getCreatedAt()
        );
    }
}
