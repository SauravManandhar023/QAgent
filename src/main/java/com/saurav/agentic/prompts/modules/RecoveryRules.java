package com.saurav.agentic.prompts.modules;

/**
 * RecoveryRules - Error recovery and fallback rules
 * Prevents hallucination when generation is ambiguous
 */
public class RecoveryRules {

    private RecoveryRules() {}

    public static String get() {
        return """
                ERROR RECOVERY RULES:
                1. If a required POM method does not exist — add a TODO comment:
                   // TODO: POM missing method — add getSuccessMessage() to page class
                   NEVER invent a method that is not in the provided POM
                
                2. If a locator cannot be determined from available information:
                   // TODO: Verify locator — element not found in page analysis
                   NEVER guess a locator
                
                3. If a test step cannot be automated with available methods:
                   // TODO: Manual verification required — automation not possible
                   DO NOT skip the test — add it with TODO comments
                
                4. If an import is uncertain — omit it rather than guessing
                   Unused or wrong imports cause compile errors
                
                5. If generation is completely impossible for a test case:
                   Generate the method signature and add:
                   // TODO: Test case cannot be automated — requires: [reason]
                   throw new org.testng.SkipException("Not yet automated");
                   
                 // Add to RecoveryRules.get():

        		6. NEVER use empty string locators: @FindBy(linkText = "") is invalid
        		If link text is unknown, use CSS href attribute instead:
        		@FindBy(css = "[href='https://example.com']")

        		7. NEVER add private helper methods in test classes that duplicate page object calls
        		All interactions go through the Page Object — not through private test methods

        		8. @Test annotation does NOT accept severity or description parameters directly
        		WRONG: @Test(description = "...", severity = SeverityLevel.NORMAL)
        		CORRECT: Use separate @Description("...") and @Severity(SeverityLevel.NORMAL)

        		9. ALL POM classes that use WebDriverWait MUST import:
        		import org.openqa.selenium.support.ui.WebDriverWait;
        		import org.openqa.selenium.support.ui.ExpectedConditions;
        		import java.time.Duration;
        		
        		10. SeverityLevel values are EXACTLY:
			    SeverityLevel.BLOCKER
			    SeverityLevel.CRITICAL
			    SeverityLevel.NORMAL   ← use this for Medium priority
			    SeverityLevel.MINOR    ← use this for Low priority
			    SeverityLevel.TRIVIAL
			    NEVER use SeverityLevel.MEDIUM — it does not exist
			    Map priority like this:
			    High   → SeverityLevel.CRITICAL
			    Medium → SeverityLevel.NORMAL
			    Low    → SeverityLevel.MINOR
			    
			    12. NEVER use empty linkText locator: @FindBy(linkText = "")
        		If link text is unknown use CSS href:
        		@FindBy(css = "[href='https://github.com/tourdedave/the-internet']")

				13. In @BeforeMethod always follow this exact order:
				    1. driver = WebDriverManager.chromedriver().create();
				    2. driver.get("URL");
				    3. pageObject = new PageClass(driver);
				    4. wait = new WebDriverWait(driver, Duration.ofSeconds(10));
				    
				14. For link elements or any element that may be off-screen or have zero size:
			    NEVER use element.click() directly for links at bottom of page
			    ALWAYS scroll into view first then use JS click:
			    js.executeScript("arguments[0].scrollIntoView({block:'center'});", element);
			    js.executeScript("arguments[0].click();", element);
			    For this, add JavascriptExecutor to the POM class:
			    private JavascriptExecutor js;
			    Initialize in constructor: this.js = (JavascriptExecutor) driver;
			    Import: import org.openqa.selenium.JavascriptExecutor;
			    
			    15. For links with target="_blank" — they open in a new tab:
			    NEVER wait for URL change on the original window
			    ALWAYS switch to the new tab first:
			    wait.until(ExpectedConditions.numberOfWindowsToBe(2));
			    for (String handle : driver.getWindowHandles()) {
			        if (!handle.equals(originalWindow)) {
			            driver.switchTo().window(handle);
			            break;
			        }
			    }
			    Then assert on the new tab URL.
			    
			    """;
    }
}