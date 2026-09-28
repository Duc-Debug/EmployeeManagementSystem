package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.report.excel;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ProjectAllocationReportExcelPersistenceAdapter Tests")
class ProjectAllocationReportExcelPersistenceAdapterTest {

    private EntityManager entityManager;
    private ProjectAllocationReportExcelPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        adapter = new ProjectAllocationReportExcelPersistenceAdapter(entityManager);
    }

    @Test
    @DisplayName("Nạp toàn bộ allocations của project theo thứ tự năm và tuần")
    void testLoadAllAllocationsForProject_Success() {
        WeeklyProjectAllocationJpaEntity entity1 = new WeeklyProjectAllocationJpaEntity(
                1L, 100L, 10L, 2024, 15, BigDecimal.valueOf(40), BigDecimal.valueOf(100),
                false, null, null, null, null, 1L, 0L
        );
        WeeklyProjectAllocationJpaEntity entity2 = new WeeklyProjectAllocationJpaEntity(
                2L, 101L, 10L, 2026, 2, BigDecimal.valueOf(20), BigDecimal.valueOf(50),
                false, null, null, null, null, 1L, 0L
        );

        @SuppressWarnings("unchecked")
        TypedQuery<WeeklyProjectAllocationJpaEntity> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(WeeklyProjectAllocationJpaEntity.class))).thenReturn(query);
        when(query.setParameter("projectId", 10L)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(entity1, entity2));

        List<WeeklyProjectAllocation> allocations = adapter.loadAllAllocationsForProject(10L);

        assertNotNull(allocations);
        assertEquals(2, allocations.size());
        assertEquals(2024, allocations.get(0).getYear());
        assertEquals(15, allocations.get(0).getWeekNumber());
        assertEquals(2026, allocations.get(1).getYear());
        assertEquals(2, allocations.get(1).getWeekNumber());
    }

    @Test
    @DisplayName("Truyền projectId = null trả về danh sách rỗng")
    void testLoadAllAllocationsForProject_NullProjectId_ReturnsEmpty() {
        List<WeeklyProjectAllocation> allocations = adapter.loadAllAllocationsForProject(null);
        assertNotNull(allocations);
        assertTrue(allocations.isEmpty());
        verify(entityManager, never()).createQuery(anyString(), any());
    }
}
