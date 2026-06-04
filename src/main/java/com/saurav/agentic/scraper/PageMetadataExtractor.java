package com.saurav.agentic.scraper;

import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.PageElement;
import com.saurav.agentic.models.PageMetadata;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.List;

/**
 * PageMetadataExtractor - Extracts structured DOM metadata from a web page
 *
 * Behaves like a developer inspecting the page in browser DevTools:
 * - Reads every interactive element with all its attributes
 * - Detects initial state (checked, selected, disabled)
 * - Generates best locator strategy per element
 * - Detects page type and behavior hints
 * - Identifies target="_blank" links
 *
 * Output: PageMetadata — structured, rich, ready for AI prompts
 */
public class PageMetadataExtractor {

    /**
     * Extract full page metadata from a URL
     */
    public PageMetadata extract(String url) {
        System.out.println(FrameworkConstants.LOG_INFO +
                " Extracting page metadata: " + url);

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(options);
        PageMetadata metadata = new PageMetadata();

        try {
            driver.get(url);
            Thread.sleep(2000); // wait for page to settle

            metadata.setUrl(url);
            metadata.setPageTitle(driver.getTitle());

            JavascriptExecutor js = (JavascriptExecutor) driver;

            // ── Extract forms ────────────────────────────────────────────────
            extractForms(driver, metadata);

            // ── Extract inputs ───────────────────────────────────────────────
            extractInputs(driver, metadata, js);

            // ── Extract buttons ──────────────────────────────────────────────
            extractButtons(driver, metadata, js);

            // ── Extract checkboxes ───────────────────────────────────────────
            extractCheckboxes(driver, metadata, js);

            // ── Extract dropdowns ────────────────────────────────────────────
            extractDropdowns(driver, metadata, js);

            // ── Extract links ────────────────────────────────────────────────
            extractLinks(driver, metadata, js);

            // ── Extract images ───────────────────────────────────────────────
            extractImages(driver, metadata);

            // ── Detect page type ─────────────────────────────────────────────
            detectPageType(metadata);

            // ── Detect page behavior ─────────────────────────────────────────
            detectPageBehavior(metadata, url);

            System.out.println(FrameworkConstants.LOG_SUCCESS +
                    " Metadata extracted — " +
                    metadata.getAllElements().size() + " elements found");
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Page type: " + metadata.getPageType());

        } catch (Exception e) {
            System.out.println(FrameworkConstants.LOG_ERROR +
                    " Metadata extraction failed: " + e.getMessage());
        } finally {
            driver.quit();
        }

        return metadata;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FORM EXTRACTION
    // ─────────────────────────────────────────────────────────────────────────

    private void extractForms(WebDriver driver, PageMetadata metadata) {
        List<WebElement> forms = driver.findElements(By.tagName("form"));
        for (WebElement form : forms) {
            PageElement el = new PageElement();
            el.setTag("form");
            el.setId(attr(form, "id"));
            el.setAction(attr(form, "action"));
            el.setMethod(attr(form, "method"));
            el.setClassName(attr(form, "class"));
            metadata.addForm(el);
            metadata.setHasForm(true);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INPUT EXTRACTION
    // ─────────────────────────────────────────────────────────────────────────

    private void extractInputs(WebDriver driver, PageMetadata metadata,
                                JavascriptExecutor js) {
        List<WebElement> inputs = driver.findElements(
                By.cssSelector("input:not([type='checkbox']):not([type='radio'])" +
                                ":not([type='submit']):not([type='button'])"));

        for (WebElement input : inputs) {
            PageElement el = new PageElement();
            el.setTag("input");
            el.setId(attr(input, "id"));
            el.setName(attr(input, "name"));
            el.setType(attr(input, "type"));
            el.setClassName(attr(input, "class"));
            el.setPlaceholder(attr(input, "placeholder"));
            el.setValue(attr(input, "value"));
            el.setAriaLabel(attr(input, "aria-label"));
            el.setDataTestId(attr(input, "data-testid"));
            el.setRequired(!attr(input, "required").isEmpty());
            el.setDisabled(!attr(input, "disabled").isEmpty());
            el.setVisible(input.isDisplayed());
            el.setInteractable(input.isDisplayed() && input.isEnabled());

            // Find parent form
            try {
                WebElement form = input.findElement(
                        By.xpath("./ancestor::form[1]"));
                el.setFormId(attr(form, "id"));
            } catch (Exception ignored) {}

            // Best locator
            setBestLocator(el);
            generateCssSelector(el);

            metadata.addInput(el);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUTTON EXTRACTION
    // ─────────────────────────────────────────────────────────────────────────

    private void extractButtons(WebDriver driver, PageMetadata metadata,
                                 JavascriptExecutor js) {
        // Get both <button> and <input type="submit/button">
        List<WebElement> buttons = driver.findElements(
                By.cssSelector("button, input[type='submit'], input[type='button']"));

        for (WebElement button : buttons) {
            PageElement el = new PageElement();
            el.setTag(button.getTagName());
            el.setId(attr(button, "id"));
            el.setName(attr(button, "name"));
            el.setType(attr(button, "type"));
            el.setClassName(attr(button, "class"));
            el.setText(button.getText());
            el.setValue(attr(button, "value"));
            el.setAriaLabel(attr(button, "aria-label"));
            el.setDataTestId(attr(button, "data-testid"));
            el.setDisabled(!attr(button, "disabled").isEmpty());
            el.setVisible(button.isDisplayed());
            el.setInteractable(button.isDisplayed() && button.isEnabled());

            setBestLocator(el);
            generateCssSelector(el);

            metadata.addButton(el);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHECKBOX EXTRACTION
    // ─────────────────────────────────────────────────────────────────────────

    private void extractCheckboxes(WebDriver driver, PageMetadata metadata,
                                    JavascriptExecutor js) {
        List<WebElement> checkboxes = driver.findElements(
                By.cssSelector("input[type='checkbox'], input[type='radio']"));

        for (WebElement checkbox : checkboxes) {
            PageElement el = new PageElement();
            el.setTag("input");
            el.setId(attr(checkbox, "id"));
            el.setName(attr(checkbox, "name"));
            el.setType(attr(checkbox, "type"));
            el.setClassName(attr(checkbox, "class"));
            el.setValue(attr(checkbox, "value"));
            el.setChecked(checkbox.isSelected()); // ← actual DOM state
            el.setDisabled(!attr(checkbox, "disabled").isEmpty());
            el.setVisible(checkbox.isDisplayed());
            el.setInteractable(checkbox.isDisplayed() && checkbox.isEnabled());

            // Find associated label
            String id = attr(checkbox, "id");
            if (!id.isEmpty()) {
                try {
                    WebElement label = driver.findElement(
                            By.cssSelector("label[for='" + id + "']"));
                    el.setText(label.getText());
                } catch (Exception ignored) {}
            }

            setBestLocator(el);
            generateCssSelector(el);

            metadata.addCheckbox(el);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DROPDOWN EXTRACTION
    // ─────────────────────────────────────────────────────────────────────────

    private void extractDropdowns(WebDriver driver, PageMetadata metadata,
                                   JavascriptExecutor js) {
        List<WebElement> selects = driver.findElements(By.tagName("select"));

        for (WebElement select : selects) {
            PageElement el = new PageElement();
            el.setTag("select");
            el.setId(attr(select, "id"));
            el.setName(attr(select, "name"));
            el.setClassName(attr(select, "class"));
            el.setAriaLabel(attr(select, "aria-label"));
            el.setDataTestId(attr(select, "data-testid"));
            el.setDisabled(!attr(select, "disabled").isEmpty());
            el.setVisible(select.isDisplayed());
            el.setInteractable(select.isDisplayed() && select.isEnabled());

            // Get all options
            List<WebElement> options = select.findElements(By.tagName("option"));
            StringBuilder optionText = new StringBuilder();
            for (WebElement option : options) {
                String val  = attr(option, "value");
                String text = option.getText();
                boolean sel = option.isSelected();
                boolean dis = !attr(option, "disabled").isEmpty();
                
                optionText.append(text)
                          .append("(value=").append(val).append(")")
                          .append(sel ? "[CURRENTLY_SELECTED_DEFAULT]" : "")
                          .append(dis ? "[DISABLED_CANNOT_SELECT]" : "")
                          .append(", ");
            }
            el.setText(optionText.toString());

            setBestLocator(el);
            generateCssSelector(el);

            metadata.addDropdown(el);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LINK EXTRACTION
    // ─────────────────────────────────────────────────────────────────────────

    private void extractLinks(WebDriver driver, PageMetadata metadata,
                               JavascriptExecutor js) {
        List<WebElement> links = driver.findElements(By.tagName("a"));

        for (WebElement link : links) {
            String href = attr(link, "href");
            String text = link.getText().trim();

            // Skip empty or javascript: links
            if (href.isEmpty() || href.startsWith("javascript:")) continue;

            PageElement el = new PageElement();
            el.setTag("a");
            el.setId(attr(link, "id"));
            el.setClassName(attr(link, "class"));
            el.setHref(href);
            el.setText(text);
            el.setTarget(attr(link, "target")); // ← captures _blank!
            el.setAriaLabel(attr(link, "aria-label"));
            el.setDataTestId(attr(link, "data-testid"));
            el.setVisible(link.isDisplayed());

            // Best locator for links
            if (!attr(link, "id").isEmpty()) {
                el.setBestLocator("id");
                el.setBestLocatorValue(attr(link, "id"));
            } else if (!text.isEmpty()) {
                el.setBestLocator("linkText");
                el.setBestLocatorValue(text);
            } else if (!href.isEmpty()) {
                el.setBestLocator("css");
                el.setBestLocatorValue("[href='" + href + "']");
            }

            metadata.addLink(el);
            metadata.setHasLinks(true);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IMAGE EXTRACTION
    // ─────────────────────────────────────────────────────────────────────────

    private void extractImages(WebDriver driver, PageMetadata metadata) {
        List<WebElement> images = driver.findElements(By.tagName("img"));

        for (WebElement image : images) {
            PageElement el = new PageElement();
            el.setTag("img");
            el.setId(attr(image, "id"));
            el.setSrc(attr(image, "src"));
            el.setAlt(attr(image, "alt"));
            el.setClassName(attr(image, "class"));
            el.setVisible(image.isDisplayed());

            setBestLocator(el);
            metadata.addImage(el);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAGE TYPE DETECTION
    // ─────────────────────────────────────────────────────────────────────────

    private void detectPageType(PageMetadata metadata) {
        boolean hasForm      = !metadata.getForms().isEmpty();
        boolean hasInputs    = !metadata.getInputs().isEmpty();
        boolean hasCheckbox  = !metadata.getCheckboxes().isEmpty();
        boolean hasDropdown  = !metadata.getDropdowns().isEmpty();
        boolean hasLinks     = !metadata.getLinks().isEmpty();

        if (hasForm && hasInputs) {
            // Check if it's a login form
            boolean hasPassword = metadata.getInputs().stream()
                    .anyMatch(e -> "password".equals(e.getType()));
            if (hasPassword) {
                metadata.setPageType("login-form");
            } else {
                metadata.setPageType("form");
            }
        } else if (hasCheckbox) {
            metadata.setPageType("checkboxes");
        } else if (hasDropdown) {
            metadata.setPageType("dropdown");
        } else if (hasLinks) {
            metadata.setPageType("navigation");
        } else {
            metadata.setPageType("content");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAGE BEHAVIOR DETECTION
    // ─────────────────────────────────────────────────────────────────────────

    private void detectPageBehavior(PageMetadata metadata, String url) {
        String pageType = metadata.getPageType();

        switch (pageType) {
            case "login-form" -> {
                metadata.setSuccessCondition(
                    "URL changes to contain '/secure' and success flash message appears");
                metadata.setFailureCondition(
                    "URL stays the same, error flash message appears in id='flash'");
                metadata.setPrimaryAction("Submit login form");
                metadata.setRequiresAuth(false);
            }
            case "checkboxes" -> {
                metadata.setSuccessCondition(
                    "Checkbox state changes — use isSelected() to verify");
                metadata.setFailureCondition(
                    "Checkbox state does not change after click");
                metadata.setPrimaryAction("Check/uncheck checkboxes");

                // Add initial state info
                StringBuilder states = new StringBuilder();
                for (int i = 0; i < metadata.getCheckboxes().size(); i++) {
                    PageElement cb = metadata.getCheckboxes().get(i);
                    states.append("Checkbox ").append(i + 1)
                          .append(": ").append(cb.isChecked() ? "CHECKED" : "UNCHECKED")
                          .append("\n");
                }
                metadata.setPlainTextSummary(states.toString());
            }
            case "dropdown" -> {
                // Build actual option list from extracted dropdowns
                StringBuilder optionList = new StringBuilder();
                if (!metadata.getDropdowns().isEmpty()) {
                    String rawOptions = metadata.getDropdowns().get(0).getText();
                    optionList.append(rawOptions);
                }

                metadata.setSuccessCondition(
                    "Selected option changes — use Select class and getFirstSelectedOption().getText() to verify");
                metadata.setFailureCondition(
                    "Selected option does not change");
                metadata.setPrimaryAction("Select dropdown option using Select class");
                metadata.setPlainTextSummary(
                		"CRITICAL: Only use EXACT option text that exists in the dropdown.\n" +
    			        "Actual dropdown options found on page: " + optionList + "\n" +
    			        "NOTE: 'Please select an option' is DISABLED and cannot be selected.\n" +
    			        "Only 'Option 1' and 'Option 2' are selectable options.\n" +
    			        "NEVER use XSS payloads, SQL injection, or made-up values as dropdown options.\n" +
    			        "NEVER use option numbers like '1' or '2' — use full text like 'Option 1', 'Option 2'.\n" +
    			        "Security tests on dropdowns should assert that injection attempts are rejected,\n" +
    			        "NOT attempt to select them as if they were valid options."
                );
            }
            case "navigation" -> {
                metadata.setSuccessCondition(
                    "URL changes after clicking link");
                metadata.setFailureCondition(
                    "URL does not change or element not found");
                metadata.setPrimaryAction("Navigate via links");
                metadata.setSuccessCondition(
                        "URL changes after clicking link. " +
                        "If URL contains '#google_vignette' an ad intercepted — " +
                        "wait for it to clear before asserting URL.");
            }
            default -> {
                metadata.setSuccessCondition(
                    "Verify expected elements are visible and correct");
                metadata.setPrimaryAction("Interact with page elements");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOCATOR HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Determines best locator strategy — mirrors DevTools locator priority
     * id > name > data-testid > aria-label > css > xpath
     */
    private void setBestLocator(PageElement el) {
        if (el.getId() != null && !el.getId().isEmpty()) {
            el.setBestLocator("id");
            el.setBestLocatorValue(el.getId());
        } else if (el.getName() != null && !el.getName().isEmpty()) {
            el.setBestLocator("name");
            el.setBestLocatorValue(el.getName());
        } else if (el.getDataTestId() != null && !el.getDataTestId().isEmpty()) {
            el.setBestLocator("css");
            el.setBestLocatorValue("[data-testid='" + el.getDataTestId() + "']");
        } else if (el.getAriaLabel() != null && !el.getAriaLabel().isEmpty()) {
            el.setBestLocator("css");
            el.setBestLocatorValue("[aria-label='" + el.getAriaLabel() + "']");
        } else if (el.getClassName() != null && !el.getClassName().isEmpty()) {
            String firstClass = el.getClassName().split(" ")[0];
            el.setBestLocator("css");
            el.setBestLocatorValue(el.getTag() + "." + firstClass);
        } else if (el.getText() != null && !el.getText().isEmpty()
                && el.getTag().equals("button")) {
            el.setBestLocator("xpath");
            el.setBestLocatorValue(
                    "//button[contains(text(),'" + el.getText() + "')]");
        } else {
            el.setBestLocator("css");
            el.setBestLocatorValue(el.getTag());
        }
    }

    /**
     * Generates a CSS selector for the element
     */
    private void generateCssSelector(PageElement el) {
        if (el.getId() != null && !el.getId().isEmpty()) {
            el.setCssSelector("#" + el.getId());
        } else if (el.getClassName() != null && !el.getClassName().isEmpty()) {
            String firstClass = el.getClassName().split(" ")[0];
            el.setCssSelector(el.getTag() + "." + firstClass);
        } else if (el.getName() != null && !el.getName().isEmpty()) {
            el.setCssSelector(el.getTag() + "[name='" + el.getName() + "']");
        } else {
            el.setCssSelector(el.getTag());
        }
    }

    /**
     * Safely gets attribute value — returns empty string if null
     */
    private String attr(WebElement el, String attr) {
        String val = el.getAttribute(attr);
        return val != null ? val : "";
    }
}