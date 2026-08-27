package com.hrm.employeemanagement.domain.skill;

import java.time.LocalDateTime;

public class EmployeeSkill {

    private final Long id;
    private final Long employeeId;
    private final Long skillId;
    private int proficiencyLevel;
    private double yearsOfExperience;
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
            double yearsOfExperience,
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
    public static EmployeeSkill declare(Long employeeId, Long skillId, int proficiencyLevel, double yearsOfExperience) {
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
    public void updateProficiency(int newProficiencyLevel, double newYearsOfExperience) {
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

    private static void validateInputs(Long employeeId, Long skillId, int proficiencyLevel, double yearsOfExperience) {
        if (employeeId == null) {
            throw new IllegalArgumentException("ID nhân viên không được để trống");
        }
        if (skillId == null) {
            throw new IllegalArgumentException("ID kỹ năng không được để trống");
        }
        validateProficiencyAndExperience(proficiencyLevel, yearsOfExperience);
    }

    private static void validateProficiencyAndExperience(int proficiencyLevel, double yearsOfExperience) {
        if (proficiencyLevel < 1 || proficiencyLevel > 5) {
            throw new IllegalArgumentException("Mức thành thạo phải từ 1 đến 5");
        }
        if (yearsOfExperience < 0) {
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

    public double getYearsOfExperience() {
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
