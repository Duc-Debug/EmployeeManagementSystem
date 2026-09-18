package com.hrm.employeemanagement.infrastructure.transaction.importdata;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.port.outbound.importdata.SingleRowEmployeeImportPort;
import com.hrm.employeemanagement.application.port.outbound.importdata.SingleRowImportResult;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveUserPort;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.user.User;

/**
 * Thực thi lưu dữ liệu cho từng dòng nhân viên trong một transaction cô lập (REQUIRES_NEW).
 * Đảm bảo tính năng Partial Import: dòng lỗi chỉ rollback sub-transaction của chính nó,
 * không làm ảnh hưởng đến các dòng thành công khác trong cùng batch.
 */
@Component
public class TransactionalSingleRowEmployeeImportExecutor implements SingleRowEmployeeImportPort {

    private static final Logger log = LoggerFactory.getLogger(TransactionalSingleRowEmployeeImportExecutor.class);

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveEmployeePort saveEmployeePort;

    public TransactionalSingleRowEmployeeImportExecutor(
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            LoadEmployeePort loadEmployeePort,
            SaveEmployeePort saveEmployeePort
    ) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.loadEmployeePort = loadEmployeePort;
        this.saveEmployeePort = saveEmployeePort;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SingleRowImportResult importSingleRow(
            int rowNumber,
            String employeeCode,
            String fullName,
            String username,
            String encodedPassword,
            String email,
            Long resolvedOrgUnitId,
            Role role,
            Long scopeOrgUnitId,
            String professionalRole,
            int standardHours,
            LocalDate startDate,
            LocalDate contractEndDate,
            boolean isOutsourced
    ) {
        try {
            // Kiểm tra chống xung đột đồng thời (Concurrency / TOCTOU check)
            if (loadUserPort.existsByUsername(username)) {
                return SingleRowImportResult.ofFailure("Dòng " + rowNumber + " (" + employeeCode + "): Tên đăng nhập '" + username + "' đã tồn tại trong hệ thống.");
            }
            if (loadEmployeePort.existsByEmployeeCode(employeeCode)) {
                return SingleRowImportResult.ofFailure("Dòng " + rowNumber + " (" + employeeCode + "): Mã nhân viên '" + employeeCode + "' đã tồn tại trong hệ thống.");
            }
            if (email != null && !email.isBlank()) {
                if (loadUserPort.existsByEmail(email)) {
                    return SingleRowImportResult.ofFailure("Dòng " + rowNumber + " (" + employeeCode + "): Email '" + email + "' đã tồn tại trong hệ thống.");
                }
                if (loadUserPort.existsByUsername(email)) {
                    return SingleRowImportResult.ofFailure("Dòng " + rowNumber + " (" + employeeCode + "): Email '" + email + "' trùng với tên đăng nhập của tài khoản khác.");
                }
            }

            User newUser = User.createNew(
                    username,
                    encodedPassword,
                    role,
                    null,
                    email,
                    scopeOrgUnitId
            );
            User savedUser = saveUserPort.save(newUser);

            Employee employee = new Employee(
                    null,
                    savedUser.getId(),
                    resolvedOrgUnitId,
                    employeeCode,
                    fullName,
                    professionalRole,
                    startDate,
                    contractEndDate,
                    isOutsourced,
                    standardHours,
                    EmployeeStatus.ACTIVE
            );
            Employee savedEmployee = saveEmployeePort.save(employee);

            savedUser.linkEmployee(savedEmployee.getId());
            saveUserPort.save(savedUser);

            return SingleRowImportResult.ofSuccess();
        } catch (DataIntegrityViolationException ex) {
            log.warn("Xung đột toàn vẹn dữ liệu khi import dòng {} ({})", rowNumber, employeeCode, ex);
            if (isUniqueConstraintViolation(ex)) {
                return SingleRowImportResult.ofFailure("Dòng " + rowNumber + " (" + employeeCode + "): Xung đột dữ liệu trong hệ thống (Tên đăng nhập, email hoặc mã nhân viên bị trùng lặp).");
            }
            return SingleRowImportResult.ofFailure("Dòng " + rowNumber + " (" + employeeCode + "): Vi phạm ràng buộc toàn vẹn dữ liệu khi lưu vào cơ sở dữ liệu.");
        } catch (Exception ex) {
            log.error("Lỗi nội bộ khi import dòng {} ({})", rowNumber, employeeCode, ex);
            return SingleRowImportResult.ofFailure("Dòng " + rowNumber + " (" + employeeCode + "): Không thể lưu bản ghi do lỗi hệ thống.");
        }
    }

    private boolean isUniqueConstraintViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getRootCause();
        if (cause == null) {
            cause = ex.getCause();
        }
        String message = cause != null && cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
        if (message.contains("uk_") || message.contains("duplicate") || message.contains("trùng") || message.contains("unique")) {
            return true;
        }
        if (cause instanceof java.sql.SQLException sqlEx) {
            int errorCode = sqlEx.getErrorCode();
            String sqlState = sqlEx.getSQLState();
            if (errorCode == 1062 || "23505".equals(sqlState)) {
                return true;
            }
        }
        return false;
    }
}