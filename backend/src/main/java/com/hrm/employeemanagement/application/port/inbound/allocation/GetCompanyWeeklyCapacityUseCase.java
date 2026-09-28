package com.hrm.employeemanagement.application.port.inbound.allocation;

import com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityMatrixResult;
import com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityQuery;

public interface GetCompanyWeeklyCapacityUseCase {
    CompanyWeeklyCapacityMatrixResult getWeeklyCapacityMatrix(CompanyWeeklyCapacityQuery query);
}
