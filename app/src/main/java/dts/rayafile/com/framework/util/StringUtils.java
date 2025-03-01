package dts.rayafile.com.framework.util;

import android.text.TextUtils;

import java.nio.charset.StandardCharsets;

public class StringUtils {

//    /**
//     * <blockquote><pre>
//     * input = abc, length = 1
//     * return a
//     *
//     * input = abc, length = -1
//     * return c
//     * </pre></blockquote>
//     */
//    public static String slice(String input, int subLength) {
//        if (TextUtils.isEmpty(input)) {
//            return input;
//        }
//
//        //如果绝对值大于总长度，返回原值
//        if (Math.abs(subLength) > input.length()) {
//            return input;
//        }
//
//        //Start at the back
//        if (subLength < 0) {
//            subLength = Math.abs(subLength);
//            return input.substring(input.length() - subLength);
//        }
//
//        return input.substring(0, input.length() - subLength);
//    }


//    /**
//     * => org.apache.commons.lang3.StringUtils#countMatches(c,c)
//     */
//    public static int countMatches(String inputStr, String matchStr) {
//        if (TextUtils.isEmpty(inputStr)) {
//            return 0;
//        }
//        if (TextUtils.isEmpty(matchStr)) {
//            return 0;
//        }
//
//        return org.apache.commons.lang3.StringUtils.countMatches(inputStr, matchStr);
//    }

    /**
     * <blockquote><pre>
     * input = abc, length = 1
     * return c
     * </pre></blockquote>
     */
    public static String getEndChar(String input, int subLength) {
        if (TextUtils.isEmpty(input)) {
            return "";
        }

        if (subLength > input.length()) {
            return input;
        }

        return input.substring(input.length() - subLength);
    }

    /**
     * <blockquote><pre>
     * input = abc, length = 1
     * return ab
     * </pre></blockquote>
     */
    public static String getHeadChar(String input, int subLength) {
        if (TextUtils.isEmpty(input)) {
            return "";
        }

        if (subLength > input.length()) {
            return input;
        }

        return input.substring(0, input.length() - subLength);
    }

    /**
     * trim start and end
     */
    public static String trim(String input, String character) {
        if (TextUtils.isEmpty(input)) {
            return input;
        }

        while (input.startsWith(character)) {
            input = input.substring(character.length());
        }

        while (input.endsWith(character)) {
            input = input.substring(0, input.length() - character.length());
        }

        return input;
    }

    public static String trimEnd(String input, String character) {
        if (TextUtils.isEmpty(input)) {
            return input;
        }

        while (input.endsWith(character)) {
            input = input.substring(0, input.length() - character.length());
        }

        return input;
    }

    public static String trimStart(String input, String character) {
        if (TextUtils.isEmpty(input)) {
            return input;
        }

        while (input.startsWith(character)) {
            input = input.substring(character.length());
        }

        return input;
    }

    /**
     * <blockquote>
     * <pre>
     *  ""string"" => "string"
     *  null => null
     *  "" => ""
     * </pre>
     * </blockquote>
     */
    public static String deString(String input) {
        if (TextUtils.isEmpty(input)) {
            return input;
        }

        input = trim(input, "\"");

        return input;
    }

    /**
     * <blockquote>
     * <pre>
     * ""string"" => "string"
     * null => ""
     * "" => ""
     * </pre>
     * </blockquote>
     */
    public static String deStringReturnNonNull(String input) {
        if (TextUtils.isEmpty(input)) {
            return "";
        }

        input = trim(input, "\"");

        return input;
    }

    public static int getStringAsciiSum(String input) {
        int sum = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            sum += c;
        }
        return sum;
    }

    public static int getHexStringAsciiSum(String input) {

        byte[] bytes = hexStringToByteArray(input);
        int sum = 0;
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF; // Convert bytes to unsigned integers
            sum += value;
        }
        return sum;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) (
                    (Character.digit(s.charAt(i), 16) << 4)
                            + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * => org.apache.commons.lang3.StringUtils#countMatches(c,c)
     */
    public static int countMatches(String inputStr, String matchStr) {
        if (TextUtils.isEmpty(inputStr)) {
            return 0;
        }
        if (TextUtils.isEmpty(matchStr)) {
            return 0;
        }

        return org.apache.commons.lang3.StringUtils.countMatches(inputStr, matchStr);
    }
}
