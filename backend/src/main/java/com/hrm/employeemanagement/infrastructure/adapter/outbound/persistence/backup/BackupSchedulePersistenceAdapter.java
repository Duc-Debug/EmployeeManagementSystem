package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup;

import com.hrm.employeemanagement.application.port.outbound.BackupScheduleRepositoryPort;
import com.hrm.employeemanagement.domain.backup.BackupSchedule;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup.entity.BackupScheduleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup.repository.SpringDataBackupScheduleRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BackupSchedulePersistenceAdapter implements BackupScheduleRepositoryPort {

    private final SpringDataBackupScheduleRepository scheduleRepository;

    public BackupSchedulePersistenceAdapter(SpringDataBackupScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public BackupSchedule save(BackupSchedule schedule) {
        BackupScheduleJpaEntity entity = BackupScheduleJpaEntity.fromDomain(schedule);
        BackupScheduleJpaEntity saved = scheduleRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<BackupSchedule> findSchedule() {
        return scheduleRepository.findById(1L).map(BackupScheduleJpaEntity::toDomain);
    }
}
