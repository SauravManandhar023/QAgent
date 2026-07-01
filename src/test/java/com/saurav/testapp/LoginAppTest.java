package com.saurav.testapp;

import com.saurav.testapp.LoginApp.LoginResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for LoginApp — these tests serve double duty:
 * 1. Validate the LoginApp model behavior
 * 2. Provide a baseline for PITest mutation testing validation
 *
 * Run PITest with: mvn pitest:mutationCoverage -P mutation-testing
 * Goal: 100% mutation coverage (every injected fault caught by a test).
 */
public class LoginAppTest {

    private LoginApp app;

    @BeforeMethod
    public void setUp() {
        app = new LoginApp();
    }

    // ── Registration Tests ────────────────────────────────────────────

    @Test
    public void testRegisterValidUser() {
        assertTrue(app.register("new@test.com", "password123"),
                "Should register a new user with valid credentials");
        assertEquals(app.getUserCount(), 2,
                "Should have 2 users after registration (1 default + 1 new)");
    }

    @Test
    public void testRegisterDuplicateEmail() {
        assertFalse(app.register("admin@test.com", "anotherPassword"),
                "Should reject registration with existing email");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRegisterEmptyEmail() {
        app.register("", "password123");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRegisterNullEmail() {
        app.register(null, "password123");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRegisterShortPassword() {
        app.register("test@test.com", "12345");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRegisterNullPassword() {
        app.register("test@test.com", null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRegisterInvalidEmailFormat() {
        app.register("not-an-email", "password123");
    }

    @Test
    public void testRegisterWithPlusInEmail() {
        assertTrue(app.register("user+tag@test.com", "password123"),
                "Should allow email with + tag");
    }

    // ── Login Tests ───────────────────────────────────────────────────

    @Test
    public void testLoginSuccess() {
        assertEquals(app.login("admin@test.com", "password123"), LoginResult.SUCCESS,
                "Valid credentials should return SUCCESS");
    }

    @Test
    public void testLoginWrongPassword() {
        assertEquals(app.login("admin@test.com", "wrongpassword"), LoginResult.WRONG_PASSWORD,
                "Wrong password should return WRONG_PASSWORD");
    }

    @Test
    public void testLoginUserNotFound() {
        assertEquals(app.login("nonexistent@test.com", "password123"), LoginResult.USER_NOT_FOUND,
                "Unknown email should return USER_NOT_FOUND");
    }

    @Test
    public void testLoginInvalidEmail() {
        assertEquals(app.login("", "password123"), LoginResult.INVALID_EMAIL,
                "Empty email should return INVALID_EMAIL");
    }

    @Test
    public void testLoginNullEmailReturnsInvalid() {
        assertEquals(app.login(null, "password123"), LoginResult.INVALID_EMAIL,
                "Null email should return INVALID_EMAIL");
    }

    // ── Rate Limiting Tests ───────────────────────────────────────────

    @Test
    public void testAccountLockedAfterMaxAttempts() {
        // Attempt login with wrong password 5 times
        for (int i = 0; i < 5; i++) {
            app.login("admin@test.com", "wrongpassword");
        }

        assertTrue(app.isAccountLocked("admin@test.com"),
                "Account should be locked after 5 failed attempts");
    }

    @Test
    public void testLoginReturnsLockedAfterMaxAttempts() {
        for (int i = 0; i < 5; i++) {
            app.login("admin@test.com", "wrong");
        }

        assertEquals(app.login("admin@test.com", "stillwrong"), LoginResult.ACCOUNT_LOCKED,
                "Should return ACCOUNT_LOCKED after max attempts");
    }

    @Test
    public void testSuccessfulLoginResetsAttempts() {
        // 4 failed attempts
        for (int i = 0; i < 4; i++) {
            app.login("admin@test.com", "wrong");
        }

        // Successful login
        assertEquals(app.login("admin@test.com", "password123"), LoginResult.SUCCESS);

        // Should not be locked after successful login
        assertFalse(app.isAccountLocked("admin@test.com"),
                "Successful login should reset attempt counter");
    }

    // ── Password Reset Tests ──────────────────────────────────────────

    @Test
    public void testResetPassword() {
        assertTrue(app.resetPassword("admin@test.com", "newPassword123"),
                "Should reset password for existing user");
        assertEquals(app.login("admin@test.com", "newPassword123"), LoginResult.SUCCESS,
                "Should log in with new password after reset");
    }

    @Test
    public void testResetPasswordForNonexistentUser() {
        assertFalse(app.resetPassword("ghost@test.com", "newPassword123"),
                "Should return false for nonexistent user");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testResetPasswordShortNewPassword() {
        app.resetPassword("admin@test.com", "short");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testResetPasswordNullNewPassword() {
        app.resetPassword("admin@test.com", null);
    }

    // ── Edge Case Tests ───────────────────────────────────────────────

    @Test
    public void testInitialUserCount() {
        assertEquals(app.getUserCount(), 1,
                "Should start with 1 default user");
    }

    @Test
    public void testEmailCaseSensitivity() {
        // The app does case-sensitive email matching
        assertEquals(app.login("Admin@test.com", "password123"), LoginResult.USER_NOT_FOUND,
                "Login should be case-sensitive by default");
    }
}
