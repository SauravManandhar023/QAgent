package com.saurav.agentic.agents;

import com.saurav.agentic.config.ModelConfig;
import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.TestCase;
import com.saurav.agentic.prompts.composers.PomPromptComposer;
import com.saurav.agentic.prompts.composers.ScriptPromptComposer;
import com.saurav.agentic.llm.LLMService;
import com.saurav.agentic.utils.ExcelUtil;
import com.google.gson.JsonObject;
import com.saurav.agentic.utils.PromptBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScriptGeneratorAgent {

    private final LLMService llmService;
    private final ModelConfig modelConfig;

    private static final String PAGES_OUTPUT_DIR = "src/test/java/pages/";
    private static final String TESTS_OUTPUT_DIR = "src/test/java/generated/ui/";
    private static final String TESTS_TEMP_DIR   = "src/test/java/generated/ui_temp/";
    private static final int MAX_CASES_PER_BATCH = 5;
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    // ── Tracked across run ────────────────────────────────────────────────────
    private int filePairsGenerated       = 0;
    private int consecutiveRateLimitFails = 0;
    private List<String> pendingComponents = new ArrayList<>();
    private final Set<String> generatedClassNamesThisRun = new HashSet<>();
    private boolean exhausted = false;

    public int getFilePairsGenerated() { return filePairsGenerated; }

    public ScriptGeneratorAgent() {
        this.llmService  = new LLMService();
        this.modelConfig = ModelConfig.getInstance();
    }

    public void run(String excelPath, String pageUrl, String pageAnalysis) {

        System.out.println("\n" + FrameworkConstants.LOG_INFO +
                " ============================================");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Agent 2: Script Generator Started");
        System.out.println(FrameworkConstants.LOG_INFO +
                " ============================================");

        try {
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Reading test cases from: " + excelPath);

            List<TestCase> allTestCases = ExcelUtil.readTestCases(excelPath);

            List<TestCase> feasibleCases = allTestCases.stream()
                    .filter(TestCase::isAutomationFeasible)
                    .toList();

            System.out.println(FrameworkConstants.LOG_INFO +
                    " Total test cases     : " + allTestCases.size());
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Automation feasible  : " + feasibleCases.size());

            if (feasibleCases.isEmpty()) {
                System.out.println(FrameworkConstants.LOG_WARNING +
                        " No automation-feasible test cases found. Exiting Agent 2.");
                return;
            }

            Map<String, List<TestCase>> byComponent = groupByComponent(feasibleCases);

            System.out.println(FrameworkConstants.LOG_INFO +
                    " Components found     : " + byComponent.size());

            // ── Write to TEMP dir first ───────────────────────────────────────
            createDirectory(PAGES_OUTPUT_DIR);
            createDirectory(TESTS_TEMP_DIR);

            int componentNumber = 1;
            filePairsGenerated  = 0;
            boolean exhausted   = false;

            for (Map.Entry<String, List<TestCase>> entry : byComponent.entrySet()) {
                if (exhausted) {
                    // Save remaining as pending
                    pendingComponents.add(entry.getKey());
                    continue;
                }

                String component     = entry.getKey();
                List<TestCase> cases = entry.getValue();

                System.out.println("\n" + FrameworkConstants.LOG_INFO +
                        " Processing component " + componentNumber +
                        "/" + byComponent.size() + ": " + component +
                        " (" + cases.size() + " test cases)");

                String className = PromptBuilder.toPascalCase(component);

                // ── Generate POM class ───────────────────────────────────────
                String pomCode = generatePomClass(
                        component, className, pageUrl, pageAnalysis
                );

                // Check for exhaustion
                if (consecutiveRateLimitFails >= MAX_CONSECUTIVE_FAILURES) {
                    exhausted = true;
                    pendingComponents.add(component);
                    System.out.println(FrameworkConstants.LOG_ERROR +
                            " Rate limit EXHAUSTED — stopping component processing.");
                    System.out.println(FrameworkConstants.LOG_INFO +
                            " Quota resets at 5:45 AM NPT.");
                    savePendingComponents(pendingComponents);
                    continue;
                }

                System.out.println(FrameworkConstants.LOG_INFO +
                        "   Waiting between POM and Test generation... (set to 0s for Ollama local)");
                sleep(0);

                boolean allBatchesSucceeded = false;
                List<String> generatedTestClassNamesForComponent = new ArrayList<>();

                if (pomCode != null) {
                    String trimmedPom = extractPublicMethods(pomCode);
                    List<List<TestCase>> batches = splitIntoBatches(cases, MAX_CASES_PER_BATCH);
                    int batchNumber   = 1;
                    int batchesOk     = 0;

                    for (List<TestCase> batch : batches) {
                        if (consecutiveRateLimitFails >= MAX_CONSECUTIVE_FAILURES) {
                            exhausted = true;
                            break;
                        }

                        String batchClassName = batches.size() > 1
                                ? className + "Part" + batchNumber
                                : className;
                        String testClassName = batchClassName + "Test";

                        System.out.println(FrameworkConstants.LOG_INFO +
                                "   Generating test class batch " + batchNumber +
                                "/" + batches.size() +
                                " (" + batch.size() + " tests)...");

                        boolean batchSuccess = generateTestClass(
                                component, testClassName,
                                pageUrl, batch, trimmedPom, pageAnalysis
                        );

                        if (batchSuccess) {
                            batchesOk++;
                            generatedTestClassNamesForComponent.add(testClassName);
                        }

                        if (batchNumber < batches.size()) {
                            System.out.println(FrameworkConstants.LOG_INFO +
                                    "   Waiting between batches... (set to 0s for Ollama local)");
                            sleep(0);
                        }

                        batchNumber++;
                    }

                    allBatchesSucceeded = (batchesOk == batches.size());

                } else {
                    System.out.println(FrameworkConstants.LOG_WARNING +
                            "   Skipping test generation — POM generation failed.");
                }

                if (pomCode != null && allBatchesSucceeded) {
                    filePairsGenerated++;
                    consecutiveRateLimitFails = 0; // reset on success
                    System.out.println(FrameworkConstants.LOG_SUCCESS +
                            " Generated: " + className + "Page.java + " +
                            className + "Test.java");
                    generatedClassNamesThisRun.add(className + "Page");
                    generatedClassNamesThisRun.addAll(generatedTestClassNamesForComponent);
                }

                if (componentNumber < byComponent.size()) {
                    System.out.println(FrameworkConstants.LOG_INFO +
                            "   Waiting before next component... (set to 0s for Ollama local)");
                    sleep(0);
                }

                componentNumber++;
            }

            // ── Swap temp → production only if something generated ────────────
            swapTempToOutput(filePairsGenerated);

            printSummary(byComponent, filePairsGenerated);

        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().equals("Rate limit exhausted")) {
                exhausted = true;
                System.out.println(FrameworkConstants.LOG_ERROR +
                        " Agent 2 failed due to rate limit exhaustion.");
                // The pending components have already been saved in the loop.
                // We will save them again to be safe.
                savePendingComponents(pendingComponents);
            } else {
                System.out.println(FrameworkConstants.LOG_ERROR +
                        " Agent 2 failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEMP → OUTPUT SWAP
    // ─────────────────────────────────────────────────────────────────────────

    private void swapTempToOutput(int generated) {
        if (generated == 0) {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " No files generated this run — keeping existing files unchanged.");
            deleteDirectory(new File(TESTS_TEMP_DIR));
            return;
        }
        // Delete old output and replace with temp
        deleteDirectory(new File(TESTS_OUTPUT_DIR));
        new File(TESTS_TEMP_DIR).renameTo(new File(TESTS_OUTPUT_DIR));
        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Output updated: " + generated + " component(s) generated.");
    }

    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            dir.delete();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PENDING COMPONENTS — persist for next run resume
    // ─────────────────────────────────────────────────────────────────────────

    private void savePendingComponents(List<String> pending) {
        try {
            Files.createDirectories(Paths.get("test-output"));
            try (FileWriter fw = new FileWriter("test-output/pending-components.txt")) {
                for (String c : pending) fw.write(c + "\n");
            }
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Pending components saved: " + pending.size() +
                    " → test-output/pending-components.txt");
        } catch (IOException e) {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " Could not save pending components.");
        }
    }

    public List<String> loadPendingComponents() {
        File f = new File("test-output/pending-components.txt");
        if (!f.exists()) return new ArrayList<>();
        try {
            return Files.readAllLines(f.toPath());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FUZZY COMPONENT GROUPING
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, List<TestCase>> groupByComponent(List<TestCase> testCases) {
        Map<String, List<TestCase>> grouped = new LinkedHashMap<>();
        for (TestCase tc : testCases) {
            String component = tc.getComponent();
            if (component == null || component.isBlank()) {
                component = "General";
            }
            // Find canonical name via fuzzy matching
            String canonical = findCanonicalComponent(component, grouped.keySet());
            grouped.computeIfAbsent(canonical, k -> new ArrayList<>()).add(tc);
        }
        return grouped;
    }

    private String normalizeComponentName(String name) {
        return name.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private String findCanonicalComponent(String name, Set<String> existing) {
        String normalized = normalizeComponentName(name);
        // Stoplist of generic terms that should not be merged if they are the only word in the name
        Set<String> stoplist = Set.of("form", "button", "link", "menu", "page", "section", "modal", "dialog", "field");

        double bestOverlap = 0.0;
        String bestMatch = null;

        for (String existing_name : existing) {
            String existingNorm = normalizeComponentName(existing_name);
            // Compute word sets
            Set<String> normalizedSet = new HashSet<>(Arrays.asList(normalized.split(" ")));
            Set<String> existingSet = new HashSet<>(Arrays.asList(existingNorm.split(" ")));
            // Remove empty strings
            normalizedSet.remove("");
            existingSet.remove("");

            // Compute word overlap (Jaccard index)
            Set<String> intersection = new HashSet<>(normalizedSet);
            intersection.retainAll(existingSet);
            Set<String> union = new HashSet<>(normalizedSet);
            union.addAll(existingSet);
            double overlap = (double) intersection.size() / union.size();

            // Check if either name is a single word from the stoplist
            boolean isNormalizedStopword = normalizedSet.size() == 1 && stoplist.contains(normalized);
            boolean isExistingStopword = existingSet.size() == 1 && stoplist.contains(existing_name);

            if (overlap > bestOverlap && !isNormalizedStopword && !isExistingStopword) {
                bestOverlap = overlap;
                bestMatch = existing_name;
            }
        }

        if (bestOverlap >= 0.6 && bestMatch != null) {
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Merged component '" + name +
                    "' → '" + bestMatch + "' (overlap=" + bestOverlap + ")");
            return bestMatch;
        }

        return name;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POM CLASS GENERATION
    // ─────────────────────────────────────────────────────────────────────────

    private String generatePomClass(String component, String className,
                                     String pageUrl, String pageAnalysis) throws IOException {
        String systemPrompt = PomPromptComposer.systemPrompt();
        String userPrompt   = PomPromptComposer.userPrompt(
                component, className + "Page", pageUrl, pageAnalysis
        );
        String filePath = PAGES_OUTPUT_DIR + className + "Page.java";

        try {
            System.out.println(FrameworkConstants.LOG_INFO +
                    "   Generating POM class: " + className + "Page.java...");

            String javaCode = generateCode(
                    "generate_pom", "Generate Page Object Model Java class",
                    systemPrompt, userPrompt,
                    modelConfig.getAgent2PomModel(),
                    modelConfig.getAgent2Temperature(),
                    modelConfig.getAgent2MaxTokens()
            );

            if (!isValidJavaFile(javaCode, false)) {
                throw new IOException("Generated POM appears truncated or empty");
            }

            saveFile(filePath, javaCode);
            System.out.println(FrameworkConstants.LOG_SUCCESS +
                    "   Saved: " + filePath);
            consecutiveRateLimitFails = 0; // reset on success
            return javaCode;

        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                consecutiveRateLimitFails++;
                if (consecutiveRateLimitFails >= MAX_CONSECUTIVE_FAILURES) {
                    // Exhaustion detected
                    System.out.println(FrameworkConstants.LOG_WARNING +
                            "   Rate limit EXHAUSTED — stopping component processing.");
                    System.out.println(FrameworkConstants.LOG_INFO +
                            " Quota resets at 5:45 AM NPT.");
                    throw new IOException("Rate limit exhausted");
                }
                System.out.println(FrameworkConstants.LOG_WARNING +
                        "   Rate limit hit (" + consecutiveRateLimitFails +
                        "/" + MAX_CONSECUTIVE_FAILURES + ")");
            }
            System.out.println(FrameworkConstants.LOG_WARNING +
                    "   Failed or truncated, waiting to retry... (set to 0s for Ollama local)");
            try {
                Thread.sleep(0);
                String javaCode = generateCode(
                        "generate_pom", "Generate Page Object Model Java class",
                        systemPrompt, userPrompt,
                        modelConfig.getAgent2PomModel(),
                        modelConfig.getAgent2Temperature(),
                        modelConfig.getAgent2MaxTokens()
                );

                if (!isValidJavaFile(javaCode, false)) {
                    throw new IOException("Retry also returned truncated POM");
                }

                saveFile(filePath, javaCode);
                System.out.println(FrameworkConstants.LOG_SUCCESS +
                        "   Retry successful: " + filePath);
                consecutiveRateLimitFails = 0;
                return javaCode;
            } catch (Exception retryEx) {
                if (retryEx.getMessage() != null &&
                    retryEx.getMessage().contains("429")) {
                    consecutiveRateLimitFails++;
                }
                System.out.println(FrameworkConstants.LOG_ERROR +
                        "   Retry also failed for: " + component +
                        " — " + retryEx.getMessage());
                return null;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST CLASS GENERATION
    // ─────────────────────────────────────────────────────────────────────────

    private boolean generateTestClass(String component, String className,
                                       String pageUrl, List<TestCase> cases,
                                       String trimmedPom, String pageAnalysis) throws IOException {
        String testCasesText = formatTestCasesForPrompt(cases);
        String systemPrompt  = ScriptPromptComposer.systemPrompt();
        String userPrompt    = ScriptPromptComposer.userPrompt(
                component, className, pageUrl, testCasesText, trimmedPom, pageAnalysis
        );
        // ← write to TEMP dir
        String filePath = TESTS_TEMP_DIR + className + ".java";

        try {
            System.out.println(FrameworkConstants.LOG_INFO +
                    "   Generating test class: " + className + ".java...");

            String javaCode = generateCode(
                    "generate_test", "Generate TestNG test class",
                    systemPrompt, userPrompt,
                    modelConfig.getAgent2TestModel(),
                    modelConfig.getAgent2Temperature(),
                    modelConfig.getAgent2MaxTokens()
            );

            if (!isValidJavaFile(javaCode, true)) {
                throw new IOException("Generated test class appears truncated or empty");
            }

            saveFile(filePath, javaCode);
            System.out.println(FrameworkConstants.LOG_SUCCESS +
                    "   Saved: " + filePath);
            consecutiveRateLimitFails = 0;
            return true;

        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                consecutiveRateLimitFails++;
                if (consecutiveRateLimitFails >= MAX_CONSECUTIVE_FAILURES) {
                    // Exhaustion detected
                    System.out.println(FrameworkConstants.LOG_WARNING +
                            "   Rate limit EXHAUSTED — stopping component processing.");
                    System.out.println(FrameworkConstants.LOG_INFO +
                            " Quota resets at 5:45 AM NPT.");
                    throw new IOException("Rate limit exhausted");
                }
                System.out.println(FrameworkConstants.LOG_WARNING +
                        "   Rate limit hit (" + consecutiveRateLimitFails +
                        "/" + MAX_CONSECUTIVE_FAILURES + ")");
            }
            System.out.println(FrameworkConstants.LOG_WARNING +
                    "   Failed or truncated, waiting to retry... (set to 0s for Ollama local)");
            try {
                Thread.sleep(0);
                String javaCode = generateCode(
                        "generate_test", "Generate TestNG test class",
                        systemPrompt, userPrompt,
                        modelConfig.getAgent2TestModel(),
                        modelConfig.getAgent2Temperature(),
                        modelConfig.getAgent2MaxTokens()
                );

                if (!isValidJavaFile(javaCode, true)) {
                    throw new IOException("Retry also returned truncated test class");
                }

                saveFile(filePath, javaCode);
                System.out.println(FrameworkConstants.LOG_SUCCESS +
                        "   Retry successful: " + filePath);
                consecutiveRateLimitFails = 0;
                return true;
            } catch (Exception retryEx) {
                if (retryEx.getMessage() != null &&
                    retryEx.getMessage().contains("429")) {
                    consecutiveRateLimitFails++;
                }
                System.out.println(FrameworkConstants.LOG_ERROR +
                        "   Retry also failed for: " + component +
                        " — " + retryEx.getMessage());
                return false;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isValidJavaFile(String code, boolean isTest) {
        if (code == null || code.isBlank()) return false;
        if (!code.contains("package ")) return false;
        if (!code.contains("public class ")) return false;
        if (isTest && !code.contains("@Test")) return false;
        if (code.length() < 200) return false;
        return true;
    }

    private List<List<TestCase>> splitIntoBatches(List<TestCase> cases, int batchSize) {
        List<List<TestCase>> batches = new ArrayList<>();
        for (int i = 0; i < cases.size(); i += batchSize) {
            batches.add(new ArrayList<>(
                    cases.subList(i, Math.min(i + batchSize, cases.size()))
            ));
        }
        return batches;
    }

    private String extractPublicMethods(String pomCode) {
        if (pomCode == null) return "";
        StringBuilder sb = new StringBuilder();
        String[] lines = pomCode.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("public class")) {
                sb.append(line).append("\n");
            }
            if (trimmed.startsWith("public") && trimmed.contains("(")) {
                sb.append("    ").append(trimmed).append("\n");
            }
        }
        return sb.toString();
    }

    private String formatTestCasesForPrompt(List<TestCase> cases) {
        StringBuilder sb = new StringBuilder();
        for (TestCase tc : cases) {
            sb.append("---\n");
            sb.append("ID       : ").append(tc.getTestCaseId()).append("\n");
            sb.append("Name     : ").append(tc.getTestCaseName()).append("\n");
            sb.append("Type     : ").append(tc.getTestType()).append("\n");
            sb.append("Priority : ").append(tc.getPriority()).append("\n");
            sb.append("Steps    : ").append(tc.getTestSteps()).append("\n");
            sb.append("TestData : ").append(tc.getTestData()).append("\n");
            sb.append("Expected : ").append(tc.getExpectedResult()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Uses tool-calling API to generate code, extracting it from the structured response.
     * No markdown/backtick stripping needed — the response is already typed JSON.
     */
    private String generateCode(String toolName, String toolDescription,
                                 String systemPrompt, String userPrompt,
                                 String model, double temperature, int maxTokens) throws IOException {
        Map<String, Object> schema = LLMService.jsonSchema(
                Map.of("code", LLMService.stringProperty("Complete Java source code for the file")),
                List.of("code")
        );
        JsonObject result = llmService.chatWithTools(
                toolName, toolDescription, schema,
                systemPrompt, userPrompt, model, temperature, maxTokens
        );
        return result.has("code") && !result.get("code").isJsonNull()
                ? result.get("code").getAsString()
                : "";
    }

    private void saveFile(String filePath, String content) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }

    private void createDirectory(String path) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " Could not create directory: " + path);
        }
    }

    private void printSummary(Map<String, List<TestCase>> byComponent,
                               int totalGenerated) {
        System.out.println("\n" + FrameworkConstants.LOG_INFO +
                " ============================================");
        System.out.println(FrameworkConstants.LOG_SUCCESS + " Agent 2 Complete!");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Components processed : " + byComponent.size());
        System.out.println(FrameworkConstants.LOG_INFO +
                " File pairs generated : " + totalGenerated +
                " (POM + Test per component)");
        if (!pendingComponents.isEmpty()) {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " Pending components   : " + pendingComponents.size() +
                    " (saved to test-output/pending-components.txt)");
        }
        System.out.println(FrameworkConstants.LOG_INFO +
                " POM classes saved to : " + PAGES_OUTPUT_DIR);
        System.out.println(FrameworkConstants.LOG_INFO +
                " Test classes saved to: " + TESTS_OUTPUT_DIR);
        System.out.println(FrameworkConstants.LOG_INFO +
                " ============================================\n");
    }

    public Set<
    
    String> getGeneratedClassNamesThisRun() {
        return generatedClassNamesThisRun;
    }
    
   

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}