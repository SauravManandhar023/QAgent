package com.saurav.agentic.runners;

import com.saurav.agentic.agents.Agent3_Compiler;
import com.saurav.agentic.agents.Agent4_Reviewer;
import com.saurav.agentic.agents.TestExecutionAgent;
import com.saurav.agentic.agents.ApiDiscoveryAgent;
import com.saurav.agentic.agents.ScriptGeneratorAgent;
import com.saurav.agentic.agents.TestCaseGeneratorAgent;
import com.saurav.agentic.config.FrameworkConfig;
import com.saurav.agentic.config.ModelConfig;
import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.llm.ProviderFactory;
import com.saurav.agentic.models.ApiEndpoint;
import com.saurav.agentic.models.CompileResult;
import com.saurav.agentic.scraper.PageMetadataExtractor;
import com.saurav.agentic.models.PageMetadata;
import com.saurav.agentic.agents.ApiTestCaseGeneratorAgent;
import com.saurav.agentic.models.ApiTestCase;
import com.saurav.agentic.utils.ApiExcelUtil;

import java.io.File;
import java.util.List;

public class Main {

    // Set to true to skip Agent 1 and reuse existing Excel
    // Useful when testing Agent 2/3/4 changes without re-scraping
    // WARNING: Make sure Excel matches current URL before enabling
    private static final boolean SKIP_AGENT1 = true;

    // Set pipeline.interactive=false in config.properties to skip the human
    // validation prompt between Agent 1 and Agent 2 (required for CI/CD).
    private static final boolean INTERACTIVE = FrameworkConfig.getInstance().isInteractive();

    // ── LLM Provider ──────────────────────────────────────────────────────────
    // Provider is selected via config.properties: llm.provider
    // Supported: groq, ollama, claude, openai, gemini, nim, deepseek, fcc
    // No code changes needed to switch providers.

    // ── API Toggle ────────────────────────────────────────────────────────────
    private static final boolean RUN_API_AGENTS = true;

    public static void main(String[] args) throws Exception {

        FrameworkConfig config = FrameworkConfig.getInstance();
        config.printConfig();
        ModelConfig.getInstance().printModelConfig();

        // Print which provider is active
        String provider = config.getLlmProvider();
        System.out.println(FrameworkConstants.LOG_INFO +
                " Active LLM Provider: " + provider + " (change via llm.provider in config.properties)");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Supported providers: groq, ollama, claude, openai, gemini, nim, deepseek, fcc");

        String url       = config.getBaseUrl();
        String excelPath = config.getUiExcelOutputPath();
        String pageAnalysis = "";

        // Smart skip — only skip if manual flag set AND excel exists
        boolean shouldSkip = SKIP_AGENT1 && new File(excelPath).exists();

        // ── Step 0: Extract metadata ONCE (single browser launch) ────────────
        System.out.println("\n" + FrameworkConstants.LOG_INFO +
                " Extracting page metadata (single browser launch)...");
        PageMetadataExtractor extractor = new PageMetadataExtractor();
        PageMetadata metadata = extractor.extract(url);
        pageAnalysis = metadata.toPromptString();

        System.out.println(FrameworkConstants.LOG_INFO +
                " Page type detected: " + metadata.getPageType());
        System.out.println(FrameworkConstants.LOG_INFO +
                " Elements found    : " + metadata.getAllElements().size());

        // ── Agent 1 ──────────────────────────────────────────────────────────
        if (!shouldSkip) {
            System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
            System.out.println(FrameworkConstants.LOG_INFO + " Starting Agent 1...");
            System.out.println(FrameworkConstants.LOG_SEPARATOR);

            TestCaseGeneratorAgent agent1 = new TestCaseGeneratorAgent();
            agent1.run(url, pageAnalysis);

            System.out.println(FrameworkConstants.LOG_SUCCESS +
                    " Agent 1 complete!");

            System.out.println(FrameworkConstants.LOG_INFO +
                    " Excel: " + excelPath);

            // ── Human Validation Step ────────────────────────────────────────────
            if (INTERACTIVE) {
                humanValidationStep();
            } else {
                System.out.println(FrameworkConstants.LOG_INFO +
                        " [CI MODE] Skipping human validation — proceeding automatically.");
            }

        } else {
            System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
            System.out.println(FrameworkConstants.LOG_INFO +
                    " [SKIP] Agent 1 skipped — reusing existing Excel: " + excelPath);
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " Make sure Excel matches current URL: " + url);
            System.out.println(FrameworkConstants.LOG_SEPARATOR);
        }

