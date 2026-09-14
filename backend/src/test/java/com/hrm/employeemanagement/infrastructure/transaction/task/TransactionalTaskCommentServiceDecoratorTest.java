package com.hrm.employeemanagement.infrastructure.transaction.task;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.hrm.employeemanagement.application.port.outbound.task.comment.LoadTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.TaskAttachmentStoragePort;
import com.hrm.employeemanagement.application.service.task.comment.TaskCommentApplicationService;
import com.hrm.employeemanagement.domain.task.comment.TaskAttachment;
import com.hrm.employeemanagement.domain.task.comment.TaskComment;
import com.hrm.employeemanagement.domain.task.comment.TaskCommentId;

class TransactionalTaskCommentServiceDecoratorTest {

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createMethodDefinesTransactionBoundary() throws Exception {
        Method method = TransactionalTaskCommentServiceDecorator.class.getMethod(
                "execute", com.hrm.employeemanagement.application.dto.task.comment.CreateTaskCommentCommand.class);
        assertTrue(method.isAnnotationPresent(Transactional.class));
    }

    @Test
    void deletesPhysicalFilesOnlyAfterDatabaseCommit() {
        TaskCommentApplicationService delegate = mock(TaskCommentApplicationService.class);
        LoadTaskCommentPort comments = mock(LoadTaskCommentPort.class);
        TaskAttachmentStoragePort storage = mock(TaskAttachmentStoragePort.class);
        TaskComment comment = mock(TaskComment.class);
        TaskAttachment attachment = mock(TaskAttachment.class);
        when(attachment.getFilePath()).thenReturn("tasks/10/evidence.pdf");
        when(comment.getAttachments()).thenReturn(List.of(attachment));
        when(comments.findById(TaskCommentId.of(20L))).thenReturn(Optional.of(comment));
        TransactionalTaskCommentServiceDecorator decorator =
                new TransactionalTaskCommentServiceDecorator(delegate, comments, storage);

        TransactionSynchronizationManager.initSynchronization();
        decorator.execute(10L, 20L, 30L);

        verify(storage, never()).deleteFile("tasks/10/evidence.pdf");
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(storage).deleteFile("tasks/10/evidence.pdf");
    }

    @Test
    void keepsPhysicalFilesWhenDatabaseTransactionRollsBack() {
        TaskCommentApplicationService delegate = mock(TaskCommentApplicationService.class);
        LoadTaskCommentPort comments = mock(LoadTaskCommentPort.class);
        TaskAttachmentStoragePort storage = mock(TaskAttachmentStoragePort.class);
        TaskComment comment = mock(TaskComment.class);
        TaskAttachment attachment = mock(TaskAttachment.class);
        when(attachment.getFilePath()).thenReturn("tasks/10/evidence.pdf");
        when(comment.getAttachments()).thenReturn(List.of(attachment));
        when(comments.findById(TaskCommentId.of(20L))).thenReturn(Optional.of(comment));
        TransactionalTaskCommentServiceDecorator decorator =
                new TransactionalTaskCommentServiceDecorator(delegate, comments, storage);

        TransactionSynchronizationManager.initSynchronization();
        decorator.execute(10L, 20L, 30L);
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        verify(storage, never()).deleteFile("tasks/10/evidence.pdf");
    }
}
