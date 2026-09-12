package com.hrm.employeemanagement.application.port.outbound.task.comment;

import java.io.InputStream;

public interface TaskAttachmentStoragePort {
    String storeFile(Long taskId, String fileName, InputStream inputStream, long fileSize);

    InputStream loadFile(String filePath);

    void deleteFile(String filePath);
}

