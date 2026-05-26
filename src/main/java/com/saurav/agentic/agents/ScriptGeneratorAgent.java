package com.saurav.agentic.agents;

import com.saurav.agentic.config.ModelConfig;
import com.saurav.agentic.prompts.composers.PomPromptComposer;
import com.saurav.agentic.prompts.composers.ScriptPromptComposer;

import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.TestCase;
import com.saurav.agentic.utils.ExcelUtil;
import com.saurav.agentic.utils.GroqClient;
import com.saurav.agentic.utils.PromptBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ScriptGeneratorAgent - Agent 2
 *
 * Reads automation-feasible test cases from Excel Groups them by component
 * Calls Groq AI per component to generate: 1. A Page Object Model (POM) class →
 * src/test/java/pages/ 2. A TestNG test class → src/test/java/generated/ui/
 */
public class ScriptGeneratorAgent {

	private final GroqClient groqClient;
	private final ModelConfig modelConfig;

	private static final String PAGES_OUTPUT_DIR = "src/test/java/pages/";
	private static final String TESTS_OUTPUT_DIR = "src/test/java/generated/ui/";

	public ScriptGeneratorAgent() {
		this.groqClient = new GroqClient();
		this.modelConfig  = ModelConfig.getInstance();
	}

	/**
	 * Main entry point for Agent 2
	 */
	public void run(String excelPath, String pageUrl, String pageAnalysis) {

		System.out.println("\n" + FrameworkConstants.LOG_INFO + " ============================================");
		System.out.println(FrameworkConstants.LOG_INFO + " Agent 2: Script Generator Started");
		System.out.println(FrameworkConstants.LOG_INFO + " ============================================");

		try {
			// ── Step 1: Read test cases from Excel ──────────────────────────
			System.out.println(FrameworkConstants.LOG_INFO + " Reading test cases from: " + excelPath);

			List<TestCase> allTestCases = ExcelUtil.readTestCases(excelPath);

			// ── Step 2: Filter only automation-feasible test cases ───────────
			List<TestCase> feasibleCases = allTestCases.stream().filter(TestCase::isAutomationFeasible).toList();

			System.out.println(FrameworkConstants.LOG_INFO + " Total test cases     : " + allTestCases.size());
			System.out.println(FrameworkConstants.LOG_INFO + " Automation feasible  : " + feasibleCases.size());

			if (feasibleCases.isEmpty()) {
				System.out.println(
						FrameworkConstants.LOG_WARNING + " No automation-feasible test cases found. Exiting Agent 2.");
				return;
			}

			// ── Step 3: Group by component ───────────────────────────────────
			Map<String, List<TestCase>> byComponent = groupByComponent(feasibleCases);

			System.out.println(FrameworkConstants.LOG_INFO + " Components found     : " + byComponent.size());

			// ── Step 4: Create output directories ───────────────────────────
			createDirectory(PAGES_OUTPUT_DIR);
			createDirectory(TESTS_OUTPUT_DIR);

			// ── Step 5: For each component → generate POM + Test class ──────
			int componentNumber = 1;
			int totalGenerated = 0;

			for (Map.Entry<String, List<TestCase>> entry : byComponent.entrySet()) {
				String component = entry.getKey();
				List<TestCase> cases = entry.getValue();

				System.out.println("\n" + FrameworkConstants.LOG_INFO + " Processing component " + componentNumber + "/"
						+ byComponent.size() + ": " + component + " (" + cases.size() + " test cases)");

				String className = PromptBuilder.toPascalCase(component);

				// Generate POM class — returns the generated code
				String pomCode = generatePomClass(component, className, pageUrl, pageAnalysis);

				// Brief pause between POM and Test generation
				System.out.println(FrameworkConstants.LOG_INFO + "   Waiting 10s between POM and Test generation...");
				sleep(10000);

				// Generate Test class — pass pomCode so AI knows exact methods
				boolean testSuccess = false;
				if (pomCode != null) {
					testSuccess = generateTestClass(component, className, pageUrl, cases, pomCode);
				} else {
					System.out.println(
							FrameworkConstants.LOG_WARNING + "   Skipping test generation — POM generation failed.");
				}

				// Count success only if both succeeded
				if (pomCode != null && testSuccess) {
					totalGenerated++;
					System.out.println(FrameworkConstants.LOG_SUCCESS + " Generated: " + className + "Page.java + "
							+ className + "Test.java");
				}

				// Pause between components
				if (componentNumber < byComponent.size()) {
					System.out.println(FrameworkConstants.LOG_INFO + "   Waiting 15s before next component...");
					sleep(30000);
				}

				componentNumber++;
			}

			// ── Step 6: Print summary ────────────────────────────────────────
			printSummary(byComponent, totalGenerated);

		} catch (IOException e) {
			System.out.println(FrameworkConstants.LOG_ERROR + " Agent 2 failed: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// ─────────────────────────────────────────────────────────────────────────
	// POM CLASS GENERATION
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * Calls Groq AI to generate a POM class. Returns the generated Java code
	 * string, or null if failed.
	 */
	private String generatePomClass(String component, String className, String pageUrl, String pageAnalysis) {
		String systemPrompt = PomPromptComposer.systemPrompt();
		String userPrompt   = PomPromptComposer.userPrompt(
		        component, className + "Page", pageUrl, pageAnalysis
		);
		String filePath = PAGES_OUTPUT_DIR + className + "Page.java";

		try {
			System.out.println(FrameworkConstants.LOG_INFO + "   Generating POM class: " + className + "Page.java...");

			String javaCode = groqClient.chat(
				    systemPrompt, userPrompt,
				    modelConfig.getAgent2Model(),
				    modelConfig.getAgent2Temperature(),
				    modelConfig.getAgent2MaxTokens()
				);
			javaCode = cleanJavaCode(javaCode);
			saveFile(filePath, javaCode);

			System.out.println(FrameworkConstants.LOG_SUCCESS + "   Saved: " + filePath);
			return javaCode;

		} catch (IOException e) {
			System.out.println(FrameworkConstants.LOG_WARNING + "   Rate limited, waiting 30s and retrying...");
			try {
				Thread.sleep(60000);
				String javaCode = groqClient.chat(
					    systemPrompt, userPrompt,
					    modelConfig.getAgent2Model(),
					    modelConfig.getAgent2Temperature(),
					    modelConfig.getAgent2MaxTokens()
					);
				javaCode = cleanJavaCode(javaCode);
				saveFile(filePath, javaCode);
				System.out.println(FrameworkConstants.LOG_SUCCESS + "   Retry successful: " + filePath);
				return javaCode;
			} catch (Exception retryEx) {
				System.out.println(FrameworkConstants.LOG_ERROR + "   Retry also failed for: " + component + " — "
						+ retryEx.getMessage());
				return null;
			}
		}
	}

	// ─────────────────────────────────────────────────────────────────────────
	// TEST CLASS GENERATION
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * Calls Groq AI to generate a TestNG test class. Receives pomCode so AI only
	 * calls methods that actually exist.
	 */
	private boolean generateTestClass(String component, String className, String pageUrl, List<TestCase> cases,
			String pomCode) {
		String testCasesText = formatTestCasesForPrompt(cases);
		String systemPrompt  = ScriptPromptComposer.systemPrompt();
		String userPrompt    = ScriptPromptComposer.userPrompt(
		        component, className + "Test", pageUrl,
		        testCasesText, pomCode,
		        "Login page — successful login redirects to /secure, failed login shows error message in element id=flash"
		);
		String filePath = TESTS_OUTPUT_DIR + className + "Test.java";

		try {
			System.out.println(FrameworkConstants.LOG_INFO + "   Generating test class: " + className + "Test.java...");

			String javaCode = groqClient.chat(
				    systemPrompt, userPrompt,
				    modelConfig.getAgent2Model(),
				    modelConfig.getAgent2Temperature(),
				    modelConfig.getAgent2MaxTokens()
				);
			javaCode = cleanJavaCode(javaCode);
			saveFile(filePath, javaCode);

			System.out.println(FrameworkConstants.LOG_SUCCESS + "   Saved: " + filePath);
			return true;

		} catch (IOException e) {
			System.out.println(FrameworkConstants.LOG_WARNING + "   Rate limited, waiting 30s and retrying...");
			try {
				Thread.sleep(60000);
				String javaCode = groqClient.chat(
					    systemPrompt, userPrompt,
					    modelConfig.getAgent2Model(),
					    modelConfig.getAgent2Temperature(),
					    modelConfig.getAgent2MaxTokens()
					);
				javaCode = cleanJavaCode(javaCode);
				saveFile(filePath, javaCode);
				System.out.println(FrameworkConstants.LOG_SUCCESS + "   Retry successful: " + filePath);
				return true;
			} catch (Exception retryEx) {
				System.out.println(FrameworkConstants.LOG_ERROR + "   Retry also failed for: " + component + " — "
						+ retryEx.getMessage());
				return false;
			}
		}
	}

	// ─────────────────────────────────────────────────────────────────────────
	// HELPER METHODS
	// ─────────────────────────────────────────────────────────────────────────

	private Map<String, List<TestCase>> groupByComponent(List<TestCase> testCases) {
		Map<String, List<TestCase>> grouped = new LinkedHashMap<>();
		for (TestCase tc : testCases) {
			String component = tc.getComponent();
			if (component == null || component.isBlank()) {
				component = "General";
			}
			grouped.computeIfAbsent(component, k -> new ArrayList<>()).add(tc);
		}
		return grouped;
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

	private String cleanJavaCode(String rawCode) {
		if (rawCode == null)
			return "";
		rawCode = rawCode.replaceAll("```java\\s*", "").replaceAll("```\\s*", "");
		return rawCode.trim();
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
			System.out.println(FrameworkConstants.LOG_WARNING + " Could not create directory: " + path);
		}
	}

	private void printSummary(Map<String, List<TestCase>> byComponent, int totalGenerated) {
		System.out.println("\n" + FrameworkConstants.LOG_INFO + " ============================================");
		System.out.println(FrameworkConstants.LOG_SUCCESS + " Agent 2 Complete!");
		System.out.println(FrameworkConstants.LOG_INFO + " Components processed : " + byComponent.size());
		System.out.println(FrameworkConstants.LOG_INFO + " File pairs generated : " + totalGenerated
				+ " (POM + Test per component)");
		System.out.println(FrameworkConstants.LOG_INFO + " POM classes saved to : " + PAGES_OUTPUT_DIR);
		System.out.println(FrameworkConstants.LOG_INFO + " Test classes saved to: " + TESTS_OUTPUT_DIR);
		System.out.println(FrameworkConstants.LOG_INFO + " ============================================\n");
	}

	private void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}