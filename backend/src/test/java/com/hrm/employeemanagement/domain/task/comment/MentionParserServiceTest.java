package com.hrm.employeemanagement.domain.task.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MentionParserServiceTest {

    private final MentionParserService service = new MentionParserService();

    @Test
    @DisplayName("Trích xuất chính xác danh sách các username có tiền tố @")
    void shouldExtractUsernamesCorrectly() {
        String content = "Chào @anh.tuan và @tran_van_b, vui lòng xem file mô phỏng mới nhất nhé!";
        Set<String> usernames = service.extractMentionedUsernames(content);

        assertEquals(2, usernames.size());
        assertTrue(usernames.contains("anh.tuan"));
        assertTrue(usernames.contains("tran_van_b"));
    }

    @Test
    @DisplayName("Trả về tập rỗng khi không có mention hoặc chuỗi rỗng")
    void shouldReturnEmptySetWhenNoMentions() {
        assertTrue(service.extractMentionedUsernames("Không có ai được tag ở đây").isEmpty());
        assertTrue(service.extractMentionedUsernames("").isEmpty());
        assertTrue(service.extractMentionedUsernames(null).isEmpty());
    }
}

