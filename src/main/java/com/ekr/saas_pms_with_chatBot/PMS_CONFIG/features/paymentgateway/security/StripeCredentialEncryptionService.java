
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class StripeCredentialEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private static final int IV_LENGTH = 12;

    private static final int TAG_LENGTH = 128;

    private final SecretKeySpec secretKey;

    private final SecureRandom secureRandom = new SecureRandom();

    public StripeCredentialEncryptionService(
            @Value("${payment-gateway.encryption-key}")
            String base64Key
    ) {

        byte[] keyBytes = Base64.getDecoder().decode(base64Key);

        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "Encryption key phải có độ dài 32 bytes"
            );
        }

        this.secretKey = new SecretKeySpec(
                keyBytes,
                "AES"
        );
    }

    public String encrypt(String plainText) {

        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException(
                    "Credential không được để trống"
            );
        }

        try {

            byte[] iv = new byte[IV_LENGTH];

            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH, iv)
            );

            byte[] encrypted = cipher.doFinal(
                    plainText.getBytes(
                            java.nio.charset.StandardCharsets.UTF_8
                    )
            );

            ByteBuffer buffer = ByteBuffer.allocate(
                    iv.length + encrypted.length
            );

            buffer.put(iv);
            buffer.put(encrypted);

            return Base64.getEncoder().encodeToString(
                    buffer.array()
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Không thể mã hóa Stripe Credential",
                    exception
            );
        }
    }

    public String decrypt(String encryptedText) {

        if (encryptedText == null || encryptedText.isBlank()) {
            throw new IllegalArgumentException(
                    "Encrypted Credential không hợp lệ"
            );
        }

        try {

            byte[] decoded = Base64.getDecoder().decode(
                    encryptedText
            );

            if (decoded.length <= IV_LENGTH) {
                throw new IllegalArgumentException(
                        "Encrypted Credential không hợp lệ"
                );
            }

            byte[] iv = Arrays.copyOfRange(
                    decoded,
                    0,
                    IV_LENGTH
            );

            byte[] encrypted = Arrays.copyOfRange(
                    decoded,
                    IV_LENGTH,
                    decoded.length
            );

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH, iv)
            );

            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(
                    decrypted,
                    java.nio.charset.StandardCharsets.UTF_8
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Không thể giải mã Stripe Credential",
                    exception
            );
        }
    }
}