package com.hrm.employeemanagement.infrastructure.adapter.outbound.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemBackupStorageAdapterTest {

    @TempDir
    Path tempDir;

    private FileSystemBackupStorageAdapter storageAdapter;

    @BeforeEach
    void setUp() {
        storageAdapter = new FileSystemBackupStorageAdapter(tempDir.toString());
    }

    @Test
    @DisplayName("Lưu và đọc file hợp lệ thành công")
    void testStoreAndReadBackupFile_Success() throws Exception {
        String fileName = "backup_test.json";
        byte[] content = "{\"tables\": {}}".getBytes();

        storageAdapter.storeBackupFile(fileName, new ByteArrayInputStream(content));

        assertThat(storageAdapter.exists(fileName)).isTrue();
        assertThat(storageAdapter.getFileSize(fileName)).isEqualTo(content.length);
        assertThat(storageAdapter.calculateChecksum(fileName)).isNotNull();

        try (InputStream is = storageAdapter.readBackupFile(fileName)) {
            byte[] readBytes = is.readAllBytes();
            assertThat(readBytes).isEqualTo(content);
        }

        storageAdapter.deleteBackupFile(fileName);
        assertThat(storageAdapter.exists(fileName)).isFalse();
    }

    @Test
    @DisplayName("Chặn tấn công Path Traversal khi tên file chứa ../ hoặc ký tự chuyển hướng thư mục")
    void testPathTraversal_ThrowsException() {
        // Tên file độc hại cố gắng thoát khỏi thư mục backup
        String maliciousName1 = "../../../../etc/passwd";
        String maliciousName2 = "..\\..\\windows\\system32\\cmd.exe";

        // Với resolveBackupPath: Chỉ lấy fileName (basename) và giữ an toàn trong backupDirectory
        Path resolved = storageAdapter.resolveBackupPath(maliciousName1);
        assertThat(resolved.startsWith(tempDir)).isTrue();
        assertThat(resolved.getFileName().toString()).isEqualTo("passwd");

        Path resolved2 = storageAdapter.resolveBackupPath(maliciousName2);
        assertThat(resolved2.startsWith(tempDir)).isTrue();

        // Tên file rỗng hoặc null phải ném IllegalArgumentException
        assertThatThrownBy(() -> storageAdapter.resolveBackupPath(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storageAdapter.resolveBackupPath(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storageAdapter.resolveBackupPath("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
