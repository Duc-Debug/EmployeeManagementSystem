package com.hrm.employeemanagement.infrastructure.transaction.task;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.hrm.employeemanagement.application.dto.task.comment.CreateTaskCommentCommand;
import com.hrm.employeemanagement.application.dto.task.comment.TaskAttachmentDownloadResult;
import com.hrm.employeemanagement.application.dto.task.comment.TaskCommentResult;
import com.hrm.employeemanagement.application.port.inbound.task.comment.CreateTaskCommentUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.comment.DeleteTaskCommentUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.comment.DownloadTaskAttachmentUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.comment.GetTaskCommentsUseCase;
import com.hrm.employeemanagement.application.port.outbound.task.comment.LoadTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.TaskAttachmentStoragePort;
import com.hrm.employeemanagement.application.service.task.comment.TaskCommentApplicationService;
import com.hrm.employeemanagement.domain.task.comment.TaskCommentId;

/** Transaction boundary for comment, attachment metadata, and mention notifications. */
public class TransactionalTaskCommentServiceDecorator
        implements CreateTaskCommentUseCase, GetTaskCommentsUseCase, DeleteTaskCommentUseCase, DownloadTaskAttachmentUseCase {

    private final TaskCommentApplicationService delegate;
    private final LoadTaskCommentPort loadTaskCommentPort;
    private final TaskAttachmentStoragePort storagePort;

    public TransactionalTaskCommentServiceDecorator(TaskCommentApplicationService delegate,
            LoadTaskCommentPort loadTaskCommentPort, TaskAttachmentStoragePort storagePort) {
        this.delegate = Objects.requireNonNull(delegate);
        this.loadTaskCommentPort = Objects.requireNonNull(loadTaskCommentPort);
        this.storagePort = Objects.requireNonNull(storagePort);
    }

    @Override
    @Transactional
    public TaskCommentResult execute(CreateTaskCommentCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskCommentResult> execute(Long taskId) {
        return delegate.execute(taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskAttachmentDownloadResult downloadAttachment(Long attachmentId) {
        return delegate.downloadAttachment(attachmentId);
    }

    @Override
    @Transactional
    public void execute(Long taskId, Long commentId, Long requestingUserId) {
        List<String> filePaths = loadTaskCommentPort.findById(TaskCommentId.of(commentId))
                .map(comment -> comment.getAttachments().stream().map(a -> a.getFilePath()).toList())
                .orElseGet(List::of);

        delegate.execute(taskId, commentId, requestingUserId);

        if (!filePaths.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    filePaths.forEach(TransactionalTaskCommentServiceDecorator.this::deleteQuietly);
                }
            });
        }
    }

    private void deleteQuietly(String filePath) {
        try {
            storagePort.deleteFile(filePath);
        } catch (RuntimeException ignored) {
            // The database transaction is already committed; cleanup may be retried separately.
        }
    }
}
