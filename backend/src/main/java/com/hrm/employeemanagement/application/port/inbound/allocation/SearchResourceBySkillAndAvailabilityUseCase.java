package com.hrm.employeemanagement.application.port.inbound.allocation;

import java.util.List;

import com.hrm.employeemanagement.application.dto.allocation.ResourceSearchResult;
import com.hrm.employeemanagement.application.dto.allocation.SearchResourceQuery;

/**
 * NCL-02-CN-004: Port nhận yêu cầu tìm kiếm nhân sự theo kỹ năng và mức độ
 * rảnh.
 */
public interface SearchResourceBySkillAndAvailabilityUseCase {

    /**
     * Tìm kiếm và trả về danh sách nhân sự có kỹ năng phù hợp, sắp xếp theo mức
     * độ rảnh giảm dần.
     */
    List<ResourceSearchResult> search(SearchResourceQuery query);
}
