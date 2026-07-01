package com.saurav.agentic.config;

import com.saurav.agentic.constants.FrameworkConstants;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;

/**
 * DriverFactory - Creates and manages WebDriver instances
 * Supports Chrome, Firefox, Edge
 * Reads browser and headless settings from FrameworkConfig
 *
 */
public class DriverFactory {

    // ThreadLocal ensures each thread gets its own WebDriver (parallel execution safe)
    private static final ThreadLocal<WebDriver> driverThread = new ThreadLocal<>();
    private static final FrameworkConfig config = FrameworkConfig.getInstance();

    private DriverFactory() {}

    /**
     * Initialize WebDriver based on config settings
     */
    public static WebDriver initDriver() {
        String browser = config.getBrowser().toLowerCase();
        boolean headless = config.isHeadless();

        System.out.println(FrameworkConstants.LOG_SUCCESS + " Initializing " + browser +
                " driver (headless: " + headless + ")");

        WebDriver driver = switch (browser) {
            case FrameworkConstants.BROWSER_FIREFOX -> createFirefoxDriver(headless);
            case FrameworkConstants.BROWSER_EDGE -> createEdgeDriver(headless);
            default -> createChromeDriver(headless);
        };

        // Apply common settings
        driver.manage().window().maximize();
//        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(config.getImplicitWait()));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(config.getPageLoadTimeout()));
        // Script timeout is only needed for executeAsyncScript(), which we don't use
        // We only use executeScript() (synchronous JS) in our code

        driverThread.set(driver);
        System.out.println(FrameworkConstants.LOG_SUCCESS + " Driver initialized successfully");
        return driver;
    }

    /**
     * Get current thread's WebDriver
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverThread.get();
        if (driver == null) {
            throw new RuntimeException(
                FrameworkConstants.LOG_ERROR + " WebDriver not initialized! Call initDriver() first."
            );
        }
        return driver;
    }

    /**
     * Quit driver and clean up
     */
    public static void quitDriver() {
        WebDriver driver = driverThread.get();
        if (driver != null) {
            driver.quit();
            driverThread.remove();
            System.out.println(FrameworkConstants.LOG_SUCCESS + " Driver closed successfully");
        }
    }

    // ===== BROWSER-SPECIFIC SETUP =====

    private static WebDriver createChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");
        return new ChromeDriver(options);
    }

    private static WebDriver createFirefoxDriver(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("--headless");
        }
        return new FirefoxDriver(options);
    }

    private static WebDriver createEdgeDriver(boolean headless) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions options = new EdgeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        return new EdgeDriver(options);
    }
}
