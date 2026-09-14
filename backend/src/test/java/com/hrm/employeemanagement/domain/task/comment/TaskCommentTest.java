package com.hrm.employeemanagement.domain.task.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.exception.task.InvalidCommentDataException;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.user.UserId;

class TaskCommentTest {

    @Test
    @DisplayName("Tạo ghi chú trao đổi hợp lệ thành công")
    void shouldCreateTaskCommentSuccessfully() {
        TaskId taskId = new TaskId(100L);
        UserId authorId = new UserId(1L);
        String content = "Đã cập nhật bản vẽ mô phỏng phần kết cấu móng @nguyenvana";

        TaskComment comment = TaskComment.create(taskId, authorId, content, Set.of(new UserId(2L)));

        assertNotNull(comment);
        assertEquals(taskId, comment.getTaskId());
        assertEquals(authorId, comment.getAuthorId());
        assertEquals(content, comment.getContent());
        assertEquals(1, comment.getMentionedUserIds().size());
        assertTrue(comment.getMentionedUserIds().contains(new UserId(2L)));
        assertNotNull(comment.getCreatedAt());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi nội dung trao đổi rỗng hoặc null")
    void shouldThrowExceptionWhenContentIsBlank() {
        TaskId taskId = new TaskId(100L);
        UserId authorId = new UserId(1L);

        assertThrows(InvalidCommentDataException.class, () ->
                TaskComment.create(taskId, authorId, "   ", Set.of()));

        assertThrows(InvalidCommentDataException.class, () ->
                TaskComment.create(taskId, authorId, null, Set.of()));
    }

    @Test
    @DisplayName("Cho phép nội dung rỗng khi có đính kèm tệp")
    void shouldAllowEmptyContentWhenAttachmentsExist() {
        TaskId taskId = new TaskId(100L);
        UserId authorId = new UserId(1L);

        TaskAttachment att = new TaskAttachment(
                null,
                null,
                taskId,
                "Token Discord.txt",
                "/storage/task_100/token.txt",
                37L,
                "text/plain",
                authorId,
                null);

        TaskComment comment = new TaskComment(
                null,
                taskId,
                authorId,
                "",
                java.util.List.of(att),
                Set.of(),
                null,
                null,
                0L);

        assertNotNull(comment);
        assertEquals("", comment.getContent());
        assertEquals(1, comment.getAttachments().size());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi nội dung vượt quá 5000 ký tự")
    void shouldThrowExceptionWhenContentExceedsMaxLength() {
        TaskId taskId = new TaskId(100L);
        UserId authorId = new UserId(1L);
        String longContent = "A".repeat(5001);

        assertThrows(InvalidCommentDataException.class, () ->
                TaskComment.create(taskId, authorId, longContent, Set.of()));
    }

    @Test
    @DisplayName("Cập nhật nội dung ghi chú thành công")
    void shouldUpdateContentSuccessfully() {
        TaskComment comment = TaskComment.create(new TaskId(100L), new UserId(1L), "Nội dung cũ", Set.of());
        comment.updateContent("Nội dung mới đã chỉnh sửa");

        assertEquals("Nội dung mới đã chỉnh sửa", comment.getContent());
        assertNotNull(comment.getUpdatedAt());
    }

    @Test
    @DisplayName("Không cho phép tác giả tự tag chính mình")
    void shouldNotAllowAuthorToMentionSelf() {
        TaskComment comment = TaskComment.create(new TaskId(100L), new UserId(1L), "Trao đổi", Set.of());
        comment.addMention(new UserId(1L)); // Author id
        comment.addMention(new UserId(2L)); // Colleague id

        assertFalse(comment.getMentionedUserIds().contains(new UserId(1L)));
        assertTrue(comment.getMentionedUserIds().contains(new UserId(2L)));
    }
}

