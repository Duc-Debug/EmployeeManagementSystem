package com.hrm.employeemanagement.domain.skill;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmployeeSkill Domain Entity Unit Tests")
class EmployeeSkillTest {

    @Test
    @DisplayName("Khai báo kỹ năng mới (declare): Khởi tạo trạng thái PENDING")
    void declareSkill_Success() {
        EmployeeSkill skill = EmployeeSkill.declare(101L, 1L, ProficiencyLevel.INTERMEDIATE, new BigDecimal("2.0"));

        assertNotNull(skill);
        assertEquals(101L, skill.getEmployeeId());
        assertEquals(1L, skill.getSkillId());
        assertEquals(2, skill.getProficiencyLevelValue());
        assertEquals(new BigDecimal("2.0"), skill.getYearsOfExperience());
        assertEquals(SkillStatus.PENDING, skill.getStatus());
        assertNull(skill.getApprovedBy());
        assertNull(skill.getApprovedAt());
        assertTrue(skill.isPendingReview());
    }

    @Test
    @DisplayName("Duyệt kỹ năng mới (approve): Cập nhật trạng thái APPROVED, ghi nhận approvedBy và approvedAt")
    void approveNewSkill_Success() {
        EmployeeSkill skill = EmployeeSkill.declare(101L, 1L, ProficiencyLevel.ADVANCED, new BigDecimal("3.0"));
        Long reviewerId = 50L;

        skill.approve(reviewerId, "Đạt yêu cầu");

        assertEquals(SkillStatus.APPROVED, skill.getStatus());
        assertEquals(3, skill.getProficiencyLevelValue());
        assertEquals(new BigDecimal("3.0"), skill.getYearsOfExperience());
        assertEquals(reviewerId, skill.getApprovedBy());
        assertNotNull(skill.getApprovedAt());
        assertEquals("Đạt yêu cầu", skill.getReviewNotes());
        assertFalse(skill.isPendingReview());
    }

    @Test
    @DisplayName("Từ chối kỹ năng mới (reject): Chuyển sang REJECTED, ghi nhận reviewer và lý do từ chối")
    void rejectNewSkill_Success() {
        EmployeeSkill skill = EmployeeSkill.declare(101L, 1L, ProficiencyLevel.ADVANCED, new BigDecimal("3.0"));
        Long reviewerId = 50L;

        skill.reject(reviewerId, "Chưa đủ chứng chỉ chứng minh");

        assertEquals(SkillStatus.REJECTED, skill.getStatus());
        assertEquals("Chưa đủ chứng chỉ chứng minh", skill.getRejectionReason());
        assertEquals(reviewerId, skill.getApprovedBy());
        assertNotNull(skill.getApprovedAt());
        assertFalse(skill.isPendingReview());
    }

    @Test
    @DisplayName("Nhân viên cập nhật kỹ năng đã duyệt: Lưu thông tin vào pending fields, giữ nguyên APPROVED")
    void updateProficiency_OnApprovedSkill_SetsPendingFields() {
        LocalDateTime approvedTime = LocalDateTime.of(2026, 1, 1, 9, 0, 0);
        EmployeeSkill skill = new EmployeeSkill(
                1L, 101L, 1L, ProficiencyLevel.INTERMEDIATE, new BigDecimal("2.0"),
                SkillStatus.APPROVED, 10L, approvedTime, null, "Approved initially",
                2, new BigDecimal("2.0"), null, null,
                LocalDateTime.now(), LocalDateTime.now(), 1L
        );

        skill.updateProficiency(ProficiencyLevel.ADVANCED, new BigDecimal("3.5"));

        assertEquals(SkillStatus.APPROVED, skill.getStatus());
        assertEquals(2, skill.getProficiencyLevelValue(), "Giá trị hiện tại không đổi khi chờ duyệt");
        assertEquals(new BigDecimal("2.0"), skill.getYearsOfExperience());
        assertEquals(3, skill.getPendingProficiencyLevel());
        assertEquals(new BigDecimal("3.5"), skill.getPendingYearsOfExperience());
        assertTrue(skill.isPendingReview());
    }

