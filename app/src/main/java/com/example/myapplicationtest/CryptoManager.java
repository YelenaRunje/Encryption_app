package com.example.myapplicationtest;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoManager {
    private static final String CIPHER_INSTANCE = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_INSTANCE = "PBKDF2WithHmacSHA1";
    public static final byte[] SALT_CONSTANT = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10};
    public static final byte[] IV_CONSTANT = {0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20};
    static final int ENCRYPTION_SUCCESSFUL = 1;
    static final int DECRYPTION_SUCCESSFUL = 2;
    static final int OPERATION_FAILED = 0;

    public static boolean encryptFile(String key, String filename, byte[] initializationVector, byte[] aesSalt) {
        try {
            SecretKeySpec sks = new SecretKeySpec(getRaw(key, aesSalt), "AES");
            Cipher cipher = Cipher.getInstance(CIPHER_INSTANCE);
            cipher.init(Cipher.ENCRYPT_MODE, sks, new IvParameterSpec(initializationVector));

            FileInputStream fis = new FileInputStream(filename);
            CipherInputStream cis = new CipherInputStream(fis, cipher);
            String encryptedFilePath = filename + ".enc";
            FileOutputStream fos = new FileOutputStream(encryptedFilePath);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();
            cis.close();
            fis.close();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean decryptFile(String key, String filename, byte[] initializationVector, byte[] aesSalt) {
        String decryptedFilePath = null;
        try {
            FileInputStream fis = new FileInputStream(filename);
            SecretKeySpec sks = new SecretKeySpec(getRaw(key, aesSalt), "AES");
            Cipher cipher = Cipher.getInstance(CIPHER_INSTANCE);
            cipher.init(Cipher.DECRYPT_MODE, sks, new IvParameterSpec(initializationVector));

            decryptedFilePath = filename.substring(0, filename.length() - 4);
            FileOutputStream fos = new FileOutputStream(decryptedFilePath);
            CipherInputStream cis = new CipherInputStream(fis, cipher);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            fos.close();
            cis.close();
            fis.close();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            boolean deletionSuccessful = deleteFile(decryptedFilePath);
            return false;
        }
    }

    static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private static byte[] getRaw(String plainText, byte[] salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_INSTANCE);
            KeySpec spec = new PBEKeySpec(plainText.toCharArray(), salt, 65536, 256);
            SecretKey secretKey = factory.generateSecret(spec);
            return secretKey.getEncoded();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean deleteFile(String filename) {
        File fileToDelete = new File(filename);
        if (fileToDelete.exists()) {
            if (fileToDelete.delete()) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public static class CryptoTask extends AsyncTask<Void, Void, Integer> {
        private final String key;
        private final String filename;
        private final byte[] iv;
        private final byte[] salt;
        private final boolean isEncrypting;
        private final Handler handler;

        public CryptoTask(String key, String filename, boolean isEncrypting, Handler handler) {
            this.key = key;
            this.filename = filename;
            this.iv = IV_CONSTANT;
            this.salt = SALT_CONSTANT;
            this.isEncrypting = isEncrypting;
            this.handler = handler;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                if (isEncrypting) {
                    // Encryption code
                    boolean encryptionSuccessful = encryptFile(key, filename, iv, salt);
                    if (encryptionSuccessful && deleteFile(filename)) {
                        return ENCRYPTION_SUCCESSFUL;
                    }
                } else {
                    // Decryption code
                    boolean decryptionSuccessful = decryptFile(key, filename, iv, salt);
                    if (decryptionSuccessful && deleteFile(filename)) {
                        return DECRYPTION_SUCCESSFUL;
                    }
                }
                return OPERATION_FAILED;
            } catch (Exception e) {
                e.printStackTrace();
                return OPERATION_FAILED;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            Message message = handler.obtainMessage();
            message.arg1 = result;
            handler.sendMessage(message);
        }
    }
}
