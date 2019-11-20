package com.socrata.datasync.config.userpreferences;

import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.NoSuchPaddingException;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.Arrays;
import java.util.prefs.Preferences;

import org.apache.commons.net.util.Base64;

final class CryptoUtil {
    private static final byte[] obfuscatedPrefix;
    private static final byte[] key;

    static {
        int[] ints =
            // SOCRATAOBFUS in control characters
            new int[] { 0x13, 0x0f, 0x03, 0x12, 0x01, 0x14, 0x01, 0x0f, 0x02, 0x06, 0x15, 0x13, 0x0a };
        byte[] bytes = new byte[ints.length];
        for(int i = 0; i != ints.length; ++i) {
            bytes[i] = (byte) ints[i];
        }
        obfuscatedPrefix = bytes;

        Preferences p = Preferences.userRoot().node("SocrataIntegrationPrefs");
        bytes = p.getByteArray("junk", null);
        if(bytes == null || bytes.length != 16) {
            bytes = new byte[16];
            new SecureRandom().nextBytes(bytes);
            p.putByteArray("junk", bytes);
        }
        key = bytes;
    }

    private static byte[] writeIv(OutputStream os) throws IOException {
        byte[] result = new byte[16];
        new SecureRandom().nextBytes(result);
        os.write(result);
        return result;
    }

    private static byte[] readIv(InputStream is) throws IOException {
        byte[] result = new byte[16];
        int off = 0;
        while(off < result.length) {
            int count = is.read(result, off, result.length - off);
            if(count == -1) throw new EOFException();
            else off += count;
        }
        return result;
    }

    private static Cipher cipher(int mode, byte[] iv) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        cipher.init(mode, new SecretKeySpec(Arrays.copyOf(key, key.length), "AES"), new IvParameterSpec(iv));
        return cipher;
    }

    public static String obfuscate(String s) {
        if(s == null) return null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(obfuscatedPrefix);

            try(CipherOutputStream cos = new CipherOutputStream(baos, cipher(Cipher.ENCRYPT_MODE, writeIv(baos)));
                OutputStreamWriter osw = new OutputStreamWriter(cos, StandardCharsets.UTF_8)) {
                osw.write(s);
            }

            return Base64.encodeBase64URLSafeString(baos.toByteArray());
        } catch(Exception e) {
            throw new RuntimeException("Unexpected error", e);
        }
    }

    public static String deobfuscate(String s, String ifInvalid) {
        if(s == null) return null;

        try {
            byte[] bytes = Base64.decodeBase64(s);
            if(bytes.length >= obfuscatedPrefix.length && Arrays.equals(obfuscatedPrefix, Arrays.copyOfRange(bytes, 0, obfuscatedPrefix.length))) {
                bytes = Arrays.copyOfRange(bytes, obfuscatedPrefix.length, bytes.length);
                try(ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                    CipherInputStream cis = new CipherInputStream(bais, cipher(Cipher.DECRYPT_MODE, readIv(bais)));
                    InputStreamReader isr = new InputStreamReader(cis, StandardCharsets.UTF_8)) {
                    StringBuilder sb = new StringBuilder();
                    int c;
                    while((c = isr.read()) != -1) {
                        sb.append((char) c);
                    }
                    return sb.toString();
                }
            } else {
                return s;
            }
        } catch(IOException e) {
            return ifInvalid;
        } catch(IndexOutOfBoundsException e) {
            return ifInvalid;
        } catch(Exception e) {
            throw new RuntimeException("Unexpected error", e);
        }
    }
}
