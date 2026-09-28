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
 * Managed by Spring via BeanContainer, injecting 'app.outbox.encryption-key'.
 */
@Component
@Converter
public class PasswordResetTokenEncryptionConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetTokenEncryptionConverter.class);

    private static final String ENC_PREFIX = "ENC:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private final String encryptionKey;

    public PasswordResetTokenEncryptionConverter(
            @Value("${app.outbox.encryption-key:${APP_OUTBOX_ENCRYPTION_KEY:}}") String encryptionKey
    ) {
        this.encryptionKey = encryptionKey;
    }

    private byte[] getDerivedKey() {
        if (encryptionKey == null || encryptionKey.trim().isBlank()) {
            throw new IllegalStateException(
                    "Outbox token encryption key is missing or not configured. " +
                    "Please configure 'app.outbox.encryption-key' or 'APP_OUTBOX_ENCRYPTION_KEY'."
            );
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(encryptionKey.trim().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
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
            return "";
        }
        if (!dbData.startsWith(ENC_PREFIX)) {
            throw new IllegalStateException(
                    "Security Violation: Detected unencrypted reset token at rest in database. " +
                    "All reset tokens must be encrypted (with prefix 'ENC:') or cleared."
            );
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
