package com.hrm.employeemanagement.domain.milestone;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.hrm.employeemanagement.domain.exception.milestone.InvalidMilestoneDataException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.user.UserId;

public class Milestone {
    private MilestoneId id;
    private ProjectId projectId;
    private String name;
    private String description;
    private LocalDate plannedDate;
    private LocalDate actualDate;
    private Set<TaskId> linkedTaskIds;
    private UserId createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public Milestone(
            MilestoneId id,
            ProjectId projectId,
            String name,
            String description,
            LocalDate plannedDate,
            LocalDate actualDate,
            Set<TaskId> linkedTaskIds,
            UserId createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        validateProjectId(projectId);
        validateName(name);
        validatePlannedDate(plannedDate);
        validateActualDate(plannedDate, actualDate);

        this.id = id;
        this.projectId = projectId;
        this.name = name.trim();
        this.description = description != null ? description.trim() : null;
        this.plannedDate = plannedDate;
        this.actualDate = actualDate;
        this.linkedTaskIds = linkedTaskIds != null ? new HashSet<>(linkedTaskIds) : new HashSet<>();
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static Milestone createNew(
            ProjectId projectId,
            String name,
            String description,
            LocalDate plannedDate,
            Set<TaskId> linkedTaskIds,
            UserId createdBy) {
        return new Milestone(
                null,
                projectId,
                name,
                description,
                plannedDate,
                null,
                linkedTaskIds,
                createdBy,
                LocalDateTime.now(),
                null,
                null);
    }

    public void updateDetails(
            String name,
            String description,
            LocalDate plannedDate,
            LocalDate actualDate,
            Set<TaskId> newLinkedTaskIds) {
        if (name != null) {
            validateName(name);
            this.name = name.trim();
        }
        if (description != null) {
            this.description = description.trim().isEmpty() ? null : description.trim();
        }
        if (plannedDate != null) {
            validatePlannedDate(plannedDate);
            validateActualDate(plannedDate, this.actualDate);
            this.plannedDate = plannedDate;
        }
        if (actualDate != null) {
            validateActualDate(this.plannedDate, actualDate);
            this.actualDate = actualDate;
        }
        if (newLinkedTaskIds != null) {
            this.linkedTaskIds = new HashSet<>(newLinkedTaskIds);
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void complete(LocalDate completionDate) {
        LocalDate actual = completionDate != null ? completionDate : LocalDate.now();
        validateActualDate(this.plannedDate, actual);
        this.actualDate = actual;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Rà soát trạng thái tiến độ dựa trên ngày hiện tại và trạng thái của các hạng mục liên kết (AC-02 / TC-02).
     */
    public MilestoneStatus evaluateStatus(LocalDate currentDate, boolean allLinkedTasksCompleted) {
        if (!this.linkedTaskIds.isEmpty()) {
            if (allLinkedTasksCompleted) {
                return MilestoneStatus.COMPLETED;
            }
        } else if (this.actualDate != null) {
            return MilestoneStatus.COMPLETED;
        }
        LocalDate checkDate = currentDate != null ? currentDate : LocalDate.now();
        if (checkDate.isAfter(this.plannedDate)) {
            return MilestoneStatus.DELAYED;
        }
        return MilestoneStatus.ON_TRACK;
    }

    /**
     * Tính toán số ngày trễ nếu mốc tiến độ đã quá hạn mà các hạng mục chưa hoàn thành (AC-02 / TC-02).
     */
    public long calculateDelayDays(LocalDate currentDate, boolean allLinkedTasksCompleted) {
        MilestoneStatus currentEval = evaluateStatus(currentDate, allLinkedTasksCompleted);
        if (currentEval == MilestoneStatus.DELAYED) {
            LocalDate checkDate = currentDate != null ? currentDate : LocalDate.now();
            return ChronoUnit.DAYS.between(this.plannedDate, checkDate);
        }
        return 0;
    }

    // Validations
    private void validateProjectId(ProjectId projectId) {
        if (projectId == null || projectId.value() == null) {
            throw new InvalidMilestoneDataException("Mã dự án (projectId) không được để trống");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidMilestoneDataException("Tên mốc tiến độ không được để trống");
        }
        if (name.trim().length() > 255) {
            throw new InvalidMilestoneDataException("Tên mốc tiến độ không được vượt quá 255 ký tự");
        }
    }

    private void validatePlannedDate(LocalDate plannedDate) {
        if (plannedDate == null) {
            throw new InvalidMilestoneDataException("Ngày kế hoạch của mốc tiến độ không được để trống");
        }
    }

    private void validateActualDate(LocalDate plannedDate, LocalDate actualDate) {
        if (actualDate != null && plannedDate != null && actualDate.isBefore(plannedDate)) {
            throw new InvalidMilestoneDataException("Ngày hoàn thành thực tế không được trước ngày kế hoạch");
        }
    }

    // Getters
    public MilestoneId getId() {
        return id;
    }

    public Long getIdValue() {
        return id != null ? id.value() : null;
    }

    public ProjectId getProjectId() {
        return projectId;
    }

    public Long getProjectIdValue() {
        return projectId != null ? projectId.value() : null;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public LocalDate getActualDate() {
        return actualDate;
    }

    public Set<TaskId> getLinkedTaskIds() {
        return Collections.unmodifiableSet(linkedTaskIds);
    }

    public UserId getCreatedBy() {
        return createdBy;
    }

    public Long getCreatedByValue() {
        return createdBy != null ? createdBy.value() : null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
