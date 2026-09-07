package com.hrm.employeemanagement.domain.skill;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EmployeeSkill {

    private final Long id;
    private final Long employeeId;
    private final Long skillId;
    private ProficiencyLevel proficiencyLevel;
    private BigDecimal yearsOfExperience;
    private SkillStatus status;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private String reviewNotes;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final Long version;

    public EmployeeSkill(
            Long id,
            Long employeeId,
            Long skillId,
            ProficiencyLevel proficiencyLevel,
            BigDecimal yearsOfExperience,
            SkillStatus status,
            Long approvedBy,
            LocalDateTime approvedAt,
            String rejectionReason,
            String reviewNotes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, employeeId, skillId, proficiencyLevel, yearsOfExperience, status, approvedBy, approvedAt, rejectionReason, reviewNotes, createdAt, updatedAt, null);
    }

    public EmployeeSkill(
            Long id,
            Long employeeId,
            Long skillId,
            ProficiencyLevel proficiencyLevel,
            BigDecimal yearsOfExperience,
            SkillStatus status,
            Long approvedBy,
            LocalDateTime approvedAt,
            String rejectionReason,
            String reviewNotes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version
    ) {
        validateInputs(employeeId, skillId, proficiencyLevel, yearsOfExperience);
        this.id = id;
        this.employeeId = employeeId;
        this.skillId = skillId;
        this.proficiencyLevel = proficiencyLevel;
        this.yearsOfExperience = yearsOfExperience;
        this.status = status != null ? status : SkillStatus.PENDING;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.rejectionReason = rejectionReason;
        this.reviewNotes = reviewNotes;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
        this.version = version;
    }

    public EmployeeSkill(
            Long id,
            Long employeeId,
            Long skillId,
            ProficiencyLevel proficiencyLevel,
            BigDecimal yearsOfExperience,
            SkillStatus status,
            Long approvedBy,
            LocalDateTime approvedAt,
            String rejectionReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, employeeId, skillId, proficiencyLevel, yearsOfExperience, status, approvedBy, approvedAt, rejectionReason, null, createdAt, updatedAt);
    }

    public EmployeeSkill(
            Long id,
            Long employeeId,
            Long skillId,
            int proficiencyLevel,
            BigDecimal yearsOfExperience,
            SkillStatus status,
            Long approvedBy,
            LocalDateTime approvedAt,
            String rejectionReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, employeeId, skillId, ProficiencyLevel.fromValue(proficiencyLevel), yearsOfExperience, status, approvedBy, approvedAt, rejectionReason, null, createdAt, updatedAt);
    }

    public EmployeeSkill(
            Long id,
            Long employeeId,
            Long skillId,
            int proficiencyLevel,
            BigDecimal yearsOfExperience,
            SkillStatus status,
            Long approvedBy,
            LocalDateTime approvedAt,
            String rejectionReason,
            String reviewNotes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, employeeId, skillId, ProficiencyLevel.fromValue(proficiencyLevel), yearsOfExperience, status, approvedBy, approvedAt, rejectionReason, reviewNotes, createdAt, updatedAt);
    }

    /**
     * Phương thức nghiệp vụ: Khai báo kỹ năng mới (TC-01). Luôn mặc định khởi
     * tạo ở trạng thái PENDING để Quản lý nguồn lực xác nhận.
     */
    public static EmployeeSkill declare(Long employeeId, Long skillId, ProficiencyLevel proficiencyLevel, BigDecimal yearsOfExperience) {
        return new EmployeeSkill(
                null,
                employeeId,
                skillId,
                proficiencyLevel,
                yearsOfExperience,
                SkillStatus.PENDING, // TC-01: Lưu ở trạng thái chờ xác nhận
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public static EmployeeSkill declare(Long employeeId, Long skillId, int proficiencyLevel, BigDecimal yearsOfExperience) {
        return declare(employeeId, skillId, ProficiencyLevel.fromValue(proficiencyLevel), yearsOfExperience);
    }

    /**
     * Cập nhật mức thành thạo và số năm kinh nghiệm
     */
    public void updateProficiency(ProficiencyLevel newProficiencyLevel, BigDecimal newYearsOfExperience) {
        validateProficiencyAndExperience(newProficiencyLevel, newYearsOfExperience);
        this.proficiencyLevel = newProficiencyLevel;
        this.yearsOfExperience = newYearsOfExperience;
        this.status = SkillStatus.PENDING; // Yêu cầu duyệt lại khi có thay đổi
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProficiency(int newProficiencyLevel, BigDecimal newYearsOfExperience) {
        updateProficiency(ProficiencyLevel.fromValue(newProficiencyLevel), newYearsOfExperience);
    }

    /**
     * Phê duyệt kỹ năng (Dành cho RM / VT-03) - giữ nguyên mức tự khai
     */
    public void approve(Long reviewerId, String reviewNotes) {
        if (reviewerId == null) {
            throw new IllegalArgumentException("Người duyệt không được để trống");
        }
        if (this.status != SkillStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể xác nhận kỹ năng đang ở trạng thái chờ duyệt (PENDING)");
        }
        this.status = SkillStatus.APPROVED;
        this.approvedBy = reviewerId;
        this.approvedAt = LocalDateTime.now();
        this.rejectionReason = null;
        this.reviewNotes = reviewNotes;
        this.updatedAt = LocalDateTime.now();
    }

    public void approve(Long reviewerId) {
        approve(reviewerId, null);
    }

    /**
     * Điều chỉnh mức thành thạo và phê duyệt (Dành cho RM / VT-03)
     */
    public void adjustAndApprove(Long reviewerId, ProficiencyLevel newProficiencyLevel, String reviewNotes) {
        if (reviewerId == null) {
            throw new IllegalArgumentException("Người duyệt không được để trống");
        }
        if (newProficiencyLevel == null) {
            throw new IllegalArgumentException("Mức thành thạo mới không được để trống");
        }
        if (this.status != SkillStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể xác nhận kỹ năng đang ở trạng thái chờ duyệt (PENDING)");
        }
        if (reviewNotes == null || reviewNotes.trim().isEmpty()) {
            throw new IllegalArgumentException("Bắt buộc nhập ghi chú khi điều chỉnh mức thành thạo");
        }
        this.proficiencyLevel = newProficiencyLevel;
        this.status = SkillStatus.APPROVED;
        this.approvedBy = reviewerId;
        this.approvedAt = LocalDateTime.now();
        this.rejectionReason = null;
        this.reviewNotes = reviewNotes.trim();
        this.updatedAt = LocalDateTime.now();
    }

    public void adjustAndApprove(Long reviewerId, int newProficiencyLevel, String reviewNotes) {
        adjustAndApprove(reviewerId, ProficiencyLevel.fromValue(newProficiencyLevel), reviewNotes);
    }

    private static void validateInputs(Long employeeId, Long skillId, ProficiencyLevel proficiencyLevel, BigDecimal yearsOfExperience) {
        if (employeeId == null) {
            throw new IllegalArgumentException("ID nhân viên không được để trống");
        }
        if (skillId == null) {
            throw new IllegalArgumentException("ID kỹ năng không được để trống");
        }
        validateProficiencyAndExperience(proficiencyLevel, yearsOfExperience);
    }

    private static void validateProficiencyAndExperience(ProficiencyLevel proficiencyLevel, BigDecimal yearsOfExperience) {
        if (proficiencyLevel == null) {
            throw new IllegalArgumentException("Mức thành thạo không được để trống");
        }
        if (yearsOfExperience == null || yearsOfExperience.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số năm kinh nghiệm không được nhỏ hơn 0");
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getSkillId() {
        return skillId;
    }

    public ProficiencyLevel getProficiencyLevel() {
        return proficiencyLevel;
    }

    public int getProficiencyLevelValue() {
        return proficiencyLevel != null ? proficiencyLevel.getValue() : 0;
    }

    public BigDecimal getYearsOfExperience() {
        return yearsOfExperience;
    }

    public SkillStatus getStatus() {
        return status;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getReviewNotes() {
        return reviewNotes;
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
