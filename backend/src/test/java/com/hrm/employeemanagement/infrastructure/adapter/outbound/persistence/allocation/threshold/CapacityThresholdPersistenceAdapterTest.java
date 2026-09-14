package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.threshold;

import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.domain.exception.allocation.CapacityThresholdVersionConflictException;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.threshold.entity.CapacityThresholdConfigJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.threshold.repository.SpringDataCapacityThresholdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CapacityThresholdPersistenceAdapter Test")
class CapacityThresholdPersistenceAdapterTest {

    @Mock
    private SpringDataCapacityThresholdRepository repository;

    @Mock
    private CapacityThresholdPersistenceMapper mapper;

    private CapacityThresholdPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CapacityThresholdPersistenceAdapter(repository, mapper);
    }

    private CapacityThresholdConfig createConfig(Long id, Long version) {
        return new CapacityThresholdConfig(
                id,
                CapacityThresholdScope.COMPANY,
                "COMPANY",
                null,
                new BigDecimal("100.0"),
                new BigDecimal("50.0"),
                version,
                1L,
                LocalDateTime.now(),
                1L,
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("findByScope: Tra ve cau hinh nguong khi repository tim thay")
    void findByScope_success() {
        CapacityThresholdConfigJpaEntity entity = new CapacityThresholdConfigJpaEntity();
        CapacityThresholdConfig domain = createConfig(1L, 0L);

        when(repository.findByScopeTypeAndOrgUnitId(CapacityThresholdScope.COMPANY, null))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<CapacityThresholdConfig> result = adapter.findByScope(CapacityThresholdScope.COMPANY, null);

        assertThat(result).isPresent();
        assertThat(result.get().getScopeKey()).isEqualTo("COMPANY");
    }

    @Test
    @DisplayName("save: Nem ngoai le khi phien ban gui len khong khop phien ban DB (pre-check)")
    void save_versionMismatchPreCheck_throwsException() {
        CapacityThresholdConfig config = createConfig(1L, 1L);

        CapacityThresholdConfigJpaEntity entityInDb = new CapacityThresholdConfigJpaEntity();
        entityInDb.setVersion(2L); // DB da nhay len version 2

        when(repository.findById(1L)).thenReturn(Optional.of(entityInDb));

        assertThatThrownBy(() -> adapter.save(config))
                .isInstanceOf(CapacityThresholdVersionConflictException.class)
                .hasMessageContaining("Xung đột phiên bản");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("save: Chuyen doi OptimisticLockingFailureException tu JPA sang CapacityThresholdVersionConflictException")
    void save_optimisticLockingFailure_throwsDomainException() {
        CapacityThresholdConfig config = createConfig(1L, 1L);

        CapacityThresholdConfigJpaEntity entityInDb = new CapacityThresholdConfigJpaEntity();
        entityInDb.setVersion(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(entityInDb));
        when(repository.saveAndFlush(any())).thenThrow(new OptimisticLockingFailureException("Stale object state"));

        assertThatThrownBy(() -> adapter.save(config))
                .isInstanceOf(CapacityThresholdVersionConflictException.class)
                .hasMessageContaining("Xung đột phiên bản dữ liệu");
    }

    @Test
    @DisplayName("save: Luu thanh cong khi phien ban hop le")
    void save_success() {
        CapacityThresholdConfig config = createConfig(1L, 1L);

        CapacityThresholdConfigJpaEntity entityInDb = new CapacityThresholdConfigJpaEntity();
        entityInDb.setVersion(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(entityInDb));
        when(repository.saveAndFlush(entityInDb)).thenReturn(entityInDb);
        when(mapper.toDomain(entityInDb)).thenReturn(config);

        CapacityThresholdConfig saved = adapter.save(config);

        assertThat(saved).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(1L);
        verify(mapper).updateJpaEntity(entityInDb, config);
        verify(repository).saveAndFlush(entityInDb);
    }
}
