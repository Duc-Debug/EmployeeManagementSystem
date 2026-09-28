package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.project.DuplicateResourceDemandException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.demand.ProjectResourceDemand;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectResourceDemandJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectResourceDemandRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectResourceDemandRepositoryAdapter Tests")
class ProjectResourceDemandRepositoryAdapterTest {

    @Mock
    private SpringDataProjectResourceDemandRepository repository;

    private ProjectResourceDemandPersistenceMapper mapper;
    private ProjectResourceDemandRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        mapper = new ProjectResourceDemandPersistenceMapper();
        adapter = new ProjectResourceDemandRepositoryAdapter(repository, mapper);
    }

    @Test
    @DisplayName("Lưu danh sách ProjectResourceDemand thành công")
    void testSaveAll_Success() {
        ProjectResourceDemand demand = ProjectResourceDemand.createNew(
                new ProjectId(1L), new ProjectRoleId(4L), YearWeek.of(2026, 41), new BigDecimal("20.00"));

        when(repository.saveAllAndFlush(any())).thenAnswer(invocation -> {
            List<ProjectResourceDemandJpaEntity> list = invocation.getArgument(0);
            list.get(0).setId(10L);
            return list;
        });

        List<ProjectResourceDemand> result = adapter.saveAll(List.of(demand));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        verify(repository).saveAllAndFlush(any());
    }

    @Test
    @DisplayName("Chuyển đổi DataIntegrityViolationException thành DuplicateResourceDemandException khi vi phạm Unique Constraint")
    void testSaveAll_DuplicateConstraintViolation_ThrowsDomainException() {
        ProjectResourceDemand demand = ProjectResourceDemand.createNew(
                new ProjectId(1L), new ProjectRoleId(4L), YearWeek.of(2026, 41), new BigDecimal("20.00"));

        when(repository.saveAllAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key uk_proj_res_demand_proj_role_week"));

        assertThatThrownBy(() -> adapter.saveAll(List.of(demand)))
                .isInstanceOf(DuplicateResourceDemandException.class)
                .hasMessageContaining("Xung đột dữ liệu");
    }

    @Test
    @DisplayName("Xóa danh sách ProjectResourceDemand qua deleteAll")
    void testDeleteAll_Success() {
        ProjectResourceDemand demand = ProjectResourceDemand.createNew(
                new ProjectId(1L), new ProjectRoleId(4L), YearWeek.of(2026, 41), new BigDecimal("20.00"));

        adapter.deleteAll(List.of(demand));

        verify(repository).deleteAll(any());
    }

    @Test
    @DisplayName("Tìm kiếm nhu cầu theo projectId, roleId và yearWeek")
    void testFindByProjectIdAndRoleIdAndYearWeek_Found() {
        ProjectResourceDemandJpaEntity entity = new ProjectResourceDemandJpaEntity(
                10L, 1L, 4L, 2026, 41, new BigDecimal("20.00"), 0L);

        when(repository.findByProjectIdAndRoleIdAndYearAndWeekNumber(1L, 4L, 2026, 41))
                .thenReturn(Optional.of(entity));

        Optional<ProjectResourceDemand> result = adapter.findByProjectIdAndRoleIdAndYearWeek(
                new ProjectId(1L), new ProjectRoleId(4L), YearWeek.of(2026, 41));

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(10L);
        assertThat(result.get().getRequiredHours()).isEqualByComparingTo(new BigDecimal("20.00"));
    }
}