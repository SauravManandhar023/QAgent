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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 */
public class TestCaseGeneratorAgent {

    private final FrameworkConfig config;
    private final SeleniumScraper scraper;
    private final GroqClient groqClient;
    private String lastPageAnalysis = "";

    public TestCaseGeneratorAgent() {
        this.config = FrameworkConfig.getInstance();
        this.scraper = new SeleniumScraper();
        this.groqClient = new GroqClient();
    }

    /**
     * Main entry point for Agent 1
     * @param url - target URL to analyze and generate test cases for
     * @return list of generated TestCase objects
     */
    public List<TestCase> run(String url) throws Exception {
        System.out.println(FrameworkConstants.LOG_AGENT1_START);
        System.out.println(FrameworkConstants.LOG_SEPARATOR);

        // Step 1: Scrape the page
        System.out.println("[STEP 1] Scraping UI elements...");
        String pageAnalysis = scraper.scrape(url);
        this.lastPageAnalysis = pageAnalysis;
        System.out.println(FrameworkConstants.LOG_SUCCESS + " Scraping complete!");

        // Step 2: Send to Groq AI
        System.out.println("\n[STEP 2] Sending to Groq AI...");
        String systemPrompt = PromptBuilder.uiTestCaseSystemPrompt();
        String userPrompt = PromptBuilder.uiTestCaseUserPrompt(pageAnalysis, url);
        String aiResponse = groqClient.chat(systemPrompt, userPrompt);
        System.out.println(FrameworkConstants.LOG_SUCCESS + " AI response received!");

        // Step 3: Parse response
        System.out.println("\n[STEP 3] Parsing test cases...");
        List<TestCase> testCases = parseTestCases(aiResponse);
        System.out.println(FrameworkConstants.LOG_SUCCESS + " Parsed: " + testCases.size() + " test cases!");

        // Step 4: Save to Excel
        System.out.println("\n[STEP 4] Saving to Excel...");
        String excelPath = config.getUiExcelOutputPath();
        ExcelUtil.writeTestCases(testCases, excelPath);
        System.out.println(FrameworkConstants.LOG_SUCCESS + " Excel saved: " + excelPath);

        // Step 5: Print summary
        System.out.println(FrameworkConstants.LOG_SEPARATOR);
        printSummary(testCases);

        return testCases;
    }

    /**
     * Parse Groq AI JSON response into TestCase list
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
                        "TC_" + String.format("%03d", i + 1)));
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
}