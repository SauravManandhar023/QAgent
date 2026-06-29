package com.saurav.agentic.constants;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for FrameworkConstants — validates that all string constants are
 * non-empty/non-null and URLs are well-formed.
 */
public class FrameworkConstantsTest {

    // ── Config file names ─────────────────────────────────────────────────

    @Test
    public void testBaseConfigFileNonEmpty() {
        assertNotNull(FrameworkConstants.BASE_CONFIG_FILE);
        assertFalse(FrameworkConstants.BASE_CONFIG_FILE.isEmpty());
    }

    @Test
    public void testLocalConfigFileNonEmpty() {
        assertNotNull(FrameworkConstants.LOCAL_CONFIG_FILE);
        assertFalse(FrameworkConstants.LOCAL_CONFIG_FILE.isEmpty());
    }

    // ── Groq AI ───────────────────────────────────────────────────────────

    @Test
    public void testGroqApiUrlIsValid() {
        assertNotNull(FrameworkConstants.GROQ_API_URL);
        assertTrue(FrameworkConstants.GROQ_API_URL.startsWith("https://"));
        assertTrue(FrameworkConstants.GROQ_API_URL.contains("api.groq.com"));
    }

    @Test
    public void testGroqDefaultModelNonEmpty() {
        assertNotNull(FrameworkConstants.GROQ_DEFAULT_MODEL);
        assertFalse(FrameworkConstants.GROQ_DEFAULT_MODEL.isEmpty());
    }

    @Test
    public void testGroqDefaultMaxTokensPositive() {
        assertTrue(FrameworkConstants.GROQ_DEFAULT_MAX_TOKENS > 0);
    }

    @Test
    public void testGroqDefaultTemperatureInRange() {
        assertTrue(FrameworkConstants.GROQ_DEFAULT_TEMPERATURE >= 0.0);
        assertTrue(FrameworkConstants.GROQ_DEFAULT_TEMPERATURE <= 2.0);
    }

    // ── Browser ───────────────────────────────────────────────────────────

    @Test
    public void testBrowserConstantsNonEmpty() {
        assertFalse(FrameworkConstants.BROWSER_CHROME.isEmpty());
        assertFalse(FrameworkConstants.BROWSER_FIREFOX.isEmpty());
        assertFalse(FrameworkConstants.BROWSER_EDGE.isEmpty());
    }

    @Test
    public void testBrowserChromeIsChrome() {
        assertEquals(FrameworkConstants.BROWSER_CHROME, "chrome");
    }

    @Test
    public void testBrowserFirefoxIsFirefox() {
        assertEquals(FrameworkConstants.BROWSER_FIREFOX, "firefox");
    }

    // ── Wait times ────────────────────────────────────────────────────────

    @Test
    public void testWaitTimesPositive() {
        assertTrue(FrameworkConstants.DEFAULT_IMPLICIT_WAIT > 0);
        assertTrue(FrameworkConstants.DEFAULT_EXPLICIT_WAIT > 0);
        assertTrue(FrameworkConstants.DEFAULT_PAGE_LOAD_TIMEOUT > 0);
        assertTrue(FrameworkConstants.SCRAPER_WAIT_SECONDS > 0);
    }

    @Test
    public void testExplicitWaitGreaterThanImplicit() {
        assertTrue(FrameworkConstants.DEFAULT_EXPLICIT_WAIT >=
                FrameworkConstants.DEFAULT_IMPLICIT_WAIT);
    }

    // ── Output paths ──────────────────────────────────────────────────────

    @Test
    public void testOutputRootNonEmpty() {
        assertFalse(FrameworkConstants.OUTPUT_ROOT.isEmpty());
    }

    @Test
    public void testOutputPathsAreRelative() {
        // All paths should be relative (not absolute) for portability
        assertFalse(FrameworkConstants.OUTPUT_EXCEL_DIR.startsWith("/"));
        assertFalse(FrameworkConstants.OUTPUT_EXCEL_DIR.startsWith("C:"));
        assertFalse(FrameworkConstants.OUTPUT_EXCEL_UI.startsWith("/"));
        assertFalse(FrameworkConstants.OUTPUT_EXCEL_API.startsWith("/"));
    }

    @Test
    public void testOutputExcelPathsEndWithXlsx() {
        assertTrue(FrameworkConstants.OUTPUT_EXCEL_UI.endsWith(".xlsx"));
        assertTrue(FrameworkConstants.OUTPUT_EXCEL_API.endsWith(".xlsx"));
    }

    // ── Generated code paths ──────────────────────────────────────────────

