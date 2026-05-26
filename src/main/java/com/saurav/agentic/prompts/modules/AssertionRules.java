package com.saurav.agentic.prompts.modules;

/**
 * AssertionRules - Assertion strategy rules
 * Ensures meaningful, reliable assertions
 */
public class AssertionRules {

    private AssertionRules() {}

    public static String get() {
        return """
                ASSERTION RULES:
                1. ALWAYS wait before asserting:
                   wait.until(ExpectedConditions.urlContains("secure"));
                   Then: Assert.assertTrue(driver.getCurrentUrl().contains("secure"));
                
                2. NEVER assert on page title for dynamic outcomes:
                   WRONG: Assert.assertTrue(driver.getTitle().contains("Dashboard"))
                   Page titles often stay the same regardless of outcome.
                
                3. For POSITIVE tests — assert URL change or success message:
                   Assert.assertTrue(driver.getCurrentUrl().contains("secure"))
                   Assert.assertTrue(page.getSuccessMessage().isDisplayed())
                
                4. For NEGATIVE tests — assert error message text:
                   Assert.assertTrue(page.getErrorMessage().contains("invalid"))
                   NEVER assert page title for failure detection
                
                5. Use descriptive failure messages in assertions:
                   Assert.assertTrue(condition, "Expected login to succeed but it failed")
                
                6. One logical assertion per test — avoid multiple unrelated assertions
                
                7. Use correct Assert methods:
                   import org.testng.Assert;        ← CORRECT
                   import org.testng.asserts.Assert; ← WRONG
                
                8. For element visibility assertions:
                   Assert.assertTrue(element.isDisplayed(), "Element should be visible")
                   Assert.assertFalse(element.isDisplayed(), "Element should be hidden")
                """;
    }
}