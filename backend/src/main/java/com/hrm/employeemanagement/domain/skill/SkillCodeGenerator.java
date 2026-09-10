package com.hrm.employeemanagement.domain.skill;

import java.util.Objects;
import java.util.UUID;

public final class SkillCodeGenerator {

    private SkillCodeGenerator() {
    }

    /**
     * Sinh mã kỹ năng chuẩn có dạng SKILL-XXXXXXXXXX duy nhất, an toàn khi chạy đồng thời.
     */
    public static String generate() {
        String uuidHex = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "SKILL-" + uuidHex.substring(0, 10);
    }

    /**
     * Sinh mã kỹ năng dựa trên tên hoặc tiền tố tùy chọn nếu cần.
     */
    public static String generateFromPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return generate();
        }
        String cleanPrefix = prefix.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (cleanPrefix.length() > 10) {
            cleanPrefix = cleanPrefix.substring(0, 10);
        }
        String uuidHex = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "SKILL-" + cleanPrefix + "-" + uuidHex.substring(0, 6);
    }
}
