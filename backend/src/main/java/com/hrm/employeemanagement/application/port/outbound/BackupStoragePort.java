package com.hrm.employeemanagement.application.port.outbound;

import java.io.InputStream;
import java.nio.file.Path;

public interface BackupStoragePort {
    Path resolveBackupPath(String fileName);
    void storeBackupFile(String fileName, InputStream inputStream);
    InputStream readBackupFile(String filePath);
    boolean exists(String filePath);
    void deleteBackupFile(String filePath);
    String calculateChecksum(String filePath);
    long getFileSize(String filePath);
}
