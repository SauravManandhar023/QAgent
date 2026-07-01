package com.saurav.agentic.prompts.composers;

import com.saurav.agentic.prompts.modules.AssertionRules;
import com.saurav.agentic.prompts.modules.CoreRules;
import com.saurav.agentic.prompts.modules.ProjectConfig;
import com.saurav.agentic.prompts.modules.RecoveryRules;
import com.saurav.agentic.prompts.modules.SeleniumRules;

/**
 * ScriptPromptComposer - Composes prompts for TestNG test class generation
 * Uses only relevant modules — no unnecessary rules
 */
public class ScriptPromptComposer {

    private ScriptPromptComposer() {}

    public static String systemPrompt() {
        return """
                You are a senior Selenium automation engineer.
                Generate a complete TestNG test class for the given component.
                Respond with PURE Java code only — no explanation, no markdown, no backticks.
                Start your response directly with: package generated.ui;

                ASSERTION QUALITY RULES:
				1. Default selected option — ALWAYS read from page metadata, never assume.
				   The metadata shows which option has [SELECTED] tag — use that as the expected value.
				   WRONG: Assert.assertTrue(option.contains("Option 1"))
				   CORRECT: Assert.assertEquals(select.getFirstSelectedOption().getText(), "Please select an option")

				2. Dropdown value validation — ALWAYS use Select class, NEVER use element.getText()
				   element.getText() on a <select> returns ALL option text concatenated — completely wrong.
				   WRONG: String selected = page.getDropdown().getText()
				   CORRECT: Select select = new Select(page.getDropdown());
				   			String selected = select.getFirstSelectedOption().getText();

				3. Assertion strength — use assertEquals not assertTrue/contains for exact values
				   WRONG: Assert.assertTrue(selectedOption.contains("Option 2"))
				   CORRECT: Assert.assertEquals(select.getFirstSelectedOption().getText(), "Option 2")

				4. Redundant waits — only wait ONCE before the action, never after
				   WRONG: wait before + wait after every action
				   CORRECT: wait.until(ExpectedConditions.visibilityOf(element)); // once before action

				5. Meaningful tests only — do NOT generate tests that repeat the same action twice
				   unless the application has specific re-selection logic
				   WRONG: selectOption("Option 1"); selectOption("Option 1");
				   CORRECT: selectOption("Option 1"); verify; selectOption("Option 2"); verify change

				6. Base ALL assertions on actual DOM state from page metadata
				   The metadata section contains actual option values, initial states, and element attributes.
				   Use these EXACTLY — do not assume or invent values.

				7. AD OVERLAY HANDLING:
				- If URL contains "#google_vignette" after a click, an ad intercepted the click
				- Handle by waiting for the overlay to disappear before clicking:
				  wait.until(ExpectedConditions.not(
				      ExpectedConditions.urlContains("google_vignette")
				  ));
				  // then retry the click
				- Or dismiss using JavaScript:
				  js.executeScript("document.querySelector('.modal, .overlay, [id*=vignette]')?.remove()");

				8. AD OVERLAY RULE:
					- On sites with ads, before clicking any navigation link call dismissAdOverlay()
					- After clicking a link, use urlContains("keyword") not exact URL match
					- If URL contains "#google_vignette" the ad intercepted — dismiss and retry
		        9. LOCATOR RULES FOR DYNAMIC CONTENT:
						- Brand/category links with product counts use partialLinkText not full linkText
						  WRONG: @FindBy(linkText = "(6) POLO")
						  CORRECT: @FindBy(partialLinkText = "POLO")
						  OR: @FindBy(css = "a[href*='polo']")
						- Never hardcode numbers that appear before brand names in parentheses
						- For sidebar filter links always prefer CSS href selector over linkText
				10. TESTNG USAGE RULES:
					- USE ONLY TestNG annotations (org.testng.annotations.*). NEVER use JUnit 5 annotations.
					- For test metadata, use separate @Description and @Severity annotations, NOT parameters in @Test.
					- Example: @Test(description = "Verify login works", @Severity(SeverityLevel.CRITICAL))
					- NEVER mix org.junit.jupiter.api.* with org.testng.annotations.* in the same file.
					- ALWAYS initialize WebDriverWait in @BeforeMethod: wait = new WebDriverWait(driver, Duration.ofSeconds(10));
					- NEVER use Thread.sleep() - always use WebDriverWait for waits
                """
                + ProjectConfig.getBase() + "\n"
                + ProjectConfig.getTestImports() + "\n"
                + SeleniumRules.get() + "\n"
                + AssertionRules.get() + "\n"
                + CoreRules.get() + "\n"
                + RecoveryRules.get();
    }

    public static String userPrompt(String component, String className,
                                     String pageUrl, String testCasesText,
                                     String pomCode, String pageMetadata) {
        return """
                Generate a complete Selenium TestNG test class for the '%s' component.

                Target URL: %s
                Class Name: %s

                PAGE BEHAVIOR (read before writing assertions):
                %s

                THE PAGE OBJECT CLASS — use ONLY methods defined here:
                %s

                Test cases to implement:
                %s

                Requirements:
                - Each test case becomes one @Test method
                - ONLY call methods that exist in the Page Object above
                - If a method is missing — add TODO comment, do not invent
                - Declare WebDriverWait as class field, initialize in @BeforeMethod
                - Always wait before asserting
                - Add @Description and @Severity to every @Test
                - Remove all unused imports

                Start response with: package generated.ui;
                """.formatted(component, pageUrl, className,
                              pageMetadata, pomCode, testCasesText);
    }
}