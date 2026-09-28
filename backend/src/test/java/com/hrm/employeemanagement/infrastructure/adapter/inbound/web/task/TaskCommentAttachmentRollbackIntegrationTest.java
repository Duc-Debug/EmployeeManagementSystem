package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.application.service.task.comment.TaskDiscussionAccessService;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.repository.SpringDataTaskCommentRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.EmployeeRepositoryAdapter;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.UserRepositoryAdapter;

@SpringBootTest(properties = {
        "app.storage.task-attachments-dir=target/test-uploads/task-comment-rollback",
        "app.storage.task-attachments.cleanup-delay-ms=86400000"
})
@ActiveProfiles("test")
class TaskCommentAttachmentRollbackIntegrationTest {

    private static final long MISSING_TASK_ID = 9_999_991L;
    private static final long MISSING_USER_ID = 9_999_992L;
    private static final Path STORAGE_DIR = Path.of(
            "target/test-uploads/task-comment-rollback").toAbsolutePath().normalize();

    @Autowired
    private TaskCommentController controller;

    @Autowired
    private SpringDataTaskCommentRepository commentRepository;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @MockitoBean
    private TaskDiscussionAccessService accessService;

    @MockitoBean
    private UserRepositoryAdapter loadUserPort;

    @MockitoBean
    private EmployeeRepositoryAdapter loadEmployeePort;

    @BeforeEach
    void setUp() throws IOException {
        deleteStorageDirectory();

        Task task = mock(Task.class);
        when(task.getId()).thenReturn(TaskId.of(MISSING_TASK_ID));
        when(task.getProjectId()).thenReturn(new ProjectId(1L));
        when(task.getName()).thenReturn("Rollback task");
        when(accessService.requireCreateAccess(MISSING_TASK_ID, MISSING_USER_ID)).thenReturn(task);

        User author = mock(User.class);
        UserId authorId = new UserId(MISSING_USER_ID);
        when(author.getId()).thenReturn(authorId);
        when(author.getUsername()).thenReturn("rollback-author");
        when(author.getEmail()).thenReturn("rollback-author@example.com");
        when(loadUserPort.findById(authorId)).thenReturn(Optional.of(author));
        when(loadUserPort.findAllByIdIn(any())).thenReturn(List.of(author));
        when(loadEmployeePort.findByUserId(authorId)).thenReturn(Optional.empty());
        when(loadEmployeePort.findAllByUserIdIn(any())).thenReturn(List.of());
        when(currentUserPort.getCurrentUserId()).thenReturn(Optional.of(MISSING_USER_ID));
    }

    @AfterEach
    void tearDown() throws IOException {
        deleteStorageDirectory();
    }

    @Test
    void createComment_WhenDatabaseCommitFails_RollsBackMetadataAndDeletesPhysicalFile() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "rollback.txt", "text/plain", "rollback-content".getBytes());

        assertThrows(RuntimeException.class, () -> controller.createComment(
                MISSING_TASK_ID, "Force a foreign-key failure", null, List.of(file)));

        assertEquals(0, commentRepository.findByTaskIdOrderByCreatedAtAsc(MISSING_TASK_ID).size());
        assertEquals(0, countStoredFiles());
    }

    private long countStoredFiles() {
        if (!Files.exists(STORAGE_DIR)) {
            return 0;
        }
        try (var paths = Files.walk(STORAGE_DIR)) {
            return paths.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteStorageDirectory() throws IOException {
        if (!Files.exists(STORAGE_DIR)) {
            return;
        }
        try (var paths = Files.walk(STORAGE_DIR)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
