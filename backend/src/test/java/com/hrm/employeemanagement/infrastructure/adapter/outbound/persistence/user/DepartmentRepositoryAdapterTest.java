package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import com.hrm.employeemanagement.domain.department.Department;
import com.hrm.employeemanagement.domain.department.DepartmentId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.DepartmentJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataDepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentRepositoryAdapterTest {

    @Mock
    private SpringDataDepartmentRepository springDataDepartmentRepository;

    private DepartmentRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DepartmentRepositoryAdapter(springDataDepartmentRepository);
    }

    @Test
    @DisplayName("findById nạp phòng ban thành công")
    void testFindById_Success() {
        DepartmentJpaEntity entity = new DepartmentJpaEntity(1L, "PB-01", "Ban giám đốc", null);
        when(springDataDepartmentRepository.findById(1L)).thenReturn(Optional.of(entity));

        Optional<Department> deptOpt = adapter.findById(new DepartmentId(1L));

        assertTrue(deptOpt.isPresent());
        assertEquals("PB-01", deptOpt.get().getCode());
        assertEquals("Ban giám đốc", deptOpt.get().getName());
    }

    @Test
    @DisplayName("findByCode nạp phòng ban thành công")
    void testFindByCode_Success() {
        DepartmentJpaEntity entity = new DepartmentJpaEntity(2L, "PB-02", "Phòng Kỹ thuật", 1L);
        when(springDataDepartmentRepository.findByCode("PB-02")).thenReturn(Optional.of(entity));

        Optional<Department> deptOpt = adapter.findByCode("PB-02");

        assertTrue(deptOpt.isPresent());
        assertEquals("Phòng Kỹ thuật", deptOpt.get().getName());
    }

    @Test
    @DisplayName("findAllByIdIn nạp danh sách phòng ban dạng batch thành công")
    void testFindAllByIdIn_Success() {
        DepartmentJpaEntity e1 = new DepartmentJpaEntity(1L, "PB-01", "Ban giám đốc", null);
        DepartmentJpaEntity e2 = new DepartmentJpaEntity(2L, "PB-02", "Phòng Kỹ thuật", 1L);
        when(springDataDepartmentRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(e1, e2));

        List<Department> list = adapter.findAllByIdIn(List.of(1L, 2L));

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("Ban giám đốc", list.get(0).getName());
        assertEquals("Phòng Kỹ thuật", list.get(1).getName());
        verify(springDataDepartmentRepository).findAllById(List.of(1L, 2L));
    }
}
