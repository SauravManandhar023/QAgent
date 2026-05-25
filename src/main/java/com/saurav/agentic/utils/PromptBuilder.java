package com.saurav.agentic.utils;

import com.saurav.agentic.constants.FrameworkConstants;

/**
 * PromptBuilder - Builds structured prompts for Groq AI
 * Separates prompt logic from agent logic
 * Makes prompts easy to update without touching agent code
 *
 
 */
public class PromptBuilder {

    private PromptBuilder() {}

    // ===================================================
    // AGENT 1 - UI TEST CASE GENERATION PROMPTS
    // ===================================================

    /**
     * System prompt for UI test case generation
     * Tells Groq AI exactly what role to play and what format to return
     */
    public static String uiTestCaseSystemPrompt() {
        return """
                You are a senior QA engineer with 10+ years of experience in web application testing.
                Your job is to analyze web page UI elements and generate comprehensive test cases.

                For each UI component found, generate test cases covering:
                - Positive cases: valid inputs and happy path flows
                - Negative cases: invalid inputs, boundary values, wrong data types
                - Edge cases: empty fields, special characters, max/min length, SQL injection attempts
                - Accessibility cases: keyboard navigation, missing alt text, form labels

                STRICT RULES:
                1. Respond ONLY with a valid JSON array
                2. No explanation before or after the JSON
                3. No markdown code blocks or backticks
                4. Every field must be present in every test case
                5. automationFeasible must be true or false (boolean)
                6. testType must be exactly one of: Positive, Negative, Edge, Accessibility
                7. priority must be exactly one of: High, Medium, Low

                JSON structure for each test case:
                {
                    "testCaseId": "TC_001",
                    "testCaseName": "Verify successful login with valid credentials",
                    "description": "Test that user can login with correct username and password",
                    "preconditions": "User must be registered. Browser is open on login page.",
                    "testSteps": "1. Navigate to login URL\\n2. Enter valid username\\n3. Enter valid password\\n4. Click Login button",
                    "expectedResult": "User is redirected to dashboard. Welcome message is displayed.",
                    "testType": "Positive",
                    "priority": "High",
                    "component": "Login Form",
                    "automationFeasible": true
                }
                """;
    }

    /**
     * User prompt for UI test case generation
     * Injects the scraped page analysis into the prompt
     */
    public static String uiTestCaseUserPrompt(String pageAnalysis, String url) {
        return """
                Analyze the following web page and generate comprehensive test cases.
                
                Target URL: %s
                
                PAGE ANALYSIS:
                %s
                
                Requirements:
                - Generate at least 15 test cases covering all UI components found
                - Cover all 4 test types: Positive, Negative, Edge, Accessibility
                - Focus on form validation, button interactions, and navigation
                - Make test steps detailed and executable by an automation engineer
                - Set automationFeasible=true for cases that can be automated with Selenium
                - Set automationFeasible=false for visual/manual-only cases
                
                Return ONLY the JSON array. Start with [ and end with ]
                """.formatted(url, pageAnalysis);
    }

    // ===================================================
    // AGENT 2 - SELENIUM SCRIPT GENERATION PROMPTS
    // ===================================================

    /**
     * System prompt for Selenium script generation
     */
    public static String seleniumScriptSystemPrompt() {
        return """
                You are a senior Selenium automation engineer with 10+ years of experience.
                Your job is to write clean, production-quality Selenium Java test scripts.
                
                STRICT RULES:
                1. Use Page Object Model (POM) pattern
                2. Use WebDriverManager for driver setup
                3. Use TestNG annotations: @Test, @BeforeMethod, @AfterMethod
                4. Use WebDriverWait for all element interactions — NEVER use Thread.sleep
                5. Use meaningful variable names and add comments
                6. Add Allure annotations: @Description, @Severity
                7. Follow Java naming conventions (camelCase methods, PascalCase classes)
                8. Package must be: generated.ui
                9. Import only what is used
                10. Respond with PURE Java code only — no explanation, no markdown, no backticks
                
                Start your response directly with: package generated.ui;
                """;
    }

