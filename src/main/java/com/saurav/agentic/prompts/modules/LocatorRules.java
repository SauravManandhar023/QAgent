package com.saurav.agentic.prompts.modules;

/**
 * LocatorRules - Locator strategy priority rules
 * Ensures stable, maintainable locators
 */
public class LocatorRules {

    private LocatorRules() {}

    public static String get() {
        return """
                LOCATOR STRATEGY (use in this priority order):
                1st: id          → @FindBy(id = "username")
                2nd: name        → @FindBy(name = "username")
                3rd: data-testid → @FindBy(css = "[data-testid='submit-btn']")
                4th: aria-label  → @FindBy(css = "[aria-label='Submit']")
                5th: stable CSS  → @FindBy(css = ".flash.error")
                6th: linkText    → @FindBy(linkText = "Forgot Password")
                LAST: XPath      → only when nothing above works
                
                NEVER USE:
                - Absolute XPath: /html/body/div[2]/form/button
                - Index-based XPath: //div[3]/button[1]
                - Dynamic IDs that change on reload
                - Position-based CSS: nth-child() unless stable
                
                GOOD EXAMPLES:
                By.id("username")
                By.cssSelector(".flash.error")
                By.cssSelector("[data-testid='login-btn']")
                By.linkText("Forgot Password")
                
                BAD EXAMPLES:
                By.xpath("/html/body/div[2]/div/form/div[1]/input")
                By.xpath("//div[3]/button[2]")
                By.cssSelector("div:nth-child(3) > button:nth-child(2)")
                """;
    }
}