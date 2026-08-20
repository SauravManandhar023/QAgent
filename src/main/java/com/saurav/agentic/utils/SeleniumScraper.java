package com.saurav.agentic.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.saurav.agentic.config.FrameworkConfig;
import com.saurav.agentic.constants.FrameworkConstants;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * SeleniumScraper - Scrapes full UI element details from a web page
 * Uses Selenium to load the page + JSoup for deep HTML analysis
 * Returns structured page analysis string for Groq AI
 *
 */
public class SeleniumScraper {

    private final FrameworkConfig config;

    public SeleniumScraper() {
        this.config = FrameworkConfig.getInstance();
    }

    /**
     * Scrape a URL and return full page analysis as a structured string
     * This string is sent directly to Groq AI for test case generation
     */
    public String scrape(String url) {
        System.out.println(FrameworkConstants.LOG_INFO + " Scraping: " + url);
        System.out.println(FrameworkConstants.LOG_SEPARATOR);

        StringBuilder analysis = new StringBuilder();
        WebDriver driver = null;

        try {
            // Setup driver
            driver = setupDriver();
            driver.get(url);

            // Wait for page to fully load
            int waitSeconds = config.getScraperWaitSeconds();
            System.out.println("⏳ Waiting " + waitSeconds + "s for page to load...");
            Thread.sleep(waitSeconds * 1000L);

            String pageTitle = driver.getTitle();
            String currentUrl = driver.getCurrentUrl();
            String pageSource = driver.getPageSource();

            System.out.println(FrameworkConstants.LOG_SUCCESS + " Page loaded: " + pageTitle);

            // Build analysis
            analysis.append("=== PAGE INFORMATION ===\n");
            analysis.append("URL: ").append(currentUrl).append("\n");
            analysis.append("Title: ").append(pageTitle).append("\n\n");

            // Parse with JSoup for deep analysis
            Document doc = Jsoup.parse(pageSource);

            // Extract all sections
            analyzeForms(doc, analysis);
            analyzeInputs(doc, analysis);
            analyzeButtons(doc, analysis);
            analyzeLinks(doc, analysis);
            analyzeNavigation(doc, analysis);
            analyzeTables(doc, analysis);
            analyzeDropdowns(doc, analysis);
            analyzeCheckboxesAndRadios(doc, analysis);
            analyzeSearch(doc, analysis);
            analyzeImages(doc, analysis);
            analyzeAlerts(doc, analysis);
            analyzeModals(doc, analysis);
            analyzePageMeta(doc, analysis);

            // Print summary to console
            printSummary(analysis.toString(), url);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(FrameworkConstants.LOG_ERROR + " Scraping interrupted: " + e.getMessage());
        } catch (Exception e) {
            System.err.println(FrameworkConstants.LOG_ERROR + " Scraping failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println(FrameworkConstants.LOG_SUCCESS + " Browser closed");
            }
        }

        return analysis.toString();
    }

    private void analyzeForms(Document doc, StringBuilder analysis) {
        Elements forms = doc.select("form");
        if (forms.isEmpty()) return;

        analysis.append("=== FORMS (").append(forms.size()).append(") ===\n");
        for (int i = 0; i < forms.size(); i++) {
            Element form = forms.get(i);
            // Use compact CSS selector-like format for better token efficiency
            analysis.append("  - ").append(form.tagName());
            String id = form.attr("id");
            if (!id.isEmpty()) analysis.append("#").append(id);
            String name = form.attr("name");
            if (!name.isEmpty()) analysis.append("[name='").append(name).append("']");
            String action = form.attr("action");
            if (!action.isEmpty()) analysis.append("[action=\"").append(action).append("\"]");
            String method = form.attr("method");
            if (!method.isEmpty()) analysis.append("[method=").append(method).append("]");
            analysis.append("\n");

            Elements formInputs = form.select("input, textarea, select");
            analysis.append("     fields: ").append(formInputs.size()).append("\n");
        }
        analysis.append("\n");
    }

