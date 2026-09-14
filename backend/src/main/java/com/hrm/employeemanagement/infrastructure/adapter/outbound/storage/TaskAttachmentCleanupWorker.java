package com.hrm.employeemanagement.infrastructure.adapter.outbound.storage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.task.comment.LoadTaskAttachmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.TaskAttachmentStoragePort;

/**
 * Scheduled worker that reconciles physical task attachment files with the database.
 * Deletes orphan files that exist on disk but are not referenced in the task_attachments table,
 * providing safe lifecycle cleanup for failed uploads or rollbacked database transactions.
 */
@Component
public class TaskAttachmentCleanupWorker {

    private static final Logger log = LoggerFactory.getLogger(TaskAttachmentCleanupWorker.class);

    private final TaskAttachmentStoragePort storagePort;
    private final LoadTaskAttachmentPort loadTaskAttachmentPort;
    private final long orphanAgeMinutes;

    public TaskAttachmentCleanupWorker(
            TaskAttachmentStoragePort storagePort,
            LoadTaskAttachmentPort loadTaskAttachmentPort,
            @Value("${app.storage.task-attachments.orphan-age-minutes:30}") long orphanAgeMinutes) {
        this.storagePort = Objects.requireNonNull(storagePort, "TaskAttachmentStoragePort không được null");
        this.loadTaskAttachmentPort = Objects.requireNonNull(loadTaskAttachmentPort, "LoadTaskAttachmentPort không được null");
        this.orphanAgeMinutes = orphanAgeMinutes;
    }

    @Scheduled(fixedDelayString = "${app.storage.task-attachments.cleanup-delay-ms:3600000}")
    public void cleanupOrphanAttachments() {
        try {
            Instant threshold = Instant.now().minus(orphanAgeMinutes, ChronoUnit.MINUTES);
            List<String> files = storagePort.listFilesOlderThan(threshold);
            if (files.isEmpty()) {
                return;
            }

            int deletedCount = 0;
            for (String filePath : files) {
                try {
                    boolean existsInDb = loadTaskAttachmentPort.existsByFilePath(filePath)
                            || loadTaskAttachmentPort.existsByFilePath(filePath.replace('\\', '/'))
                            || loadTaskAttachmentPort.existsByFilePath(filePath.replace('/', '\\'));
                    if (!existsInDb) {
                        log.info("Phát hiện tệp đính kèm mồ côi (không có trong DB), tiến hành xóa: {}", filePath);
                        storagePort.deleteFile(filePath);
                        deletedCount++;
                    }
                } catch (Exception e) {
                    log.warn("Không thể xóa tệp đính kèm mồ côi: {}", filePath, e);
                }
            }
            if (deletedCount > 0) {
                log.info("Đã dọn dẹp thành công {} tệp đính kèm mồ côi.", deletedCount);
            }
        } catch (Exception e) {
            log.error("Lỗi trong quá trình quét dọn dẹp tệp đính kèm mồ côi", e);
        }
    }
}

