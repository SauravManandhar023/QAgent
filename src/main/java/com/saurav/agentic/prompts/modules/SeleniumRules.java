package com.saurav.agentic.prompts.modules;

/**
 * SeleniumRules - Selenium 4 specific rules
 * Prevents use of deprecated Selenium 3 patterns
 */
public class SeleniumRules {

    private SeleniumRules() {}

    public static String get() {
        return """
                SELENIUM 4 RULES:
                1. ALWAYS use Duration.ofSeconds() for WebDriverWait:
                   CORRECT: new WebDriverWait(driver, Duration.ofSeconds(10))
                   WRONG:   new WebDriverWait(driver, 10)
                
                2. NEVER use deprecated Selenium 3 methods:
                   WRONG: driver.findElementByCssSelector()
                   WRONG: driver.findElementById()
                   CORRECT: driver.findElement(By.cssSelector())
                   CORRECT: driver.findElement(By.id())
                
                3. Declare WebDriverWait as a CLASS FIELD:
                   private WebDriverWait wait;
                   Initialize ONCE in @BeforeMethod:
                   wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                   NEVER create WebDriverWait inside @Test methods
                
                4. NEVER use Thread.sleep() — always use WebDriverWait:
                   WRONG: Thread.sleep(2000);
                   CORRECT: wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
                
                5. Use PageFactory for POM classes:
                   Constructor must call: PageFactory.initElements(driver, this);
                
                6. For @FindBy annotations — field must be WebElement type:
                   CORRECT: @FindBy(id = "username") private WebElement usernameInput;
                   WRONG:   @FindBy(id = "username") private By usernameInput;
                
                7. Always wait before interacting with elements:
                   wait.until(ExpectedConditions.elementToBeClickable(locator));
                   wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
                """;
    }
}