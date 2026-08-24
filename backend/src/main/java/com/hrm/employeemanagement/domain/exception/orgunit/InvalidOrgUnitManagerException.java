package com.hrm.employeemanagement.domain.exception.orgunit;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ngoại lệ nghiệp vụ khi thông tin Người quản lý (managerId) của đơn vị tổ chức không hợp lệ (bị null hoặc <= 0).
 */
public class InvalidOrgUnitManagerException extends DomainException {

    public InvalidOrgUnitManagerException(String message) {
        super(message);
    }

    public static InvalidOrgUnitManagerException missing() {
        return new InvalidOrgUnitManagerException("Người quản lý (managerId) không được để trống và phải lớn hơn 0.");
    }
}