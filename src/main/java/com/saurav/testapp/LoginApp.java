package com.saurav.testapp;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * LoginApp — A simplified application model representing a login system.
 *
 * PITest mutates this class to inject bugs. If the generated tests
 * (or hand-written unit tests) are high quality, they will detect
 * these mutations and the mutation coverage score will be high.
 *
 * This simulates a typical web application login form:
 * - Validates username/password
 * - Checks email format for registration
 * - Has rate limiting
 */
public class LoginApp {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final Map<String, String> users = new HashMap<>();
    private final Map<String, Integer> loginAttempts = new HashMap<>();
    private static final int MAX_ATTEMPTS = 5;

    public LoginApp() {
        // Default test user
        users.put("admin@test.com", "password123");
    }

    /**
     * Registers a new user with email and password.
     * @return true if registration succeeded, false if email already exists
     * @throws IllegalArgumentException if email or password is invalid
     */
    public boolean register(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (users.containsKey(email)) {
            return false; // User already exists
        }
        users.put(email, password);
        return true;
    }

    /**
     * Attempts to log in with email and password.
     * @return LoginResult indicating success or failure reason
     */
    public LoginResult login(String email, String password) {
        // Rate limiting check
        int attempts = loginAttempts.getOrDefault(email, 0);
        if (attempts >= MAX_ATTEMPTS) {
            return LoginResult.ACCOUNT_LOCKED;
        }

        if (email == null || email.trim().isEmpty()) {
            loginAttempts.put(email, attempts + 1);
            return LoginResult.INVALID_EMAIL;
        }

        if (!users.containsKey(email)) {
            loginAttempts.put(email, attempts + 1);
            return LoginResult.USER_NOT_FOUND;
        }

        String storedPassword = users.get(email);
        if (!storedPassword.equals(password)) {
            loginAttempts.put(email, attempts + 1);
            return LoginResult.WRONG_PASSWORD;
        }

        // Successful login — reset attempt counter
        loginAttempts.put(email, 0);
        return LoginResult.SUCCESS;
    }

    /**
     * Checks if an account is locked due to too many failed attempts.
     */
    public boolean isAccountLocked(String email) {
        return loginAttempts.getOrDefault(email, 0) >= MAX_ATTEMPTS;
    }

    /**
     * Resets the password for an existing user.
     * @return true if password was reset
     */
    public boolean resetPassword(String email, String newPassword) {
        if (!users.containsKey(email)) {
            return false;
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        users.put(email, newPassword);
        loginAttempts.put(email, 0); // Reset attempts on password reset
        return true;
    }

    /**
     * Gets the number of registered users.
     */
    public int getUserCount() {
        return users.size();
    }

    public enum LoginResult {
        SUCCESS,
        INVALID_EMAIL,
        USER_NOT_FOUND,
        WRONG_PASSWORD,
        ACCOUNT_LOCKED
    }
}
