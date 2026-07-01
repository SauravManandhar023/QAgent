package com.saurav.agentic.agents;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.saurav.agentic.config.FrameworkConfig;
import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.TestCase;
import com.saurav.agentic.utils.ExcelUtil;
import com.saurav.agentic.llm.LLMService;
import com.saurav.agentic.utils.PromptBuilder;
import com.saurav.agentic.utils.SeleniumScraper;
import com.saurav.agentic.config.ModelConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TestCaseGeneratorAgent - Agent 1
 *
 * Flow:
 * 1. Takes a URL
 * 2. Scrapes UI elements using SeleniumScraper
 * 3. Sends analysis to Groq AI via GroqClient
 * 4. Parses AI response into TestCase objects
 * 5. Saves to Excel using ExcelUtil
 *
 * P2.2 Fix: Ensures globally unique Test Case IDs by renumbering new batch
 *            starting from maxExistingId + 1 to prevent ambiguity across runs.
 *
 */
public class TestCaseGeneratorAgent {

    private final FrameworkConfig config;
    private final SeleniumScraper scraper;
    private final ModelConfig modelConfig;
    private final LLMService llmService;
    private String lastPageAnalysis = "";

    public TestCaseGeneratorAgent() {
        this.config = FrameworkConfig.getInstance();
        this.scraper = new SeleniumScraper();
        this.modelConfig = ModelConfig.getInstance();
        this.llmService = new LLMService();
    }

    /**
     * Main entry point for Agent 1 — full scrape + generate.
     * @param url - target URL to analyze and generate test cases for
     * @return list of generated TestCase objects
     */
    public List<TestCase> run(String url) throws Exception {
        return run(url, null);
    }

    /**
     * Main entry point for Agent 1 — reuses pre-scraped page analysis.
     * Use this when PageMetadata has already been extracted (avoids redundant browser launch).
     *
     * @param url          - target URL
     * @param pageAnalysis - pre-scraped page analysis (from PageMetadata.toPromptString())
     * @return list of generated TestCase objects
     */
    public List<TestCase> run(String url, String pageAnalysis) throws Exception {
        System.out.println(FrameworkConstants.LOG_AGENT1_START);
        System.out.println(FrameworkConstants.LOG_SEPARATOR);

        // Step 1: Scrape the page (with retry logic) — or reuse pre-scraped analysis
        boolean isPreScraped = (pageAnalysis != null && pageAnalysis.length() > 100);

        if (!isPreScraped) {
            System.out.println("[STEP 1] Scraping UI elements...");
            int maxRetries = 3;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    System.out.println("  Attempt " + attempt + " of " + maxRetries);
                    pageAnalysis = scraper.scrape(url);
                    this.lastPageAnalysis = pageAnalysis;

                    if (pageAnalysis != null && pageAnalysis.length() > 100) {
                        System.out.println(FrameworkConstants.LOG_SUCCESS + " Scraping complete!");
                        System.out.println("Page analysis length: " + pageAnalysis.length() + " characters");
                        break;
                    } else {
                        System.out.println(FrameworkConstants.LOG_WARNING + " Scraping returned minimal data, retrying...");
                        if (attempt < maxRetries) {
                            Thread.sleep(2000);
                        }
                    }
                } catch (Exception e) {
                    System.out.println(FrameworkConstants.LOG_WARNING + " Scraping attempt " + attempt + " failed: " + e.getMessage());
                    if (attempt < maxRetries) {
                        Thread.sleep(2000);
                    } else {
                        throw e;
                    }
                }
            }

