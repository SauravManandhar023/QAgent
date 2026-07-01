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
            analysis.append("Form ").append(i + 1).append(":\n");
            analysis.append("  id=").append(form.attr("id")).append("\n");
            analysis.append("  name=").append(form.attr("name")).append("\n");
            analysis.append("  action=").append(form.attr("action")).append("\n");
            analysis.append("  method=").append(form.attr("method")).append("\n");

            Elements formInputs = form.select("input, textarea, select");
            analysis.append("  fields=").append(formInputs.size()).append("\n");
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

            analysis.append("  - type=").append(type)
                    .append(", id=").append(input.attr("id"))
                    .append(", name=").append(input.attr("name"))
                    .append(", placeholder=").append(input.attr("placeholder"))
                    .append(", required=").append(input.hasAttr("required"))
                    .append(", maxlength=").append(input.attr("maxlength"))
                    .append(", pattern=").append(input.attr("pattern"))
                    .append(", readonly=").append(input.hasAttr("readonly"))
                    .append(", disabled=").append(input.hasAttr("disabled"))
                    .append("\n");
        }
        analysis.append("\n");
    }

    private void analyzeButtons(Document doc, StringBuilder analysis) {
        Elements buttons = doc.select("button, input[type=submit], input[type=button], [role=button]");
        if (buttons.isEmpty()) return;

        analysis.append("=== BUTTONS (").append(buttons.size()).append(") ===\n");
        for (Element btn : buttons) {
            analysis.append("  - text='").append(btn.text().trim())
                    .append("', id=").append(btn.attr("id"))
                    .append(", type=").append(btn.attr("type"))
                    .append(", class=").append(btn.attr("class"))
                    .append(", disabled=").append(btn.hasAttr("disabled"))
                    .append("\n");
        }
        analysis.append("\n");
    }

    private void analyzeLinks(Document doc, StringBuilder analysis) {
        Elements links = doc.select("a[href]");
        if (links.isEmpty()) return;

        analysis.append("=== LINKS (").append(links.size()).append(") ===\n");
        int count = 0;
        for (Element link : links) {
            if (count >= 10) {
                analysis.append("  ... and ").append(links.size() - 10).append(" more\n");
                break;
            }
            analysis.append("  - text='").append(link.text().trim())
                    .append("', href=").append(link.attr("href"))
                    .append("\n");
            count++;
        }
        analysis.append("\n");
    }

    private void analyzeNavigation(Document doc, StringBuilder analysis) {
        Elements navLinks = doc.select("nav a, .nav a, .navbar a, .menu a, header a");
        if (navLinks.isEmpty()) return;

        analysis.append("=== NAVIGATION (").append(navLinks.size()).append(" items) ===\n");
        for (Element link : navLinks) {
            analysis.append("  - text='").append(link.text().trim())
                    .append("', href=").append(link.attr("href"))
                    .append("\n");
        }
        analysis.append("\n");
    }

    private void analyzeTables(Document doc, StringBuilder analysis) {
        Elements tables = doc.select("table");
        if (tables.isEmpty()) return;

        analysis.append("=== TABLES (").append(tables.size()).append(") ===\n");
        for (int i = 0; i < tables.size(); i++) {
            Element table = tables.get(i);
            Elements headers = table.select("th");
            Elements rows = table.select("tr");
            analysis.append("  Table ").append(i + 1).append(":\n");
            analysis.append("    columns=").append(headers.size())
                    .append(", rows=").append(rows.size()).append("\n");
            if (!headers.isEmpty()) {
                analysis.append("    headers: ");
                headers.forEach(h -> analysis.append(h.text()).append(", "));
                analysis.append("\n");
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
            analysis.append("  - id=").append(select.attr("id"))
                    .append(", name=").append(select.attr("name"))
                    .append(", options=").append(options.size())
                    .append(", required=").append(select.hasAttr("required"))
                    .append("\n");
            // List first 5 options
            int optCount = 0;
            for (Element opt : options) {
                if (optCount >= 5) break;
                analysis.append("    option: value='").append(opt.attr("value"))
                        .append("', text='").append(opt.text()).append("'\n");
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
                analysis.append("  - id=").append(cb.attr("id"))
                        .append(", name=").append(cb.attr("name"))
                        .append(", value=").append(cb.attr("value"))
                        .append(", checked=").append(cb.hasAttr("checked"))
                        .append("\n");
            }
            analysis.append("\n");
        }

        if (!radios.isEmpty()) {
            analysis.append("=== RADIO BUTTONS (").append(radios.size()).append(") ===\n");
            for (Element rb : radios) {
                analysis.append("  - id=").append(rb.attr("id"))
                        .append(", name=").append(rb.attr("name"))
                        .append(", value=").append(rb.attr("value"))
                        .append("\n");
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
            analysis.append("  - id=").append(search.attr("id"))
                    .append(", placeholder=").append(search.attr("placeholder"))
                    .append("\n");
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
            analysis.append("  - class=").append(alert.attr("class"))
                    .append(", id=").append(alert.attr("id")).append("\n");
        }
        analysis.append("\n");
    }

    private void analyzeModals(Document doc, StringBuilder analysis) {
        Elements modals = doc.select("[role=dialog], .modal, .popup, .overlay");
        if (modals.isEmpty()) return;

        analysis.append("=== MODALS/DIALOGS (").append(modals.size()).append(") ===\n");
        for (Element modal : modals) {
            analysis.append("  - id=").append(modal.attr("id"))
                    .append(", class=").append(modal.attr("class")).append("\n");
        }
        analysis.append("\n");
    }

    private void analyzePageMeta(Document doc, StringBuilder analysis) {
        analysis.append("=== PAGE META ===\n");
        Elements metaDesc = doc.select("meta[name=description]");
        if (!metaDesc.isEmpty()) {
            analysis.append("  Description: ").append(metaDesc.first().attr("content")).append("\n");
        }
        Elements h1 = doc.select("h1");
        if (!h1.isEmpty()) {
            analysis.append("  H1: ").append(h1.first().text()).append("\n");
        }
        Elements h2 = doc.select("h2");
        analysis.append("  H2 count: ").append(h2.size()).append("\n");
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