package com.saurav.agentic.utils;

import com.saurav.agentic.models.TestCase;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for ExcelUtil — Excel read/write and sheet name sanitization.
 */
public class ExcelUtilTest {

    private File tempFile;
    private List<TestCase> testCases;

    @BeforeMethod
    public void setUp() throws IOException {
        tempFile = File.createTempFile("test-cases-", ".xlsx");
        tempFile.deleteOnExit();

        testCases = new ArrayList<>();

        TestCase tc1 = new TestCase();
        tc1.setTestCaseId("TC_001");
        tc1.setTestCaseName("Verify valid login");
        tc1.setDescription("Test valid login flow");
        tc1.setPreconditions("User is on login page");
        tc1.setTestSteps("1. Enter username\n2. Enter password\n3. Click Login");
        tc1.setTestData("user: admin, pass: pass123");
        tc1.setExpectedResult("Redirect to dashboard");
        tc1.setTestType("Positive");
        tc1.setPriority("High");
        tc1.setComponent("Login Form");
        tc1.setAutomationFeasible(true);

        TestCase tc2 = new TestCase();
        tc2.setTestCaseId("TC_002");
        tc2.setTestCaseName("Verify invalid login");
        tc2.setDescription("Test invalid login shows error");
        tc2.setPreconditions("User is on login page");
        tc2.setTestSteps("1. Enter wrong username\n2. Enter wrong password\n3. Click Login");
        tc2.setTestData("user: bad, pass: wrong");
        tc2.setExpectedResult("Error message displayed");
        tc2.setTestType("Negative");
        tc2.setPriority("High");
        tc2.setComponent("Login Form");
        tc2.setAutomationFeasible(true);

        testCases.add(tc1);
        testCases.add(tc2);
    }

    @AfterMethod
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    // ── sanitizeSheetName ─────────────────────────────────────────────────

    @Test
    public void testSanitizeSheetNameNormal() {
        String result = invokeSanitize("Login Form");
        assertEquals(result, "Login Form");
    }

    @Test
    public void testSanitizeSheetNameRemovesInvalidChars() {
        String result = invokeSanitize("Login/Form?Test*[2024]");
        assertEquals(result, "LoginFormTest2024");
    }

    @Test
    public void testSanitizeSheetNameTrimsWhitespace() {
        String result = invokeSanitize("  Login Form  ");
        assertEquals(result, "Login Form");
    }

    @Test
    public void testSanitizeSheetNameTruncatesTo31Chars() {
        String longName = "A".repeat(50);
        String result = invokeSanitize(longName);
        assertEquals(result.length(), 31);
    }

    @Test
    public void testSanitizeSheetNameNullInput() {
        String result = invokeSanitize(null);
        assertEquals(result, "Component");
    }

    @Test
    public void testSanitizeSheetNameEmptyInput() {
        String result = invokeSanitize("");
        assertEquals(result, "Component");
    }

    @Test
    public void testSanitizeSheetNameAllInvalid() {
        String result = invokeSanitize("\\/?*[]\":");
        assertEquals(result, "Component");
    }

    @Test
    public void testSanitizeSheetNameRemovesBackslash() {
        String result = invokeSanitize("Test\\Name");
        assertEquals(result, "TestName");
    }

    @Test
    public void testSanitizeSheetNameRemovesColon() {
        String result = invokeSanitize("Checkout: Payment");
        assertEquals(result, "Checkout Payment");
    }

    @Test
    public void testSanitizeSheetNameRemovesDoubleQuote() {
        String result = invokeSanitize("\"Quoted\" Name");
        assertEquals(result, "Quoted Name");
    }

    // ── Write and Read roundtrip ──────────────────────────────────────────

