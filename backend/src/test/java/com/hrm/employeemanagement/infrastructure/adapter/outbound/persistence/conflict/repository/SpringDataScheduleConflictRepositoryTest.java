package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.repository;

import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.entity.ScheduleConflictJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SpringDataScheduleConflictRepositoryTest {

    @Autowired
    private SpringDataScheduleConflictRepository repository;

    @Autowired
    private com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository employeeRepository;

    private Long createTestEmployee(String code, String name) {
        var emp = new com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity(
                null, null, null, code, name, "Dev", LocalDate.of(2025, 1, 1), null, false, 40, "ACTIVE", 0L
        );
        return employeeRepository.saveAndFlush(emp).getId();
    }

    @Test
    @DisplayName("Cross-year: 2026-W52 -> 2027-W02 phải lấy đầy đủ conflict trong range")
    void shouldFindConflictsAcrossIsoYearBoundary() {
        // given
        Long employeeId = createTestEmployee("EMP_TEST_101", "Nguyễn Văn A");

        persistConflict(employeeId, 2026, 52);
        persistConflict(employeeId, 2026, 53);
        persistConflict(employeeId, 2027, 1);
        persistConflict(employeeId, 2027, 2);

        persistConflict(employeeId, 2027, 3); // ngoài range

        // when
        List<ScheduleConflictJpaEntity> result =
                repository.findUnresolvedConflictsForEmployees(
                        List.of(employeeId),
                        ScheduleConflictStatus.RESOLVED,
                        2026,
                        52,
                        2027,
                        2
                );

        // then
        assertThat(result)
                .extracting(
                        ScheduleConflictJpaEntity::getYearNumber,
                        ScheduleConflictJpaEntity::getWeekNumber
                )
                .containsExactly(
                        tuple(2026, 52),
                        tuple(2026, 53),
                        tuple(2027, 1),
                        tuple(2027, 2)
                );
    }

    @Test
    @DisplayName("Không lấy conflict ngoài range được chọn")
    void shouldNotReturnConflictOutsideSelectedRange() {
        Long employeeId = createTestEmployee("EMP_TEST_102", "Trần Thị B");

        persistConflict(employeeId, 2026, 51);
        persistConflict(employeeId, 2026, 52);
        persistConflict(employeeId, 2027, 2);
        persistConflict(employeeId, 2027, 3);

        List<ScheduleConflictJpaEntity> result =
                repository.findUnresolvedConflictsForEmployees(
                        List.of(employeeId),
                        ScheduleConflictStatus.RESOLVED,
                        2026,
                        52,
                        2027,
                        2
                );

        assertThat(result)
                .extracting(
                        ScheduleConflictJpaEntity::getYearNumber,
                        ScheduleConflictJpaEntity::getWeekNumber
                )
                .containsExactly(
                        tuple(2026, 52),
                        tuple(2027, 2)
                );
    }

    @Test
    @DisplayName("Conflict có status RESOLVED không được lấy")
    void shouldNotReturnResolvedConflicts() {
        Long employeeId = createTestEmployee("EMP_TEST_103", "Lê Văn C");

        persistConflict(employeeId, 2027, 1, ScheduleConflictStatus.RESOLVED);
        persistConflict(employeeId, 2027, 2, ScheduleConflictStatus.OPEN);

        List<ScheduleConflictJpaEntity> result =
                repository.findUnresolvedConflictsForEmployees(
                        List.of(employeeId),
                        ScheduleConflictStatus.RESOLVED,
                        2027,
                        1,
                        2027,
                        2
                );

        assertThat(result)
                .extracting(
                        ScheduleConflictJpaEntity::getYearNumber,
                        ScheduleConflictJpaEntity::getWeekNumber,
                        ScheduleConflictJpaEntity::getStatus
                )
                .containsExactly(
                        tuple(2027, 2, ScheduleConflictStatus.OPEN)
                );

        assertThat(result)
                .noneMatch(x ->
                        x.getYearNumber().equals(2027)
                                && x.getWeekNumber().equals(1)
                                && x.getStatus() == ScheduleConflictStatus.RESOLVED
                );
    }

    @Test
    @DisplayName("Không lấy conflict của employee khác ngoài danh sách")
    void shouldNotReturnConflictsOfOtherEmployees() {
        Long emp1 = createTestEmployee("EMP_TEST_104", "Phạm Văn D");
        Long emp2 = createTestEmployee("EMP_TEST_105", "Hoàng Thị E");

        persistConflict(emp1, 2026, 52);
        persistConflict(emp2, 2026, 52);

        List<ScheduleConflictJpaEntity> result =
                repository.findUnresolvedConflictsForEmployees(
                        List.of(emp1),
                        ScheduleConflictStatus.RESOLVED,
                        2026,
                        52,
                        2026,
                        52
                );

        assertThat(result)
                .extracting(ScheduleConflictJpaEntity::getEmployeeId)
                .containsExactly(emp1);
    }

    private ScheduleConflictJpaEntity persistConflict(Long employeeId, int yearNumber, int weekNumber) {
        return persistConflict(employeeId, yearNumber, weekNumber, ScheduleConflictStatus.OPEN);
    }

    private ScheduleConflictJpaEntity persistConflict(
            Long employeeId,
            int yearNumber,
            int weekNumber,
            ScheduleConflictStatus status
    ) {
        ScheduleConflictJpaEntity entity = new ScheduleConflictJpaEntity();
        entity.setEmployeeId(employeeId);
        entity.setYearNumber(yearNumber);
        entity.setWeekNumber(weekNumber);
        entity.setConflictType(ConflictType.MULTI_PROJECT_ALLOCATION);
        entity.setTotalAllocatedHours(BigDecimal.valueOf(50.0));
        entity.setNetAvailableHours(BigDecimal.valueOf(40.0));
        entity.setExcessHours(BigDecimal.valueOf(10.0));
        entity.setStatus(status);
        entity.setDetails("Test conflict");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setVersion(0L);

        return repository.saveAndFlush(entity);
    }
}
