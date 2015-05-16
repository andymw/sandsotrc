package sand.common;

import java.security.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import common.codec.*;
//import org.apache.commons.codec.binary.Base32;

public class TwoFactorAuth {

    public static String createSecret() {
        // the size needed for google's code to work
        byte[] buffer = new byte[80 / 8 + 5 * 4];

        Base32 codec = new Base32();
        new Random().nextBytes(buffer);

        byte[] secretKey = Arrays.copyOf(buffer, 10);
        byte[] byteEncodedKey = codec.encode(secretKey);
        String encodedKey = new String(byteEncodedKey);

        return encodedKey;
    }

    public static String getQRcodeURL(String secret, String host, String username) {
        String format = "http://www.google.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=otpauth://totp/%s@%s%%3Fsecret%%3D%s";
        return String.format(format, host, username, secret);
    }

    public static boolean getCodeVerification(String secret, long inputCode, long t) {

        boolean isVerified = false;
        int validWindow = 3;
        Base32 codec = new Base32();
        byte[] decodedKey = codec.decode(secret);

        //System.out.println("decodedKey: "+decodedKey.toString());
        //System.out.println("secret: "+secret);

        // validWindow is used to check codes generated in the near past.
        // You can use this value to tune how far you're willing to go.
        try {
                    for (int i = -validWindow; i <= validWindow; ++i) {
                        long hash = iterateVerify(decodedKey, t + i);
                        if (hash == inputCode) {
                                isVerified=true;
                        }
                    }
                } catch (GeneralSecurityException e) {
                    //Error
                }
        return isVerified;
    }

    public static int iterateVerify( byte[] key, long t) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] data = new byte[8];
        long value = t;
        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }

        SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);

        int offset = hash[20 - 1] & 0xF;

        long truncatedHash = 0;
        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            // We are dealing with signed bytes:
            // we just keep the first byte.
            truncatedHash |= (hash[offset + i] & 0xFF);
        }

        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= 1000000;
        return (int) truncatedHash;
    }

    /*
    public static void main(String[] args) {
        sendTwoFactorCode("jimbo","sandsystem5430@gmail.com");
    }
    */
}