    @Test
    public void testWriteAndReadTestCases() throws IOException {
        ExcelUtil.writeTestCases(testCases, tempFile.getAbsolutePath());

        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testReadTestCasesRoundtrip() throws IOException {
        ExcelUtil.writeTestCases(testCases, tempFile.getAbsolutePath());

        List<TestCase> readBack = ExcelUtil.readTestCases(tempFile.getAbsolutePath());
        assertNotNull(readBack);
        assertEquals(readBack.size(), 2);

        // Verify first test case
        TestCase first = readBack.get(0);
        assertNotNull(first.getTestCaseId());
        assertFalse(first.getTestCaseName().isEmpty());
    }

    @Test
    public void testWriteRespectsComponentMapping() throws IOException {
        // Add test case from a different component
        TestCase tc3 = new TestCase();
        tc3.setTestCaseId("TC_003");
        tc3.setTestCaseName("Verify search works");
        tc3.setDescription("Search functionality test");
        tc3.setTestData("query: test");
        tc3.setTestSteps("1. Enter query\n2. Click search");
        tc3.setExpectedResult("Search results shown");
        tc3.setTestType("Positive");
        tc3.setPriority("Medium");
        tc3.setComponent("Search Bar");
        tc3.setAutomationFeasible(true);

        testCases.add(tc3);

        ExcelUtil.writeTestCases(testCases, tempFile.getAbsolutePath());
        List<TestCase> readBack = ExcelUtil.readTestCases(tempFile.getAbsolutePath());

        // Should still have 3 test cases across 2 components
        assertEquals(readBack.size(), 3);

        long loginFormCount = readBack.stream()
                .filter(tc -> "Login Form".equals(tc.getComponent()))
                .count();
        long searchCount = readBack.stream()
                .filter(tc -> "Search Bar".equals(tc.getComponent()))
                .count();

        assertEquals(loginFormCount, 2);
        assertEquals(searchCount, 1);
    }

    @Test
    public void testReadFromNonExistentFile() {
        assertThrows(IOException.class, () ->
                ExcelUtil.readTestCases("/nonexistent/path/file.xlsx"));
    }

    // ── appendTestCases ───────────────────────────────────────────────────

    @Test
    public void testAppendTestCasesFirstRun() throws IOException {
        ExcelUtil.appendTestCases(tempFile.getAbsolutePath(), testCases, true);

        List<TestCase> readBack = ExcelUtil.readTestCases(tempFile.getAbsolutePath());
        assertEquals(readBack.size(), 2);
    }

    @Test
    public void testAppendTestCasesIncremental() throws IOException {
        // First run
        ExcelUtil.appendTestCases(tempFile.getAbsolutePath(), testCases, true);

        // Second run with new data
        TestCase tc3 = new TestCase();
        tc3.setTestCaseId("TC_003");
        tc3.setTestCaseName("Verify password reset");
        tc3.setTestData("token: abc");
        tc3.setTestSteps("1. Click forgot password");
        tc3.setExpectedResult("Reset email sent");
        tc3.setTestType("Positive");
        tc3.setPriority("Medium");
        tc3.setComponent("Login Form");
        tc3.setAutomationFeasible(true);

        List<TestCase> moreTests = new ArrayList<>();
        moreTests.add(tc3);

        ExcelUtil.appendTestCases(tempFile.getAbsolutePath(), moreTests, false);

        List<TestCase> readBack = ExcelUtil.readTestCases(tempFile.getAbsolutePath());
        assertEquals(readBack.size(), 3);
    }

    @Test
    public void testAppendDeduplicatesByName() throws IOException {
        ExcelUtil.appendTestCases(tempFile.getAbsolutePath(), testCases, true);

        // Try to append a duplicate (same name as TC_001)
        TestCase dup = new TestCase();
        dup.setTestCaseId("TC_001_DUP");
        dup.setTestCaseName("Verify valid login"); // Same name
        dup.setTestData("user: admin, pass: pass123");
        dup.setTestSteps("1. Enter username\n2. Enter password\n3. Click Login");
        dup.setExpectedResult("Redirect to dashboard");
        dup.setTestType("Positive");
        dup.setPriority("High");
        dup.setComponent("Login Form");
        dup.setAutomationFeasible(true);

        List<TestCase> dups = new ArrayList<>();
        dups.add(dup);

        ExcelUtil.appendTestCases(tempFile.getAbsolutePath(), dups, false);

        List<TestCase> readBack = ExcelUtil.readTestCases(tempFile.getAbsolutePath());
        assertEquals(readBack.size(), 2); // Should still be 2 (no duplicate appended)
    }

    // ── Helper to invoke private sanitizeSheetName ────────────────────────

    /**
     * Invokes the private sanitizeSheetName method via reflection.
     */
    private String invokeSanitize(String input) {
        try {
            java.lang.reflect.Method method = ExcelUtil.class.getDeclaredMethod(
                    "sanitizeSheetName", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, input);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke sanitizeSheetName", e);
        }
    }
}
