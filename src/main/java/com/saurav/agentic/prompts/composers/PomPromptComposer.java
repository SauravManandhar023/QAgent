package com.saurav.agentic.prompts.composers;

import com.saurav.agentic.prompts.modules.LocatorRules;
import com.saurav.agentic.prompts.modules.ProjectConfig;
import com.saurav.agentic.prompts.modules.RecoveryRules;
import com.saurav.agentic.prompts.modules.SeleniumRules;

/**
 * PomPromptComposer - Composes prompts for POM class generation
 * Uses only relevant modules — no unnecessary rules
 */
public class PomPromptComposer {

    private PomPromptComposer() {}

    public static String systemPrompt() {
        return """
        		POM RULES:
        		1. Use @FindBy for all locators — priority: id > name > css > linkText > xpath
        		2. Constructor accepts WebDriver only — call PageFactory.initElements(driver, this)
        		3. For every @FindBy field create a public getter returning WebElement
        		4. For inputs: enterX(String value) — clear then sendKeys
        		5. For buttons: clickX() method
        		6. For messages: getText() and isDisplayed() methods
        		7. NEVER use empty linkText: @FindBy(linkText = "")
        		   Use CSS href instead: @FindBy(css = "[href='url']")
        		8. Include WebDriverWait as field, initialize in constructor
        		9. ALWAYS include a flash/error message element if the page has form submission:
        		   @FindBy(id = "flash")
        		   private WebElement flashMessage;
        		   
        		   public WebElement getFlashMessage() { return flashMessage; }
        		   public String getFlashMessageText() { return flashMessage.getText(); }
        		   public boolean isFlashMessageDisplayed() { return flashMessage.isDisplayed(); }
        		""";
    }

    public static String userPrompt(String component, String className,
                                     String pageUrl, String pageAnalysis) {
        return """
                Generate a Page Object Model class for the '%s' component.
                
                Target URL: %s
                Class Name: %s
                
                Page elements found:
                %s
                
                Requirements:
                - Create @FindBy locators for all relevant elements
                - Follow locator priority from the rules above
                - Create public getter for every @FindBy element
                - Create action methods for all interactive elements
                - If a locator is unknown — add TODO comment
                
                Start response with: package pages;
                """.formatted(component, pageUrl, className, pageAnalysis);
    }
}