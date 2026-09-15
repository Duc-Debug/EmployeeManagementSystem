package com.hrm.employeemanagement.domain.allocation.template;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.exception.allocation.InvalidRoleAllocationTemplateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Domain Unit Tests: ProjectRoleAllocationTemplate & SuggestionPolicy")
class RoleAllocationTemplateDomainTest {

    @Test
    @DisplayName("Khởi tạo mẫu phân bổ vai trò hợp lệ thành công")
    void createTemplate_ValidData_ShouldSucceed() {
        List<ProjectRoleAllocationTemplateItem> items = List.of(
                ProjectRoleAllocationTemplateItem.create(1L, BigDecimal.valueOf(40.0)),
                ProjectRoleAllocationTemplateItem.create(2L, BigDecimal.valueOf(20.0)),
                ProjectRoleAllocationTemplateItem.create(3L, BigDecimal.valueOf(20.0))
        );

        ProjectRoleAllocationTemplate template = ProjectRoleAllocationTemplate.create(
                "TPL_STANDARD_DEV",
                "Mẫu dự án phát triển chuẩn",
                "Mẫu gồm 3 vai trò Dev, Test, BA",
                100L,
                1L,
                items
        );

        assertThat(template.getTemplateCode()).isEqualTo("TPL_STANDARD_DEV");
        assertThat(template.getName()).isEqualTo("Mẫu dự án phát triển chuẩn");
        assertThat(template.getItems()).hasSize(3);
        assertThat(template.getSourceProjectId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Khởi tạo mẫu không có vai trò nào -> Ném InvalidRoleAllocationTemplateException")
    void createTemplate_EmptyItems_ShouldThrowException() {
        assertThatThrownBy(() -> ProjectRoleAllocationTemplate.create(
                "TPL_EMPTY",
                "Mẫu rỗng",
                "Mô tả",
                100L,
                1L,
                List.of()
        )).isInstanceOf(InvalidRoleAllocationTemplateException.class)
          .hasMessageContaining("Mẫu phân bổ phải có ít nhất một vai trò");
    }

    @Test
    @DisplayName("Khởi tạo mẫu có 2 vai trò trùng nhau -> Ném InvalidRoleAllocationTemplateException")
    void createTemplate_DuplicateRoles_ShouldThrowException() {
        List<ProjectRoleAllocationTemplateItem> items = List.of(
                ProjectRoleAllocationTemplateItem.create(1L, BigDecimal.valueOf(40.0)),
                ProjectRoleAllocationTemplateItem.create(1L, BigDecimal.valueOf(20.0))
        );

        assertThatThrownBy(() -> ProjectRoleAllocationTemplate.create(
                "TPL_DUP",
                "Mẫu trùng vai trò",
                "Mô tả",
                100L,
                1L,
                items
        )).isInstanceOf(InvalidRoleAllocationTemplateException.class)
          .hasMessageContaining("Không được trùng lặp vai trò");
    }

    @Test
    @DisplayName("Gợi ý nhân sự: Có ứng viên đủ giờ rảnh -> Gợi ý thành công")
    void suggestCandidate_CandidateAvailable_ShouldReturnMatched() {
        List<RoleAllocationSuggestionPolicy.CandidateAvailability> candidates = List.of(
                new RoleAllocationSuggestionPolicy.CandidateAvailability(10L, "Nguyễn Văn A", "EMP001", BigDecimal.valueOf(40.0)),
                new RoleAllocationSuggestionPolicy.CandidateAvailability(11L, "Trần Thị B", "EMP002", BigDecimal.valueOf(30.0))
        );

        RoleAllocationSuggestionItem suggestion = RoleAllocationSuggestionPolicy.suggestCandidateForRole(
                1L,
                "DEV",
                "Lập trình viên",
                BigDecimal.valueOf(40.0),
                candidates
        );

        assertThat(suggestion.isAssigned()).isTrue();
        assertThat(suggestion.getSuggestedEmployeeId()).isEqualTo(10L);
        assertThat(suggestion.getSuggestedEmployeeName()).isEqualTo("Nguyễn Văn A");
        assertThat(suggestion.getWarningMessage()).isNull();
    }

    @Test
    @DisplayName("Gợi ý nhân sự: Không có ứng viên nào đủ giờ rảnh -> Để trống và cảnh báo cần bổ sung nhân sự")
    void suggestCandidate_NoCandidateAvailable_ShouldReturnUnassignedWithWarning() {
        List<RoleAllocationSuggestionPolicy.CandidateAvailability> candidates = List.of(
                new RoleAllocationSuggestionPolicy.CandidateAvailability(11L, "Trần Thị B", "EMP002", BigDecimal.valueOf(10.0))
        );

        RoleAllocationSuggestionItem suggestion = RoleAllocationSuggestionPolicy.suggestCandidateForRole(
                1L,
                "DEV",
                "Lập trình viên",
                BigDecimal.valueOf(40.0),
                candidates
        );

        assertThat(suggestion.isAssigned()).isFalse();
        assertThat(suggestion.getSuggestedEmployeeId()).isNull();
        assertThat(suggestion.getWarningMessage()).isEqualTo("Cần bổ sung nhân sự cho vai trò Lập trình viên");
    }
}

