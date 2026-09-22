package com.hrm.employeemanagement.infrastructure.adapter.outbound.storage;

import com.hrm.employeemanagement.application.port.outbound.BackupStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class FileSystemBackupStorageAdapter implements BackupStoragePort {

    private static final Logger log = LoggerFactory.getLogger(FileSystemBackupStorageAdapter.class);

    private final Path backupDirectory;

    public FileSystemBackupStorageAdapter(
            @Value("${app.backup.storage-dir:uploads/backups}") String storageDir
    ) {
        this.backupDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.backupDirectory);
        } catch (IOException e) {
            log.error("Không thể tạo thư mục lưu trữ sao lưu: {}", this.backupDirectory, e);
        }
    }

    @Override
    public Path resolveBackupPath(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên tệp sao lưu không được để trống");
        }
        Path fileNameOnly = Paths.get(fileName).getFileName();
        if (fileNameOnly == null || fileNameOnly.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên tệp sao lưu không hợp lệ: " + fileName);
        }
        Path target = backupDirectory.resolve(fileNameOnly.toString()).normalize();
        if (!target.startsWith(backupDirectory)) {
            throw new IllegalArgumentException("Phát hiện nguy cơ Path Traversal không hợp lệ: " + fileName);
        }
        return target;
    }

    @Override
    public void storeBackupFile(String fileName, InputStream inputStream) {
        try {
            Files.createDirectories(backupDirectory);
            Path target = resolveBackupPath(fileName);
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Lưu tệp sao lưu thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream readBackupFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                path = resolveBackupPath(filePath);
            }
            return new BufferedInputStream(Files.newInputStream(path));
        } catch (IOException e) {
            throw new RuntimeException("Đọc tệp sao lưu thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String filePath) {
        if (filePath == null) return false;
        Path path = Paths.get(filePath);
        if (Files.exists(path)) return true;
        return Files.exists(resolveBackupPath(filePath));
    }

    @Override
    public void deleteBackupFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
            } else {
                Path altPath = resolveBackupPath(filePath);
                if (Files.exists(altPath)) {
                    Files.delete(altPath);
                }
            }
        } catch (IOException e) {
            log.warn("Không thể xóa file sao lưu vật lý: {}", filePath, e);
        }
    }

    @Override
    public String calculateChecksum(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            path = resolveBackupPath(filePath);
        }
        if (!Files.exists(path)) {
            return null;
        }
        try (InputStream is = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            log.error("Tính toán mã băm SHA-256 thất bại: {}", filePath, e);
            return null;
        }
    }

    @Override
    public long getFileSize(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                path = resolveBackupPath(filePath);
            }
            return Files.exists(path) ? Files.size(path) : 0L;
        } catch (IOException e) {
            return 0L;
        }
    }
}
