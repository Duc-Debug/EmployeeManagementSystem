package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.email;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA AttributeConverter that encrypts reset tokens at rest in the database using AES-256 GCM.
 * Tokens are stored as ciphertext (prefixed with "ENC:") and decrypted when read by the entity.
 */
@Component
@Converter
public class PasswordResetTokenEncryptionConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetTokenEncryptionConverter.class);

    private static final String ENC_PREFIX = "ENC:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private static volatile String staticEncryptionKey;

    private final String instanceEncryptionKey;

    public PasswordResetTokenEncryptionConverter(
            @Value("${app.outbox.encryption-key:${app.backup.encryption-key:${APP_OUTBOX_ENCRYPTION_KEY:${APP_BACKUP_ENCRYPTION_KEY:${jwt.secret:${JWT_SECRET:local-dev-outbox-aes-key-32-chars-minimum}}}}}}")
            String encryptionKey
    ) {
        this.instanceEncryptionKey = encryptionKey;
        if (encryptionKey != null && !encryptionKey.isBlank()) {
            staticEncryptionKey = encryptionKey;
        }
    }

    public PasswordResetTokenEncryptionConverter() {
        this.instanceEncryptionKey = null;
    }

    public static void setStaticEncryptionKey(String key) {
        staticEncryptionKey = key;
    }

    private String getEffectiveKey() {
        if (instanceEncryptionKey != null && !instanceEncryptionKey.isBlank()) {
            return instanceEncryptionKey;
        }
        if (staticEncryptionKey != null && !staticEncryptionKey.isBlank()) {
            return staticEncryptionKey;
        }
        String envKey = System.getenv("APP_OUTBOX_ENCRYPTION_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey;
        }
        String backupEnvKey = System.getenv("APP_BACKUP_ENCRYPTION_KEY");
        if (backupEnvKey != null && !backupEnvKey.isBlank()) {
            return backupEnvKey;
        }
        String jwtEnv = System.getenv("JWT_SECRET");
        if (jwtEnv != null && !jwtEnv.isBlank()) {
            return jwtEnv;
        }
        return "local-dev-outbox-aes-key-32-chars-minimum";
    }

    private byte[] getDerivedKey() {
        String key = getEffectiveKey();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        if (attribute.startsWith(ENC_PREFIX)) {
            return attribute;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(getDerivedKey(), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] plaintext = attribute.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintext);

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return ENC_PREFIX + Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Failed to encrypt reset token for outbox storage", e);
            throw new IllegalStateException("Failed to encrypt reset token for outbox storage", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        if (!dbData.startsWith(ENC_PREFIX)) {
            // Backward compatibility for unencrypted legacy rows
            return dbData;
        }

        try {
            String base64Payload = dbData.substring(ENC_PREFIX.length());
            byte[] combined = Base64.getDecoder().decode(base64Payload);

            if (combined.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Encrypted reset token payload is corrupted (too short)");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

            int ciphertextLen = combined.length - GCM_IV_LENGTH;
            byte[] ciphertext = new byte[ciphertextLen];
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertextLen);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(getDerivedKey(), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt reset token from outbox storage", e);
            throw new IllegalStateException("Failed to decrypt reset token from outbox storage", e);
        }
    }
}
