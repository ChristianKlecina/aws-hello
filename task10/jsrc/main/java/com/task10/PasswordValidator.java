package com.task10;

public class PasswordValidator {

    // Method to validate if the password meets all the required criteria
    public static boolean validatePassword(String password) {
        return validateLength(password, 8, 20) &&
                containsUppercase(password) &&
                containsLowercase(password) &&
                containsDigit(password) &&
                containsSpecialCharacter(password);
    }

    // Method to check if the password length is within a specific range
    private static boolean validateLength(String password, int minLength, int maxLength) {
        return password != null && password.length() >= minLength && password.length() <= maxLength;
    }

    // Method to check if the password contains at least one uppercase letter
    private static boolean containsUppercase(String password) {
        return password != null && password.matches(".*[A-Z].*");
    }

    // Method to check if the password contains at least one lowercase letter
    private static boolean containsLowercase(String password) {
        return password != null && password.matches(".*[a-z].*");
    }

    // Method to check if the password contains at least one digit
    private static boolean containsDigit(String password) {
        return password != null && password.matches(".*\\d.*");
    }

    // Method to check if the password contains at least one special character
    private static boolean containsSpecialCharacter(String password) {
        return password != null && password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
    }
}
