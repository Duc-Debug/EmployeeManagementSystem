package com.hrm.employeemanagement.domain.skill;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.hrm.employeemanagement.domain.exception.skill.InvalidSkillMergeException;
import com.hrm.employeemanagement.domain.exception.skill.RequiredFieldMissingException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Skill Domain Entity Unit Tests")
class SkillTest {

    @Test
    @DisplayName("Khởi tạo Skill hợp lệ thành công")
    void shouldCreateSkillSuccessfully() {
        Skill skill = new Skill(
                new SkillId(1L),
                10L,
                "Java",
                "Ngôn ngữ Java",
                SkillStatus.ACTIVE,
                null,
                LocalDateTime.now(),
                null
        );

        assertEquals(1L, skill.getId().value());
        assertEquals(10L, skill.getGroupId());
        assertEquals("Java", skill.getName());
        assertEquals(SkillStatus.ACTIVE, skill.getStatus());
        assertNull(skill.getMergedIntoSkillId());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tên hoặc groupId bị null/rỗng")
    void shouldThrowWhenRequiredFieldsMissing() {
        assertThrows(RequiredFieldMissingException.class, () ->
                new Skill(new SkillId(1L), 10L, "", "Desc", SkillStatus.ACTIVE, null, null, null)
        );

        assertThrows(RequiredFieldMissingException.class, () ->
                new Skill(new SkillId(1L), null, "Java", "Desc", SkillStatus.ACTIVE, null, null, null)
        );
    }

    @Test
    @DisplayName("Vô hiệu hóa kỹ năng chuyển trạng thái sang INACTIVE")
    void shouldDeactivateSkill() {
        Skill skill = new Skill(new SkillId(1L), 10L, "Java", "Desc", SkillStatus.ACTIVE, null, null, null);
        skill.deactivate();

        assertEquals(SkillStatus.INACTIVE, skill.getStatus());
        assertNotNull(skill.getUpdatedAt());
    }

    @Test
    @DisplayName("Gộp kỹ năng chuyển trạng thái sang MERGED và lưu mergedIntoSkillId")
    void shouldMergeSkillSuccessfully() {
        Skill sourceSkill = new Skill(new SkillId(2L), 10L, "Java Programming", "Desc", SkillStatus.ACTIVE, null, null, null);
        SkillId targetId = new SkillId(1L);

        sourceSkill.mergeInto(targetId);

        assertEquals(SkillStatus.MERGED, sourceSkill.getStatus());
        assertEquals(targetId, sourceSkill.getMergedIntoSkillId());
        assertNotNull(sourceSkill.getUpdatedAt());
    }

    @Test
    @DisplayName("Không được phép tự gộp kỹ năng vào chính nó")
    void shouldThrowWhenMergingIntoItself() {
        Skill skill = new Skill(new SkillId(1L), 10L, "Java", "Desc", SkillStatus.ACTIVE, null, null, null);

        assertThrows(InvalidSkillMergeException.class, () -> skill.mergeInto(new SkillId(1L)));
    }

    @Test
    @DisplayName("Không được phép gộp kỹ năng đã bị MERGED")
    void shouldThrowWhenMergingAlreadyMergedSkill() {
        Skill skill = new Skill(new SkillId(2L), 10L, "Java", "Desc", SkillStatus.ACTIVE, null, null, null);
        skill.mergeInto(new SkillId(1L));

        assertThrows(InvalidSkillMergeException.class, () -> skill.mergeInto(new SkillId(3L)));
    }

    @Test
    @DisplayName("Cập nhật thông tin kỹ năng thành công")
    void shouldUpdateInfoSuccessfully() {
        Skill skill = new Skill(new SkillId(1L), 10L, "Java", "Desc", SkillStatus.ACTIVE, null, null, null);
        skill.updateInfo("Java 21", 20L, "New Desc");

        assertEquals("Java 21", skill.getName());
        assertEquals(20L, skill.getGroupId());
        assertEquals("New Desc", skill.getDescription());
        assertNotNull(skill.getUpdatedAt());
    }
}
