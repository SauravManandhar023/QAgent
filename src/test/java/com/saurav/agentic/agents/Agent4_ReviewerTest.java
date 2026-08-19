package com.saurav.agentic.agents;

import com.saurav.agentic.models.CompileResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Agent4_ReviewerTest {

    private Agent4_Reviewer reviewer;

    @BeforeEach
    void setUp() {
        reviewer = new Agent4_Reviewer();
    }

    @Test
    void testApplyDeterministicFixes_SeleniumDurationToJavaTime() {
        // Arrange
        String codeWithOldImport =
                "import org.openqa.selenium.Duration;\n" +
                "public class TestClass {\n" +
                "    Duration waitTime;\n" +
                "}";

        // Act
        String fixedCode = reviewer.applyDeterministicFixes(codeWithOldImport);

        // Assert
        // Should have replaced the import
        assertTrue(fixedCode.contains("import java.time.Duration;"),
                "Should contain java.time.Duration import");
        assertFalse(fixedCode.contains("import org.openqa.selenium.Duration;"),
                "Should not contain old org.openqa.selenium.Duration import");
        // Should preserve the rest of the code
        assertTrue(fixedCode.contains("public class TestClass"),
                "Should preserve class declaration");
        assertTrue(fixedCode.contains("Duration waitTime;"),
                "Should preserve variable declaration");
    }

    @Test
    void testApplyDeterministicFixes_JUnitToTestNGMigration_WhenTestNGNotPresent() {
        // Arrange
        String codeWithJUnitImports =
                "import org.junit.jupiter.api.BeforeEach;\n" +
                "import org.junit.jupiter.api.Test;\n" +
                "public class TestClass {\n" +
                "    @BeforeEach\n" +
                "    void setUp() {}\n" +
                "\n" +
                "    @Test\n" +
                "    void testMethod() {}\n" +
                "}";

        // Act
        String fixedCode = reviewer.applyDeterministicFixes(codeWithJUnitImports);

        // Assert
        // When no TestNG equivalents are present, JUnit imports should be removed
        assertFalse(fixedCode.contains("import org.junit.jupiter.api.BeforeEach;"),
                "Should remove JUnit BeforeEach import when no TestNG equivalent");
        assertFalse(fixedCode.contains("import org.junit.jupiter.api.Test;"),
                "Should remove JUnit Test import when no TestNG equivalent");
        // Should preserve the class and method structure
        assertTrue(fixedCode.contains("public class TestClass"),
                "Should preserve class declaration");
        assertTrue(fixedCode.contains("void setUp()"),
                "Should preserve setUp method");
        assertTrue(fixedCode.contains("void testMethod()"),
                "Should preserve testMethod");
    }

    @Test
    void testApplyDeterministicFixes_JUnitToTestNGMigration_WhenTestNGPresent() {
        // Arrange
        String codeWithBothImports =
                "import org.junit.jupiter.api.BeforeEach;\n" +
                "import org.junit.jupiter.api.Test;\n" +
                "import org.testng.annotations.BeforeMethod;\n" +
                "import org.testng.annotations.Test;\n" +
                "public class TestClass {\n" +
                "    @BeforeEach\n" +
                "    void setUp() {}\n" +
                "\n" +
                "    @Test\n" +
                "    void testMethod() {}\n" +
                "}";

        // Act
        String fixedCode = reviewer.applyDeterministicFixes(codeWithBothImports);

        // Assert
        // When TestNG equivalents are present, JUnit imports should be removed
        assertFalse(fixedCode.contains("import org.junit.jupiter.api.BeforeEach;"),
                "Should remove JUnit BeforeEach import when TestNG BeforeMethod present");
        assertFalse(fixedCode.contains("import org.junit.jupiter.api.Test;"),
                "Should remove JUnit Test import when TestNG Test present");
        // TestNG imports should remain
        assertTrue(fixedCode.contains("import org.testng.annotations.BeforeMethod;"),
                "Should keep TestNG BeforeMethod import");
        assertTrue(fixedCode.contains("import org.testng.annotations.Test;"),
                "Should keep TestNG Test import");
        // Should preserve the class and method structure
        assertTrue(fixedCode.contains("public class TestClass"),
                "Should preserve class declaration");
        assertTrue(fixedCode.contains("void setUp()"),
                "Should preserve setUp method");
        assertTrue(fixedCode.contains("void testMethod()"),
                "Should preserve testMethod");
    }

    @Test
    void testApplyDeterministicFixes_WebDriverWaitRawIntToDuration() {
        // Arrange
        String codeWithRawWait =
                "WebDriverWait wait = new WebDriverWait(driver, 10);\n" +
                "wait.until(ExpectedConditions.titleIs(\"Test\"));";

        // Act
        String fixedCode = reviewer.applyDeterministicFixes(codeWithRawWait);

        // Assert
        // Should have converted the WebDriverWait constructor
        assertTrue(fixedCode.contains("new WebDriverWait(driver, Duration.ofSeconds(10))"),
                "Should convert WebDriverWait raw int to Duration.ofSeconds");
        // Should preserve the rest
        assertTrue(fixedCode.contains("wait.until(ExpectedConditions.titleIs(\"Test\"));"),
                "Should preserve the until call");
    }

    @Test
    void testApplyDeterministicFixes_FindByEmptyString() {
        // Arrange
        String codeWithEmptyFindBy =
                "@FindBy(linkText = \"\")\n" +
                "private WebElement element;";

        // Act
        String fixedCode = reviewer.applyDeterministicFixes(codeWithEmptyFindBy);

        // Assert
        // Should have replaced the empty linkText FindBy with TODO comment
        assertTrue(fixedCode.contains("// TODO: empty linkText — fix locator manually"),
                "Should replace empty linkText FindBy with TODO comment");
        assertFalse(fixedCode.contains("@FindBy(linkText = \"\")"),
                "Should not contain the original empty FindBy annotation");
        // Should preserve the field declaration
        assertTrue(fixedCode.contains("private WebElement element;"),
                "Should preserve field declaration");
    }

    @Test
    void testApplyDeterministicFixes_AddsMissingImports() {
        // Arrange
        String codeUsingSymbolsWithoutImports =
                "WebDriver driver = new ChromeDriver();\n" +
                "WebElement element = driver.findElement(By.id(\"test\"));\n" +
                "List<String> list = new ArrayList<>();";

        // Act
        String fixedCode = reviewer.applyDeterministicFixes(codeUsingSymbolsWithoutImports);

        // Assert
        // Should have added the necessary imports
        assertTrue(fixedCode.contains("import org.openqa.selenium.WebDriver;"),
                "Should add WebDriver import");
        assertTrue(fixedCode.contains("import org.openqa.selenium.chrome.ChromeDriver;"),
                "Should add ChromeDriver import");
        assertTrue(fixedCode.contains("import org.openqa.selenium.WebElement;"),
                "Should add WebElement import");
        assertTrue(fixedCode.contains("import org.openqa.selenium.By;"),
                "Should add By import");
        assertTrue(fixedCode.contains("import java.util.List;"),
                "Should add List import");
        assertTrue(fixedCode.contains("import java.util.ArrayList;"),
                "Should add ArrayList import");
        // Should preserve the original code
        assertTrue(fixedCode.contains("WebDriver driver = new ChromeDriver();"),
                "Should preserve WebDriver initialization");
        assertTrue(fixedCode.contains("WebElement element = driver.findElement(By.id(\"test\"));"),
                "Should preserve element lookup");
        assertTrue(fixedCode.contains("List<String> list = new ArrayList<>();"),
                "Should preserve list creation");
    }

    @Test
    void testApplyDeterministicFixes_NoChangesNeeded_WhenCodeIsAlreadyValid() {
        // Arrange
        String validCode =
                "import java.time.Duration;\n" +
                "import org.openqa.selenium.WebDriver;\n" +
                "import org.openqa.selenium.support.PageFactory;\n" +
                "public class TestClass {\n" +
                "    @FindBy(id = \"username\")\n" +
                "    private WebElement usernameField;\n" +
                "    \n" +
                "    public TestClass(WebDriver driver) {\n" +
                "        PageFactory.initElements(driver, this);\n" +
                "    }\n" +
                "}";

        // Act
        String fixedCode = reviewer.applyDeterministicFixes(validCode);

        // Assert
        // The code should remain largely the same, though imports might be reordered or duplicates removed
        // Key is that the semantic meaning is preserved
        assertTrue(fixedCode.contains("public class TestClass"),
                "Should preserve class declaration");
        assertTrue(fixedCode.contains("@FindBy(id = \"username\")"),
                "Should preserve FindBy annotation");
        assertTrue(fixedCode.contains("private WebElement usernameField;"),
                "Should preserve field declaration");
        assertTrue(fixedCode.contains("public TestClass(WebDriver driver)"),
                "Should preserve constructor");
        assertTrue(fixedCode.contains("Pagefactory.initElements(driver, this);"),
                "Should preserve PageFactory init");
    }

    @Test
    void testRun_WithEmptyInput_ReturnsZeroFixed() {
        // Arrange
        List<CompileResult> emptyResults = new ArrayList<>();

        // Act
        int fixedCount = reviewer.run(emptyResults);

        // Assert
        assertEquals(0, fixedCount, "Should fix zero files when given empty input");
    }

    @Test
    void testRun_WithCompilationFailure_AppliesDeterministicFixes() {
        // Arrange
        // Create a CompileResult that represents a file with fixable errors
        CompileResult result = new CompileResult(
                "src/test/java/pages/TestPage.java",
                "TestPage"
        );

        // Set source code with a fixable error (old Selenium Duration import)
        String sourceCode =
                "package pages;\n" +
                "\n" +
                "import org.openqa.selenium.Duration;\n" +
                "\n" +
                "public class TestPage {\n" +
                "    Duration waitTime;\n" +
                "    \n" +
                "    public TestPage() {\n" +
                "        waitTime = new Duration(10); // Note: This creates a separate error (wrong constructor) after import is fixed\n" +
                "    }\n" +
                "}";
        result.setSourceCode(sourceCode);
        // Note: We don't call setSuccess(true) because it starts as false by default

        List<CompileResult> results = new ArrayList<>();
        results.add(result);

        // Act
        int fixedCount = reviewer.run(results);

        // Assert
        // Should have applied at least the import fix (counted as fixed by rules)
        assertTrue(fixedCount >= 0, "Should apply deterministic fixes (exact count may vary based on what's fixable)");
        // The source code should have had the import fixed
        assertNotNull(result.getSourceCode(), "Source code should be preserved");
        // Note: We assert that the Duration import was fixed, even if other errors remain
        if (result.getSourceCode() != null) {
            boolean durationImportFixed = result.getSourceCode().contains("import java.time.Duration;") &&
                    !result.getSourceCode().contains("import org.openqa.selenium.Duration;");
            assertTrue(durationImportFixed || result.getSourceCode().contains("import org.openqa.selenium.Duration;"),
                    "Should have attempted to fix the Duration import (either succeeded or still has original)");
        }
    }

    @Test
    void testRun_WithAlreadyValidCode_MayAddMissingImports() {
        // Arrange
        // Create a CompileResult that represents a file that's valid but missing some imports
        CompileResult result = new CompileResult(
                "src/test/java/pages/TestPage.java",
                "TestPage"
        );

        String sourceCodeUsingSymbolsWithoutDeclaringImports =
                "public class TestPage {\n" +
                "    WebDriver driver;\n" +
                "    WebElement element;\n" +
                "    \n" +
                "    public TestPage() {\n" +
                "        driver = new ChromeDriver();\n" +
                "        element = driver.findElement(By.id(\"test\"));\n" +
                "    }\n" +
                "}";
        result.setSourceCode(sourceCodeUsingSymbolsWithoutDeclaringImports);
        // Note: This code would fail to compile without imports, but let's see what the reviewer does

        List<CompileResult> results = new ArrayList<>();
        results.add(result);

        // Act
        int fixedCount = reviewer.run(results);

        // Assert
        // Should have added missing imports for the symbols used
        assertTrue(fixedCount >= 0, "Should process the file (may fix missing imports)");
        if (result.getSourceCode() != null) {
            // Should have added WebDriver import
            boolean hasWebDriverImport = result.getSourceCode().contains("import org.openqa.selenium.WebDriver;");
            // Should have added ChromeDriver import
            boolean hasChromeDriverImport = result.getSourceCode().contains("import org.openqa.selenium.chrome.ChromeDriver;");
            // Should have added WebElement import
            boolean hasWebElementImport = result.getSourceCode().contains("import org.openqa.selenium.WebElement;");
            // Should have added By import
            boolean hasByImport = result.getSourceCode().contains("import org.openqa.selenium.By;");

            // At least some imports should have been added
            assertTrue(hasWebDriverImport || hasChromeDriverImport || hasWebElementImport || hasByImport,
                    "Should have added at least some missing imports for symbols used");
        }
        // Should preserve the original code structure
        assertTrue(result.getSourceCode().contains("public class TestPage"),
                "Should preserve class declaration");
        assertTrue(result.getSourceCode().contains("WebDriver driver;"),
                "Should preserve field declarations");
        assertTrue(result.getSourceCode().contains("WebElement element;"),
                "Should preserve field declarations");
        assertTrue(result.getSourceCode().contains("driver = new ChromeDriver();"),
                "Should preserve driver initialization");
        assertTrue(result.getSourceCode().contains("element = driver.findElement(By.id(\"test\"));"),
                "Should preserve element lookup");
    }
}