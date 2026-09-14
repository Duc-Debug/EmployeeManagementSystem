package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.hrm.employeemanagement.application.dto.task.comment.CreateTaskCommentCommand;
import com.hrm.employeemanagement.application.dto.task.comment.TaskAttachmentDownloadResult;
import com.hrm.employeemanagement.application.dto.task.comment.TaskCommentResult;
import com.hrm.employeemanagement.application.port.inbound.task.comment.CreateTaskCommentUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.comment.DeleteTaskCommentUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.comment.DownloadTaskAttachmentUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.comment.GetTaskCommentsUseCase;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.TaskAttachmentStoragePort;
import com.hrm.employeemanagement.application.service.task.comment.TaskDiscussionAccessService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

class TaskCommentControllerTest {

    private CreateTaskCommentUseCase createTaskCommentUseCase;
    private GetTaskCommentsUseCase getTaskCommentsUseCase;
    private DeleteTaskCommentUseCase deleteTaskCommentUseCase;
    private DownloadTaskAttachmentUseCase downloadTaskAttachmentUseCase;
    private CurrentUserPort currentUserPort;
    private TaskAttachmentStoragePort taskAttachmentStoragePort;
    private TaskDiscussionAccessService accessService;
    private TaskCommentController controller;

    @BeforeEach
    void setUp() {
        createTaskCommentUseCase = mock(CreateTaskCommentUseCase.class);
        getTaskCommentsUseCase = mock(GetTaskCommentsUseCase.class);
        deleteTaskCommentUseCase = mock(DeleteTaskCommentUseCase.class);
        downloadTaskAttachmentUseCase = mock(DownloadTaskAttachmentUseCase.class);
        currentUserPort = mock(CurrentUserPort.class);
        taskAttachmentStoragePort = mock(TaskAttachmentStoragePort.class);
        accessService = mock(TaskDiscussionAccessService.class);

        controller = new TaskCommentController(
                createTaskCommentUseCase,
                getTaskCommentsUseCase,
                deleteTaskCommentUseCase,
                downloadTaskAttachmentUseCase,
                currentUserPort,
                taskAttachmentStoragePort,
                accessService);

        when(currentUserPort.getCurrentUserId()).thenReturn(Optional.of(10L));
    }

    @Test
    void getTaskComments_ReturnsComments() {
        TaskCommentResult result = new TaskCommentResult(
                1L, 100L, 10L, "User", "user@test.com", "Dev", "Test comment",
                List.of(), List.of(), LocalDateTime.now(), null, 0L);
        when(getTaskCommentsUseCase.execute(100L)).thenReturn(List.of(result));

        ResponseEntity<ApiResponse<List<TaskCommentResult>>> response = controller.getTaskComments(100L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getData().size());
        verify(getTaskCommentsUseCase).execute(100L);
    }

    @Test
    void createComment_Success() {
        TaskCommentResult result = new TaskCommentResult(
                1L, 100L, 10L, "User", "user@test.com", "Dev", "Hello",
                List.of(), List.of(), LocalDateTime.now(), null, 0L);
        when(createTaskCommentUseCase.execute(any(CreateTaskCommentCommand.class))).thenReturn(result);

        ResponseEntity<ApiResponse<TaskCommentResult>> response = controller.createComment(
                100L, "Hello", List.of(20L), null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getData().id());
        verify(accessService).requireAccess(100L, PermissionCode.TASK_DISCUSSION_CREATE);
        verify(createTaskCommentUseCase).execute(any(CreateTaskCommentCommand.class));
    }

    @Test
    void createComment_WhenDbFails_CleansUpUploadedFiles() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "file1.txt", "text/plain", "data1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "file2.txt", "text/plain", "data2".getBytes());

        when(taskAttachmentStoragePort.storeFile(eq(100L), eq("file1.txt"), any(InputStream.class), eq(5L)))
                .thenReturn("uploads/tasks/100/file1.txt");
        when(taskAttachmentStoragePort.storeFile(eq(100L), eq("file2.txt"), any(InputStream.class), eq(5L)))
                .thenReturn("uploads/tasks/100/file2.txt");

        when(createTaskCommentUseCase.execute(any(CreateTaskCommentCommand.class)))
                .thenThrow(new RuntimeException("Database error saving comment"));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                controller.createComment(100L, "Attachment comment", null, List.of(file1, file2)));

        assertEquals("Database error saving comment", exception.getMessage());
        verify(taskAttachmentStoragePort).deleteFile("uploads/tasks/100/file1.txt");
        verify(taskAttachmentStoragePort).deleteFile("uploads/tasks/100/file2.txt");
    }

    @Test
    void createComment_WhenDbFailsAndStorageDeleteFailsInitially_RetriesDeletion() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "file1.txt", "text/plain", "data1".getBytes());

        when(taskAttachmentStoragePort.storeFile(eq(100L), eq("file1.txt"), any(InputStream.class), eq(5L)))
                .thenReturn("uploads/tasks/100/file1.txt");

        when(createTaskCommentUseCase.execute(any(CreateTaskCommentCommand.class)))
                .thenThrow(new RuntimeException("Database error saving comment"));

        org.mockito.Mockito.doThrow(new RuntimeException("Temporary IO lock"))
                .doNothing()
                .when(taskAttachmentStoragePort).deleteFile("uploads/tasks/100/file1.txt");

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                controller.createComment(100L, "Attachment comment", null, List.of(file1)));

        assertEquals("Database error saving comment", exception.getMessage());
        org.mockito.Mockito.verify(taskAttachmentStoragePort, org.mockito.Mockito.times(2))
                .deleteFile("uploads/tasks/100/file1.txt");
    }

    @Test
    void createComment_WhenDbFailsAndStorageDeleteFailsAllRetries_StillThrowsDbException() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "file1.txt", "text/plain", "data1".getBytes());

        when(taskAttachmentStoragePort.storeFile(eq(100L), eq("file1.txt"), any(InputStream.class), eq(5L)))
                .thenReturn("uploads/tasks/100/file1.txt");

        when(createTaskCommentUseCase.execute(any(CreateTaskCommentCommand.class)))
                .thenThrow(new RuntimeException("Database error saving comment"));

        org.mockito.Mockito.doThrow(new RuntimeException("Permanent disk failure"))
                .when(taskAttachmentStoragePort).deleteFile("uploads/tasks/100/file1.txt");

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                controller.createComment(100L, "Attachment comment", null, List.of(file1)));

        assertEquals("Database error saving comment", exception.getMessage());
        org.mockito.Mockito.verify(taskAttachmentStoragePort, org.mockito.Mockito.times(3))
                .deleteFile("uploads/tasks/100/file1.txt");
    }

    @Test
    void deleteComment_Success() {
        ResponseEntity<ApiResponse<Void>> response = controller.deleteComment(100L, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(deleteTaskCommentUseCase).execute(100L, 1L, 10L);
    }

    @Test
    void downloadAttachment_Success() {
        ByteArrayInputStream is = new ByteArrayInputStream("file content".getBytes());
        TaskAttachmentDownloadResult downloadResult = new TaskAttachmentDownloadResult(
                "document.pdf", "application/pdf", 12L, is);

        when(downloadTaskAttachmentUseCase.downloadAttachment(50L)).thenReturn(downloadResult);

        ResponseEntity<InputStreamResource> response = controller.downloadAttachment(50L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertEquals(12L, response.getHeaders().getContentLength());
        verify(downloadTaskAttachmentUseCase).downloadAttachment(50L);
    }
}

