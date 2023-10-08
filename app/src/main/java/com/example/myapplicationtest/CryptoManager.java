package com.example.myapplicationtest;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoManager {
    private static final String CIPHER_INSTANCE = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_INSTANCE = "PBKDF2WithHmacSHA1";
    public static byte[] aesSalt;
    public static byte[] initializationVector;
    static final int ENCRYPTION_SUCCESSFUL = 1;
    static final int DECRYPTION_SUCCESSFUL = 2;
    static final int OPERATION_FAILED = 0;

    public static boolean encryptFile(String key, String filename) {
        try {
            initializationVector = randomBytes();
            aesSalt = randomBytes();

            Log.d("CryptoManager", "IV enc: " + byteArrayToHexString(initializationVector));
            Log.d("CryptoManager", "salt enc: " + byteArrayToHexString(aesSalt));

            SecretKeySpec sks = new SecretKeySpec(getRaw(key, aesSalt), "AES");
            Cipher cipher = Cipher.getInstance(CIPHER_INSTANCE);
            cipher.init(Cipher.ENCRYPT_MODE, sks, new IvParameterSpec(initializationVector));

            FileInputStream fis = new FileInputStream(filename);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(aesSalt);
            outputStream.write(initializationVector);

            CipherOutputStream cos = new CipherOutputStream(outputStream, cipher);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
            fis.close();
            cos.close();

            byte[] encryptedData = outputStream.toByteArray();

            FileOutputStream fos = new FileOutputStream(filename + ".enc");
            fos.write(encryptedData);
            fos.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean decryptFile(String key, String filename) {
        String decryptedFilePath = null;
        try {
            FileInputStream fis = new FileInputStream(filename);
            byte[] headerBytes = new byte[32];
            fis.read(headerBytes);

            aesSalt = Arrays.copyOfRange(headerBytes, 0, 16);
            initializationVector = Arrays.copyOfRange(headerBytes, 16, 32);

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

    public static byte[] randomBytes(){
        byte[] values = new byte[16];
        SecureRandom rnd = new SecureRandom();
        rnd.nextBytes(values);
        return values;
    }

    public static class CryptoTask extends AsyncTask<Void, Void, Integer> {
        private final String key;
        private final String filename;
        //private final byte[] iv;
        //private final byte[] salt;
        private final boolean isEncrypting;
        private final Handler handler;

        public CryptoTask(String key, String filename, boolean isEncrypting, Handler handler) {
            this.key = key;
            this.filename = filename;
            //this.iv = randomBytes();
            //this.salt = randomBytes();
            this.isEncrypting = isEncrypting;
            this.handler = handler;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                if (isEncrypting) {
                    // Encryption code
                    boolean encryptionSuccessful = encryptFile(key, filename);
                    if (encryptionSuccessful && deleteFile(filename)) {
                        return ENCRYPTION_SUCCESSFUL;
                    }
                } else {
                    // Decryption code
                    boolean decryptionSuccessful = decryptFile(key, filename);
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