    private void analyzeInputs(Document doc, StringBuilder analysis) {
        Elements inputs = doc.select("input, textarea");
        if (inputs.isEmpty()) return;

        analysis.append("=== INPUT FIELDS (").append(inputs.size()).append(") ===\n");
        for (Element input : inputs) {
            String type = input.attr("type").isEmpty() ? "text" : input.attr("type");
            if (type.equals("hidden") || type.equals("submit") || type.equals("button")) continue;

            // Use compact CSS selector-like format for better token efficiency
            analysis.append("  - ").append(type);
            String id = input.attr("id");
            if (!id.isEmpty()) analysis.append("#").append(id);
            String name = input.attr("name");
            if (!name.isEmpty()) analysis.append("[name='").append(name).append("']");
            String placeholder = input.attr("placeholder");
            if (!placeholder.isEmpty()) analysis.append("(placeholder=\"").append(placeholder).append("\")");
            if (input.hasAttr("required")) analysis.append(",required");
            String maxlength = input.attr("maxlength");
            if (!maxlength.isEmpty()) analysis.append(",maxlength=").append(maxlength);
            String pattern = input.attr("pattern");
            if (!pattern.isEmpty()) analysis.append(",pattern=\"").append(pattern).append("\"");
            if (input.hasAttr("readonly")) analysis.append(",readonly");
            if (input.hasAttr("disabled")) analysis.append(",disabled");
            analysis.append("\n");
        }
        analysis.append("\n");
    }

    private void analyzeButtons(Document doc, StringBuilder analysis) {
        Elements buttons = doc.select("button, input[type=submit], input[type=button], [role=button]");
        if (buttons.isEmpty()) return;

        analysis.append("=== BUTTONS (").append(buttons.size()).append(") ===\n");
        for (Element btn : buttons) {
            // Use compact CSS selector-like format for better token efficiency
            analysis.append("  - ").append(btn.tagName());
            String text = btn.text().trim();
            if (!text.isEmpty()) analysis.append("[text=\"").append(text).append("\"]");
            String id = btn.attr("id");
            if (!id.isEmpty()) analysis.append("#").append(id);
            String type = btn.attr("type");
            if (!type.isEmpty()) analysis.append("[type=").append(type).append("]");
            String cls = btn.attr("class");
            if (!cls.isEmpty()) analysis.append("[class=").append(cls).append("]");
            if (btn.hasAttr("disabled")) analysis.append(",disabled");
            analysis.append("\n");
        }
        analysis.append("\n");
    }

    private void analyzeLinks(Document doc, StringBuilder analysis) {
        Elements links = doc.select("a[href]");
        if (links.isEmpty()) return;

        analysis.append("=== LINKS (").append(links.size()).append(") ===\n");
        int count = 0;
        for (Element link : links) {
            if (count >= 7) {  // Reduced from 10 to 7 to save tokens
                analysis.append("  ... and ").append(links.size() - 7).append(" more\n");
                break;
            }
            // Use compact CSS selector-like format for better token efficiency
            analysis.append("  - ").append(link.tagName());
            String text = link.text().trim();
            if (!text.isEmpty()) analysis.append("[text=\"").append(text).append("\"]");
            String id = link.attr("id");
            if (!id.isEmpty()) analysis.append("#").append(id);
            String href = link.attr("href");
            if (!href.isEmpty()) analysis.append("[href=\"").append(href).append("\"]");
            analysis.append("\n");
            count++;
        }
        analysis.append("\n");
    }

    private void analyzeNavigation(Document doc, StringBuilder analysis) {
        Elements navLinks = doc.select("nav a, .nav a, .navbar a, .menu a, header a");
        if (navLinks.isEmpty()) return;

        analysis.append("=== NAVIGATION (").append(navLinks.size()).append(") ===\n");
        for (Element link : navLinks) {
            // Use compact CSS selector-like format for better token efficiency
            analysis.append("  - ").append(link.tagName());
            String text = link.text().trim();
            if (!text.isEmpty()) analysis.append("[text=\"").append(text).append("\"]");
            String id = link.attr("id");
            if (!id.isEmpty()) analysis.append("#").append(id);
            String href = link.attr("href");
            if (!href.isEmpty()) analysis.append("[href=\"").append(href).append("\"]");
            analysis.append("\n");
        }
        analysis.append("\n");
    }

