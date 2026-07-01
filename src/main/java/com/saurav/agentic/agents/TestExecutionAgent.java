package com.saurav.agentic.agents;

import com.saurav.agentic.config.FrameworkConfig;
import com.saurav.agentic.constants.FrameworkConstants;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TestExecutionAgent — Agent 5 (post-UI-pipeline)
 *
 * Runs the generated Selenium tests via Maven Surefire, captures results,
 * and generates an Allure report. This closes the loop from "compiles" to "passes."
 *
 * Flow:
 * 1. Run `mvn test` via ProcessBuilder
 * 2. Stream real-time output
 * 3. Parse Surefire XML results into structured TestResult objects
 * 4. Run `mvn allure:report` to generate Allure HTML report
 * 5. Provide summary (pass/fail/skip counts)
 */
public class TestExecutionAgent {

    private static final Pattern TEST_COUNT_PATTERN =
            Pattern.compile(
                    "Tests run:\\s*(\\d+),\\s*Failures:\\s*(\\d+),\\s*Errors:\\s*(\\d+),\\s*Skipped:\\s*(\\d+)"
            );

    public static class TestResult {
        public final String className;
        public final int testsRun;
        public final int failures;
        public final int errors;
        public final int skipped;

        public TestResult(String className, int testsRun, int failures, int errors, int skipped) {
            this.className = className;
            this.testsRun = testsRun;
            this.failures = failures;
            this.errors = errors;
            this.skipped = skipped;
        }

        public boolean isSuccess() {
            return failures == 0 && errors == 0;
        }

        @Override
        public String toString() {
            String icon = isSuccess() ? "✅" : "❌";
            return icon + " " + className + " — " + testsRun + " tests, " +
                    failures + " failures, " + errors + " errors, " + skipped + " skipped";
        }
    }

    /**
     * Main entry: run generated tests and produce Allure report.
     * @return list of TestResult per class, empty if no tests ran
     */
    public List<TestResult> run() {
        System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
        System.out.println(FrameworkConstants.LOG_INFO +
                " Starting Agent 5 — Test Execution + Reporting");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Strategy: mvn test → parse results → Allure report");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);

        List<TestResult> results = new ArrayList<>();

        // ── Step 1: Run tests ───────────────────────────────────────────────────
        System.out.println("\n[STEP 1] Running mvn test...\n");
        boolean buildSuccess = runMavenTest(results);

        // ── Step 2: Print summary ───────────────────────────────────────────────
        System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
        long passed = results.stream().filter(TestResult::isSuccess).count();
        long failed = results.stream().filter(r -> !r.isSuccess()).count();
        long totalTests = results.stream().mapToInt(r -> r.testsRun).sum();
        long totalFailures = results.stream().mapToInt(r -> r.failures + r.errors).sum();

        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Agent 5 — Test Execution Complete!");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Test classes : " + results.size() +
                " (passed: " + passed + ", failed: " + failed + ")");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Total tests  : " + totalTests +
                " (failures+errors: " + totalFailures + ")");

        results.stream().filter(r -> !r.isSuccess()).forEach(r -> {
            System.out.println(FrameworkConstants.LOG_ERROR + "   Failed: " + r);
        });

        // ── Step 3: Generate Allure report ──────────────────────────────────────
        if (buildSuccess || totalFailures == 0) {
            generateAllureReport();
        } else {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " Skipping Allure report — build did not complete.");
        }

        System.out.println(FrameworkConstants.LOG_SEPARATOR + "\n");
        return results;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAVEN TEST EXECUTION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Runs `mvn test` with real-time output streaming and parses Surefire results.
     */
    private boolean runMavenTest(List<TestResult> results) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "mvn.cmd", "test"
            );
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder fullOutput = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    fullOutput.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            System.out.println("\n" + FrameworkConstants.LOG_INFO +
                    " mvn test exit code: " + exitCode);

            // ── Parse Surefire results ──────────────────────────────────────────
            parseSurefireResults(results);
            // Also try parsing from the raw output as fallback
            if (results.isEmpty()) {
                parseOutputFallback(results, fullOutput.toString());
            }

            return exitCode == 0;

        } catch (IOException e) {
            System.out.println(FrameworkConstants.LOG_ERROR +
                    " Could not run mvn test: " + e.getMessage());
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Make sure Maven (mvn.cmd) is on your PATH.");
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(FrameworkConstants.LOG_ERROR +
                    " Test execution interrupted.");
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUREFIRE XML PARSING
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parses Surefire XML reports from target/surefire-reports/.
     * Each XML file corresponds to one test class.
     */
    private void parseSurefireResults(List<TestResult> results) {
        Path reportsDir = Paths.get("target", "surefire-reports");
        if (!Files.exists(reportsDir)) {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " Surefire reports directory not found: " + reportsDir);
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(reportsDir, "*.xml")) {
            for (Path xmlFile : stream) {
                String content = Files.readString(xmlFile);

                // Extract test suite name from <testsuite name="...">
                Matcher nameMatcher = Pattern.compile(
                        "<testsuite\\s+[^>]*\\bname\\s*=\\s*\"([^\"]+)\""
                ).matcher(content);
                if (!nameMatcher.find()) continue;
                String suiteName = nameMatcher.group(1);

                // Extract counts from <testsuite ... tests="..." failures="..." errors="..." skipped="...">
                Matcher countMatcher = Pattern.compile(
                        "<testsuite\\s+[^>]*\\btests\\s*=\\s*\"(\\d+)\"[^>]*\\bfailures\\s*=\\s*\"(\\d+)\"[^>]*\\berrors\\s*=\\s*\"(\\d+)\"[^>]*\\bskipped\\s*=\\s*\"(\\d+)\""
                ).matcher(content);
                if (countMatcher.find()) {
                    results.add(new TestResult(
                            suiteName,
                            Integer.parseInt(countMatcher.group(1)),
                            Integer.parseInt(countMatcher.group(2)),
                            Integer.parseInt(countMatcher.group(3)),
                            Integer.parseInt(countMatcher.group(4))
                    ));
                }
            }
        } catch (IOException e) {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " Could not parse Surefire reports: " + e.getMessage());
        }
    }

    /**
     * Fallback parser — extracts summary lines from mvn output.
     * Catches the "Tests run: X, Failures: Y, Errors: Z, Skipped: W" pattern.
     */
    private void parseOutputFallback(List<TestResult> results, String output) {
        Matcher m = TEST_COUNT_PATTERN.matcher(output);
        while (m.find()) {
            results.add(new TestResult(
                    "aggregate",
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(4))
            ));
        }
        if (!results.isEmpty()) {
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Parsed " + results.size() + " aggregate result(s) from output.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ALLURE REPORT GENERATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates Allure HTML report from allure-results/ directory.
     * Runs `mvn allure:report` via Maven.
     */
    private void generateAllureReport() {
        System.out.println("\n[STEP 2] Generating Allure report...");

        Path allureResults = Paths.get("allure-results");
        if (!Files.exists(allureResults)) {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " No allure-results/ directory found — skipping Allure report.");
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Add @Step, @Description, @Severity annotations to generate Allure data.");
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "mvn.cmd", "allure:report"
            );
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(false);

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println(FrameworkConstants.LOG_SUCCESS +
                        " Allure report generated: target/site/allure-maven-plugin/index.html");
            } else {
                System.out.println(FrameworkConstants.LOG_WARNING +
                        " Allure report generation exited with code " + exitCode);
            }

        } catch (Exception e) {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " Could not generate Allure report: " + e.getMessage());
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Install Allure CLI or ensure allure-maven plugin is configured.");
        }
    }
}
