package com.hrm.employeemanagement.domain.scenario;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotModifiableException;

public class ResourceScenario {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Long orgUnitId;
    private ScenarioStatus status;
    private Integer fromYear;
    private Integer fromWeek;
    private Integer durationWeeks;
    private LocalDateTime baseSnapshotAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
    private String note;
    private String snapshotData;
    private List<ScenarioDemand> demands = new ArrayList<>();
    private List<ScenarioAllocationSnapshotItem> snapshotItems = new ArrayList<>();

    public ResourceScenario(
            Long id,
            String code,
            String name,
            String description,
            Long orgUnitId,
            ScenarioStatus status,
            Integer fromYear,
            Integer fromWeek,
            Integer durationWeeks,
            LocalDateTime baseSnapshotAt,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version
    ) {
        this(id, code, name, description, null, null, orgUnitId, status, fromYear, fromWeek, durationWeeks, baseSnapshotAt, createdBy, createdAt, updatedAt, version);
    }

    public ResourceScenario(
            Long id,
            String code,
            String name,
            String description,
            String note,
            String snapshotData,
            Long orgUnitId,
            ScenarioStatus status,
            Integer fromYear,
            Integer fromWeek,
            Integer durationWeeks,
            LocalDateTime baseSnapshotAt,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version
    ) {
        this.id = id;
        this.code = Objects.requireNonNull(code, "Mã kịch bản không được để trống");
        this.name = Objects.requireNonNull(name, "Tên kịch bản không được để trống");
        this.description = description;
        this.note = note;
        this.snapshotData = snapshotData;
        this.orgUnitId = Objects.requireNonNull(orgUnitId, "Đơn vị không được để trống");
        this.status = status != null ? status : ScenarioStatus.DRAFT;
        this.fromYear = Objects.requireNonNull(fromYear, "Năm bắt đầu không được để trống");
        this.fromWeek = Objects.requireNonNull(fromWeek, "Tuần bắt đầu không được để trống");
        this.durationWeeks = durationWeeks != null ? durationWeeks : 8;
        this.baseSnapshotAt = baseSnapshotAt != null ? baseSnapshotAt : LocalDateTime.now();
        this.createdBy = Objects.requireNonNull(createdBy, "Người tạo không được để trống");
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;
    }

    public static ResourceScenario createNew(
            String code,
            String name,
            String description,
            Long orgUnitId,
            Integer fromYear,
            Integer fromWeek,
            Integer durationWeeks,
            Long createdBy
    ) {
        return new ResourceScenario(
                null,
                code,
                name,
                description,
                orgUnitId,
                ScenarioStatus.DRAFT,
                fromYear,
                fromWeek,
                durationWeeks,
                LocalDateTime.now(),
                createdBy,
                LocalDateTime.now(),
                null,
                0L
        );
    }

    public void assertModifiable() {
        if (this.status == ScenarioStatus.SAVED) {
            transitionToDraft();
            return;
        }
        if (this.status != ScenarioStatus.DRAFT) {
            throw new ScenarioNotModifiableException(
                    "Không thể chỉnh sửa kịch bản ở trạng thái: " + this.status.getValue() + ". Chỉ được chỉnh sửa kịch bản ở trạng thái draft."
            );
        }
    }

    public void transitionToDraft() {
        if (this.status == ScenarioStatus.SAVED) {
            this.status = ScenarioStatus.DRAFT;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void saveSnapshot(String snapshotJson) {
        this.snapshotData = Objects.requireNonNull(snapshotJson, "Dữ liệu snapshot không được để trống");
        this.status = ScenarioStatus.SAVED;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateBasicInfo(String name, String note) {
        if (name != null && !name.trim().isEmpty()) {
            if (name.trim().length() > 255) {
                throw new IllegalArgumentException("Tên kịch bản không được vượt quá 255 ký tự");
            }
            this.name = name.trim();
        }
        if (note != null) {
            if (note.trim().length() > 2000) {
                throw new IllegalArgumentException("Ghi chú kịch bản không được vượt quá 2000 ký tự");
            }
            this.note = note.trim();
        }
        transitionToDraft();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isSaved() {
        return this.status == ScenarioStatus.SAVED;
    }

    public boolean isDraft() {
        return this.status == ScenarioStatus.DRAFT;
    }

    public void addDemand(ScenarioDemand demand) {
        assertModifiable();
        this.demands.add(demand);
    }

    public void setDemands(List<ScenarioDemand> demands) {
        this.demands = demands != null ? new ArrayList<>(demands) : new ArrayList<>();
    }

    public void setSnapshotItems(List<ScenarioAllocationSnapshotItem> items) {
        this.snapshotItems = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public com.hrm.employeemanagement.domain.availability.YearWeek getStartYearWeek() {
        return com.hrm.employeemanagement.domain.availability.YearWeek.of(fromYear, fromWeek);
    }

    public com.hrm.employeemanagement.domain.availability.YearWeek getEndYearWeek() {
        com.hrm.employeemanagement.domain.availability.YearWeek start = getStartYearWeek();
        java.time.LocalDate monday = start.getStartDate().plusWeeks(durationWeeks - 1);
        int y = monday.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR);
        int w = monday.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return com.hrm.employeemanagement.domain.availability.YearWeek.of(y, w);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getSnapshotData() { return snapshotData; }
    public void setSnapshotData(String snapshotData) { this.snapshotData = snapshotData; }
    public Long getOrgUnitId() { return orgUnitId; }
    public ScenarioStatus getStatus() { return status; }
    public void setStatus(ScenarioStatus status) { this.status = status; }
    public Integer getFromYear() { return fromYear; }
    public Integer getFromWeek() { return fromWeek; }
    public Integer getDurationWeeks() { return durationWeeks; }
    public LocalDateTime getBaseSnapshotAt() { return baseSnapshotAt; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    public List<ScenarioDemand> getDemands() { return Collections.unmodifiableList(demands); }
    public List<ScenarioAllocationSnapshotItem> getSnapshotItems() { return Collections.unmodifiableList(snapshotItems); }
}