        // ── Agent 2 ──────────────────────────────────────────────────────────
        System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
        System.out.println(FrameworkConstants.LOG_INFO + " Starting Agent 2...");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);

        ScriptGeneratorAgent agent2 = new ScriptGeneratorAgent();
        agent2.run(excelPath, url, pageAnalysis);

        // After Agent 2
        if (agent2.getFilePairsGenerated() == 0) {
            System.out.println(FrameworkConstants.LOG_ERROR +
                " Agent 2 generated 0 file pairs this run — skipping Agent 3/4.");
            System.out.println(FrameworkConstants.LOG_INFO +
                " Likely cause: rate-limit exhaustion. Existing generated files preserved.");
        } else {

        // ── Agent 3 ──────────────────────────────────────────────────────────
        System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
        System.out.println(FrameworkConstants.LOG_INFO + " Starting Agent 3...");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);

        Agent3_Compiler agent3 = new Agent3_Compiler();
        List<CompileResult> compileResults = agent3.run(agent2.getGeneratedClassNamesThisRun());

        long failedCount = compileResults.stream()
                .filter(r -> !r.isSuccess()).count();

        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Agent 3 complete! Failed: " + failedCount +
                "/" + compileResults.size() + " files");

        // ── Agent 4 ──────────────────────────────────────────────────────────
        System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
        System.out.println(FrameworkConstants.LOG_INFO + " Starting Agent 4...");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);

        Agent4_Reviewer agent4 = new Agent4_Reviewer();
        int fixedCount = agent4.run(compileResults);

        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Agent 4 complete! Fixed: " + fixedCount + " files");
        }

        // ── Agent 5 — Test Execution + Allure Reporting ───────────────────────
        boolean runTests = Boolean.parseBoolean(
                config.getConfigReader().get("pipeline.run.tests", "true")
        );
        if (runTests) {
            System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Starting Agent 5 — Test Execution...");
            System.out.println(FrameworkConstants.LOG_SEPARATOR);

            TestExecutionAgent agent5 = new TestExecutionAgent();
            agent5.run();
        } else {
            System.out.println(FrameworkConstants.LOG_INFO +
                    " [SKIP] Test execution disabled (pipeline.run.tests=false)");
        }

        // ── Done ─────────────────────────────────────────────────────────────
        System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
        System.out.println(FrameworkConstants.LOG_SUCCESS + " QAgent pipeline complete!");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Test classes : src/test/java/generated/ui/");
        System.out.println(FrameworkConstants.LOG_INFO +
                " POM classes  : src/test/java/pages/");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);

        // ── API Pipeline ────────────────────────────────────────────────────
        if (RUN_API_AGENTS) {
            // ── Agent 5A ─────────────────────────────────────────────────────
            System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Starting Agent 5A — API Discovery...");
            System.out.println(FrameworkConstants.LOG_SEPARATOR);

            ApiDiscoveryAgent agent5a = new ApiDiscoveryAgent(config.getApiBaseUrl());
            List<ApiEndpoint> endpoints = agent5a.discover();

            System.out.println(FrameworkConstants.LOG_SUCCESS +
                    " Agent 5A complete! Discovered: " +
                    endpoints.size() + " endpoints");

            // ── Agent 5B ─────────────────────────────────────────────────────
            System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Starting Agent 5B — API Test Case Generation...");
            System.out.println(FrameworkConstants.LOG_SEPARATOR);

            ApiTestCaseGeneratorAgent agent5b = new ApiTestCaseGeneratorAgent();
            List<ApiTestCase> apiTestCases = agent5b.generate(endpoints);

            String apiExcelPath = config.getApiExcelOutputPath();
            ApiExcelUtil.writeApiTestCases(apiTestCases, apiExcelPath);

            System.out.println(FrameworkConstants.LOG_SUCCESS +
                    " Agent 5B complete! " + apiTestCases.size() +
                    " API test cases saved to: " + apiExcelPath);
        }
    }

    /**
     * Interactive human validation step — prompts the user to review/edit
     * generated test cases in Excel before proceeding with Agent 2.
     * Skipped automatically when INTERACTIVE=false.
     */
    private static void humanValidationStep() throws Exception {
        System.out.println("\n" + FrameworkConstants.LOG_INFO +
                " [HUMAN VALIDATION] Review generated test cases in: " +
                FrameworkConfig.getInstance().getUiExcelOutputPath());
        System.out.println(FrameworkConstants.LOG_INFO +
                " You can modify, add, or delete test cases as needed.");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Choose an option:");
        System.out.println(FrameworkConstants.LOG_INFO + "     1. Continue without editing (use the Excel as is)");
        System.out.println(FrameworkConstants.LOG_INFO + "     2. Edit the test cases in Excel (then press ENTER when done)");
        System.out.print(FrameworkConstants.LOG_INFO + "     Enter choice (1 or 2): ");

        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
            String input = reader.readLine();
            String choice = input.trim();
            if (choice.isEmpty()) {
                choice = "1";
            }
            if ("2".equalsIgnoreCase(choice)) {
                System.out.println(FrameworkConstants.LOG_INFO +
                        " Please edit the Excel file now. Press ENTER when you are done.");
                reader.readLine();
            } else if (!"1".equalsIgnoreCase(choice) && !"quit".equalsIgnoreCase(choice)) {
                System.out.println(FrameworkConstants.LOG_WARNING +
                        " Invalid choice. Assuming continue without editing.");
            }
            if ("quit".equalsIgnoreCase(choice)) {
                System.out.println(FrameworkConstants.LOG_INFO + " Pipeline terminated by user.");
                System.exit(0);
            }
        } catch (Exception e) {
            System.out.println(FrameworkConstants.LOG_WARNING + " Could not read input, continuing...");
        }
    }
}
