package com.saurav.agentic.prompts.modules;

/**
 * ProjectConfig - Project-specific version and structure rules
 * Update this when dependencies change
 */
public class ProjectConfig {

    private ProjectConfig() {}

    public static String get() {
        return getBase() + getPomImports();
    }

    public static String getBase() {
        return """
                PROJECT CONFIGURATION:
                - Java             : 21
                - Selenium         : 4.44.0
                - TestNG           : 7.12.0
                - WebDriverManager : 6.3.4
                - Allure           : 2.27.0

                PACKAGE STRUCTURE:
                - Test classes : package generated.ui;
                - POM classes  : package pages;

                DRIVER SETUP:
                - ALWAYS use: driver = WebDriverManager.chromedriver().create();
                - NEVER use:  WebDriverManager.chromedriver().setup(); driver = new ChromeDriver();

                SEVERITY LEVELS (use exactly these):
                - High priority   → @Severity(SeverityLevel.CRITICAL)
                - Medium priority → @Severity(SeverityLevel.NORMAL)
                - Low priority    → @Severity(SeverityLevel.MINOR)
                - NEVER use SeverityLevel.MEDIUM, HIGH, or LOW — they do not exist
                """;
    }

    public static String getPomImports() {
        return """
                AVAILABLE IMPORTS FOR POM CLASSES (pick only what your code uses):
                import org.openqa.selenium.WebDriver;
                import org.openqa.selenium.WebElement;
                import org.openqa.selenium.By;
                import org.openqa.selenium.support.FindBy;
                import org.openqa.selenium.support.PageFactory;
                import org.openqa.selenium.support.ui.WebDriverWait;
                import org.openqa.selenium.support.ui.ExpectedConditions;
                import java.time.Duration;
                import org.openqa.selenium.JavascriptExecutor;
                IMPORT RULE: only include imports actually used in your code.
                """;
    }

    public static String getTestImports() {
        return """
                AVAILABLE IMPORTS FOR TEST CLASSES (pick only what your code uses):
                import org.openqa.selenium.WebDriver;
                import org.openqa.selenium.WebElement;
                import org.openqa.selenium.By;
                import org.openqa.selenium.support.ui.WebDriverWait;
                import org.openqa.selenium.support.ui.ExpectedConditions;
                import java.time.Duration;
                import io.github.bonigarcia.wdm.WebDriverManager;
                import org.testng.Assert;
                import org.testng.annotations.AfterMethod;
                import org.testng.annotations.BeforeMethod;
                import org.testng.annotations.Test;
                import io.qameta.allure.Description;
                import io.qameta.allure.Severity;
                import io.qameta.allure.SeverityLevel;
                import pages.*;
                IMPORT RULE: only include imports actually used in your code.
                """;
    }
}