    /**
     * User prompt for Selenium script generation per component
     */
    public static String seleniumScriptUserPrompt(String component, String className,
                                                   String pageUrl, String testCasesText) {
        return """
                Generate a complete Selenium TestNG test class for the '%s' component.
                
                Target URL: %s
                Class Name: %s
                POM Class to use: pages.%sPage
                
                Test cases to implement:
                %s
                
                Requirements:
                - Each test case becomes one @Test method
                - Use WebDriverManager.chromedriver().setup() in @BeforeMethod
                - Use driver.quit() in @AfterMethod
                - Use WebDriverWait with ExpectedConditions for all interactions
                - Add @Description("...") above each @Test method
                - Add @Severity(SeverityLevel.CRITICAL) for High priority tests
                - Add @Severity(SeverityLevel.NORMAL) for Medium priority tests
                - Add @Severity(SeverityLevel.MINOR) for Low priority tests
                - Use Assert from TestNG for all assertions
                
                Start response with: package generated.ui;
                """.formatted(component, pageUrl, className,
                              toPascalCase(component), testCasesText);
    }

    // ===================================================
    // AGENT 2 - POM CLASS GENERATION PROMPTS
    // ===================================================

    /**
     * System prompt for POM class generation
     */
    public static String pomSystemPrompt() {
        return """
                You are a senior Selenium automation engineer.
                Generate a Page Object Model (POM) class for the given web page component.
                
                STRICT RULES:
                1. Use @FindBy annotations for all locators
                2. Use PageFactory.initElements(driver, this) in constructor
                3. Include action methods that return void or String
                4. Add JavaDoc comments for the class and each method
                5. Package must be: pages
                6. Respond with PURE Java code only — no explanation, no markdown, no backticks
                
                Start your response directly with: package pages;
                """;
    }

    /**
     * User prompt for POM class generation
     */
    public static String pomUserPrompt(String component, String className,
                                        String pageUrl, String pageAnalysis) {
        return """
                Generate a Page Object Model class for the '%s' component.
                
                Target URL: %s
                Class Name: %s
                
                Page elements found:
                %s
                
                Requirements:
                - Create @FindBy locators for all relevant elements
                - Prefer id > name > cssSelector > xpath for locator priority
                - Include action methods like: enterUsername(), enterPassword(), clickLogin()
                - Include getter methods like: getErrorMessage(), getPageTitle()
                - Constructor must accept WebDriver and call PageFactory.initElements
                
                Start response with: package pages;
                """.formatted(component, pageUrl, className, pageAnalysis);
    }

    // ===================================================
    // AGENT 3 - API TEST CASE GENERATION PROMPTS
    // ===================================================

    /**
     * System prompt for API test case generation
     */
    public static String apiTestCaseSystemPrompt() {
        return """
                You are a senior API QA engineer with 10+ years of experience.
                Your job is to analyze API endpoints and generate comprehensive API test cases.
                
                For each endpoint, generate test cases covering:
                - Positive cases: valid requests with expected 2xx responses
                - Negative cases: invalid data, missing fields, wrong types
                - Auth cases: missing token, expired token, invalid token
                - Edge cases: boundary values, empty body, large payload
                
                STRICT RULES:
                1. Respond ONLY with a valid JSON array
                2. No explanation before or after the JSON
                3. No markdown code blocks or backticks
                4. Every field must be present in every test case
                
                JSON structure for each API test case:
                {
                    "testCaseId": "API_001",
                    "testCaseName": "Verify POST /login returns 200 with valid credentials",
                    "endpoint": "/login",
                    "httpMethod": "POST",
                    "requestHeaders": {"Content-Type": "application/json"},
                    "requestBody": {"username": "admin", "password": "password"},
                    "expectedStatusCode": 200,
                    "expectedResponseField": "token",
                    "testType": "Positive",
                    "priority": "High",
                    "automationFeasible": true
                }
                """;
    }

    /**
     * User prompt for API test case generation
     */
    public static String apiTestCaseUserPrompt(String baseUrl, String apiInfo) {
        return """
                Analyze the following API and generate comprehensive test cases.
                
                Base URL: %s
                
                API Information:
                %s
                
                Requirements:
                - Generate at least 10 test cases per endpoint
                - Cover all HTTP methods found
                - Include auth testing if authentication is required
                - Cover status codes: 200, 201, 400, 401, 403, 404, 500
                
                Return ONLY the JSON array. Start with [ and end with ]
                """.formatted(baseUrl, apiInfo);
    }

    // ===================================================
    // HELPER METHODS
    // ===================================================

    /**
     * Convert component name to PascalCase class name
     * e.g. "login form" -> "LoginForm"
     */
    public static String toPascalCase(String input) {
        if (input == null || input.isEmpty()) return "Component";
        String[] words = input.replaceAll("[^a-zA-Z0-9 ]", "").split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase());
            }
        }
        return result.isEmpty() ? "Component" : result.toString();
    }
}