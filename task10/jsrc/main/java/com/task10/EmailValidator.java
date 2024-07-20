package com.task10;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailValidator {

    // Regular expression pattern for validating email addresses
    private static final String EMAIL_PATTERN =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    // Compile the regular expression into a pattern
    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    // Method to validate an email address
    public static boolean validateEmail(String email) {
        if (email == null) {
            return false;
        }
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}