package com.saurav.agentic.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class BasePage {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected JavascriptExecutor js;

    private static final int DEFAULT_WAIT    = 10;
    private static final int JS_SCROLL_DELAY = 100; // ms after JS scroll

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT));
        this.js     = (JavascriptExecutor) driver;
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    protected void navigateTo(String url) {
        driver.get(url);
        waitForPageLoad();
    }

    protected String getPageTitle() {
        return driver.getTitle();
    }

    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    // ─── Core Actions (Explicit Wait + JS Fallback) ───────────────────────────

    /**
     * Tries normal Selenium click first.
     * Falls back to JS click if element is not interactable.
     */
    protected void click(By locator) {
        WebElement element = waitForClickable(locator);
        try {
            element.click();
        } catch (Exception e) {
            System.out.println("[WARN] Normal click failed on: " + locator + " — retrying with JS click");
            jsClick(element);
        }
    }

    /**
     * Clears field and types text.
     * Falls back to JS setValue if sendKeys is blocked.
     */
    protected void type(By locator, String text) {
        WebElement element = waitForVisible(locator);
        try {
            element.clear();
            element.sendKeys(text);
        } catch (Exception e) {
            System.out.println("[WARN] sendKeys failed on: " + locator + " — retrying with JS");
            jsType(element, text);
        }
    }

    protected String getText(By locator) {
        WebElement element = waitForVisible(locator);
        String text = element.getText();
        if (text == null || text.isEmpty()) {
            // Some elements store text in value attribute (inputs)
            text = element.getAttribute("value");
        }
        return text;
    }

    protected boolean isDisplayed(By locator) {
        try {
            return waitForVisible(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isEnabled(By locator) {
        try {
            WebElement element = waitForVisible(locator);
            // Check both Selenium isEnabled and JS disabled attribute
            boolean seleniumEnabled = element.isEnabled();
            Object jsDisabled = js.executeScript(
                "return arguments[0].disabled;", element
            );
            boolean jsEnabled = (jsDisabled == null || jsDisabled.equals(false));
            return seleniumEnabled && jsEnabled;
        } catch (Exception e) {
            return false;
        }
    }

    // ─── JS Helpers ───────────────────────────────────────────────────────────

    protected void jsClick(WebElement element) {
        js.executeScript("arguments[0].click();", element);
    }

    protected void jsClick(By locator) {
        jsClick(waitForPresent(locator));
    }

    protected void jsType(WebElement element, String text) {
        js.executeScript("arguments[0].value = arguments[1];", element, text);
    }

    protected void jsType(By locator, String text) {
        jsType(waitForPresent(locator), text);
    }

    protected void scrollIntoView(By locator) {
        WebElement element = waitForPresent(locator);
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
        sleep(JS_SCROLL_DELAY); // brief pause so element settles after scroll
    }

    protected void scrollToBottom() {
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    protected void scrollToTop() {
        js.executeScript("window.scrollTo(0, 0);");
    }

    /**
     * Highlights an element with a red border — useful for debugging.
     */
    protected void highlight(By locator) {
        WebElement element = waitForPresent(locator);
        js.executeScript(
            "arguments[0].style.border='3px solid red';", element
        );
    }

    // ─── Explicit Wait Methods ─────────────────────────────────────────────────

    /**
     * Waits for element to be VISIBLE in the DOM and on screen.
     */
    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits for element to be CLICKABLE (visible + enabled).
     */
    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Waits for element to be PRESENT in DOM — does not require visibility.
     * Use this before JS operations since JS can act on hidden elements.
     */
    protected WebElement waitForPresent(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Waits for element to DISAPPEAR — useful after form submissions.
     */
    protected void waitForInvisible(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    protected void waitForUrl(String urlFragment) {
        wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    protected void waitForTitle(String titleFragment) {
        wait.until(ExpectedConditions.titleContains(titleFragment));
    }

    /**
     * Waits for page's document.readyState to be 'complete'.
     * Call this after navigation or form submission.
     */
    protected void waitForPageLoad() {
        wait.until(driver -> js.executeScript(
            "return document.readyState"
        ).equals("complete"));
    }
    
    /**
     * Dismisses Google ad vignette overlay if present.
     * Call this before clicking any navigation link on ad-heavy sites.
     */
    protected void dismissAdOverlay() {
        try {
            // Wait briefly for potential ad overlay - reduced for faster execution
            Thread.sleep(500);
            String currentUrl = driver.getCurrentUrl();
            if (currentUrl.contains("google_vignette") || 
                currentUrl.contains("#")) {
                // Navigate back to clean URL
                String cleanUrl = currentUrl.split("#")[0];
                driver.get(cleanUrl);
                Thread.sleep(200); // reduced for faster execution
            }
            // Try to remove overlay via JS
            js.executeScript(
                "var overlays = document.querySelectorAll(" +
                "'[id*=vignette],[class*=vignette]," +
                "[id*=overlay],[class*=overlay]," +
                "[id*=modal],[class*=ad-modal]');" +
                "overlays.forEach(el => el.remove());"
            );
        } catch (Exception ignored) {}
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}