            if (pageAnalysis == null || pageAnalysis.length() < 100) {
                throw new Exception("Failed to get meaningful page analysis after " + maxRetries + " attempts");
            }
        } else {
            this.lastPageAnalysis = pageAnalysis;
            System.out.println(FrameworkConstants.LOG_INFO +
                    " [STEP 1] Using pre-scraped page analysis (" +
                    pageAnalysis.length() + " chars) — browser launch skipped.");
        }

        // ── Step 1.5: Load existing test cases ───────────────────────────
        String excelPath = FrameworkConfig.getInstance().getUiExcelOutputPath();
        Set<String> existingNames = loadExistingTestCaseNames(excelPath);
        Map<String, Integer> existingIds = loadExistingTestCaseIds(excelPath);

        boolean isFirstRun = existingNames.isEmpty();

        System.out.println(FrameworkConstants.LOG_INFO +
                " Mode: " + (isFirstRun ? "Fresh generation" :
                "Incremental — " + existingNames.size() + " existing cases found"));

        // ── P2.2: Determine starting ID for new test cases ──────────────
        int maxExistingId = 0;
        if (!existingIds.isEmpty()) {
            maxExistingId = existingIds.values().stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Highest existing Test Case ID: " + maxExistingId);
        }

        // ── Step 2: Send to Groq AI via function-calling ──────────────────
        System.out.println("\n[STEP 2] Sending to Groq AI (function-calling)...");
        String systemPrompt = PromptBuilder.uiTestCaseSystemPrompt();
        String userPrompt = PromptBuilder.uiTestCaseUserPrompt(pageAnalysis, existingNames);

        // Define the JSON Schema for the expected output
        Map<String, Object> testCaseProperties = new HashMap<>();
        testCaseProperties.put("testCaseId", LLMService.stringProperty("Unique test case identifier, e.g. TC_001"));
        testCaseProperties.put("testCaseName", LLMService.stringProperty("Descriptive test case name"));
        testCaseProperties.put("description", LLMService.stringProperty("Detailed description of the test"));
        testCaseProperties.put("preconditions", LLMService.stringProperty("Preconditions needed before test execution"));
        testCaseProperties.put("testSteps", LLMService.stringProperty("Step-by-step test steps"));
        testCaseProperties.put("testData", LLMService.stringProperty("Test data values used"));
        testCaseProperties.put("expectedResult", LLMService.stringProperty("Expected outcome of the test"));
        testCaseProperties.put("testType", LLMService.stringProperty("Positive, Negative, Edge, or Accessibility"));
        testCaseProperties.put("priority", LLMService.stringProperty("High, Medium, or Low"));
        testCaseProperties.put("component", LLMService.stringProperty("UI component this test targets"));
        testCaseProperties.put("automationFeasible", LLMService.booleanProperty("Whether this test can be automated with Selenium"));

        List<String> requiredFields = List.of(
                "testCaseId", "testCaseName", "description", "preconditions",
                "testSteps", "testData", "expectedResult", "testType",
                "priority", "component", "automationFeasible"
        );

        Map<String, Object> schema = LLMService.jsonSchema(
                Map.of("testCases", LLMService.arrayProperty(testCaseProperties, requiredFields,
                        "Array of test cases for this page component")),
                List.of("testCases")
        );

        JsonObject responseJson = llmService.chatWithTools(
                "generate_test_cases",
                "Generate UI test cases based on page analysis",
                schema,
                systemPrompt, userPrompt,
                modelConfig.getAgent1Model(),
                modelConfig.getAgent1Temperature(),
                modelConfig.getAgent1MaxTokens()
        );

        System.out.println(FrameworkConstants.LOG_SUCCESS + " AI response received!");

        // Step 3: Parse response from structured tool call
        System.out.println("\n[STEP 3] Parsing test cases...");
        List<TestCase> testCases = parseTestCasesFromJson(responseJson);

        // Fallback: if tool call returned no testCases but has raw content (model
        // doesn't support structured JSON output), try parsing as markdown test cases.
        if (testCases.isEmpty() && responseJson.has("code") && !responseJson.get("code").isJsonNull()) {
            String rawContent = responseJson.get("code").getAsString();
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " No structured testCases found — trying fallback: markdown parsing (" +
                    rawContent.length() + " chars).");

            // Attempt 1: Try old JSON-in-markdown parsing (handles ```json fences)
            testCases = parseTestCases(rawContent);

            // Attempt 2: If still empty, try structured markdown parser
            if (testCases.isEmpty()) {
                testCases = parseMarkdownTestCases(rawContent);
            }
        }

        System.out.println(FrameworkConstants.LOG_SUCCESS + " Parsed: " + testCases.size() + " test cases!");

        // ── P2.2: Renumber new test cases to ensure globally unique IDs ─────
        if (!isFirstRun && !testCases.isEmpty()) {
            renumberTestCasesImpl(testCases, maxExistingId + 1);
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Renumbered new test cases starting from ID: " + (maxExistingId + 1));
        }

        // ── Step 4: Save to Excel (append mode) ──────────────────────────
        System.out.println(FrameworkConstants.LOG_INFO + " [STEP 4] Saving to Excel...");
        ExcelUtil.appendTestCases(excelPath, testCases, isFirstRun);

        // Step 5: Print summary
        System.out.println(FrameworkConstants.LOG_SEPARATOR);
        printSummary(testCases);

        return testCases;
    }

    /**
     * Renumbers test cases to ensure globally unique IDs across runs
     * @param testCases list of test cases to renumber
     * @param startingId the ID to start from for the first test case in this batch
     */
    private void renumberTestCases(List<TestCase> testCases, int startingId) {
        for (int i = 0; i < testCases.size(); i++) {
            String newId = "TC_" + String.format("%03d", startingId + i);
            testCases.get(i).setTestCaseId(newId);
        }
    }

    /**
     * Parses structured JsonObject (from tool-calling API) into TestCase list.
     * No markdown/backtick stripping needed — the response is already typed JSON.
     */
    private List<TestCase> parseTestCasesFromJson(JsonObject responseJson) {
        List<TestCase> testCases = new ArrayList<>();
        Gson gson = new Gson();

        try {
            JsonArray jsonArray = responseJson.getAsJsonArray("testCases");
            if (jsonArray == null) {
                System.err.println(FrameworkConstants.LOG_ERROR +
                        " Tool response missing 'testCases' array: " + responseJson);
                return testCases;
            }

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject obj = jsonArray.get(i).getAsJsonObject();

                TestCase tc = new TestCase();
                tc.setTestCaseId(getString(obj, "testCaseId",
                        "TC_" + String.format("%03d", i + 1)));
                tc.setTestCaseName(getString(obj, "testCaseName", "Test Case " + (i + 1)));
                tc.setDescription(getString(obj, "description", ""));
                tc.setPreconditions(getString(obj, "preconditions", ""));
                tc.setTestSteps(getString(obj, "testSteps", ""));
                tc.setTestData(getString(obj, "testData", ""));
                tc.setExpectedResult(getString(obj, "expectedResult", ""));
                tc.setTestType(getString(obj, "testType", FrameworkConstants.TEST_TYPE_POSITIVE));
                tc.setPriority(getString(obj, "priority", FrameworkConstants.PRIORITY_MEDIUM));
                tc.setComponent(getString(obj, "component", "General"));
                tc.setAutomationFeasible(
                        obj.has("automationFeasible") &&
                        !obj.get("automationFeasible").isJsonNull() &&
                        obj.get("automationFeasible").getAsBoolean()
                );

                testCases.add(tc);
            }

        } catch (Exception e) {
            System.err.println(FrameworkConstants.LOG_ERROR +
                    " Failed to parse tool-call response: " + e.getMessage());
            System.err.println("Response: " + responseJson);
        }

        return testCases;
    }

    /**
     * @deprecated Replaced by parseTestCasesFromJson with tool-calling API.
     * Kept for backward compatibility with cached responses from free-text API.
     */
    @Deprecated
    private List<TestCase> parseTestCases(String aiResponse) {
        List<TestCase> testCases = new ArrayList<>();
        Gson gson = new Gson();

        try {
            // Clean response
            String cleaned = cleanJsonResponse(aiResponse);

            JsonArray jsonArray = gson.fromJson(cleaned, JsonArray.class);

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject obj = jsonArray.get(i).getAsJsonObject();

                TestCase tc = new TestCase();
                tc.setTestCaseId(getString(obj, "testCaseId",
                        "TC_" + String.format("%03d", i + 1))); // Will be renumbered if needed
                tc.setTestCaseName(getString(obj, "testCaseName", "Test Case " + (i + 1)));
                tc.setDescription(getString(obj, "description", ""));
                tc.setPreconditions(getString(obj, "preconditions", ""));
                tc.setTestSteps(getString(obj, "testSteps", ""));
                tc.setTestData(getString(obj, "testData", ""));   // ← ADD after setTestSteps
                tc.setExpectedResult(getString(obj, "expectedResult", ""));
                tc.setTestType(getString(obj, "testType", FrameworkConstants.TEST_TYPE_POSITIVE));
                tc.setPriority(getString(obj, "priority", FrameworkConstants.PRIORITY_MEDIUM));
                tc.setComponent(getString(obj, "component", "General"));
                tc.setAutomationFeasible(
                        obj.has("automationFeasible") &&
                        !obj.get("automationFeasible").isJsonNull() &&
                        obj.get("automationFeasible").getAsBoolean()
                );

                testCases.add(tc);
            }

        } catch (Exception e) {
            System.err.println(FrameworkConstants.LOG_ERROR +
                    " Failed to parse AI response: " + e.getMessage());
            System.err.println("Raw response (first 500 chars): " +
                    aiResponse.substring(0, Math.min(500, aiResponse.length())));
        }

        return testCases;
    }

    /**
     * Parses markdown-formatted test cases (produced by models like qwen2.5-coder:7b
     * that don't support structured JSON output).
     *
     * Expected format:
     * ### Test Case N:
     * **Test Title**
     * - **Description**: ...
     * - **Preconditions**:
     *   - bullet...
     * - **Steps**:
     *   1. step one
     *   2. step two
     * - **Expected Outcome**: ...
     */
    private List<TestCase> parseMarkdownTestCases(String markdown) {
        List<TestCase> testCases = new ArrayList<>();
        if (markdown == null || markdown.trim().isEmpty()) return testCases;

        try {
            // Split by "### Test Case" to get individual test case blocks
            String[] blocks = markdown.split("(?=###\\s+Test\\s+Case\\s+\\d+)");
            int count = 0;
            for (String block : blocks) {
                block = block.trim();
                if (block.isEmpty() || !block.startsWith("### Test Case")) continue;

                count++;
                TestCase tc = new TestCase();
                tc.setTestCaseId("TC_" + String.format("%03d", count));
                tc.setPriority(FrameworkConstants.PRIORITY_MEDIUM);
                tc.setTestType(FrameworkConstants.TEST_TYPE_POSITIVE);
                tc.setComponent("Home Page");
                tc.setAutomationFeasible(true);

                String[] lines = block.split("\\n");
                StringBuilder titleBuilder = new StringBuilder();
                StringBuilder descriptionBuilder = new StringBuilder();
                StringBuilder preconditionsBuilder = new StringBuilder();
                StringBuilder stepsBuilder = new StringBuilder();
                StringBuilder expectedBuilder = new StringBuilder();
                String currentSection = "";

                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("### Test Case")) continue;

                    // Standalone bold text on its own line = title
                    if (trimmed.matches("\\*\\*[^*]+\\*\\*") && titleBuilder.length() == 0) {
                        titleBuilder.append(trimmed.replaceAll("\\*\\*", ""));
                        continue;
                    }

                    if (trimmed.contains("**Description**")) {
                        currentSection = "description";
                        String d = trimmed.replaceAll("- \\*\\*Description\\*?\\*?:?", "").trim();
                        if (!d.isEmpty()) descriptionBuilder.append(d).append(" ");
                        continue;
                    }
                    if (trimmed.contains("**Preconditions**")) {
                        currentSection = "preconditions";
                        String p = trimmed.replaceAll("- \\*\\*Preconditions\\*?\\*?:?", "").trim();
                        if (!p.isEmpty()) preconditionsBuilder.append(p).append(" ");
                        continue;
                    }
                    if (trimmed.contains("**Steps**")) {
                        currentSection = "steps";
                        continue;
                    }
                    if (trimmed.contains("**Expected Outcome**") || trimmed.contains("**Expected Result**")) {
                        currentSection = "expected";
                        String e = trimmed.replaceAll("- \\*\\*Expected (Outcome|Result)\\*?\\*?:?", "").trim();
                        if (!e.isEmpty()) expectedBuilder.append(e).append(" ");
                        continue;
                    }
                    if (trimmed.contains("**Objective**")) {
                        currentSection = "description";
                        String o = trimmed.replaceAll("- \\*\\*Objective\\*?\\*?:?", "").trim();
                        if (!o.isEmpty()) descriptionBuilder.append(o).append(" ");
                        continue;
                    }

                    if (!currentSection.isEmpty() && !trimmed.isEmpty()) {
                        String cleaned = trimmed.replaceAll("^[-•*]\\s*", "")
                                                .replaceAll("^\\d+\\.\\s*", "")
                                                .trim();
                        if (!cleaned.isEmpty()) {
                            switch (currentSection) {
                                case "description":
                                    descriptionBuilder.append(cleaned).append(" ");
                                    break;
                                case "preconditions":
                                    preconditionsBuilder.append(cleaned).append("\\n");
                                    break;
                                case "steps":
                                    stepsBuilder.append(cleaned);
                                    if (!cleaned.endsWith(".")) stepsBuilder.append(".");
                                    stepsBuilder.append("\\n");
                                    break;
                                case "expected":
                                    expectedBuilder.append(cleaned).append(" ");
                                    break;
                            }
                        }
                    }
                }

                String title = titleBuilder.toString().trim();
                if (title.isEmpty()) {
                    title = descriptionBuilder.toString().trim();
                    if (title.length() > 60) title = title.substring(0, 57) + "...";
                }
                tc.setTestCaseName(title.isEmpty() ? "Test Case " + count : title);
                tc.setDescription(descriptionBuilder.toString().trim());
                tc.setPreconditions(preconditionsBuilder.toString().trim());
                tc.setTestSteps(stepsBuilder.toString().trim());
                tc.setExpectedResult(expectedBuilder.toString().trim());
                tc.setTestData("Values based on: " + title);

                // Determine test type based on content
                String allText = (title + " " + descriptionBuilder).toString().toLowerCase();
                if (allText.contains("invalid") || allText.contains("error") || allText.contains("fail") ||
                    allText.contains("wrong") || allText.contains("incorrect") || allText.contains("negative")) {
                    tc.setTestType(FrameworkConstants.TEST_TYPE_NEGATIVE);
                } else if (allText.contains("edge") || allText.contains("boundary") || allText.contains("empty") ||
                           allText.contains("limit") || allText.contains("maximum")) {
                    tc.setTestType("Edge");
                } else if (allText.contains("accessib") || allText.contains("aria") || allText.contains("keyboard") ||
                           allText.contains("screen reader") || allText.contains("contrast")) {
                    tc.setTestType("Accessibility");
                }

                // Determine automation feasibility
                if (stepsBuilder.toString().toLowerCase().contains("keyboard") ||
                    stepsBuilder.toString().toLowerCase().contains("screen reader") ||
                    stepsBuilder.toString().toLowerCase().contains("resize") ||
                    stepsBuilder.toString().toLowerCase().contains("responsive")) {
                    tc.setAutomationFeasible(false);
                }

                testCases.add(tc);
            }
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Markdown parser extracted " + testCases.size() + " test cases.");
        } catch (Exception e) {
            System.err.println(FrameworkConstants.LOG_WARNING +
                    " Markdown parsing failed: " + e.getMessage());
        }
        return testCases;
    }

    /**
     * Renumbers test cases to ensure globally unique IDs across runs
     * @param testCases list of test cases to renumber
     * @param startingId the ID to start from for the first test case in this batch
     */
    private void renumberTestCasesImpl(List<TestCase> testCases, int startingId) {
        for (int i = 0; i < testCases.size(); i++) {
            String newId = "TC_" + String.format("%03d", startingId + i);
            testCases.get(i).setTestCaseId(newId);
        }
    }

    /**
     * Loads existing test case IDs from Excel to determine the maximum ID used
     * @param excelPath path to the Excel file
     * @return map of test case names to their numeric IDs
     */
    private Map<String, Integer> loadExistingTestCaseIds(String excelPath) {
        Map<String, Integer> existingIds = new HashMap<>();
        File file = new File(excelPath);
        if (!file.exists()) return existingIds;

        try {
            List<TestCase> existingCases = ExcelUtil.readTestCases(excelPath);
            for (TestCase tc : existingCases) {
                String tcName = tc.getTestCaseName().toLowerCase().trim();
                String tcId = tc.getTestCaseId();
                // Extract numeric part from TC_XXX format
                if (tcId != null && tcId.matches("TC_\\d{3}")) {
                    try {
                        int idNum = Integer.parseInt(tcId.substring(3));
                        existingIds.put(tcName, idNum);
                    } catch (NumberFormatException e) {
                        // Ignore malformed IDs
                    }
                }
            }
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Loaded " + existingIds.size() + " existing test case IDs from Excel");
        } catch (Exception e) {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " Could not read existing Excel IDs — starting fresh");
        }
        return existingIds;
    }

    /**
     * Clean AI response — remove markdown, backticks, extra text
     */
    private String cleanJsonResponse(String response) {
        String cleaned = response.trim();

        if (cleaned.contains("```json")) {
            cleaned = cleaned.substring(cleaned.indexOf("```json") + 7);
            cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"));
        } else if (cleaned.contains("```")) {
            cleaned = cleaned.substring(cleaned.indexOf("```") + 3);
            if (cleaned.contains("```")) {
                cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"));
            }
        }

        // Find the JSON array boundaries
        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        if (start != -1 && end != -1) {
            cleaned = cleaned.substring(start, end + 1);
        }else if (start != -1) {
            // ── TRUNCATION RECOVERY ──────────────────────────────────────
            // Response was cut off — find last complete object and close the array
            cleaned = cleaned.substring(start);
            int lastCompleteObject = cleaned.lastIndexOf("},");
            if (lastCompleteObject == -1) {
                lastCompleteObject = cleaned.lastIndexOf("}");
            }
            if (lastCompleteObject != -1) {
                cleaned = cleaned.substring(0, lastCompleteObject + 1) + "]";
                System.out.println("[WARNING] JSON was truncated — recovered " +
                        "partial response, some test cases may be missing");
            }
        }

        return cleaned.trim();
    }

    /**
     * Safely get string from JSON object
     */
    private String getString(JsonObject obj, String key, String defaultValue) {
        return (obj.has(key) && !obj.get(key).isJsonNull())
                ? obj.get(key).getAsString()
                : defaultValue;
    }

    /**
     * Print summary of generated test cases
     */
    private void printSummary(List<TestCase> testCases) {
        System.out.println("\n[AGENT 1 SUMMARY]");
        System.out.println("Total Test Cases  : " + testCases.size());

        Map<String, Long> byType = new HashMap<>();
        Map<String, Long> byPriority = new HashMap<>();

        for (TestCase tc : testCases) {
            byType.merge(tc.getTestType(), 1L, Long::sum);
            byPriority.merge(tc.getPriority(), 1L, Long::sum);
        }

        System.out.println("\nBy Type:");
        byType.forEach((type, count) ->
                System.out.println("  " + type + " : " + count));

        System.out.println("\nBy Priority:");
        byPriority.forEach((priority, count) ->
                System.out.println("  " + priority + " : " + count));

        long automatable = testCases.stream()
                .filter(TestCase::isAutomationFeasible).count();
        System.out.println("\nAutomation Feasible : " + automatable + "/" + testCases.size());
        System.out.println(FrameworkConstants.LOG_SEPARATOR);
    }

    public String getLastPageAnalysis() {
        return lastPageAnalysis;
    }

    //Read existing excel before generating new test cases
    private Set<String> loadExistingTestCaseNames(String excelPath) {
        Set<String> existing = new HashSet<>();
        File file = new File(excelPath);
        if (!file.exists()) return existing;

        try {
            List<TestCase> existingCases = ExcelUtil.readTestCases(excelPath);
            for (TestCase tc : existingCases) {
                existing.add(tc.getTestCaseName().toLowerCase().trim());
            }
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Found " + existing.size() + " existing test cases in Excel");
        } catch (Exception e) {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " Could not read existing Excel — starting fresh");
        }
        return existing;
    }
}