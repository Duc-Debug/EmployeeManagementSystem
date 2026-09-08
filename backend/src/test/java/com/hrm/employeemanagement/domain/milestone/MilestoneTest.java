package com.hrm.employeemanagement.domain.milestone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.exception.milestone.InvalidMilestoneDataException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.user.UserId;

class MilestoneTest {

    @Test
    @DisplayName("Tạo mốc thành công với dữ liệu hợp lệ")
    void shouldCreateMilestoneSuccessfully() {
        LocalDate plannedDate = LocalDate.now().plusDays(10);
        Milestone milestone = Milestone.createNew(
                new ProjectId(1L),
                "Bàn giao giai đoạn 1",
                "Mô tả bàn giao",
                plannedDate,
                Set.of(new TaskId(101L), new TaskId(102L)),
                new UserId(99L));

        assertThat(milestone.getName()).isEqualTo("Bàn giao giai đoạn 1");
        assertThat(milestone.getPlannedDate()).isEqualTo(plannedDate);
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.ON_TRACK);
        assertThat(milestone.getLinkedTaskIds()).containsExactlyInAnyOrder(new TaskId(101L), new TaskId(102L));
    }

    @Test
    @DisplayName("Ném InvalidMilestoneDataException khi tên bị bỏ trống")
    void shouldThrowWhenNameIsBlank() {
        assertThatThrownBy(() -> Milestone.createNew(
                new ProjectId(1L),
                "   ",
                null,
                LocalDate.now().plusDays(5),
                Set.of(),
                new UserId(1L)))
                .isInstanceOf(InvalidMilestoneDataException.class)
                .hasMessageContaining("Tên mốc tiến độ không được để trống");
    }

    @Test
    @DisplayName("Ném InvalidMilestoneDataException khi ngày kế hoạch null")
    void shouldThrowWhenPlannedDateIsNull() {
        assertThatThrownBy(() -> Milestone.createNew(
                new ProjectId(1L),
                "Mốc hợp lệ",
                null,
                null,
                Set.of(),
                new UserId(1L)))
                .isInstanceOf(InvalidMilestoneDataException.class)
                .hasMessageContaining("Ngày kế hoạch của mốc tiến độ không được để trống");
    }

    @Test
    @DisplayName("TC-02: Đánh dấu chậm (DELAYED) kèm số ngày trễ khi đã qua ngày kế hoạch mà task chưa xong")
    void shouldBeDelayedWhenPastPlannedDateAndTasksNotDone() {
        LocalDate plannedDate = LocalDate.now().minusDays(7);
        Milestone milestone = Milestone.createNew(
                new ProjectId(1L),
                "Bàn giao giai đoạn 1",
                null,
                plannedDate,
                Set.of(new TaskId(10L)),
                new UserId(1L));

        MilestoneStatus status = milestone.evaluateStatus(LocalDate.now(), false);
        long delayDays = milestone.calculateDelayDays(LocalDate.now(), false);

        assertThat(status).isEqualTo(MilestoneStatus.DELAYED);
        assertThat(delayDays).isEqualTo(7);
    }

    @Test
    @DisplayName("Đúng hạn (ON_TRACK) khi ngày hiện tại chưa quá ngày kế hoạch")
    void shouldBeOnTrackWhenBeforePlannedDate() {
        LocalDate plannedDate = LocalDate.now().plusDays(5);
        Milestone milestone = Milestone.createNew(
                new ProjectId(1L),
                "Bàn giao giai đoạn 1",
                null,
                plannedDate,
                Set.of(new TaskId(10L)),
                new UserId(1L));

        MilestoneStatus status = milestone.evaluateStatus(LocalDate.now(), false);
        long delayDays = milestone.calculateDelayDays(LocalDate.now(), false);

        assertThat(status).isEqualTo(MilestoneStatus.ON_TRACK);
        assertThat(delayDays).isEqualTo(0);
    }

    @Test
    @DisplayName("Đã hoàn thành (COMPLETED) khi toàn bộ task liên kết đã xong")
    void shouldBeCompletedWhenAllLinkedTasksDone() {
        LocalDate plannedDate = LocalDate.now().minusDays(5);
        Milestone milestone = Milestone.createNew(
                new ProjectId(1L),
                "Bàn giao giai đoạn 1",
                null,
                plannedDate,
                Set.of(new TaskId(10L)),
                new UserId(1L));

        MilestoneStatus status = milestone.evaluateStatus(LocalDate.now(), true);
        long delayDays = milestone.calculateDelayDays(LocalDate.now(), true);

        assertThat(status).isEqualTo(MilestoneStatus.COMPLETED);
        assertThat(delayDays).isEqualTo(0);
    }

    @Test
    @DisplayName("Cập nhật thông tin mốc tiến độ thành công")
    void shouldUpdateDetailsSuccessfully() {
        Milestone milestone = Milestone.createNew(
                new ProjectId(1L),
                "Tên cũ",
                "Mô tả cũ",
                LocalDate.now().plusDays(10),
                Set.of(),
                new UserId(1L));

        LocalDate newDate = LocalDate.now().plusDays(20);
        milestone.updateDetails("Tên mới", "Mô tả mới", newDate, null, MilestoneStatus.ON_TRACK, Set.of(new TaskId(5L)));

        assertThat(milestone.getName()).isEqualTo("Tên mới");
        assertThat(milestone.getDescription()).isEqualTo("Mô tả mới");
        assertThat(milestone.getPlannedDate()).isEqualTo(newDate);
        assertThat(milestone.getLinkedTaskIds()).containsExactly(new TaskId(5L));
    }
}
