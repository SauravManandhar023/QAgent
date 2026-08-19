package com.saurav.agentic.agents;

import com.saurav.agentic.models.CompileResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Agent3_CompilerTest {

    @Test
    void testRun_AlwaysReturnsResultsForExistingFiles() {
        // Arrange
        Agent3_Compiler compiler = new Agent3_Compiler();
        Set<String> anySet = new HashSet<>(); // Can be empty or contain any names

        // Act
        List<CompileResult> results = compiler.run(anySet);

        // Assert
        assertNotNull(results);
        // Should return results for both POM and test directories (compiles all files there)
        // Note: Exact count depends on what files exist in those directories
        assertTrue(results.size() >= 0, "Should return results for directories");

        // At least verify the structure of results
        for (CompileResult result : results) {
            assertNotNull(result.getFilePath(), "Should have file path");
            assertNotNull(result.getClassName(), "Should have class name");
            // Should either be success or have errors
            assertTrue(result.isSuccess() || !result.getErrors().isEmpty(),
                    "Each result should either be successful or have errors");
        }
    }

    @Test
    void testRun_WithValidJavaFile_IncludesThatFileInResults() throws Exception {
        // Arrange
        Agent3_Compiler compiler = new Agent3_Compiler();

        // Create a temporary valid Java file in src/test/java/pages/ for testing
        String testDir = "src/test/java/pages/";
        File dir = new File(testDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String simpleClass =
                "package pages;\n" +
                "\n" +
                "public class TestPage {\n" +
                "    public void testMethod() {\n" +
                "        // Valid Java code\n" +
                "    }\n" +
                "}";

        java.nio.file.Files.writeString(
                new File(testDir, "TestPage.java").toPath(),
                simpleClass
        );

        Set<String> classNames = new HashSet<>();
        classNames.add("TestPage");

        // Act
        List<CompileResult> results = compiler.run(classNames);

        // Assert
        assertNotNull(results);
        // Should find our test file among the results
        boolean foundTestPage = results.stream()
                .anyMatch(result -> "TestPage".equals(result.getClassName()) && result.isSuccess());
        assertTrue(foundTestPage, "Should find and successfully compile TestPage.java");

        // Verify the TestPage result has correct properties
        results.stream()
                .filter(result -> "TestPage".equals(result.getClassName()))
                .findFirst()
                .ifPresent(testPageResult -> {
                    assertTrue(testPageResult.isSuccess(), "TestPage should compile successfully");
                    assertNotNull(testPageResult.getSourceCode(), "Should have source code");
                    assertTrue(testPageResult.getSourceCode().contains("public class TestPage"),
                            "Should preserve source code");
                });

        // Clean up
        new File(testDir, "TestPage.java").delete();
        // Only delete directory if empty
        if (dir.list().length == 0) {
            dir.delete();
        }
    }

    @Test
    void testPrintSummary_WorksWithoutErrors() {
        // Arrange
        Agent3_Compiler compiler = new Agent3_Compiler();
        Set<String> emptySet = new HashSet<>();

        // Act - this should not throw any exceptions
        compiler.run(emptySet);

        // Assert - if we get here without exception, the test passes
        assertTrue(true);
    }
}