package com.task10;

import java.util.regex.Pattern;

public class Validator {

    public static boolean isValidEmail(String email) {
        boolean isValid = validateEmail(email);
        if (isValid) {
            return true;
        } else {
            throw new IllegalArgumentException("Invalid Email : " + email);
        }
    }

    public static boolean isValidPassword(String password) {
        // Check length
        if (password.length() < 12) {
            return false;
        }

        // Check for at least one alphabetic character and one numeric digit
        boolean hasAlphabetic = false;
        boolean hasNumeric = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasAlphabetic = true;
            } else if (Character.isDigit(c)) {
                hasNumeric = true;
            } else if (!(c == '$' || c == '%' || c == '^' || c == '*')) {
                // Check for disallowed characters
                return false;
            }
        }

        boolean isValid = hasAlphabetic && hasNumeric;

        if (isValid) {
            return true;
        } else {
            throw new IllegalArgumentException("Invalid Password : " + password);
        }
    }

    public static boolean validateEmail(String email) {
        // Basic check for email format
        if (email == null || email.isEmpty()) {
            return false;
        }

        // Split email into local part and domain part
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return false; // Must have exactly one '@' character
        }

        String localPart = parts[0];
        String domainPart = parts[1];

        // Check local part length (RFC 5321 specifies max length of 64 characters)
        if (localPart.length() > 64) {
            return false;
        }

        // Check domain part length (RFC 5321 specifies max length of 255 characters)
        if (domainPart.length() > 255) {
            return false;
        }

        // Check domain part format (must have at least one dot, and characters must be valid)
        if (!isValidDomainPart(domainPart)) {
            return false;
        }

        return true;
    }

    private static boolean isValidDomainPart(String domainPart) {
        // Split domain part by dots
        String[] domainParts = domainPart.split("\\.");

        // Must have at least one dot and each part should not be empty
        if (domainParts.length < 2) {
            return false;
        }

        // Check each part for validity
        for (String part : domainParts) {
            if (!isValidDomainPartSegment(part)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidDomainPartSegment(String segment) {
        // Domain segment must not be empty and must consist of valid characters
        return segment.length() > 0 && Pattern.matches("[a-zA-Z0-9-]+", segment);
    }

}