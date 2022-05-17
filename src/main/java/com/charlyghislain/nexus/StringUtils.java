package com.charlyghislain.nexus;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringUtils {

    private static final String ALPHANUMERIC_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";


    public static String getRandomAlphanumericString(int length) {
        return getRandomString(length, ALPHANUMERIC_CHARS);
    }

    private static String getRandomString(int length, String allChars) {
        return Stream.generate(() -> StringUtils.getNextRandomChar(allChars))
                .limit(length)
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    private static char getNextRandomChar(String alLChars) {
        Random random = new Random();
        int randomCharIndex = random.nextInt(alLChars.length());
        char randomChar = alLChars.charAt(randomCharIndex);
        return randomChar;
    }
}
