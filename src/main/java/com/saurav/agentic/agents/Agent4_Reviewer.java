package com.saurav.agentic.agents;

import com.saurav.agentic.compiler.JavaCompilerUtil;
import com.saurav.agentic.config.ModelConfig;
import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.CompileResult;
import com.saurav.agentic.utils.GroqClient;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Agent4_Reviewer - Code Fix Agent
 *
 * Takes failed CompileResults from Agent 3
 * Sends broken code + exact errors to Groq AI
 * AI fixes the specific errors
 * Re-compiles to verify fix worked
 * Saves fixed file
 */
public class Agent4_Reviewer {

    private final GroqClient groqClient;
    private final ModelConfig modelConfig;

    private static final int MAX_FIX_ATTEMPTS = 2;

    public Agent4_Reviewer() {
        this.groqClient  = new GroqClient();
        this.modelConfig = ModelConfig.getInstance();
    }

    /**
     * Main entry point for Agent 4
     * Receives failed compile results from Agent 3 and attempts to fix them
     *
     * @param compileResults - all results from Agent 3
     * @return number of files successfully fixed
     */
    public int run(List<CompileResult> compileResults) {

        System.out.println("\n" + FrameworkConstants.LOG_INFO +
                " ============================================");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Agent 4: Reviewer Started");
        System.out.println(FrameworkConstants.LOG_INFO +
                " ============================================");

        // Filter only failed files
        List<CompileResult> failedFiles = compileResults.stream()
                .filter(r -> !r.isSuccess())
                .toList();

        if (failedFiles.isEmpty()) {
            System.out.println(FrameworkConstants.LOG_SUCCESS +
                    " No failed files — nothing to fix!");
            System.out.println(FrameworkConstants.LOG_INFO +
                    " ============================================\n");
            return 0;
        }

        System.out.println(FrameworkConstants.LOG_INFO +
                " Files to fix : " + failedFiles.size());

        int fixedCount = 0;

        for (CompileResult failed : failedFiles) {
            System.out.println("\n" + FrameworkConstants.LOG_INFO +
                    " Fixing: " + failed.getClassName());

            boolean fixed = attemptFix(failed);

            if (fixed) {
                fixedCount++;
                System.out.println(FrameworkConstants.LOG_SUCCESS +
                        " Fixed: " + failed.getClassName());
            } else {
                System.out.println(FrameworkConstants.LOG_WARNING +
                        " Could not fix: " + failed.getClassName() +
                        " — left with TODO comments");
            }

            // Pause between fixes to avoid rate limits
            sleep(15000);
        }

        printSummary(failedFiles.size(), fixedCount);
        return fixedCount;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIX LOGIC
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attempts to fix a failed file up to MAX_FIX_ATTEMPTS times
     */
    private boolean attemptFix(CompileResult failed) {
        CompileResult current = failed;

        for (int attempt = 1; attempt <= MAX_FIX_ATTEMPTS; attempt++) {
            System.out.println(FrameworkConstants.LOG_INFO +
                    "   Fix attempt " + attempt + "/" + MAX_FIX_ATTEMPTS);

            try {
                // Ask Groq AI to fix the errors
                String fixedCode = askGroqToFix(
                        current.getSourceCode(),
                        current.getErrorSummary(),
                        current.getFilePath()
                );

                if (fixedCode == null || fixedCode.isBlank()) {
                    System.out.println(FrameworkConstants.LOG_WARNING +
                            "   AI returned empty response — skipping");
                    continue;
                }

                // Clean and save the fixed code
                fixedCode = cleanJavaCode(fixedCode);
                saveFile(current.getFilePath(), fixedCode);

                // Re-compile to verify fix worked
                System.out.println(FrameworkConstants.LOG_INFO +
                        "   Re-compiling after fix...");
                CompileResult recompiled = JavaCompilerUtil.compile(current.getFilePath());

                if (recompiled.isSuccess()) {
                    System.out.println(FrameworkConstants.LOG_SUCCESS +
                            "   Compile successful after fix!");
                    return true;
                } else {
                    System.out.println(FrameworkConstants.LOG_WARNING +
                            "   Still failing after fix — " +
                            recompiled.getErrors().size() + " error(s) remain");
                    // Use recompiled result for next attempt
                    current = recompiled;
                    sleep(15000);
                }

            } catch (Exception e) {
                System.out.println(FrameworkConstants.LOG_ERROR +
                        "   Fix attempt failed: " + e.getMessage());
            }
        }

        return false;
    }

    /**
     * Sends broken code + errors to Groq AI and asks for a fix
     */
    private String askGroqToFix(String sourceCode, String errors,
                                  String filePath) throws IOException {
        String systemPrompt = buildFixSystemPrompt(filePath);
        String userPrompt   = buildFixUserPrompt(sourceCode, errors);

        return groqClient.chat(
                systemPrompt, userPrompt,
                modelConfig.getAgent4Model(),
                modelConfig.getAgent4Temperature(),
                modelConfig.getAgent4MaxTokens()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROMPTS
    // ─────────────────────────────────────────────────────────────────────────

    private String buildFixSystemPrompt(String filePath) {
        boolean isPom = filePath.contains("/pages/");
        String packageLine = isPom ? "package pages;" : "package generated.ui;";

        return """
                You are a senior Java developer fixing Selenium test code.
                You will receive broken Java code and exact compiler errors.
                Your job is to fix ONLY the reported errors — do not rewrite the entire file.

                STRICT RULES:
                1. Return ONLY the fixed Java code — no explanation, no markdown, no backticks
                2. Fix only what the errors report — keep all other code unchanged
                3. Start response directly with: %s
                4. Common fixes:
                   - Missing import → add the correct import
                   - Cannot find symbol → check method name matches POM exactly
                   - SeverityLevel.MEDIUM → change to SeverityLevel.NORMAL
                   - SeverityLevel.HIGH → change to SeverityLevel.CRITICAL
                   - SeverityLevel.LOW → change to SeverityLevel.MINOR
                   - new WebDriverWait(driver, 10) → new WebDriverWait(driver, Duration.ofSeconds(10))
                   - Empty @FindBy(linkText="") → change to @FindBy(css="[href='url']")
                   - driver.findElementByCssSelector() → driver.findElement(By.cssSelector())
                5. If an error cannot be fixed without more context, add:
                   // TODO: Cannot fix automatically — [reason]
                   and keep the rest of the code intact
                """.formatted(packageLine);
    }

    private String buildFixUserPrompt(String sourceCode, String errors) {
        return """
                Fix the following Java file. It has these compiler errors:

                ERRORS:
                %s

                BROKEN CODE:
                %s

                Instructions:
                - Fix ONLY the errors listed above
                - Do not change any other part of the code
                - Return the complete fixed file
                - Start with the package declaration
                """.formatted(errors, sourceCode);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private String cleanJavaCode(String rawCode) {
        if (rawCode == null) return "";
        rawCode = rawCode.replaceAll("```java\\s*", "").replaceAll("```\\s*", "");
        return rawCode.trim();
    }

    private void saveFile(String filePath, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void printSummary(int total, int fixed) {
        System.out.println("\n" + FrameworkConstants.LOG_INFO +
                " ============================================");
        System.out.println(FrameworkConstants.LOG_SUCCESS + " Agent 4 Complete!");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Files to fix  : " + total);
        System.out.println(FrameworkConstants.LOG_INFO +
                " Fixed         : " + fixed);
        System.out.println(FrameworkConstants.LOG_INFO +
                " Still broken  : " + (total - fixed));
        System.out.println(FrameworkConstants.LOG_INFO +
                " ============================================\n");
    }
}