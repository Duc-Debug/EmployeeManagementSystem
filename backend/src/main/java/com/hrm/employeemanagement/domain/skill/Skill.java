package com.hrm.employeemanagement.domain.skill;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.exception.skill.InvalidSkillMergeException;
import com.hrm.employeemanagement.domain.exception.skill.RequiredFieldMissingException;


public class Skill {
    private final SkillId id;
    private Long groupId;
    private String name;
    private String description;
    private SkillStatus status;
    private SkillId mergedIntoSkillId; // Null nếu chưa từng bị merge
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Constructor khởi tạo
    public Skill(SkillId id, Long groupId, String name, String description,
                 SkillStatus status, SkillId mergedIntoSkillId,
                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (name == null || name.trim().isEmpty()) {
            throw RequiredFieldMissingException.of("Tên kỹ năng (name)");
        }
        if (groupId == null || groupId <= 0) {
            throw RequiredFieldMissingException.of("Nhóm kỹ năng (groupId)");
        }
        this.id = id;
        this.groupId = groupId;
        this.name = name.trim();
        this.description = description;
        this.status = status != null ? status : SkillStatus.ACTIVE;
        this.mergedIntoSkillId = mergedIntoSkillId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
    }
    // =========================================================================
    // HÀNH VI NGHIỆP VỤ (DOMAIN BEHAVIORS) - Thay thế cho các Setter tùy tiện
    // =========================================================================
    /**
     * Quy tắc: Cập nhật thông tin kỹ năng
     */
    public void updateInfo(String newName, Long newGroupId, String newDescription) {
        if (newName == null || newName.trim().isEmpty()) {
            throw RequiredFieldMissingException.of("Tên kỹ năng (name)");
        }
        if (newGroupId == null || newGroupId <= 0) {
            throw RequiredFieldMissingException.of("Nhóm kỹ năng (groupId)");
        }
        if (this.status == SkillStatus.MERGED) {
            throw new IllegalStateException("Không thể chỉnh sửa kỹ năng đã bị MERGED.");
        }
        this.name = newName.trim();
        this.groupId = newGroupId;
        this.description = newDescription;
        this.updatedAt = LocalDateTime.now();
    }
    /**
     * Quy tắc: Vô hiệu hóa kỹ năng (Soft Deactivate)
     */
    public void deactivate() {
        if (this.status == SkillStatus.MERGED) {
            throw new IllegalStateException("Kỹ năng đã bị MERGED không thể chuyển sang INACTIVE.");
        }
        this.status = SkillStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    /**
     * Quy tắc CV-01: Gộp kỹ năng hiện tại vào kỹ năng đích
     */
    public void mergeInto(SkillId targetSkillId) {
        if (targetSkillId == null) {
            throw RequiredFieldMissingException.of("Kỹ năng đích (targetSkillId)");
        }
        if (Objects.equals(this.id, targetSkillId)) {
            throw new InvalidSkillMergeException("Một kỹ năng không thể tự gộp vào chính nó.");
        }
        if (this.status == SkillStatus.MERGED) {
            throw new InvalidSkillMergeException("Kỹ năng này đã từng bị gộp trước đó.");
        }
        // Chuyển trạng thái và lưu dấu vết kỹ năng đích
        this.status = SkillStatus.MERGED;
        this.mergedIntoSkillId = targetSkillId;
        this.updatedAt = LocalDateTime.now();
    }
    // Getters thuần túy để đọc trạng thái (Không cung cấp Setters)
    public SkillId getId() { return id; }
    public Long getGroupId() { return groupId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public SkillStatus getStatus() { return status; }
    public SkillId getMergedIntoSkillId() { return mergedIntoSkillId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}