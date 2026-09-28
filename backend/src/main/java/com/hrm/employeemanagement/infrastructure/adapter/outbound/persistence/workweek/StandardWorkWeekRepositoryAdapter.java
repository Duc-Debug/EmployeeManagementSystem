package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.workweek;

import com.hrm.employeemanagement.application.port.outbound.workweek.LoadStandardWorkWeekPort;
import com.hrm.employeemanagement.application.port.outbound.workweek.SaveStandardWorkWeekPort;
import com.hrm.employeemanagement.domain.workweek.StandardWorkWeekConfig;
import com.hrm.employeemanagement.domain.workweek.WorkWeekScope;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.workweek.entity.StandardWorkWeekConfigJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.workweek.repository.SpringDataStandardWorkWeekConfigRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class StandardWorkWeekRepositoryAdapter implements LoadStandardWorkWeekPort, SaveStandardWorkWeekPort {

    private final SpringDataStandardWorkWeekConfigRepository repository;
    private final StandardWorkWeekPersistenceMapper mapper;

    public StandardWorkWeekRepositoryAdapter(
            SpringDataStandardWorkWeekConfigRepository repository,
            StandardWorkWeekPersistenceMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "SpringDataStandardWorkWeekConfigRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "StandardWorkWeekPersistenceMapper must not be null");
    }

    @Override
    public Optional<StandardWorkWeekConfig> findByScope(WorkWeekScope scope) {
        if (scope == null) return Optional.empty();
        return repository.findByScopeKeyWithDays(scope.toScopeKey())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<StandardWorkWeekConfig> findCompanyDefault() {
        return repository.findCompanyDefaultWithDays()
                .map(mapper::toDomain);
    }

    @Override
    public StandardWorkWeekConfig save(StandardWorkWeekConfig config) {
        Objects.requireNonNull(config, "StandardWorkWeekConfig must not be null");

        StandardWorkWeekConfigJpaEntity existing = null;
        if (config.getId() != null) {
            existing = repository.findById(config.getId()).orElse(null);
        } else {
            existing = repository.findByScopeKeyWithDays(config.getScope().toScopeKey()).orElse(null);
        }

        StandardWorkWeekConfigJpaEntity toSave = mapper.toJpaEntity(config, existing);
        // Flush before mapping so the response carries the incremented @Version value.
        StandardWorkWeekConfigJpaEntity saved = repository.saveAndFlush(toSave);
        return mapper.toDomain(saved);
    }
}

