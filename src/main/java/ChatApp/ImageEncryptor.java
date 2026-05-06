package ChatApp;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * Utility class for encrypting and decrypting image data using AES-256.
 * Uses PBKDF2WithHmacSHA256 for key derivation from a user-provided password.
 * Format of encrypted output: [16 bytes salt][16 bytes IV][encrypted data]
 */
public class ImageEncryptor {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;

    /**
     * Encrypt image bytes with a password.
     *
     * @param data     the raw image bytes
     * @param password the password to encrypt with
     * @return encrypted bytes (salt + IV + ciphertext)
     * @throws Exception if encryption fails
     */
    public static byte[] encrypt(byte[] data, String password) throws Exception {
        SecureRandom random = new SecureRandom();

        // Generate random salt
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);

        // Generate random IV
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);

        // Derive key from password
        SecretKey key = deriveKey(password, salt);

        // Encrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(data);

        // Combine: salt + IV + encrypted data
        byte[] result = new byte[SALT_LENGTH + IV_LENGTH + encrypted.length];
        System.arraycopy(salt, 0, result, 0, SALT_LENGTH);
        System.arraycopy(iv, 0, result, SALT_LENGTH, IV_LENGTH);
        System.arraycopy(encrypted, 0, result, SALT_LENGTH + IV_LENGTH, encrypted.length);

        return result;
    }

    /**
     * Decrypt image bytes with a password.
     *
     * @param encryptedData the encrypted bytes (salt + IV + ciphertext)
     * @param password      the password to decrypt with
     * @return the original image bytes
     * @throws Exception if decryption fails (wrong password or corrupted data)
     */
    public static byte[] decrypt(byte[] encryptedData, String password) throws Exception {
        // Extract salt
        byte[] salt = Arrays.copyOfRange(encryptedData, 0, SALT_LENGTH);

        // Extract IV
        byte[] iv = Arrays.copyOfRange(encryptedData, SALT_LENGTH, SALT_LENGTH + IV_LENGTH);

        // Extract ciphertext
        byte[] ciphertext = Arrays.copyOfRange(encryptedData, SALT_LENGTH + IV_LENGTH, encryptedData.length);

        // Derive key from password
        SecretKey key = deriveKey(password, salt);

        // Decrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(ciphertext);
    }

    /**
     * Derive an AES key from a password and salt using PBKDF2.
     */
    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
