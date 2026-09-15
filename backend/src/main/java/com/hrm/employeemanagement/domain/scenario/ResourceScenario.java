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
        this.id = id;
        this.code = Objects.requireNonNull(code, "Mã kịch bản không được để trống");
        this.name = Objects.requireNonNull(name, "Tên kịch bản không được để trống");
        this.description = description;
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
        if (this.status != ScenarioStatus.DRAFT) {
            throw new ScenarioNotModifiableException(
                    "Không thể chỉnh sửa kịch bản ở trạng thái: " + this.status.getValue() + ". Chỉ được chỉnh sửa kịch bản ở trạng thái draft."
            );
        }
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
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
