package com.hrm.employeemanagement.domain.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDateRangeException;
import com.hrm.employeemanagement.domain.exception.project.ProjectAlreadyClosedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotClosedException;
import com.hrm.employeemanagement.domain.user.UserId;

class ProjectTest {

    private static final String VALID_CODE = "PRJ-2026-001";
    private static final String VALID_NAME = "Hệ thống Quản lý Nhân sự";
    private static final Long ORG_UNIT_ID = 100L;
    private static final EmployeeId MANAGER_ID = new EmployeeId(10L);
    private static final UserId CREATED_BY = new UserId(1L);

    @Nested
    @DisplayName("Kiểm tra khởi tạo dự án mới (createNew)")
    class CreateNewTests {

        @Test
        @DisplayName("Tạo dự án thành công với dữ liệu hợp lệ")
        void shouldCreateProjectSuccessfully() {
            Project project = Project.createNew(
                    VALID_CODE,
                    VALID_NAME,
                    ORG_UNIT_ID,
                    MANAGER_ID,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    new BigDecimal("150.50"),
                    "Mô tả dự án",
                    CREATED_BY);

            assertThat(project).isNotNull();
            assertThat(project.getProjectCode()).isEqualTo(VALID_CODE);
            assertThat(project.getProjectName()).isEqualTo(VALID_NAME);
            assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
            assertThat(project.getEstimatedHours()).isEqualTo(new BigDecimal("150.50"));
            assertThat(project.getDescription()).isEqualTo("Mô tả dự án");
        }

        @Test
        @DisplayName("Ném lỗi khi tên dự án để trống")
        void shouldThrowWhenProjectNameIsBlank() {
            assertThatThrownBy(() -> Project.createNew(
                    VALID_CODE,
                    "   ",
                    ORG_UNIT_ID,
                    MANAGER_ID,
                    null,
                    null,
                    BigDecimal.ZERO,
                    null,
                    CREATED_BY))
                    .isInstanceOf(InvalidProjectDataException.class)
                    .hasMessageContaining("Tên dự án không được để trống");
        }

        @Test
        @DisplayName("Ném lỗi khi ngày kết thúc sớm hơn ngày bắt đầu")
        void shouldThrowWhenEndDateBeforeStartDate() {
            assertThatThrownBy(() -> Project.createNew(
                    VALID_CODE,
                    VALID_NAME,
                    ORG_UNIT_ID,
                    MANAGER_ID,
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 5, 1),
                    BigDecimal.ZERO,
                    null,
                    CREATED_BY))
                    .isInstanceOf(InvalidProjectDateRangeException.class);
        }

        @Test
        @DisplayName("Ném lỗi khi số giờ dự kiến nhỏ hơn 0")
        void shouldThrowWhenEstimatedHoursNegative() {
            assertThatThrownBy(() -> Project.createNew(
                    VALID_CODE,
                    VALID_NAME,
                    ORG_UNIT_ID,
                    MANAGER_ID,
                    null,
                    null,
                    new BigDecimal("-0.01"),
                    null,
                    CREATED_BY))
                    .isInstanceOf(InvalidProjectDataException.class)
                    .hasMessageContaining("Tổng giờ dự kiến không được nhỏ hơn 0");
        }

        @Test
        @DisplayName("Ném lỗi khi số giờ dự kiến vượt quá 99,999,999.99")
        void shouldThrowWhenEstimatedHoursExceedsMax() {
            assertThatThrownBy(() -> Project.createNew(
                    VALID_CODE,
                    VALID_NAME,
                    ORG_UNIT_ID,
                    MANAGER_ID,
                    null,
                    null,
                    new BigDecimal("100000000.00"),
                    null,
                    CREATED_BY))
                    .isInstanceOf(InvalidProjectDataException.class)
                    .hasMessageContaining("Tổng giờ dự kiến không được vượt quá 99,999,999.99");
        }

        @Test
        @DisplayName("Ném lỗi khi số giờ dự kiến có nhiều hơn 2 chữ số thập phân (e.g. 0.00000001)")
        void shouldThrowWhenEstimatedHoursHasTooManyDecimals() {
            assertThatThrownBy(() -> Project.createNew(
                    VALID_CODE,
                    VALID_NAME,
                    ORG_UNIT_ID,
                    MANAGER_ID,
                    null,
                    null,
                    new BigDecimal("0.00000001"),
                    null,
                    CREATED_BY))
                    .isInstanceOf(InvalidProjectDataException.class)
                    .hasMessageContaining("Tổng giờ dự kiến chỉ được có tối đa 2 chữ số thập phân");
        }

