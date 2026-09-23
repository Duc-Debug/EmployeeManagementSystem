package com.hrm.employeemanagement.domain.scenario;

import java.time.LocalDateTime;
import java.util.Objects;

public class ScenarioShare {
    public static final String ACCESS_LEVEL_VIEW_ONLY = "VIEW_ONLY";

    private Long id;
    private Long scenarioId;
    private Long sharedWithUserId;
    private Long sharedByUserId;
    private String accessLevel;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;
    private Long version;

    public ScenarioShare(
            Long id,
            Long scenarioId,
            Long sharedWithUserId,
            Long sharedByUserId,
            String accessLevel,
            LocalDateTime createdAt,
            LocalDateTime revokedAt,
            Long version
    ) {
        this.id = id;
        this.scenarioId = Objects.requireNonNull(scenarioId, "Mã kịch bản không được để trống");
        this.sharedWithUserId = Objects.requireNonNull(sharedWithUserId, "Người được chia sẻ không được để trống");
        this.sharedByUserId = Objects.requireNonNull(sharedByUserId, "Người chia sẻ không được để trống");
        if (sharedWithUserId.equals(sharedByUserId)) {
            throw new IllegalArgumentException("Không thể chia sẻ kịch bản cho chính mình");
        }
        this.accessLevel = accessLevel != null ? accessLevel.trim().toUpperCase() : ACCESS_LEVEL_VIEW_ONLY;
        if (!ACCESS_LEVEL_VIEW_ONLY.equals(this.accessLevel)) {
            throw new IllegalArgumentException("Chế độ truy cập chỉ hỗ trợ " + ACCESS_LEVEL_VIEW_ONLY);
        }
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.revokedAt = revokedAt;
        this.version = version != null ? version : 0L;
    }

    public static ScenarioShare create(Long scenarioId, Long sharedWithUserId, Long sharedByUserId) {
        return new ScenarioShare(
                null,
                scenarioId,
                sharedWithUserId,
                sharedByUserId,
                ACCESS_LEVEL_VIEW_ONLY,
                LocalDateTime.now(),
                null,
                0L
        );
    }

    public boolean isActive() {
        return this.revokedAt == null;
    }

    public void revoke() {
        if (this.revokedAt != null) {
            throw new IllegalStateException("Quyền chia sẻ này đã bị thu hồi trước đó");
        }
        this.revokedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public Long getSharedWithUserId() { return sharedWithUserId; }
    public Long getSharedByUserId() { return sharedByUserId; }
    public String getAccessLevel() { return accessLevel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public Long getVersion() { return version; }
}
