package com.hrm.employeemanagement.domain.skill;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SkillCodeGenerator Domain Utility Unit Tests")
class SkillCodeGeneratorTest {

    @Test
    @DisplayName("Sinh mã kỹ năng mặc định đúng định dạng và không null")
    void shouldGenerateValidDefaultSkillCode() {
        String code = SkillCodeGenerator.generate();
        assertNotNull(code);
        assertTrue(code.startsWith("SKILL-"));
        assertEquals(16, code.length()); // "SKILL-" (6) + 10 chars
    }

    @Test
    @DisplayName("Sinh 1000 mã kỹ năng liên tiếp đảm bảo tính duy nhất (Collision-free)")
    void shouldGenerateUniqueSkillCodesConcurrently() {
        int count = 1000;
        Set<String> generatedCodes = new HashSet<>();

        for (int i = 0; i < count; i++) {
            String code = SkillCodeGenerator.generate();
            boolean isUnique = generatedCodes.add(code);
            assertTrue(isUnique, "Phát hiện mã trùng lặp: " + code);
        }

        assertEquals(count, generatedCodes.size());
    }

    @Test
    @DisplayName("Sinh mã kỹ năng theo prefix tùy chỉnh đúng định dạng và giới hạn độ dài")
    void shouldGenerateSkillCodeWithCustomPrefix() {
        String code = SkillCodeGenerator.generateFromPrefix("BACKEND_DEV_JAVA");
        assertNotNull(code);
        assertTrue(code.startsWith("SKILL-BACKENDDEV-"));

        String nullPrefixCode = SkillCodeGenerator.generateFromPrefix(null);
        assertTrue(nullPrefixCode.startsWith("SKILL-"));
    }
}
