package common;

import java.security.SecureRandom;

public class LoginUtilities {

    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMBERS = "0123456789";
    private static final String CHARSET = LOWERCASE + UPPERCASE + NUMBERS;

    /**
     * Checks for username / password : length , special characters , non-zero  + avoid username=password
     */
    public static boolean passwordChecker(String username, String password) {
        if (!(guidelineChecker(password)))
            return false;
        return !username.equalsIgnoreCase(password);
    }

    /**
     * Checks for username / password : length , special characters , non-zero etc
     */
    public static boolean guidelineChecker(String userInput) {
        boolean hasSufficientLength = userInput.length() >= 8;
        boolean containsNoSpecial = userInput.matches("[A-Za-z0-9]*");
        boolean containsNoAlpha = userInput.matches("[0-9]*");
        return hasSufficientLength && containsNoSpecial && !containsNoAlpha;
    }

    /*
    * Generates a secure password that the user can us as his master password
    */
    public static String getRandomSecurePassword(int passwordLength) {
        if (passwordLength < 8) {
            return "<incorrect length>";
        }

        SecureRandom secureRandom = new SecureRandom();
        char[] password = new char[passwordLength];
        for (int i = 0; i < passwordLength; i++) {
            int index = secureRandom.nextInt(CHARSET.length() - 1);
            password[i] = CHARSET.charAt(index);
        }

        String passwordStr = new String(password);
        if (guidelineChecker(passwordStr))
            return passwordStr;
        else return getRandomSecurePassword(passwordLength);
    }

}
