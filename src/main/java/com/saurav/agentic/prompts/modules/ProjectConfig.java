package com.saurav.agentic.prompts.modules;

/**
 * ProjectConfig - Project-specific version and structure rules
 * Update this when dependencies change
 */
public class ProjectConfig {

    private ProjectConfig() {}

    public static String get() {
        return """
                PROJECT CONFIGURATION (strictly follow these versions):
                - Java          : 21
                - Selenium      : 4.44.0
                - TestNG        : 7.12.0
                - WebDriverManager : 6.3.4
                - Allure        : 2.27.0
                
                PACKAGE STRUCTURE:
                - Test classes  : package generated.ui;
                - POM classes   : package pages;
                - Base classes  : package base;
                
                DRIVER SETUP:
                - ALWAYS use: driver = WebDriverManager.chromedriver().create();
                - NEVER use:  WebDriverManager.chromedriver().setup(); driver = new ChromeDriver();
                
                REQUIRED IMPORTS FOR TEST CLASSES:
                import org.testng.Assert;
                import org.testng.annotations.AfterMethod;
                import org.testng.annotations.BeforeMethod;
                import org.testng.annotations.Test;
                import io.qameta.allure.Description;
                import io.qameta.allure.Severity;
                import io.qameta.allure.SeverityLevel;
                import io.github.bonigarcia.wdm.WebDriverManager;
                import org.openqa.selenium.WebDriver;
                import org.openqa.selenium.support.ui.WebDriverWait;
                import org.openqa.selenium.support.ui.ExpectedConditions;
                import java.time.Duration;
                
                REQUIRED IMPORTS FOR POM CLASSES:
                import org.openqa.selenium.WebDriver;
                import org.openqa.selenium.WebElement;
                import org.openqa.selenium.support.FindBy;
                import org.openqa.selenium.support.PageFactory;
                
                Remove any import that is not actually used in the code.
                """;
    }
}