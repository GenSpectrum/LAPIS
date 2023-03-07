package ch.ethz.lapis.util;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

// Based on https://github.com/eugenp/tutorials/blob/master/core-java-modules/core-java-security-algorithms/src/main/java/com/baeldung/aes/AESUtil.java
public class Encryptor {

    private final SecretKey secret;
    private final IvParameterSpec iv;
    private final String algorithm = "AES/CBC/PKCS5Padding";

    public Encryptor(String password, String salt) throws InvalidKeySpecException, NoSuchAlgorithmException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        // secret
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
        secret = new SecretKeySpec(factory.generateSecret(spec)
            .getEncoded(), "AES");
        // iv
        iv = new IvParameterSpec(Arrays.copyOf(salt.getBytes(), 16));
    }

    public String encrypt(String input) {
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, secret, iv);
            byte[] cipherText = cipher.doFinal(input.getBytes());
            return Base64.getEncoder()
                .encodeToString(cipherText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decrypt(String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, secret, iv);
            byte[] plainText = cipher.doFinal(Base64.getDecoder()
                .decode(cipherText));
            return new String(plainText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
