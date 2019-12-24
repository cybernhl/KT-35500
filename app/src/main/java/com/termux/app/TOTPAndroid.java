package com.termux.app;

import org.apache.commons.codec.binary.Base32;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
// https://github.com/gityf/java_demo/blob/master/demo/src/main/java/org/wyf/otp/TOTP.java
// the code is copy from https://tools.ietf.org/html/rfc6238
public class TOTPAndroid {
    private static final int[] DIGITS_POWER
        // 0 1 2 3 4 5 6 7 8
        = { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000 };

    private static final int SECRET_SIZE = 10;

    private static final int PASS_CODE_LENGTH = 6;

    private static final int INTERVAL = 30;

    private static final int WINDOW = 15;

    private static final String DEFAULT_CRYPTO = "HmacSHA1";

    private static final Random rand = new Random();

    private static final String TOTP_URL = "TOTP_URI_FORMAT = \"otpauth://totp/tdx:2fa_demo?secret=%s&issuer=tdx&algorithm=SHA1&digits=6&period=30\"";

    public static String generateSecret() {
        // Allocating the buffer
        byte[] buffer = new byte[SECRET_SIZE];

        // Filling the buffer with random numbers.
        rand.nextBytes(buffer);

        // Getting the key and converting it to Base32
        Base32 codec = new Base32();
        byte[] secretKey = Arrays.copyOf(buffer, SECRET_SIZE);
        byte[] encodedKey = codec.encode(secretKey);

        return new String(encodedKey);
    }

    public static boolean checkCode(String secret, long code) throws NoSuchAlgorithmException, InvalidKeyException {
        Base32 codec = new Base32();
        byte[] decodedKey = codec.decode(secret);

        // Window is used to check codes generated in the near past.
        // You can use this value to tune how far you're willing to go.
        int window = WINDOW;
        long currentInterval = getCurrentInterval();

        for (int i = -window; i <= window; ++i) {
            long hash = generateTOTP(decodedKey, currentInterval + i, PASS_CODE_LENGTH, DEFAULT_CRYPTO);

            if (hash == code) {
                return true;
            }
        }

        // The validation code is invalid.
        return false;
    }

    private static long getCurrentInterval() {
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        return currentTimeSeconds / INTERVAL;
    }

    /**
     * This method uses the JCE to provide the crypto algorithm. HMAC computes a
     * Hashed Message Authentication Code with the crypto hash algorithm as a
     * parameter.
     *
     * @param crypto
     *            : the crypto algorithm (HmacSHA1, HmacSHA256, HmacSHA512)
     * @param keyBytes
     *            : the bytes to use for the HMAC key
     * @param text
     *            : the message or text to be authenticated
     */
    private static byte[] hmacSha(String crypto, byte[] keyBytes, byte[] text) {
        try {
            Mac hmac;
            hmac = Mac.getInstance(crypto);
            SecretKeySpec macKey = new SecretKeySpec(keyBytes, "RAW");
            hmac.init(macKey);
            return hmac.doFinal(text);
        } catch (GeneralSecurityException gse) {
            throw new UndeclaredThrowableException(gse);
        }
    }

    /**
     * This method generates a net.tdx.TOTP value for the given set of parameters.
     *
     * @param key
     *            : the shared secret
     * @param time
     *            : a value that reflects a time
     * @param digits
     *            : number of digits to return
     * @param crypto
     *            : the crypto function to use
     *
     * @return: digits
     */

    public static int generateTOTP(byte[] key, long time, int digits, String crypto) {
     byte[] msg = ByteBuffer.allocate(8).putLong(time).array();
        byte[] hash = hmacSha(crypto, key, msg);

        // put selected bytes into result int
        int offset = hash[hash.length - 1] & 0xf;

        int binary = ((hash[offset] & 0x7f) << 24)
            | ((hash[offset + 1] & 0xff) << 16)
            | ((hash[offset + 2] & 0xff) << 8)
            | (hash[offset + 3] & 0xff);

        int otp = binary % DIGITS_POWER[digits];

        return otp;
    }

    /**
     * This method generates a net.tdx.TOTP value for the given set of parameters.
     *
     * @param key
     *            : the shared secret
     * @param time
     *            : a value that reflects a time
     * @param returnDigits
     *            : number of digits to return
     * @param crypto
     *            : the crypto function to use
     *
     * @return: digits
     */

    public static String generateTOTP(String key, String time,String returnDigits,String crypto){
        int codeDigits = Integer.decode(returnDigits).intValue();
        String result = null;
        while (time.length() < 16 )
            time = "0" + time;

        // Get the HEX in a Byte[]
        byte[] msg = hexStr2Bytes(time);
        byte[] k = hexStr2Bytes(key);
        byte[] hash = hmacSha(crypto, k, msg);

        // put selected bytes into result int
        int offset = hash[hash.length - 1] & 0xf;

        int binary =
            ((hash[offset] & 0x7f) << 24) |
                ((hash[offset + 1] & 0xff) << 16) |
                ((hash[offset + 2] & 0xff) << 8) |
                (hash[offset + 3] & 0xff);

        int otp = binary % DIGITS_POWER[codeDigits];

        result = Integer.toString(otp);
        while (result.length() < codeDigits) {
            result = "0" + result;
        }
        return result;
    }

    public static String generateTOTP(String key) {
        Base32 codec = new Base32();
        byte[] decodedKey = codec.decode(key);

        int totpNumber = generateTOTP(decodedKey, getCurrentInterval(), PASS_CODE_LENGTH, DEFAULT_CRYPTO);
        String format = "%0" + PASS_CODE_LENGTH + "d";
        return String.format(format, totpNumber);
    }

    public static String generateTOTP(String key,String time,String returnDigits){
        return generateTOTP(key, time, returnDigits, DEFAULT_CRYPTO);
    }

    public static String generateTOTP256(String key, String time,String returnDigits){
        return generateTOTP(key, time, returnDigits, "HmacSHA256");
    }

    public static String generateTOTP512(String key,String time,String returnDigits){
        return generateTOTP(key, time, returnDigits, "HmacSHA512");
    }

    private static byte[] hexStr2Bytes(String hex){
        byte[] bArray = new BigInteger("10" + hex,16).toByteArray();

        byte[] ret = new byte[bArray.length - 1];
        for (int i = 0; i < ret.length; i++)
            ret[i] = bArray[i+1];
        return ret;
    }
}
