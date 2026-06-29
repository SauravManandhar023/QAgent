package com.saurav.agentic.agents;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.saurav.agentic.config.FrameworkConfig;
import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.TestCase;
import com.saurav.agentic.utils.ExcelUtil;
import com.saurav.agentic.utils.GroqClient;
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
    private final GroqClient groqClient;
    private String lastPageAnalysis = "";

    public TestCaseGeneratorAgent() {
        this.config = FrameworkConfig.getInstance();
        this.scraper = new SeleniumScraper();
        this.modelConfig = ModelConfig.getInstance();
        this.groqClient = new GroqClient();
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

        // ── Step 2: Send to Groq AI ─────────────────────────────────────
        System.out.println("\n[STEP 2] Sending to Groq AI...");
        String systemPrompt = PromptBuilder.uiTestCaseSystemPrompt();
        String userPrompt = PromptBuilder.uiTestCaseUserPrompt(pageAnalysis, existingNames);
        String aiResponse = groqClient.chat(systemPrompt, userPrompt,
                modelConfig.getAgent1Model(),
                modelConfig.getAgent1Temperature(),
                modelConfig.getAgent1MaxTokens()
        );
        System.out.println(FrameworkConstants.LOG_SUCCESS + " AI response received!");

        // Step 3: Parse response
        System.out.println("\n[STEP 3] Parsing test cases...");
        List<TestCase> testCases = parseTestCases(aiResponse);
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
     * Parses Groq AI JSON response into TestCase list
     */
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