    @Test
    public void testGeneratedPathsNonEmpty() {
        assertFalse(FrameworkConstants.GENERATED_UI_DIR.isEmpty());
        assertFalse(FrameworkConstants.GENERATED_API_DIR.isEmpty());
        assertFalse(FrameworkConstants.GENERATED_PAGES_DIR.isEmpty());
    }

    // ── Excel sheet names ─────────────────────────────────────────────────

    @Test
    public void testSheetNamesNonEmpty() {
        assertFalse(FrameworkConstants.SHEET_SUMMARY.isEmpty());
        assertFalse(FrameworkConstants.SHEET_TEST_CASES.isEmpty());
        assertFalse(FrameworkConstants.SHEET_API_TEST_CASES.isEmpty());
    }

    // ── Test types ────────────────────────────────────────────────────────

    @Test
    public void testTestTypesAreDistinct() {
        assertNotEquals(FrameworkConstants.TEST_TYPE_POSITIVE,
                FrameworkConstants.TEST_TYPE_NEGATIVE);
        assertNotEquals(FrameworkConstants.TEST_TYPE_EDGE,
                FrameworkConstants.TEST_TYPE_NEGATIVE);
        assertNotEquals(FrameworkConstants.TEST_TYPE_ACCESSIBILITY,
                FrameworkConstants.TEST_TYPE_POSITIVE);
    }

    @Test
    public void testTestTypesNonEmpty() {
        assertFalse(FrameworkConstants.TEST_TYPE_POSITIVE.isEmpty());
        assertFalse(FrameworkConstants.TEST_TYPE_NEGATIVE.isEmpty());
        assertFalse(FrameworkConstants.TEST_TYPE_EDGE.isEmpty());
        assertFalse(FrameworkConstants.TEST_TYPE_ACCESSIBILITY.isEmpty());
    }

    // ── Priority levels ───────────────────────────────────────────────────

    @Test
    public void testPriorityLevels() {
        assertEquals(FrameworkConstants.PRIORITY_HIGH, "High");
        assertEquals(FrameworkConstants.PRIORITY_MEDIUM, "Medium");
        assertEquals(FrameworkConstants.PRIORITY_LOW, "Low");
    }

    // ── Logging ───────────────────────────────────────────────────────────

    @Test
    public void testLogConstantsNonEmpty() {
        assertFalse(FrameworkConstants.LOG_SEPARATOR.isEmpty());
        assertFalse(FrameworkConstants.LOG_SUCCESS.isEmpty());
        assertFalse(FrameworkConstants.LOG_ERROR.isEmpty());
        assertFalse(FrameworkConstants.LOG_WARNING.isEmpty());
        assertFalse(FrameworkConstants.LOG_INFO.isEmpty());
    }

    @Test
    public void testLogPrefixedWithBrackets() {
        assertTrue(FrameworkConstants.LOG_SUCCESS.startsWith("["));
        assertTrue(FrameworkConstants.LOG_ERROR.startsWith("["));
        assertTrue(FrameworkConstants.LOG_WARNING.startsWith("["));
        assertTrue(FrameworkConstants.LOG_INFO.startsWith("["));
    }

    @Test
    public void testLogSuccessEndsWithBracket() {
        assertTrue(FrameworkConstants.LOG_SUCCESS.endsWith("]"));
        assertTrue(FrameworkConstants.LOG_ERROR.endsWith("]"));
        assertTrue(FrameworkConstants.LOG_WARNING.endsWith("]"));
        assertTrue(FrameworkConstants.LOG_INFO.endsWith("]"));
    }

    // ── Generated file paths ──────────────────────────────────────────────

    @Test
    public void testGeneratedClasspathFileNonEmpty() {
        assertNotNull(FrameworkConstants.GENERATED_CLASSPATH_FILE);
        assertFalse(FrameworkConstants.GENERATED_CLASSPATH_FILE.isEmpty());
        assertTrue(FrameworkConstants.GENERATED_CLASSPATH_FILE.endsWith(".txt"));
    }

    // ── TestNG XML ────────────────────────────────────────────────────────

    @Test
    public void testTestngXmlNonEmpty() {
        assertFalse(FrameworkConstants.TESTNG_XML.isEmpty());
    }

    // ── Screenshot ────────────────────────────────────────────────────────

    @Test
    public void testScreenshotConstants() {
        assertEquals(FrameworkConstants.SCREENSHOT_EXTENSION, ".png");
        assertFalse(FrameworkConstants.SCREENSHOT_DATE_FORMAT.isEmpty());
    }
}
