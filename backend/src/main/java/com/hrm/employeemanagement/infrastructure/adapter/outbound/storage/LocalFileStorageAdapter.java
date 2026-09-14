package com.hrm.employeemanagement.infrastructure.adapter.outbound.storage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.task.comment.TaskAttachmentStoragePort;

@Component
public class LocalFileStorageAdapter implements TaskAttachmentStoragePort {

    private final Path baseStorageLocation;

    public LocalFileStorageAdapter(
            @Value("${app.storage.task-attachments-dir:./uploads/task-attachments}") String storageDir) {
        this.baseStorageLocation = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Không thể khởi tạo thư mục lưu trữ file đính kèm", e);
        }
    }

    @Override
    public String storeFile(Long taskId, String fileName, InputStream inputStream, long fileSize) {
        try {
            String safeFileName = sanitizeFileName(fileName);
            String storedFileName = UUID.randomUUID().toString().substring(0, 8) + "_" + safeFileName;
            Path taskDir = this.baseStorageLocation.resolve("task_" + taskId).normalize();
            Files.createDirectories(taskDir);

            Path targetPath = taskDir.resolve(storedFileName).normalize();
            ensureWithinStorage(targetPath);
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi lưu trữ tệp đính kèm: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream loadFile(String filePath) {
        try {
            Path path = resolveStoredPath(filePath);
            if (!Files.exists(path)) {
                throw new RuntimeException("Tệp đính kèm không tồn tại: " + filePath);
            }
            return new FileInputStream(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc tệp đính kèm: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        try {
            Path path = resolveStoredPath(filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Không thể xóa file lưu trữ: " + filePath, e);
        }
    }

    @Override
    public List<String> listFilesOlderThan(Instant threshold) {
        if (!Files.exists(baseStorageLocation)) {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.walk(baseStorageLocation)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toInstant().isBefore(threshold);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .map(Path::toString)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi quét thư mục lưu trữ tệp đính kèm: " + e.getMessage(), e);
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "attachment";
        }
        return Paths.get(fileName).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Path resolveStoredPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Đường dẫn tệp đính kèm không hợp lệ");
        }
        Path path = Paths.get(filePath);
        if (!path.isAbsolute()) {
            path = baseStorageLocation.resolve(path);
        }
        path = path.toAbsolutePath().normalize();
        ensureWithinStorage(path);
        return path;
    }

    private void ensureWithinStorage(Path path) {
        if (!path.startsWith(baseStorageLocation)) {
            throw new IllegalArgumentException("Không được phép truy cập tệp ngoài thư mục lưu trữ");
        }
    }
}