    private void analyzeTables(Document doc, StringBuilder analysis) {
        Elements tables = doc.select("table");
        if (tables.isEmpty()) return;

        analysis.append("=== TABLES (").append(tables.size()).append(") ===\n");
        for (int i = 0; i < tables.size(); i++) {
            Element table = tables.get(i);
            // Use compact CSS selector-like format for better token efficiency
            analysis.append("  - ").append(table.tagName());
            String id = table.attr("id");
            if (!id.isEmpty()) analysis.append("#").append(id);
            // We don't usually need to show name for tables
            analysis.append("\n");

            Elements headers = table.select("th");
            Elements rows = table.select("tr");
            analysis.append("     rows: ").append(rows.size()).append(", columns: ").append(headers.size()).append("\n");
            if (!headers.isEmpty()) {
                analysis.append("     headers: [");
                headers.forEach(h -> {
                    String hText = h.text().trim();
                    if (!hText.isEmpty()) analysis.append('"').append(hText).append("\", ");
                });
                if (headers.size() > 0) {
                    // Remove the trailing ", "
                    int len = analysis.length();
                    if (len >= 2 && analysis.substring(len-2).equals(", ")) {
                        analysis.delete(len-2, len);
                    }
                }
                analysis.append("]\n");
            }
        }
        analysis.append("\n");
    }

    private void analyzeDropdowns(Document doc, StringBuilder analysis) {
        Elements selects = doc.select("select");
        if (selects.isEmpty()) return;

        analysis.append("=== DROPDOWNS (").append(selects.size()).append(") ===\n");
        for (Element select : selects) {
            Elements options = select.select("option");
            // Use compact CSS selector-like format for better token efficiency
            analysis.append("  - ").append(select.tagName());
            String id = select.attr("id");
            if (!id.isEmpty()) analysis.append("#").append(id);
            String name = select.attr("name");
            if (!name.isEmpty()) analysis.append("[name='").append(name).append("']");
            if (select.hasAttr("required")) analysis.append(",required");
            analysis.append(",options=").append(options.size()).append("\n");
            // List first 3 options (reduced from 5 to save tokens)
            int optCount = 0;
            for (Element opt : options) {
                if (optCount >= 3) break;
                analysis.append("    - ").append(opt.tagName());
                String value = opt.attr("value");
                if (!value.isEmpty()) analysis.append("[value=\"").append(value).append("\"]");
                String text = opt.text().trim();
                if (!text.isEmpty()) analysis.append("[text=\"").append(text).append("\"]");
                analysis.append("\n");
                optCount++;
            }
        }
        analysis.append("\n");
    }

    private void analyzeCheckboxesAndRadios(Document doc, StringBuilder analysis) {
        Elements checkboxes = doc.select("input[type=checkbox]");
        Elements radios = doc.select("input[type=radio]");

        if (!checkboxes.isEmpty()) {
            analysis.append("=== CHECKBOXES (").append(checkboxes.size()).append(") ===\n");
            for (Element cb : checkboxes) {
                // Use compact CSS selector-like format for better token efficiency
                analysis.append("  - ").append(cb.tagName()).append("[type=checkbox]");
                String id = cb.attr("id");
                if (!id.isEmpty()) analysis.append("#").append(id);
                String name = cb.attr("name");
                if (!name.isEmpty()) analysis.append("[name='").append(name).append("']");
                String value = cb.attr("value");
                if (!value.isEmpty()) analysis.append("[value=\"").append(value).append("\"]");
                if (cb.hasAttr("checked")) analysis.append(",checked");
                analysis.append("\n");
            }
            analysis.append("\n");
        }

        if (!radios.isEmpty()) {
            analysis.append("=== RADIO BUTTONS (").append(radios.size()).append(") ===\n");
            for (Element rb : radios) {
                // Use compact CSS selector-like format for better token efficiency
                analysis.append("  - ").append(rb.tagName()).append("[type=radio]");
                String id = rb.attr("id");
                if (!id.isEmpty()) analysis.append("#").append(id);
                String name = rb.attr("name");
                if (!name.isEmpty()) analysis.append("[name='").append(name).append("']");
                String value = rb.attr("value");
                if (!value.isEmpty()) analysis.append("[value=\"").append(value).append("\"]");
                analysis.append("\n");
            }
            analysis.append("\n");
        }
    }

