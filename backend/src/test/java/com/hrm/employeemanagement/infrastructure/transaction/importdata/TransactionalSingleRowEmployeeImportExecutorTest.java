package com.hrm.employeemanagement.infrastructure.transaction.importdata;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.dao.DataIntegrityViolationException;

import com.hrm.employeemanagement.application.port.outbound.importdata.SingleRowImportResult;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveUserPort;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

@DisplayName("TransactionalSingleRowEmployeeImportExecutor Tests")
class TransactionalSingleRowEmployeeImportExecutorTest {

    private LoadUserPort loadUserPort;
    private SaveUserPort saveUserPort;
    private LoadEmployeePort loadEmployeePort;
    private SaveEmployeePort saveEmployeePort;

    private TransactionalSingleRowEmployeeImportExecutor executor;
    private Role role;

    @BeforeEach
    void setUp() {
        loadUserPort = mock(LoadUserPort.class);
        saveUserPort = mock(SaveUserPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        saveEmployeePort = mock(SaveEmployeePort.class);

        executor = new TransactionalSingleRowEmployeeImportExecutor(
                loadUserPort, saveUserPort, loadEmployeePort, saveEmployeePort
        );
        role = new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên chuyên môn");
    }

    @Test
    @DisplayName("Lưu thành công khi dữ liệu hợp lệ")
    void importSingleRow_Success() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(new UserId(10L));
        when(saveUserPort.save(any(User.class))).thenReturn(user);

        Employee emp = mock(Employee.class);
        when(emp.getId()).thenReturn(new EmployeeId(20L));
        when(saveEmployeePort.save(any(Employee.class))).thenReturn(emp);

        SingleRowImportResult result = executor.importSingleRow(
                2, "EMP001", "Nguyen Van A", "an.nguyen", "hash",
                "an.nguyen@test.com", 100L, role, null, "Developer",
                40, LocalDate.of(2026, 1, 1), null, false
        );

        assertTrue(result.success());
    }

    @Test
    @DisplayName("Xử lý ngoại lệ Unique Constraint -> Báo lỗi xung đột dữ liệu")
    void importSingleRow_UniqueConstraintViolation() {
        SQLException sqlEx = new SQLException("Duplicate entry 'EMP001' for key 'uk_employee_code'", "23000", 1062);
        DataIntegrityViolationException dive = new DataIntegrityViolationException("Duplicate key", sqlEx);

        when(saveUserPort.save(any(User.class))).thenThrow(dive);

        SingleRowImportResult result = executor.importSingleRow(
                2, "EMP001", "Nguyen Van A", "an.nguyen", "hash",
                "an.nguyen@test.com", 100L, role, null, "Developer",
                40, LocalDate.of(2026, 1, 1), null, false
        );

        assertFalse(result.success());
        assertTrue(result.errorMessage().contains("Xung đột dữ liệu trong hệ thống"));
    }

    @Test
    @DisplayName("Xử lý ngoại lệ ràng buộc khác (FK / Check / Not Null) -> Báo lỗi vi phạm toàn vẹn dữ liệu")
    void importSingleRow_OtherIntegrityViolation() {
        SQLException sqlEx = new SQLException("Cannot add or update a child row: a foreign key constraint fails", "23000", 1452);
        DataIntegrityViolationException dive = new DataIntegrityViolationException("FK failure", sqlEx);

        when(saveUserPort.save(any(User.class))).thenThrow(dive);

        SingleRowImportResult result = executor.importSingleRow(
                2, "EMP001", "Nguyen Van A", "an.nguyen", "hash",
                "an.nguyen@test.com", 100L, role, null, "Developer",
                40, LocalDate.of(2026, 1, 1), null, false
        );

        assertFalse(result.success());
        assertTrue(result.errorMessage().contains("Vi phạm ràng buộc toàn vẹn dữ liệu"));
    }
}