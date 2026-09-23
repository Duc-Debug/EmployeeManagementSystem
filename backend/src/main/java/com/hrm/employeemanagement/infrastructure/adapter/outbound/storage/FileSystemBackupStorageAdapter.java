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

    private Path resolveExistingBackupPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Đường dẫn file backup không được để trống");
        }
        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        if (!path.startsWith(backupDirectory)) {
            path = resolveBackupPath(filePath);
        }
        if (!path.startsWith(backupDirectory)) {
            throw new IllegalArgumentException("Backup path nằm ngoài thư mục lưu trữ cho phép: " + filePath);
        }
        if (!Files.exists(path)) {
            throw new IllegalStateException("File backup không tồn tại: " + filePath);
        }
        return path;
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
            Path path = resolveExistingBackupPath(filePath);
            return new BufferedInputStream(Files.newInputStream(path));
        } catch (IOException e) {
            throw new RuntimeException("Đọc tệp sao lưu thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String filePath) {
        if (filePath == null || filePath.isBlank()) return false;
        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            if (path.startsWith(backupDirectory) && Files.exists(path)) return true;
            Path resolved = resolveBackupPath(filePath);
            return Files.exists(resolved);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void deleteBackupFile(String filePath) {
        if (filePath == null || filePath.isBlank()) return;
        try {
            Path path = resolveExistingBackupPath(filePath);
            Files.delete(path);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể xóa file sao lưu vật lý: " + filePath, e);
        }
    }

    @Override
    public String calculateChecksum(String filePath) {
        Path path = resolveExistingBackupPath(filePath);
        try (InputStream is = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            String checksum = HexFormat.of().formatHex(digest.digest());
            if (checksum.isBlank()) {
                throw new IllegalStateException("Không thể tạo SHA-256 checksum cho file backup");
            }
            return checksum;
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException("Tính SHA-256 checksum thất bại: " + filePath, e);
        }
    }

    @Override
    public long getFileSize(String filePath) {
        Path path = resolveExistingBackupPath(filePath);
        try {
            long size = Files.size(path);
            if (size <= 0) {
                throw new IllegalStateException("File backup rỗng: " + filePath);
            }
            return size;
        } catch (IOException e) {
            throw new IllegalStateException("Không thể đọc kích thước file backup: " + filePath, e);
        }
    }
}
