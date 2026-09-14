package com.hrm.employeemanagement.application.service.task.comment;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.task.comment.CreateTaskCommentCommand;
import com.hrm.employeemanagement.application.dto.task.comment.TaskAttachmentDownloadResult;
import com.hrm.employeemanagement.application.dto.task.comment.TaskCommentResult;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.DeleteTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.LoadTaskAttachmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.LoadTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.SaveTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.TaskAttachmentStoragePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.task.TaskAttachmentNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.TaskCommentNotFoundException;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.comment.TaskAttachment;
import com.hrm.employeemanagement.domain.task.comment.TaskAttachmentId;
import com.hrm.employeemanagement.domain.task.comment.TaskComment;
import com.hrm.employeemanagement.domain.task.comment.TaskCommentId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

class TaskCommentApplicationServiceTest {

    private LoadTaskCommentPort loadTaskCommentPort;
    private LoadTaskAttachmentPort loadTaskAttachmentPort;
    private SaveTaskCommentPort saveTaskCommentPort;
    private DeleteTaskCommentPort deleteTaskCommentPort;
    private LoadUserPort loadUserPort;
    private LoadEmployeePort loadEmployeePort;
    private SaveNotificationPort saveNotificationPort;
    private TaskAttachmentStoragePort taskAttachmentStoragePort;
    private TaskDiscussionAccessService accessService;

    private TaskCommentApplicationService service;

    @BeforeEach
    void setUp() {
        loadTaskCommentPort = mock(LoadTaskCommentPort.class);
        loadTaskAttachmentPort = mock(LoadTaskAttachmentPort.class);
        saveTaskCommentPort = mock(SaveTaskCommentPort.class);
        deleteTaskCommentPort = mock(DeleteTaskCommentPort.class);
        loadUserPort = mock(LoadUserPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        saveNotificationPort = mock(SaveNotificationPort.class);
        taskAttachmentStoragePort = mock(TaskAttachmentStoragePort.class);
        accessService = mock(TaskDiscussionAccessService.class);

        service = new TaskCommentApplicationService(
                loadTaskCommentPort,
                loadTaskAttachmentPort,
                saveTaskCommentPort,
                deleteTaskCommentPort,
                loadUserPort,
                loadEmployeePort,
                saveNotificationPort,
                taskAttachmentStoragePort,
                accessService);
    }

    @Test
    void executeCreate_FiltersOutMentionsOfUnauthorizedOrNonExistentUsers() {
        Task task = mock(Task.class);
        when(task.getName()).thenReturn("Project Task 1");
        when(task.getId()).thenReturn(TaskId.of(100L));
        when(accessService.requireCreateAccess(100L, 1L)).thenReturn(task);

        User author = mock(User.class);
        when(author.getId()).thenReturn(new UserId(1L));
        when(author.getUsername()).thenReturn("author");
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(author));

        // Candidate 2 exists and has access; candidate 3 exists but has NO access; candidate 999 doesn't exist
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(new UserId(2L));
        when(user2.getUsername()).thenReturn("user2");

        User user3 = mock(User.class);
        when(user3.getId()).thenReturn(new UserId(3L));
        when(user3.getUsername()).thenReturn("user3");

        when(loadUserPort.findAllByIdIn(any())).thenReturn(List.of(user2, user3));
        when(accessService.canUserAccess(user2, task)).thenReturn(true);
        when(accessService.canUserAccess(user3, task)).thenReturn(false);

        when(saveTaskCommentPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateTaskCommentCommand command = new CreateTaskCommentCommand(
                100L, 1L, "Hello @user2 and @user3", Set.of(2L, 3L, 999L), List.of());

        TaskCommentResult result = service.execute(command);

        assertNotNull(result);
        // Only user2 should be notified!
        verify(saveNotificationPort).save(any(Notification.class));
        verify(accessService).canUserAccess(user2, task);
        verify(accessService).canUserAccess(user3, task);
    }

    @Test
    void executeDelete_DelegatesToAccessServiceAndDeletes() {
        TaskComment comment = mock(TaskComment.class);
        when(comment.getTaskId()).thenReturn(TaskId.of(100L));
        when(comment.getAuthorId()).thenReturn(new UserId(1L));
        when(loadTaskCommentPort.findById(TaskCommentId.of(5L))).thenReturn(Optional.of(comment));

        service.execute(100L, 5L, 1L);

        verify(accessService).requireDeleteAccess(100L, 1L, 1L);
        verify(deleteTaskCommentPort).deleteById(TaskCommentId.of(5L));
    }

    @Test
    void executeDelete_ThrowsExceptionWhenTaskDoesNotMatch() {
        TaskComment comment = mock(TaskComment.class);
        when(comment.getTaskId()).thenReturn(TaskId.of(200L));
        when(loadTaskCommentPort.findById(TaskCommentId.of(5L))).thenReturn(Optional.of(comment));

        assertThrows(TaskCommentNotFoundException.class,
                () -> service.execute(100L, 5L, 1L));

        verify(deleteTaskCommentPort, never()).deleteById(any());
    }

    @Test
    void downloadAttachment_Success() {
        TaskAttachment attachment = mock(TaskAttachment.class);
        when(attachment.getTaskId()).thenReturn(TaskId.of(100L));
        when(attachment.getFileName()).thenReturn("report.pdf");
        when(attachment.getFileType()).thenReturn("application/pdf");
        when(attachment.getFileSize()).thenReturn(1024L);
        when(attachment.getFilePath()).thenReturn("uploads/tasks/100/report.pdf");

        when(loadTaskAttachmentPort.findById(TaskAttachmentId.of(50L))).thenReturn(Optional.of(attachment));
        ByteArrayInputStream is = new ByteArrayInputStream("pdf-content".getBytes());
        when(taskAttachmentStoragePort.loadFile("uploads/tasks/100/report.pdf")).thenReturn(is);

        TaskAttachmentDownloadResult result = service.downloadAttachment(50L);

        assertNotNull(result);
        assertEquals("report.pdf", result.fileName());
        assertEquals("application/pdf", result.fileType());
        assertEquals(1024L, result.fileSize());
        verify(accessService).requireAccess(100L, PermissionCode.TASK_DISCUSSION_READ);
        verify(taskAttachmentStoragePort).loadFile("uploads/tasks/100/report.pdf");
    }

    @Test
    void downloadAttachment_NotFound_ThrowsException() {
        when(loadTaskAttachmentPort.findById(TaskAttachmentId.of(999L))).thenReturn(Optional.empty());

        assertThrows(TaskAttachmentNotFoundException.class,
                () -> service.downloadAttachment(999L));
    }
}

