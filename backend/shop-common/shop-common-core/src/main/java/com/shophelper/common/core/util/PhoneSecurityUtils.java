package com.shophelper.common.core.util;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 手机号安全处理工具：归一化、哈希、加密。
 */
public final class PhoneSecurityUtils {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";
    private static final int AES_BLOCK_SIZE = 16;

    private PhoneSecurityUtils() {
    }

    public static String normalizeChinaPhoneToE164(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new IllegalArgumentException("手机号不能为空");
        }

        String sanitized = rawPhone.trim()
                .replace(" ", "")
                .replace("-", "");

        if (sanitized.startsWith("+86")) {
            sanitized = sanitized.substring(3);
        } else if (sanitized.startsWith("86") && sanitized.length() == 13) {
            sanitized = sanitized.substring(2);
        }

        if (!sanitized.matches("^1\\d{10}$")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }

        return "+86" + sanitized;
    }

    public static String hmacSha256Hex(String rawPhone, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] digest = mac.doFinal(normalizeChinaPhoneToE164(rawPhone).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("手机号哈希计算失败", e);
        }
    }

    public static String encryptToBase64(String rawPhone, String secret) {
        try {
            byte[] iv = new byte[AES_BLOCK_SIZE];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(buildAesKey(secret), "AES"),
                    new IvParameterSpec(iv)
            );

            byte[] encrypted = cipher.doFinal(normalizeChinaPhoneToE164(rawPhone).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("手机号加密失败", e);
        }
    }

    public static String decryptFromBase64(String encryptedPhone, String secret) {
        try {
            String[] parts = encryptedPhone.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("手机号密文格式不正确");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(buildAesKey(secret), "AES"),
                    new IvParameterSpec(iv)
            );
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("手机号解密失败", e);
        }
    }

    public static String maskChinaPhone(String phone) {
        String normalizedPhone = normalizeChinaPhoneToE164(phone);
        String digits = normalizedPhone.substring(3);
        return digits.substring(0, 3) + "****" + digits.substring(7);
    }

    private static byte[] buildAesKey(String secret) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        return Arrays.copyOf(hash, AES_BLOCK_SIZE);
    }
}
