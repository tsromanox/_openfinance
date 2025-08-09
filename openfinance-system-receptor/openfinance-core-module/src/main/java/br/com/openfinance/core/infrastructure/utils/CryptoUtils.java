package br.com.openfinance.core.infrastructure.utils;

import lombok.extern.slf4j.Slf4j;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
public class CryptoUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int SALT_LENGTH_BYTE = 16;

    public static String encrypt(String plainText, String password) {
        try {
            byte[] salt = getRandomNonce(SALT_LENGTH_BYTE);
            byte[] iv = getRandomNonce(IV_LENGTH_BYTE);

            SecretKey aesKey = getAESKey(password, salt);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes());
            byte[] cipherTextWithIvSalt = ByteBuffer.allocate(iv.length + salt.length + cipherText.length)
                    .put(iv)
                    .put(salt)
                    .put(cipherText)
                    .array();

            return Base64.getEncoder().encodeToString(cipherTextWithIvSalt);
        } catch (Exception e) {
            log.error("Error encrypting data", e);
            throw new RuntimeException("Encryption error", e);
        }
    }

    public static String decrypt(String cipherText, String password) {
        try {
            byte[] decode = Base64.getDecoder().decode(cipherText);

            ByteBuffer bb = ByteBuffer.wrap(decode);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            bb.get(iv);
            byte[] salt = new byte[SALT_LENGTH_BYTE];
            bb.get(salt);
            byte[] cipherTextBytes = new byte[bb.remaining()];
            bb.get(cipherTextBytes);

            SecretKey aesKey = getAESKey(password, salt);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] plainText = cipher.doFinal(cipherTextBytes);
            return new String(plainText);
        } catch (Exception e) {
            log.error("Error decrypting data", e);
            throw new RuntimeException("Decryption error", e);
        }
    }

    private static SecretKey getAESKey(String password, byte[] salt) throws Exception {
        SecretKeySpec secret = new SecretKeySpec(password.getBytes(), "AES");
        return secret;
    }

    private static byte[] getRandomNonce(int numBytes) {
        byte[] nonce = new byte[numBytes];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    public static String generateApiKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            log.error("Error generating API key", e);
            throw new RuntimeException("Key generation error", e);
        }
    }
}
