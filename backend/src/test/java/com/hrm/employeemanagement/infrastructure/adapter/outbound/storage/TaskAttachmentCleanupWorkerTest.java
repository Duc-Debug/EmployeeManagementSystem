package com.hrm.employeemanagement.infrastructure.adapter.outbound.storage;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.port.outbound.task.comment.LoadTaskAttachmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.TaskAttachmentStoragePort;

class TaskAttachmentCleanupWorkerTest {

    private TaskAttachmentStoragePort storagePort;
    private LoadTaskAttachmentPort loadTaskAttachmentPort;
    private TaskAttachmentCleanupWorker worker;

    @BeforeEach
    void setUp() {
        storagePort = mock(TaskAttachmentStoragePort.class);
        loadTaskAttachmentPort = mock(LoadTaskAttachmentPort.class);
        worker = new TaskAttachmentCleanupWorker(storagePort, loadTaskAttachmentPort, 30L);
    }

    @Test
    void cleanupOrphanAttachments_WhenFilesAreEmpty_DoesNothing() {
        when(storagePort.listFilesOlderThan(any(Instant.class))).thenReturn(Collections.emptyList());

        worker.cleanupOrphanAttachments();

        verify(storagePort, never()).deleteFile(any());
    }

    @Test
    void cleanupOrphanAttachments_WhenFileNotInDb_DeletesOrphanFile() {
        String orphanPath = "uploads/task-attachments/task_10/orphan.pdf";
        when(storagePort.listFilesOlderThan(any(Instant.class))).thenReturn(List.of(orphanPath));
        when(loadTaskAttachmentPort.existsByFilePath(orphanPath)).thenReturn(false);

        worker.cleanupOrphanAttachments();

        verify(storagePort).deleteFile(orphanPath);
    }

    @Test
    void cleanupOrphanAttachments_WhenFileExistsInDb_PreservesFile() {
        String validPath = "uploads/task-attachments/task_10/valid.pdf";
        when(storagePort.listFilesOlderThan(any(Instant.class))).thenReturn(List.of(validPath));
        when(loadTaskAttachmentPort.existsByFilePath(validPath)).thenReturn(true);

        worker.cleanupOrphanAttachments();

        verify(storagePort, never()).deleteFile(validPath);
    }

    @Test
    void cleanupOrphanAttachments_WhenDeleteThrows_ContinuesWithNextFile() {
        String orphan1 = "uploads/task-attachments/task_10/orphan1.pdf";
        String orphan2 = "uploads/task-attachments/task_10/orphan2.pdf";
        when(storagePort.listFilesOlderThan(any(Instant.class))).thenReturn(List.of(orphan1, orphan2));
        when(loadTaskAttachmentPort.existsByFilePath(orphan1)).thenReturn(false);
        when(loadTaskAttachmentPort.existsByFilePath(orphan2)).thenReturn(false);

        doThrow(new RuntimeException("Delete error")).when(storagePort).deleteFile(orphan1);

        worker.cleanupOrphanAttachments();

        verify(storagePort).deleteFile(orphan1);
        verify(storagePort).deleteFile(orphan2);
    }
}