        @Test
        @DisplayName("Ném lỗi khi mô tả dự án vượt quá 2000 ký tự")
        void shouldThrowWhenDescriptionTooLong() {
            String longDescription = "A".repeat(2001);
            assertThatThrownBy(() -> Project.createNew(
                    VALID_CODE,
                    VALID_NAME,
                    ORG_UNIT_ID,
                    MANAGER_ID,
                    null,
                    null,
                    BigDecimal.ZERO,
                    longDescription,
                    CREATED_BY))
                    .isInstanceOf(InvalidProjectDataException.class)
                    .hasMessageContaining("Mô tả dự án không được vượt quá 2000 ký tự");
        }
    }

    @Nested
    @DisplayName("Kiểm tra cập nhật thông tin dự án (updateInfo)")
    class UpdateInfoTests {

        private Project createActiveProject() {
            return new Project(
                    new ProjectId(1L),
                    VALID_CODE,
                    VALID_NAME,
                    ORG_UNIT_ID,
                    MANAGER_ID,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    new BigDecimal("100.00"),
                    "Mô tả ban đầu",
                    ProjectStatus.ACTIVE,
                    CREATED_BY,
                    LocalDateTime.now(),
                    null,
                    0L);
        }

        @Test
        @DisplayName("Cập nhật thành công khi dữ liệu hợp lệ")
        void shouldUpdateSuccessfully() {
            Project project = createActiveProject();

            project.updateInfo(
                    "Tên dự án đã cập nhật",
                    new EmployeeId(20L),
                    LocalDate.of(2026, 2, 1),
                    LocalDate.of(2026, 11, 30),
                    new BigDecimal("250.75"),
                    "Mô tả cập nhật");

            assertThat(project.getProjectName()).isEqualTo("Tên dự án đã cập nhật");
            assertThat(project.getManagerId()).isEqualTo(new EmployeeId(20L));
            assertThat(project.getEstimatedHours()).isEqualTo(new BigDecimal("250.75"));
            assertThat(project.getDescription()).isEqualTo("Mô tả cập nhật");
            assertThat(project.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Ném lỗi khi cập nhật dự án không ở trạng thái ACTIVE")
        void shouldThrowWhenUpdatingInactiveProject() {
            Project project = new Project(
                    new ProjectId(1L),
                    VALID_CODE,
                    VALID_NAME,
                    ORG_UNIT_ID,
                    MANAGER_ID,
                    null,
                    null,
                    BigDecimal.ZERO,
                    null,
                    ProjectStatus.INACTIVE,
                    CREATED_BY,
                    LocalDateTime.now(),
                    null,
                    0L);

            assertThatThrownBy(() -> project.updateInfo(
                    "Tên mới",
                    MANAGER_ID,
                    null,
                    null,
                    BigDecimal.TEN,
                    null))
                    .isInstanceOf(InvalidProjectDataException.class)
                    .hasMessageContaining("trạng thái hoạt động");
        }

        @Test
        @DisplayName("Ném lỗi khi cập nhật số giờ có nhiều hơn 2 chữ số thập phân")
        void shouldThrowWhenUpdateEstimatedHoursHasTooManyDecimals() {
            Project project = createActiveProject();

            assertThatThrownBy(() -> project.updateInfo(
                    VALID_NAME,
                    MANAGER_ID,
                    null,
                    null,
                    new BigDecimal("0.00000001"),
                    null))
                    .isInstanceOf(InvalidProjectDataException.class)
                    .hasMessageContaining("Tổng giờ dự kiến chỉ được có tối đa 2 chữ số thập phân");
        }

        @Test
        @DisplayName("Ném lỗi khi cập nhật mô tả vượt quá 2000 ký tự")
        void shouldThrowWhenUpdateDescriptionTooLong() {
            Project project = createActiveProject();
            String longDescription = "X".repeat(2001);

            assertThatThrownBy(() -> project.updateInfo(
                    VALID_NAME,
                    MANAGER_ID,
                    null,
                    null,
                    BigDecimal.TEN,
                    longDescription))
                    .isInstanceOf(InvalidProjectDataException.class)
                    .hasMessageContaining("Mô tả dự án không được vượt quá 2000 ký tự");
        }

        @Test
        @DisplayName("Xác minh isManagedBy hoạt động chính xác")
        void shouldVerifyIsManagedBy() {
            Project project = createActiveProject();

            assertThat(project.isManagedBy(MANAGER_ID)).isTrue();
            assertThat(project.isManagedBy(new EmployeeId(999L))).isFalse();
            assertThat(project.isManagedBy(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Kiểm tra nghiệp vụ Đóng và Mở lại dự án (Close & Reopen)")
    class CloseAndReopenTests {

        private Project createActiveProject() {
            return Project.createNew(
                    VALID_CODE,
                    VALID_NAME,
                    ORG_UNIT_ID,
                    MANAGER_ID,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    new BigDecimal("100.00"),
                    "Mô tả dự án",
                    CREATED_BY);
        }

        @Test
        @DisplayName("Đóng dự án thành công và cập nhật đúng thông tin")
        void shouldCloseProjectSuccessfully() {
            Project project = createActiveProject();
            UserId closer = new UserId(99L);
            String reason = "Dự án đã bàn giao và nghiệm thu xong";

            project.close(closer, reason);

            assertThat(project.getStatus()).isEqualTo(ProjectStatus.CLOSED);
            assertThat(project.isClosed()).isTrue();
            assertThat(project.getClosedBy()).isEqualTo(closer);
            assertThat(project.getClosedByValue()).isEqualTo(99L);
            assertThat(project.getClosureReason()).isEqualTo(reason);
            assertThat(project.getClosedAt()).isNotNull();
        }

        @Test
        @DisplayName("Ném lỗi khi đóng dự án đã ở trạng thái CLOSED")
        void shouldThrowWhenClosingAlreadyClosedProject() {
            Project project = createActiveProject();
            project.close(CREATED_BY, "Đóng lần 1");

            assertThatThrownBy(() -> project.close(CREATED_BY, "Đóng lần 2"))
                    .isInstanceOf(ProjectAlreadyClosedException.class)
                    .hasMessageContaining("Dự án đã ở trạng thái đóng từ trước");
        }

        @Test
        @DisplayName("Mở lại dự án đã đóng thành công khi có lý do hợp lệ >= 10 ký tự")
        void shouldReopenClosedProjectSuccessfully() {
            Project project = createActiveProject();
            project.close(CREATED_BY, "Đóng dự án");

            UserId opener = new UserId(88L);
            String reopenReason = "Mở lại theo phụ lục hợp đồng số 02";

            project.reopen(opener, reopenReason);

            assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
            assertThat(project.isActive()).isTrue();
            assertThat(project.getReopenedBy()).isEqualTo(opener);
            assertThat(project.getReopenedByValue()).isEqualTo(88L);
            assertThat(project.getReopenReason()).isEqualTo(reopenReason);
            assertThat(project.getReopenedAt()).isNotNull();
        }

        @Test
        @DisplayName("Ném lỗi khi mở lại dự án đang ở trạng thái ACTIVE")
        void shouldThrowWhenReopeningActiveProject() {
            Project project = createActiveProject();

            assertThatThrownBy(() -> project.reopen(CREATED_BY, "Lý do mở lại dự án hợp lệ"))
                    .isInstanceOf(ProjectNotClosedException.class)
                    .hasMessageContaining("Chỉ có thể mở lại dự án đang ở trạng thái đóng");
        }

        @Test
        @DisplayName("Ném lỗi khi mở lại với lý do dưới 10 ký tự hoặc để trống")
        void shouldThrowWhenReopenReasonIsInvalid() {
            Project project = createActiveProject();
            project.close(CREATED_BY, "Đóng dự án");

            assertThatThrownBy(() -> project.reopen(CREATED_BY, "Quá ngắn"))
                    .isInstanceOf(InvalidProjectDataException.class)
                    .hasMessageContaining("ít nhất 10 ký tự");

            assertThatThrownBy(() -> project.reopen(CREATED_BY, null))
                    .isInstanceOf(InvalidProjectDataException.class)
                    .hasMessageContaining("ít nhất 10 ký tự");

            assertThatThrownBy(() -> project.reopen(null, "Lý do hợp lệ trên 10 ký tự"))
                    .isInstanceOf(InvalidProjectDataException.class)
                    .hasMessageContaining("Người thực hiện mở lại dự án không được để trống");
        }
    }
}
