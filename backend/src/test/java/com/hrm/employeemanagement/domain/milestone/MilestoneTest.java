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
        assertThat(milestone.evaluateStatus(LocalDate.now(), false)).isEqualTo(MilestoneStatus.ON_TRACK);
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
        milestone.updateDetails("Tên mới", "Mô tả mới", newDate, null, Set.of(new TaskId(5L)));

        assertThat(milestone.getName()).isEqualTo("Tên mới");
        assertThat(milestone.getDescription()).isEqualTo("Mô tả mới");
        assertThat(milestone.getPlannedDate()).isEqualTo(newDate);
        assertThat(milestone.getLinkedTaskIds()).containsExactly(new TaskId(5L));
    }

    @Test
    @DisplayName("Không cho phép COMPLETED nếu các task liên kết chưa hoàn thành dù ngày kế hoạch ở thời điểm nào")
    void shouldNotBeCompletedWhenTasksNotDone() {
        LocalDate plannedDate = LocalDate.now().plusDays(5);
        Milestone milestone = new Milestone(
                new MilestoneId(1L),
                new ProjectId(1L),
                "Mốc tiến độ",
                null,
                plannedDate,
                null,
                Set.of(new TaskId(10L)),
                new UserId(1L),
                null,
                null,
                0L);

        MilestoneStatus status = milestone.evaluateStatus(LocalDate.now(), false);
        assertThat(status).isEqualTo(MilestoneStatus.ON_TRACK);
    }

    @Test
    @DisplayName("Trạng thái mốc tự động chuyển sang COMPLETED khi toàn bộ task hoàn thành mà không cần update mốc")
    void shouldAutomaticallyBecomeCompletedWhenTasksAreDone() {
        Milestone milestone = Milestone.createNew(
                new ProjectId(1L),
                "Bàn giao giai đoạn 1",
                null,
                LocalDate.now().plusDays(10),
                Set.of(new TaskId(101L)),
                new UserId(1L));

        // Ban đầu task chưa xong -> ON_TRACK
        assertThat(milestone.evaluateStatus(LocalDate.now(), false)).isEqualTo(MilestoneStatus.ON_TRACK);

        // Khi toàn bộ task hoàn thành -> tự động COMPLETED mà không cần cập nhật mốc
        assertThat(milestone.evaluateStatus(LocalDate.now(), true)).isEqualTo(MilestoneStatus.COMPLETED);
    }

    @Test
    @DisplayName("Ném InvalidMilestoneDataException khi ngày hoàn thành thực tế trước ngày kế hoạch (Constructor)")
    void shouldThrowWhenActualDateIsBeforePlannedDateInConstructor() {
        LocalDate plannedDate = LocalDate.now().plusDays(10);
        LocalDate invalidActualDate = LocalDate.now().plusDays(5); // trước plannedDate

        assertThatThrownBy(() -> new Milestone(
                new MilestoneId(1L),
                new ProjectId(1L),
                "Mốc bàn giao",
                null,
                plannedDate,
                invalidActualDate,
                Set.of(),
                new UserId(1L),
                null,
                null,
                0L))
                .isInstanceOf(InvalidMilestoneDataException.class)
                .hasMessageContaining("Ngày hoàn thành thực tế không được trước ngày kế hoạch");
    }

    @Test
    @DisplayName("Ném InvalidMilestoneDataException khi cập nhật ngày thực tế trước ngày kế hoạch")
    void shouldThrowWhenActualDateIsBeforePlannedDateInUpdateDetails() {
        LocalDate plannedDate = LocalDate.now().plusDays(10);
        Milestone milestone = Milestone.createNew(
                new ProjectId(1L),
                "Mốc bàn giao",
                null,
                plannedDate,
                Set.of(),
                new UserId(1L));

        LocalDate invalidActualDate = LocalDate.now().plusDays(3); // trước plannedDate

        assertThatThrownBy(() -> milestone.updateDetails(null, null, null, invalidActualDate, null))
                .isInstanceOf(InvalidMilestoneDataException.class)
                .hasMessageContaining("Ngày hoàn thành thực tế không được trước ngày kế hoạch");
    }

    @Test
    @DisplayName("Cho phép ngày hoàn thành thực tế bằng hoặc sau ngày kế hoạch")
    void shouldAllowActualDateOnOrAfterPlannedDate() {
        LocalDate plannedDate = LocalDate.now().plusDays(10);
        Milestone milestone = Milestone.createNew(
                new ProjectId(1L),
                "Mốc bàn giao",
                null,
                plannedDate,
                Set.of(),
                new UserId(1L));

        LocalDate validActualDate = plannedDate.plusDays(2);
        milestone.updateDetails(null, null, null, validActualDate, null);

        assertThat(milestone.getActualDate()).isEqualTo(validActualDate);
    }
}
