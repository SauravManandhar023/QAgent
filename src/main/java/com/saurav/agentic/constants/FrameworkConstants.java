package com.saurav.agentic.constants;

/**
 * FrameworkConstants - All hardcoded strings in one place
 * Never scatter magic strings across the codebase
 *
 */
public final class FrameworkConstants {

    // Prevent instantiation
    private FrameworkConstants() {}

    // ===== CONFIG FILE NAMES =====
    public static final String BASE_CONFIG_FILE = "config.properties";
    public static final String LOCAL_CONFIG_FILE = "config.local.properties";

    // ===== GROQ AI =====
    public static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    public static final String GROQ_DEFAULT_MODEL = "llama-3.3-70b-versatile";
    public static final int GROQ_DEFAULT_MAX_TOKENS = 4096;
    public static final double GROQ_DEFAULT_TEMPERATURE = 0.3;

    // ===== BROWSER =====
    public static final String BROWSER_CHROME = "chrome";
    public static final String BROWSER_FIREFOX = "firefox";
    public static final String BROWSER_EDGE = "edge";

    // ===== WAIT TIMES (seconds) =====
    public static final int DEFAULT_IMPLICIT_WAIT = 10;
    public static final int DEFAULT_EXPLICIT_WAIT = 20;
    public static final int DEFAULT_PAGE_LOAD_TIMEOUT = 30;
    public static final int SCRAPER_WAIT_SECONDS = 3;

    // ===== OUTPUT PATHS =====
    public static final String OUTPUT_ROOT = "test-output";
    public static final String OUTPUT_EXCEL_DIR = "test-output/excel";
    public static final String OUTPUT_LOGS_DIR = "test-output/logs";
    public static final String OUTPUT_REPORTS_DIR = "test-output/reports";
    public static final String OUTPUT_SCREENSHOTS_DIR = "test-output/screenshots/failures";
    public static final String OUTPUT_EXCEL_UI = "test-output/excel/ui-test-cases.xlsx";
    public static final String OUTPUT_EXCEL_API = "test-output/excel/api-test-cases.xlsx";

    // ===== GENERATED CODE PATHS =====
    public static final String GENERATED_UI_DIR = "src/test/java/generated/ui";
    public static final String GENERATED_API_DIR = "src/test/java/generated/api";
    public static final String GENERATED_PAGES_DIR = "src/test/java/pages";

    // ===== EXCEL SHEET NAMES =====
    public static final String SHEET_SUMMARY = "Summary";
    public static final String SHEET_TEST_CASES = "TestCases";
    public static final String SHEET_API_TEST_CASES = "ApiTestCases";

    // ===== TEST TYPES =====
    public static final String TEST_TYPE_POSITIVE = "Positive";
    public static final String TEST_TYPE_NEGATIVE = "Negative";
    public static final String TEST_TYPE_EDGE = "Edge";
    public static final String TEST_TYPE_ACCESSIBILITY = "Accessibility";

    // ===== PRIORITY LEVELS =====
    public static final String PRIORITY_HIGH = "High";
    public static final String PRIORITY_MEDIUM = "Medium";
    public static final String PRIORITY_LOW = "Low";

    // ===== PROMPT FILE PATHS =====
    public static final String PROMPT_UI_TESTCASE = "src/main/resources/prompts/ui-testcase-prompt.txt";
    public static final String PROMPT_SELENIUM_SCRIPT = "src/main/resources/prompts/selenium-script-prompt.txt";
    public static final String PROMPT_API_TESTCASE = "src/main/resources/prompts/api-testcase-prompt.txt";
    public static final String PROMPT_API_SCRIPT = "src/main/resources/prompts/api-script-prompt.txt";

    // ===== TEMPLATE FILE PATHS =====
    public static final String TEMPLATE_PAGE = "src/main/resources/templates/page-template.txt";
    public static final String TEMPLATE_TEST = "src/main/resources/templates/test-template.txt";

    // ===== TESTNG XML =====
    public static final String TESTNG_XML = "testng.xml";

    // ===== SCREENSHOT =====
    public static final String SCREENSHOT_EXTENSION = ".png";
    public static final String SCREENSHOT_DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss";

    // ===== LOGGING =====
    public static final String LOG_SEPARATOR = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    public static final String LOG_AGENT1_START = "🤖 Agent 1: TestCaseGeneratorAgent starting...";
    public static final String LOG_AGENT2_START = "🤖 Agent 2: ScriptGeneratorAgent starting...";
    public static final String LOG_AGENT3_START = "🤖 Agent 3: ApiTestCaseAgent starting...";
    public static final String LOG_AGENT4_START = "🤖 Agent 4: ApiScriptGeneratorAgent starting...";
    public static final String LOG_SUCCESS = "[SUCCESS]";
    public static final String LOG_ERROR = "[ERROR]";
    public static final String LOG_WARNING = "[WARNING]";
    public static final String LOG_INFO = "[INFO]";
}
