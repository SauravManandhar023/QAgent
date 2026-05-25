package base;

import com.saurav.agentic.config.DriverFactory;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class BaseTest {

    protected WebDriver driver;

    @BeforeMethod
    public void setUp() {
        driver = DriverFactory.initDriver();
        // browser + maximize + timeouts all handled inside DriverFactory
    }

    @AfterMethod
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}