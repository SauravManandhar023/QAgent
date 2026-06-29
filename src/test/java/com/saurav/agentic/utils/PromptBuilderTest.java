package com.saurav.agentic.utils;

import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * Unit tests for PromptBuilder — prompt generation and utility methods.
 */
public class PromptBuilderTest {

    // ── toPascalCase ──────────────────────────────────────────────────────

    @Test
    public void testToPascalCaseNormal() {
        assertEquals(PromptBuilder.toPascalCase("login form"), "LoginForm");
    }

    @Test
    public void testToPascalCaseMultipleWords() {
        assertEquals(PromptBuilder.toPascalCase("forgot password page"), "ForgotPasswordPage");
    }

    @Test
    public void testToPascalCaseSingleWord() {
        assertEquals(PromptBuilder.toPascalCase("dashboard"), "Dashboard");
    }

    @Test
    public void testToPascalCaseWithSpecialChars() {
        // Special chars are stripped; "checkout-page_2" becomes "checkoutpage2",
        // then split by space yields one word, so first letter uppercased = "Checkoutpage2"
        assertEquals(PromptBuilder.toPascalCase("checkout-page_2"), "Checkoutpage2");
    }

    @Test
    public void testToPascalCaseNullInput() {
        assertEquals(PromptBuilder.toPascalCase(null), "Component");
    }

    @Test
    public void testToPascalCaseEmptyInput() {
        assertEquals(PromptBuilder.toPascalCase(""), "Component");
    }

    @Test
    public void testToPascalCaseAlreadyPascal() {
        assertEquals(PromptBuilder.toPascalCase("LoginForm"), "Loginform");
    }

    @Test
    public void testToPascalCaseMixedCase() {
        assertEquals(PromptBuilder.toPascalCase("LOGIN PAGE"), "LoginPage");
    }

    @Test
    public void testToPascalCaseWithNumbers() {
        assertEquals(PromptBuilder.toPascalCase("page 2"), "Page2");
    }

    @Test
    public void testToPascalCaseLeadingTrailingSpaces() {
        assertEquals(PromptBuilder.toPascalCase("  hello world  "), "HelloWorld");
    }

    // ── uiTestCaseSystemPrompt ────────────────────────────────────────────

    @Test
    public void testUiTestCaseSystemPromptNotEmpty() {
        String prompt = PromptBuilder.uiTestCaseSystemPrompt();
        assertNotNull(prompt);
        assertFalse(prompt.isEmpty());
        assertTrue(prompt.contains("JSON"));
        assertTrue(prompt.contains("QA engineer"));
    }

    // ── uiTestCaseUserPrompt ──────────────────────────────────────────────

    @Test
    public void testUiTestCaseUserPromptWithoutExisting() {
        String prompt = PromptBuilder.uiTestCaseUserPrompt("Page analysis content", null);
        assertNotNull(prompt);
        assertTrue(prompt.contains("Page analysis content"));
        assertTrue(prompt.contains("Generate 15 test cases"));
    }

    @Test
    public void testUiTestCaseUserPromptWithExisting() {
        Set<String> existing = new HashSet<>();
        existing.add("TC_001 - Verify login");
        existing.add("TC_002 - Verify logout");
        existing.add("TC_003 - Verify error handling");

        String prompt = PromptBuilder.uiTestCaseUserPrompt("Login page", existing);
        assertTrue(prompt.contains("ALREADY COVERED"));
        assertTrue(prompt.contains("Verify login"));
        assertTrue(prompt.contains("Generate 15 NEW test cases"));
    }

    @Test
    public void testUiTestCaseUserPromptWithManyExisting() {
        Set<String> existing = new HashSet<>();
        for (int i = 0; i < 15; i++) {
            existing.add("TC_" + String.format("%03d", i));
        }

        String prompt = PromptBuilder.uiTestCaseUserPrompt("Page", existing);
        assertTrue(prompt.contains("ALREADY COVERED"));
        assertTrue(prompt.contains("... and 5 more"));
    }

    // ── seleniumScriptSystemPrompt ────────────────────────────────────────

    @Test
    public void testSeleniumScriptSystemPromptNotEmpty() {
        String prompt = PromptBuilder.seleniumScriptSystemPrompt();
        assertNotNull(prompt);
        assertFalse(prompt.isEmpty());
        assertTrue(prompt.contains("Page Object Model"));
        assertTrue(prompt.contains("Duration.ofSeconds()"));
        assertTrue(prompt.contains("TestNG"));
    }

    // ── seleniumScriptUserPrompt ──────────────────────────────────────────

    @Test
    public void testSeleniumScriptUserPromptNotEmpty() {
        String prompt = PromptBuilder.seleniumScriptUserPrompt(
                "Login", "LoginTest", "https://example.com/login",
                "Test cases text", "POM code");
        assertNotNull(prompt);
        assertFalse(prompt.isEmpty());
        assertTrue(prompt.contains("Login"));
        assertTrue(prompt.contains("LoginTest"));
        assertTrue(prompt.contains("https://example.com/login"));
        assertTrue(prompt.contains("Test cases text"));
        assertTrue(prompt.contains("POM code"));
        assertTrue(prompt.contains("package generated.ui;"));
    }

    // ── pomSystemPrompt ───────────────────────────────────────────────────

    @Test
    public void testPomSystemPromptNotEmpty() {
        String prompt = PromptBuilder.pomSystemPrompt();
        assertNotNull(prompt);
        assertFalse(prompt.isEmpty());
        assertTrue(prompt.contains("@FindBy"));
        assertTrue(prompt.contains("PageFactory.initElements"));
        assertTrue(prompt.contains("package pages;"));
    }

    // ── pomUserPrompt ─────────────────────────────────────────────────────

    @Test
    public void testPomUserPromptNotEmpty() {
        String prompt = PromptBuilder.pomUserPrompt(
                "Login", "LoginPage", "https://example.com/login",
                "input#username, button#submit");
        assertNotNull(prompt);
        assertFalse(prompt.isEmpty());
        assertTrue(prompt.contains("Login"));
        assertTrue(prompt.contains("LoginPage"));
        assertTrue(prompt.contains("https://example.com/login"));
        assertTrue(prompt.contains("package pages;"));
    }

    // ── API prompts ───────────────────────────────────────────────────────

    @Test
    public void testApiTestCaseSystemPromptNotEmpty() {
        String prompt = PromptBuilder.apiTestCaseSystemPrompt();
        assertNotNull(prompt);
        assertFalse(prompt.isEmpty());
        assertTrue(prompt.contains("JSON array"));
    }

    @Test
    public void testApiTestCaseUserPromptNotEmpty() {
        String prompt = PromptBuilder.apiTestCaseUserPrompt(
                "https://api.example.com", "GET /users, POST /login");
        assertNotNull(prompt);
        assertFalse(prompt.isEmpty());
        assertTrue(prompt.contains("https://api.example.com"));
        assertTrue(prompt.contains("GET /users"));
        assertTrue(prompt.contains("POST /login"));
    }
}
