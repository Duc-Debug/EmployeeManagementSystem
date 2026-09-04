package com.hrm.employeemanagement.domain.skill;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Skill Domain Entity Unit Tests")
class SkillTest {

    @Test
    @DisplayName("Khởi tạo Skill hợp lệ thành công")
    void shouldCreateSkillSuccessfully() {
        Skill skill = new Skill(
                1L,
                "JAVA",
                "Java",
                "Backend",
                "Lập trình Java Core & Spring Boot",
                LocalDateTime.now()
        );

        assertEquals(1L, skill.getId());
        assertEquals("JAVA", skill.getCode());
        assertEquals("Java", skill.getName());
        assertEquals("Backend", skill.getCategory());
        assertEquals("Lập trình Java Core & Spring Boot", skill.getDescription());
        assertNotNull(skill.getCreatedAt());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi mã hoặc tên kỹ năng bị null/rỗng")
    void shouldThrowWhenCodeOrNameMissing() {
        assertThrows(IllegalArgumentException.class, () ->
                new Skill(1L, "", "Java", "Backend", "Desc", LocalDateTime.now())
        );

        assertThrows(IllegalArgumentException.class, () ->
                new Skill(1L, "JAVA", "", "Backend", "Desc", LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("Kiểm tra equals và hashCode của Skill")
    void testEqualsAndHashCode() {
        Skill skill1 = Skill.create("JAVA", "Java", "Backend", "Desc");
        Skill skill2 = Skill.create("REACT", "React", "Frontend", "Desc");
        Skill skill3 = Skill.create("JAVA", "Java", "Backend", "Desc");

        assertNotEquals(skill1, skill2);
        assertEquals(skill1, skill3);
        assertEquals(skill1.hashCode(), skill3.hashCode());
    }
}
