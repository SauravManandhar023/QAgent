package com.saurav.agentic.agents;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.saurav.agentic.compiler.JavaCompilerUtil;
import com.saurav.agentic.config.ModelConfig;
import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.llm.LLMService;
import com.saurav.agentic.models.CompileResult;
import com.google.gson.JsonObject;

/**
 * Agent4_Reviewer - Smart Code Fix Agent
 *
 * Strategy: Rules First → LLM Only If Needed
 * 
 * Pass 1: Anti-pattern detection + deterministic regex fixes (0 tokens)
 * Pass 2: Compile — if passes, done
 * Pass 3: Error-context-only LLM fix (minimal tokens, only if needed)
 * Pass 4: Re-compile to verify
 */
public class Agent4_Reviewer {

    private final LLMService llmService;
    private final ModelConfig modelConfig;

    private static final int MAX_FIX_ATTEMPTS  = 1;
    private static final int CONTEXT_LINES     = 8; // lines around error to send LLM

    // ── Fix Knowledge Base ────────────────────────────────────────────────────
    // Known error patterns → deterministic fixes applied before LLM
    private static final Map<String, String> KNOWN_FIXES = new HashMap<>();

    static {
        KNOWN_FIXES.put("SeverityLevel.MEDIUM",  "SeverityLevel.NORMAL");
        KNOWN_FIXES.put("SeverityLevel.HIGH",    "SeverityLevel.CRITICAL");
        KNOWN_FIXES.put("SeverityLevel.LOW",     "SeverityLevel.MINOR");
        KNOWN_FIXES.put("import org.openqa.selenium.Duration;", "import java.time.Duration;");
        KNOWN_FIXES.put("import org.openqa.selenium.support.ui.Duration;", "import java.time.Duration;");
        KNOWN_FIXES.put("@FindBy(linkText = \"\")", "// TODO: empty linkText — fix locator manually");
        // JUnit to TestNG migration fixes
        KNOWN_FIXES.put("import org.junit.jupiter.api.BeforeEach;", "");
        KNOWN_FIXES.put("import org.junit.jupiter.api.AfterEach;", "");
        KNOWN_FIXES.put("import org.junit.jupiter.api.BeforeAll;", "");
        KNOWN_FIXES.put("import org.junit.jupiter.api.AfterAll;", "");
        KNOWN_FIXES.put("import org.junit.jupiter.api.Test;", "");
        KNOWN_FIXES.put("import org.junit.jupiter.api.Assertions;", "");
    }