    @Test
    @DisplayName("Từ chối yêu cầu cập nhật (pending update): Giữ nguyên status=APPROVED, level cũ, và KHÔNG ghi đè approvedBy / approvedAt")
    void rejectPendingUpdate_PreservesApprovedByAndApprovedAt() {
        Long originalApprover = 10L;
        LocalDateTime originalApprovedAt = LocalDateTime.of(2026, 1, 1, 9, 0, 0);
        EmployeeSkill skill = new EmployeeSkill(
                1L, 101L, 1L, ProficiencyLevel.INTERMEDIATE, new BigDecimal("2.0"),
                SkillStatus.APPROVED, originalApprover, originalApprovedAt, null, "Approved initially",
                2, new BigDecimal("2.0"), null, null,
                LocalDateTime.now(), LocalDateTime.now(), 1L
        );

        // Employee đề xuất nâng lên Level 4, 3.5 năm
        skill.updateProficiency(ProficiencyLevel.PROFICIENT, new BigDecimal("3.5"));
        assertEquals(4, skill.getPendingProficiencyLevel());

        // Reviewer khác (id = 99L) từ chối đề xuất nâng level
        Long rejectingReviewer = 99L;
        skill.reject(rejectingReviewer, "Chưa đủ dự án thực tế để lên Senior");

        // Verify:
        assertEquals(SkillStatus.APPROVED, skill.getStatus(), "Trạng thái kỹ năng vẫn là APPROVED");
        assertEquals(2, skill.getProficiencyLevelValue(), "Mức thành thạo giữ nguyên Level 2");
        assertEquals(new BigDecimal("2.0"), skill.getYearsOfExperience(), "Năm kinh nghiệm giữ nguyên 2.0");
        assertNull(skill.getPendingProficiencyLevel(), "Pending level phải được dọn sạch");
        assertNull(skill.getPendingYearsOfExperience(), "Pending years phải được dọn sạch");
        assertEquals("Chưa đủ dự án thực tế để lên Senior", skill.getRejectionReason());

        // QUAN TRỌNG: approvedBy và approvedAt không bị ghi đè bởi người reject (99L)
        assertEquals(originalApprover, skill.getApprovedBy(), "approvedBy phải giữ nguyên người approve ban đầu (10L)");
        assertEquals(originalApprovedAt, skill.getApprovedAt(), "approvedAt phải giữ nguyên thời điểm approve ban đầu");
        assertFalse(skill.isPendingReview());
    }

    @Test
    @DisplayName("Duyệt yêu cầu cập nhật (pending update): Cập nhật level mới, ghi đè approvedBy và approvedAt mới")
    void approvePendingUpdate_UpdatesValuesAndApprover() {
        Long originalApprover = 10L;
        LocalDateTime originalApprovedAt = LocalDateTime.of(2026, 1, 1, 9, 0, 0);
        EmployeeSkill skill = new EmployeeSkill(
                1L, 101L, 1L, ProficiencyLevel.INTERMEDIATE, new BigDecimal("2.0"),
                SkillStatus.APPROVED, originalApprover, originalApprovedAt, null, "Approved initially",
                2, new BigDecimal("2.0"), null, null,
                LocalDateTime.now(), LocalDateTime.now(), 1L
        );

        // Employee đề xuất nâng lên Level 4, 3.5 năm
        skill.updateProficiency(ProficiencyLevel.PROFICIENT, new BigDecimal("3.5"));

        // Reviewer 50L duyệt
        Long newReviewer = 50L;
        skill.approve(newReviewer, "Đồng ý nâng cấp");

        assertEquals(SkillStatus.APPROVED, skill.getStatus());
        assertEquals(4, skill.getProficiencyLevelValue());
        assertEquals(new BigDecimal("3.5"), skill.getYearsOfExperience());
        assertNull(skill.getPendingProficiencyLevel());
        assertNull(skill.getPendingYearsOfExperience());
        assertEquals(newReviewer, skill.getApprovedBy(), "approvedBy cập nhật người duyệt mới");
        assertNotNull(skill.getApprovedAt());
        assertFalse(skill.isPendingReview());
    }
}
