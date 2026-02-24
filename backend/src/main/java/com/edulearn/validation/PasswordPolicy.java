package com.edulearn.validation;

public final class PasswordPolicy {

    public static final String REGEX = "^(?=\\S+$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,128}$";
    public static final String MESSAGE =
            "Password must be 8-128 characters, include uppercase, lowercase, number, and special character, and contain no spaces";

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null && password.matches(REGEX);
    }
}
