package com.pintu.security;

import com.pintu.common.util.Hashing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class SensitiveDataCrypto {

    private static final String PREFIX = "gcm:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public SensitiveDataCrypto(@Value("${security.data-encryption-key}") String encodedKey) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY must be Base64 encoded", ex);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY must decode to exactly 32 bytes");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank() || plaintext.startsWith(PREFIX)) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to encrypt sensitive data", ex);
        }
    }

    public String decrypt(String storedValue) {
        if (storedValue == null || storedValue.isBlank() || !storedValue.startsWith(PREFIX)) {
            return storedValue;
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(storedValue.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(payload, IV_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("Unable to decrypt sensitive data", ex);
        }
    }

    public String blindIndex(String plaintext) {
        return plaintext == null ? null : Hashing.sha256(plaintext);
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }
}
