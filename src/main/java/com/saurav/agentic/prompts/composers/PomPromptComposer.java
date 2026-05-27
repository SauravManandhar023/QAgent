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
                You are a senior Selenium automation engineer.
                Generate a Page Object Model (POM) class for the given web component.
                Respond with PURE Java code only — no explanation, no markdown, no backticks.
                Start your response directly with: package pages;
                
                """
                + ProjectConfig.getBase() + "\n"
                + ProjectConfig.getPomImports() + "\n"
                + SeleniumRules.get() + "\n"
                + LocatorRules.get() + "\n"
                + RecoveryRules.get() + "\n"
                + """
                POM SPECIFIC RULES:
                1. Use @FindBy for all locators — never findElement() inside POM
                2. Constructor accepts ONLY WebDriver — no other parameters
                3. Call PageFactory.initElements(driver, this) in constructor
                4. For every @FindBy field — create a public getter returning WebElement
                5. For input fields — create enterX(String value) that clears then sendKeys
                6. For buttons — create clickX() method
                7. For message elements — create getText() returning String
                   and isDisplayed() returning boolean
                8. Add JavaDoc for class and every public method
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