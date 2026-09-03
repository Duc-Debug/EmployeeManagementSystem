package com.hrm.employeemanagement.domain.skill;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EmployeeSkill {

    private final Long id;
    private final Long employeeId;
    private final Long skillId;
    private int proficiencyLevel;
    private BigDecimal yearsOfExperience;
    private SkillStatus status;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    /**
     * Phương thức nghiệp vụ: Khai báo kỹ năng mới (TC-01). Luôn mặc định khởi
     * tạo ở trạng thái PENDING để Quản lý nguồn lực xác nhận.
     */
    public static EmployeeSkill declare(Long employeeId, Long skillId, int proficiencyLevel, BigDecimal yearsOfExperience) {
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

    /**
     * Cập nhật mức thành thạo và số năm kinh nghiệm
     */
    public void updateProficiency(int newProficiencyLevel, BigDecimal newYearsOfExperience) {
        validateProficiencyAndExperience(newProficiencyLevel, newYearsOfExperience);
        this.proficiencyLevel = newProficiencyLevel;
        this.yearsOfExperience = newYearsOfExperience;
        this.status = SkillStatus.PENDING; // Yêu cầu duyệt lại khi có thay đổi
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Phê duyệt kỹ năng (Dành cho RM / VT-03)
     */
    public void approve(Long reviewerId) {
        if (reviewerId == null) {
            throw new IllegalArgumentException("Người duyệt không được để trống");
        }
        this.status = SkillStatus.APPROVED;
        this.approvedBy = reviewerId;
        this.approvedAt = LocalDateTime.now();
        this.rejectionReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateInputs(Long employeeId, Long skillId, int proficiencyLevel, BigDecimal yearsOfExperience) {
        if (employeeId == null) {
            throw new IllegalArgumentException("ID nhân viên không được để trống");
        }
        if (skillId == null) {
            throw new IllegalArgumentException("ID kỹ năng không được để trống");
        }
        validateProficiencyAndExperience(proficiencyLevel, yearsOfExperience);
    }

    private static void validateProficiencyAndExperience(int proficiencyLevel, BigDecimal yearsOfExperience) {
        // Tận dụng trực tiếp Domain Type ProficiencyLevel để kiểm tra ràng buộc mức thành thạo (1-5)
        ProficiencyLevel.fromValue(proficiencyLevel);
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

    public int getProficiencyLevel() {
        return proficiencyLevel;
    }

    public ProficiencyLevel getProficiencyLevelEnum() {
        return ProficiencyLevel.fromValue(proficiencyLevel);
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