    private void analyzeSearch(Document doc, StringBuilder analysis) {
        Elements searchInputs = doc.select(
                "input[type=search], input[placeholder*=search i], input[name*=search i], " +
                "input[id*=search i], [role=search]"
        );
        if (searchInputs.isEmpty()) return;

        analysis.append("=== SEARCH FUNCTIONALITY ===\n");
        for (Element search : searchInputs) {
            // Use compact CSS selector-like format for better token efficiency
            analysis.append("  - ").append(search.tagName());
            String id = search.attr("id");
            if (!id.isEmpty()) analysis.append("#").append(id);
            String placeholder = search.attr("placeholder");
            if (!placeholder.isEmpty()) analysis.append("(placeholder=\"").append(placeholder).append("\")");
            analysis.append("\n");
        }
        analysis.append("\n");
    }

    private void analyzeImages(Document doc, StringBuilder analysis) {
        Elements images = doc.select("img");
        analysis.append("=== IMAGES ===\n");
        analysis.append("  Total images: ").append(images.size()).append("\n");
        long missingAlt = images.stream().filter(img -> img.attr("alt").isEmpty()).count();
        analysis.append("  Missing alt text: ").append(missingAlt).append(" (accessibility concern)\n\n");
    }

    private void analyzeAlerts(Document doc, StringBuilder analysis) {
        Elements alerts = doc.select("[role=alert], .alert, .error, .warning, .success, .message");
        if (alerts.isEmpty()) return;

        analysis.append("=== ALERT/MESSAGE AREAS (").append(alerts.size()).append(") ===\n");
        for (Element alert : alerts) {
            // Use compact CSS selector-like format for better token efficiency
            analysis.append("  - ").append(alert.tagName());
            String id = alert.attr("id");
            if (!id.isEmpty()) analysis.append("#").append(id);
            String cls = alert.attr("class");
            if (!cls.isEmpty()) analysis.append("[class=").append(cls).append("]");
            analysis.append("\n");
        }
        analysis.append("\n");
    }

    private void analyzeModals(Document doc, StringBuilder analysis) {
        Elements modals = doc.select("[role=dialog], .modal, .popup, .overlay");
        if (modals.isEmpty()) return;

        analysis.append("=== MODALS/DIALOGS (").append(modals.size()).append(") ===\n");
        for (Element modal : modals) {
            // Use compact CSS selector-like format for better token efficiency
            analysis.append("  - ").append(modal.tagName());
            String id = modal.attr("id");
            if (!id.isEmpty()) analysis.append("#").append(id);
            String cls = modal.attr("class");
            if (!cls.isEmpty()) analysis.append("[class=").append(cls).append("]");
            analysis.append("\n");
        }
        analysis.append("\n");
    }

    private void analyzePageMeta(Document doc, StringBuilder analysis) {
        analysis.append("=== PAGE META ===\n");
        Elements metaDesc = doc.select("meta[name=description]");
        if (!metaDesc.isEmpty()) {
            analysis.append("  meta[name=description]=\"").append(metaDesc.first().attr("content")).append("\"\n");
        }
        Elements h1 = doc.select("h1");
        if (!h1.isEmpty()) {
            analysis.append("  h1: ").append(h1.first().text()).append("\n");
        }
        Elements h2 = doc.select("h2");
        analysis.append("  h2 count: ").append(h2.size()).append("\n");
        analysis.append("\n");
    }

    private WebDriver setupDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        if (config.isScraperHeadless()) {
            options.addArguments("--headless=new");
            System.out.println("🔇 Running in headless mode");
        } else {
            System.out.println("👁️  Running in visible mode");
        }

        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");
        options.addArguments("--disable-images");
        options.addArguments("--disable-background-timer-throttling");
        options.addArguments("--disable-backgrounding-occluded-windows");
        options.addArguments("--disable-renderer-backgrounding");

        return new ChromeDriver(options);
    }

    private void printSummary(String analysis, String url) {
        System.out.println(FrameworkConstants.LOG_SEPARATOR);
        System.out.println(FrameworkConstants.LOG_SUCCESS + " Scraping complete for: " + url);
        System.out.println("📄 Analysis length: " + analysis.length() + " characters");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);
    }

    // ===== SIMPLE STATIC METHOD (backward compatible) =====
    public static void scrapeWebsites(String url) {
        new SeleniumScraper().scrape(url);
    }
}