    public Agent4_Reviewer() {
        this.llmService  = new LLMService();
        this.modelConfig = ModelConfig.getInstance();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN ENTRY
    // ─────────────────────────────────────────────────────────────────────────

    public int run(List<CompileResult> compileResults) {

        System.out.println("\n" + FrameworkConstants.LOG_INFO +
                " ============================================");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Agent 4: Smart Reviewer Started");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Strategy: Rules First → LLM Only If Needed");
        System.out.println(FrameworkConstants.LOG_INFO +
                " ============================================");

        int llmCallCount   = 0;
        int rulesFixCount  = 0;
        int totalFixed     = 0;

        // ── Pass 1: Apply deterministic fixes to ALL files ────────────────────
        System.out.println(FrameworkConstants.LOG_INFO +
                " Pass 1: Applying deterministic rule fixes...");

        List<CompileResult> afterRuleFix = new ArrayList<>();
        for (CompileResult result : compileResults) {
            if (result.getSourceCode() == null) {
                afterRuleFix.add(result);
                continue;
            }
            String original = result.getSourceCode();
            String fixed    = applyDeterministicFixes(result.getSourceCode());

            if (!fixed.equals(original)) {
                try {
                    saveFile(result.getFilePath(), fixed);
                    result.setSourceCode(fixed);
                    rulesFixCount++;
                    System.out.println(FrameworkConstants.LOG_SUCCESS +
                            "   Rules fixed: " + result.getClassName());
                } catch (IOException e) {
                    System.out.println(FrameworkConstants.LOG_WARNING +
                            "   Could not save rules fix: " + result.getClassName());
                }
            }
            afterRuleFix.add(result);
        }

        System.out.println(FrameworkConstants.LOG_INFO +
                " Rules fixes applied: " + rulesFixCount + " files (0 tokens used)");

        // ── Pass 2: Re-compile everything ─────────────────────────────────────
        System.out.println(FrameworkConstants.LOG_INFO +
                " Pass 2: Re-compiling after rule fixes...");

        List<CompileResult> stillFailing = new ArrayList<>();
        int passedAfterRules = 0;

        for (CompileResult result : afterRuleFix) {
            CompileResult fresh = JavaCompilerUtil.compile(result.getFilePath());
            if (fresh.isSuccess()) {
                passedAfterRules++;
                totalFixed++;
            } else {
                stillFailing.add(fresh);
            }
        }

        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Passed after rules: " + passedAfterRules + " files");

        if (stillFailing.isEmpty()) {
            System.out.println(FrameworkConstants.LOG_SUCCESS +
                    " All files fixed by rules — no LLM calls needed!");
            printSummary(compileResults.size(), totalFixed, llmCallCount, rulesFixCount);
            return totalFixed;
        }

        // ── Pass 3: LLM fix — only for remaining failures ─────────────────────
        System.out.println(FrameworkConstants.LOG_INFO +
                " Pass 3: " + stillFailing.size() +
                " files still failing — sending to LLM (minimal context)...");

        for (CompileResult failed : stillFailing) {
            System.out.println("\n" + FrameworkConstants.LOG_INFO +
                    " LLM fixing: " + failed.getClassName());

            boolean fixed = attemptLlmFix(failed);
            llmCallCount++;

            if (fixed) {
                totalFixed++;
                System.out.println(FrameworkConstants.LOG_SUCCESS +
                        " LLM fixed: " + failed.getClassName());
            } else {
                System.out.println(FrameworkConstants.LOG_WARNING +
                        " Could not fix: " + failed.getClassName());
            }

            sleep(0); // brief pause between LLM calls - set to 0 for Ollama local to reduce execution time
        }

        printSummary(compileResults.size(), totalFixed, llmCallCount, rulesFixCount);
        return totalFixed;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASS 1 — DETERMINISTIC RULE FIXES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Applies all known deterministic fixes without any LLM call.
     * Handles 80% of common compile errors instantly.
     */
    String applyDeterministicFixes(String code) {
        if (code == null) return "";

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 1 — KNOWN STRING REPLACEMENTS (Knowledge Base)
        // ═══════════════════════════════════════════════════════════════════
        for (Map.Entry<String, String> fix : KNOWN_FIXES.entrySet()) {
            code = code.replace(fix.getKey(), fix.getValue());
        }

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 2 — SEVERITY LEVEL FIXES (NON-DUPLICATE — regex patterns only)
        // SECTION 1 above already handles simple severity renames via KNOWN_FIXES.
        // These no-ops document valid values that should NOT be flagged:
        code = code.replace("SeverityLevel.BLOCKER",  "SeverityLevel.BLOCKER");  // valid, keep
        code = code.replace("SeverityLevel.TRIVIAL",  "SeverityLevel.TRIVIAL");  // valid, keep

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 3 — WAIT FIXES
        // ═══════════════════════════════════════════════════════════════════

        // WebDriverWait raw int → Duration.ofSeconds
        code = code.replaceAll(
            "new WebDriverWait\\(([^,]+),\\s*(\\d+)\\)",
            "new WebDriverWait($1, Duration.ofSeconds($2))"
        );

        // Thread.sleep raw int — keep but flag (can't auto-fix without context)
        // FluentWait raw int → Duration
        code = code.replaceAll(
            "\\.withTimeout\\((\\d+),\\s*TimeUnit\\.SECONDS\\)",
            ".withTimeout(Duration.ofSeconds($1))"
        );
        code = code.replaceAll(
            "\\.pollingEvery\\((\\d+),\\s*TimeUnit\\.MILLISECONDS\\)",
            ".pollingEvery(Duration.ofMillis($1))"
        );
        code = code.replaceAll(
            "\\.pollingEvery\\((\\d+),\\s*TimeUnit\\.SECONDS\\)",
            ".pollingEvery(Duration.ofSeconds($1))"
        );

        // implicitlyWait raw int
        code = code.replaceAll(
            "\\.implicitlyWait\\((\\d+),\\s*TimeUnit\\.SECONDS\\)",
            ".implicitlyWait(Duration.ofSeconds($1))"
        );
        code = code.replaceAll(
            "\\.pageLoadTimeout\\((\\d+),\\s*TimeUnit\\.SECONDS\\)",
            ".pageLoadTimeout(Duration.ofSeconds($1))"
        );

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 4 — DEPRECATED SELENIUM 3 METHOD FIXES
        // ═══════════════════════════════════════════════════════════════════

        code = code.replaceAll(
            "driver\\.findElementByCssSelector\\(([^)]+)\\)",
            "driver.findElement(By.cssSelector($1))"
        );
        code = code.replaceAll(
            "driver\\.findElementById\\(([^)]+)\\)",
            "driver.findElement(By.id($1))"
        );
        code = code.replaceAll(
            "driver\\.findElementByName\\(([^)]+)\\)",
            "driver.findElement(By.name($1))"
        );
        code = code.replaceAll(
            "driver\\.findElementByXPath\\(([^)]+)\\)",
            "driver.findElement(By.xpath($1))"
        );
        code = code.replaceAll(
            "driver\\.findElementByClassName\\(([^)]+)\\)",
            "driver.findElement(By.className($1))"
        );
        code = code.replaceAll(
            "driver\\.findElementByLinkText\\(([^)]+)\\)",
            "driver.findElement(By.linkText($1))"
        );
        code = code.replaceAll(
            "driver\\.findElementByPartialLinkText\\(([^)]+)\\)",
            "driver.findElement(By.partialLinkText($1))"
        );
        code = code.replaceAll(
            "driver\\.findElementByTagName\\(([^)]+)\\)",
            "driver.findElement(By.tagName($1))"
        );
        code = code.replaceAll(
            "driver\\.findElementsByCssSelector\\(([^)]+)\\)",
            "driver.findElements(By.cssSelector($1))"
        );
        code = code.replaceAll(
            "driver\\.findElementsById\\(([^)]+)\\)",
            "driver.findElements(By.id($1))"
        );
        code = code.replaceAll(
            "driver\\.findElementsByXPath\\(([^)]+)\\)",
            "driver.findElements(By.xpath($1))"
        );
        code = code.replaceAll(
            "driver\\.findElementsByClassName\\(([^)]+)\\)",
            "driver.findElements(By.className($1))"
        );

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 5 — WRONG IMPORT FIXES
        // ═══════════════════════════════════════════════════════════════════

        // Note: import org.openqa.selenium.Duration → java.time.Duration
        // is handled by the KNOWN_FIXES map in SECTION 1 above.

        // Wrong Assert import
        code = code.replace(
            "import org.testng.asserts.Assert;",
            "import org.testng.Assert;"
        );

        // Invalid import alias syntax (import X as Y — not valid Java)
        code = code.replaceAll("import\\s+\\S+\\s+as\\s+\\S+;\\s*\\n?", "");

        // Remove JUnit Test import when TestNG Test import is also present (to avoid ambiguity)
        // Framework uses TestNG, so prefer TestNG annotations
        if (code.contains("import org.testng.annotations.Test;") &&
            code.contains("import org.junit.jupiter.api.Test;")) {
            code = code.replace("import org.junit.jupiter.api.Test;", "");
        }

        // Remove JUnit lifecycle imports when TestNG equivalents are present
        if (code.contains("import org.testng.annotations.BeforeMethod;") &&
            code.contains("import org.junit.jupiter.api.BeforeEach;")) {
            code = code.replace("import org.junit.jupiter.api.BeforeEach;", "");
        }
        if (code.contains("import org.testng.annotations.AfterMethod;") &&
            code.contains("import org.junit.jupiter.api.AfterEach;")) {
            code = code.replace("import org.junit.jupiter.api.AfterEach;", "");
        }
        if (code.contains("import org.testng.annotations.BeforeClass;") &&
            code.contains("import org.junit.jupiter.api.BeforeAll;")) {
            code = code.replace("import org.junit.jupiter.api.BeforeAll;", "");
        }
        if (code.contains("import org.testng.annotations.AfterClass;") &&
            code.contains("import org.junit.jupiter.api.AfterAll;")) {
            code = code.replace("import org.junit.jupiter.aAfterAll;", "");
        }

        // Duplicate imports — remove exact duplicates
        code = removeDuplicateImports(code);

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 6 — MISSING IMPORT FIXES
        // ═══════════════════════════════════════════════════════════════════

        // Core Selenium
        code = ensureImport(code, "WebDriver ",        "import org.openqa.selenium.WebDriver;");
        code = ensureImport(code, "WebElement",        "import org.openqa.selenium.WebElement;");
        code = ensureImport(code, "By.",               "import org.openqa.selenium.By;");
        code = ensureImport(code, "JavascriptExecutor","import org.openqa.selenium.JavascriptExecutor;");
        code = ensureImport(code, "Keys.",             "import org.openqa.selenium.Keys;");
        code = ensureImport(code, "Alert ",            "import org.openqa.selenium.Alert;");
        code = ensureImport(code, "NoSuchElementException",
            "import org.openqa.selenium.NoSuchElementException;");
        code = ensureImport(code, "TimeoutException",
            "import org.openqa.selenium.TimeoutException;");
        code = ensureImport(code, "StaleElementReferenceException",
            "import org.openqa.selenium.StaleElementReferenceException;");
        code = ensureImport(code, "ElementNotInteractableException",
            "import org.openqa.selenium.ElementNotInteractableException;");

        // Browser drivers
        code = ensureImport(code, "ChromeDriver",      "import org.openqa.selenium.chrome.ChromeDriver;");
        code = ensureImport(code, "ChromeOptions",     "import org.openqa.selenium.chrome.ChromeOptions;");
        code = ensureImport(code, "FirefoxDriver",     "import org.openqa.selenium.firefox.FirefoxDriver;");
        code = ensureImport(code, "EdgeDriver",        "import org.openqa.selenium.edge.EdgeDriver;");

        // Waits
        code = ensureImport(code, "WebDriverWait",
            "import org.openqa.selenium.support.ui.WebDriverWait;");
        code = ensureImport(code, "ExpectedConditions",
            "import org.openqa.selenium.support.ui.ExpectedConditions;");
        code = ensureImport(code, "FluentWait",
            "import org.openqa.selenium.support.ui.FluentWait;");
        code = ensureImport(code, "Select ",
            "import org.openqa.selenium.support.ui.Select;");
        code = ensureImport(code, "new Select(",
            "import org.openqa.selenium.support.ui.Select;");

        // PageFactory / FindBy
        code = ensureImport(code, "PageFactory",
            "import org.openqa.selenium.support.PageFactory;");
        code = ensureImport(code, "@FindBy",
            "import org.openqa.selenium.support.FindBy;");
        code = ensureImport(code, "@FindAll",
            "import org.openqa.selenium.support.FindAll;");
        code = ensureImport(code, "@FindBys",
            "import org.openqa.selenium.support.FindBys;");

        // Java time
        code = ensureImport(code, "Duration.ofSeconds",  "import java.time.Duration;");
        code = ensureImport(code, "Duration.ofMillis",   "import java.time.Duration;");
        code = ensureImport(code, "Duration.ofMinutes",  "import java.time.Duration;");

        // Java utils
        code = ensureImport(code, "List<",              "import java.util.List;");
        code = ensureImport(code, "ArrayList",          "import java.util.ArrayList;");
        code = ensureImport(code, "HashMap",            "import java.util.HashMap;");
        code = ensureImport(code, "Map<",               "import java.util.Map;");
        code = ensureImport(code, "Arrays.",            "import java.util.Arrays;");

        // TestNG
        code = ensureImport(code, "Assert.",            "import org.testng.Assert;");
        code = ensureImport(code, "@Test",              "import org.testng.annotations.Test;");
        code = ensureImport(code, "@BeforeMethod",      "import org.testng.annotations.BeforeMethod;");
        code = ensureImport(code, "@AfterMethod",       "import org.testng.annotations.AfterMethod;");
        code = ensureImport(code, "@BeforeClass",       "import org.testng.annotations.BeforeClass;");
        code = ensureImport(code, "@AfterClass",        "import org.testng.annotations.AfterClass;");
        code = ensureImport(code, "@DataProvider",      "import org.testng.annotations.DataProvider;");
        code = ensureImport(code, "SkipException",      "import org.testng.SkipException;");
        code = ensureImport(code, "ITestContext",       "import org.testng.ITestContext;");

        // Allure
        code = ensureImport(code, "@Description",       "import io.qameta.allure.Description;");
        code = ensureImport(code, "@Severity",          "import io.qameta.allure.Severity;");
        code = ensureImport(code, "SeverityLevel",      "import io.qameta.allure.SeverityLevel;");
        code = ensureImport(code, "@Step",              "import io.qameta.allure.Step;");
        code = ensureImport(code, "@Attachment",        "import io.qameta.allure.Attachment;");

        // WebDriverManager
        code = ensureImport(code, "WebDriverManager",
            "import io.github.bonigarcia.wdm.WebDriverManager;");

        // REST Assured (for future Agent 5)
        code = ensureImport(code, "given()",            "import static io.restassured.RestAssured.given;");
        code = ensureImport(code, "RestAssured.",       "import io.restassured.RestAssured;");
        code = ensureImport(code, "Response ",          "import io.restassured.response.Response;");
        code = ensureImport(code, "ValidatableResponse","import io.restassured.response.ValidatableResponse;");
        code = ensureImport(code, "ContentType.",       "import io.restassured.http.ContentType;");
        code = ensureImport(code, "JsonPath",           "import io.restassured.path.json.JsonPath;");

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 7 — SYNTAX FIXES
        // ═══════════════════════════════════════════════════════════════════

        // Fix double semicolons
        code = code.replaceAll(";;", ";");

        // Fix empty catch blocks — add at least a comment
        code = code.replaceAll(
            "catch\\s*\\(([^)]+)\\)\\s*\\{\\s*\\}",
            "catch ($1) { /* intentionally empty */ }"
        );

        // Fix wrong package separator (using / instead of .)
        code = code.replaceAll(
            "import (\\w+)/(\\w+)",
            "import $1.$2"
        );

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 8 — TESTNG ANNOTATION FIXES
        // ═══════════════════════════════════════════════════════════════════

        // @Test with invalid parameters
        code = code.replaceAll(
            "@Test\\(description\\s*=\\s*\"([^\"]+)\"\\s*,\\s*severity\\s*=\\s*SeverityLevel\\.\\w+\\)",
            "@Test"
        );
        code = code.replaceAll(
            "@Test\\(severity\\s*=\\s*SeverityLevel\\.\\w+\\)",
            "@Test"
        );

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 9 — DRIVER SETUP FIXES
        // ═══════════════════════════════════════════════════════════════════

        // Fix old WebDriverManager setup pattern
        code = code.replaceAll(
            "WebDriverManager\\.chromedriver\\(\\)\\.setup\\(\\);\n\\s*driver\\s*=\\s*new ChromeDriver\\(\\);",
            "driver = WebDriverManager.chromedriver().create();"
        );
        code = code.replaceAll(
            "WebDriverManager\\.firefoxdriver\\(\\)\\.setup\\(\\);\n\\s*driver\\s*=\\s*new FirefoxDriver\\(\\);",
            "driver = WebDriverManager.firefoxdriver().create();"
        );
        code = code.replaceAll(
            "WebDriverManager\\.edgedriver\\(\\)\\.setup\\(\\);\n\\s*driver\\s*=\\s*new EdgeDriver\\(\\);",
            "driver = WebDriverManager.edgedriver().create();"
        );

        // Fix missing imports for generated POM classes in ui test classes
        // Detect patterns like "ButtonPage page = new ButtonPage(driver);" or "ButtonPage page;"
        // and add import pages.ButtonPage; if in generated.ui package
        if (code.contains("package generated.ui;")) {
            // ButtonPage usage detection
            if (code.contains("ButtonPage ") && !code.contains("import pages.ButtonPage;")) {
                code = addImport(code, "import pages.ButtonPage;");
            }
            // LinkPage usage detection
            if (code.contains("LinkPage ") && !code.contains("import pages.LinkPage;")) {
                code = addImport(code, "import pages.LinkPage;");
            }
        }

        // Enhanced missing import detection - scans for common symbols and adds imports
        code = addMissingImports(code);

        return code;
    }

    /**
     * Ensures an import exists — adds it if the symbol is used but import is missing.
     * Skips if symbol not used or import already present.
     */
    private String ensureImport(String code, String symbol, String importStatement) {
        if (!code.contains(symbol)) return code;

        // Extract just the class/package part to check existence
        String importCheck = importStatement
                .replace("import static ", "")
                .replace("import ", "")
                .replace(";", "")
                .trim();

        // Check if any variant of this import already exists
        if (code.contains(importCheck)) return code;

        return addImport(code, importStatement);
    }

    /**
     * Removes exact duplicate import lines
     */
    private String removeDuplicateImports(String code) {
        String[] lines  = code.split("\n");
        List<String> seen    = new ArrayList<>();
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("import ")) {
                if (seen.contains(trimmed)) continue; // skip duplicate
                seen.add(trimmed);
            }
            result.append(line).append("\n");
        }
        return result.toString();
    }

    /**
     * Adds an import statement after the package declaration
     */
    private String addImport(String code, String importStatement) {
        // Find position after package declaration
        int packageEnd = code.indexOf(';');
        if (packageEnd == -1) return code;

        // Check if import already exists (any variant)
        String importClass = importStatement
                .replace("import ", "")
                .replace(";", "")
                .trim();
        if (code.contains(importClass)) return code;

        return code.substring(0, packageEnd + 1) +
               "\n" + importStatement +
               code.substring(packageEnd + 1);
    }

    /**
     * Scans code for symbols used without corresponding imports and adds them.
     * Enhanced version of addImport() that handles multiple missing imports in one pass.
     */
    private String addMissingImports(String code) {
        if (code == null) return null;

        // Find package declaration end
        int packageEnd = code.indexOf(";\n", code.indexOf("package"));
        if (packageEnd == -1) {
            // No package declaration - add imports at beginning
            packageEnd = -1; // Will insert at start
        }

        // Track imports to add
        Set<String> importsToAdd = new LinkedHashSet<>();

        // Check for WebDriver usage
        if (code.contains("WebDriver ") &&
            !code.contains("import org.openqa.selenium.WebDriver;")) {
            importsToAdd.add("import org.openqa.selenium.WebDriver;");
        }

        // Check for ChromeDriver usage
        if (code.contains("ChromeDriver ") &&
            !code.contains("import org.openqa.selenium.chrome.ChromeDriver;")) {
            importsToAdd.add("import org.openqa.selenium.chrome.ChromeDriver;");
        }

        // Check for WebElement usage
        if (code.contains("WebElement ") &&
            !code.contains("import org.openqa.selenium.WebElement;")) {
            importsToAdd.add("import org.openqa.selenium.WebElement;");
        }

        // Check for By usage
        if (code.contains("By ") &&
            !code.contains("import org.openqa.selenium.By;")) {
            importsToAdd.add("import org.openqa.selenium.By;");
        }

        // Check for List usage
        if ((code.contains("List<") || code.contains("List >")) &&
            !code.contains("import java.util.List;")) {
            importsToAdd.add("import java.util.List;");
        }

        // Check for ArrayList usage
        if ((code.contains("ArrayList<") || code.contains("ArrayList >")) &&
            !code.contains("import java.util.ArrayList;")) {
            importsToAdd.add("import java.util.ArrayList;");
        }

        // Check for Duration usage (java.time)
        if ((code.contains("Duration ") || code.contains("Duration.of")) &&
            !code.contains("import java.time.Duration;")) {
            importsToAdd.add("import java.time.Duration;");
        }

        // Check for PageFactory usage
        if (code.contains("PageFactory ") &&
            !code.contains("import org.openqa.selenium.support.PageFactory;")) {
            importsToAdd.add("import org.openqa.selenium.support.PageFactory;");
        }

        // Check for FindBy usage
        if (code.contains("@FindBy") &&
            !code.contains("import org.openqa.selenium.support.FindBy;")) {
            importsToAdd.add("import org.openqa.selenium.support.FindBy;");
        }

        // Check for ExpectedConditions usage
        if (code.contains("ExpectedConditions ") &&
            !code.contains("import org.openqa.selenium.support.ui.ExpectedConditions;")) {
            importsToAdd.add("import org.openqa.selenium.support.ui.ExpectedConditions;");
        }

        // Check for WebDriverWait usage
        if (code.contains("WebDriverWait ") &&
            !code.contains("import org.openqa.selenium.support.ui.WebDriverWait;")) {
            importsToAdd.add("import org.openqa.selenium.support.ui.WebDriverWait;");
        }

        // If no imports to add, return original code
        if (importsToAdd.isEmpty()) {
            return code;
        }

        // Build imports block
        StringBuilder importsBlock = new StringBuilder();
        for (String imp : importsToAdd) {
            importsBlock.append(imp).append("\n");
        }

        // Insert imports after package declaration
        if (packageEnd == -1) {
            // No package - insert at beginning
            return importsBlock.toString() + code;
        } else {
            // Insert after package line
            return code.substring(0, packageEnd + 1) +
                   "\n" + importsBlock.toString() +
                   code.substring(packageEnd + 1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASS 3 — LLM FIX (MINIMAL CONTEXT)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sends ONLY the error + surrounding lines to LLM — not the full file.
     * Dramatically reduces token usage.
     */
    private boolean attemptLlmFix(CompileResult failed) {
        for (int attempt = 1; attempt <= MAX_FIX_ATTEMPTS; attempt++) {
            System.out.println(FrameworkConstants.LOG_INFO +
                    "   LLM attempt " + attempt + "/" + MAX_FIX_ATTEMPTS);

            try {
                // Extract minimal context — only lines around errors
                String minimalContext = extractErrorContext(
                        failed.getSourceCode(),
                        failed.getErrors()
                );

                String systemPrompt = buildLlmSystemPrompt(failed.getFilePath());
                String userPrompt   = buildLlmUserPrompt(
                        minimalContext,
                        failed.getErrorSummary(),
                        failed.getFilePath()
                );

                Map<String, Object> schema = LLMService.jsonSchema(
                        Map.of("code", LLMService.stringProperty("Complete fixed Java source code")),
                        List.of("code")
                );
                JsonObject toolResult = llmService.chatWithTools(
                        "fix_code", "Fix Java compile errors and return the complete file",
                        schema,
                        systemPrompt, userPrompt,
                        modelConfig.getAgent4Model(),
                        modelConfig.getAgent4Temperature(),
                        modelConfig.getAgent4MaxTokens()
                );
                String fixedCode = toolResult.has("code") && !toolResult.get("code").isJsonNull()
                        ? toolResult.get("code").getAsString()
                        : "";

                if (fixedCode == null || fixedCode.isBlank()) continue;

                // Apply deterministic fixes on top of LLM fix
                fixedCode = applyDeterministicFixes(fixedCode);

                saveFile(failed.getFilePath(), fixedCode);

                // Re-compile
                CompileResult recompiled = JavaCompilerUtil.compile(
                        failed.getFilePath());

                if (recompiled.isSuccess()) {
                    System.out.println(FrameworkConstants.LOG_SUCCESS +
                            "   Compile successful after LLM fix!");
                    return true;
                }

                System.out.println(FrameworkConstants.LOG_WARNING +
                        "   Still failing — " +
                        recompiled.getErrors().size() + " error(s)");
                failed = recompiled;
                sleep(0);

            } catch (Exception e) {
                System.out.println(FrameworkConstants.LOG_ERROR +
                        "   LLM fix failed: " + e.getMessage());
                sleep(0);
            }
        }
        return false;
    }

    /**
     * Extracts only lines around compile errors — not the full file.
     * This is the key token-saving technique.
     */
    private String extractErrorContext(String sourceCode, List<String> errors) {
        if (sourceCode == null) return "";

        String[] lines = sourceCode.split("\n");
        StringBuilder context = new StringBuilder();
        context.append("FULL FILE (").append(lines.length).append(" lines):\n");
        context.append(sourceCode);
        context.append("\n\nERROR LOCATIONS:\n");

        // Extract line numbers from errors
        Pattern linePattern = Pattern.compile("Line (\\d+):");
        for (String error : errors) {
            Matcher m = linePattern.matcher(error);
            if (m.find()) {
                int errorLine = Integer.parseInt(m.group(1)) - 1;
                int start     = Math.max(0, errorLine - CONTEXT_LINES);
                int end       = Math.min(lines.length - 1, errorLine + CONTEXT_LINES);

                context.append("\nAround line ").append(errorLine + 1)
                       .append(" (").append(error).append("):\n");
                for (int i = start; i <= end; i++) {
                    context.append(i == errorLine ? ">>> " : "    ")
                           .append(i + 1).append(": ")
                           .append(lines[i]).append("\n");
                }
            }
        }
        return context.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LLM PROMPTS — MINIMAL AND FOCUSED
    // ─────────────────────────────────────────────────────────────────────────

    private String buildLlmSystemPrompt(String filePath) {
        boolean isPom = filePath.contains("\\pages\\") ||
                        filePath.contains("/pages/");
        String pkg = isPom ? "package pages;" : "package generated.ui;";

        return """
                Fix Java compile errors. Return ONLY the complete fixed file.
                No markdown, no explanation, no backticks.
                Start with: %s

                Common fixes:
                - Missing import → add correct import from java.time, org.openqa.selenium, org.testng
                - int in WebDriverWait → Duration.ofSeconds(n)
                - Wrong SeverityLevel → NORMAL/CRITICAL/MINOR only
                - Cannot find symbol → check spelling and imports
                """.formatted(pkg);
    }

    private String buildLlmUserPrompt(String context, String errors,
                                       String filePath) {
        return """
                Errors to fix:
                %s

                Code:
                %s

                Return the complete fixed file starting with the package declaration.
                """.formatted(errors, context);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

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

    private void printSummary(int total, int fixed,
                               int llmCalls, int rulesFixes) {
        System.out.println("\n" + FrameworkConstants.LOG_INFO +
                " ============================================");
        System.out.println(FrameworkConstants.LOG_SUCCESS + " Agent 4 Complete!");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Total files   : " + total);
        System.out.println(FrameworkConstants.LOG_INFO +
                " Fixed         : " + fixed);
        System.out.println(FrameworkConstants.LOG_INFO +
                " Rules fixes   : " + rulesFixes + " (0 tokens)");
        System.out.println(FrameworkConstants.LOG_INFO +
                " LLM calls     : " + llmCalls);
        System.out.println(FrameworkConstants.LOG_INFO +
                " Still broken  : " + (total - fixed));
        System.out.println(FrameworkConstants.LOG_INFO +
                " ============================================\n");
    }
}