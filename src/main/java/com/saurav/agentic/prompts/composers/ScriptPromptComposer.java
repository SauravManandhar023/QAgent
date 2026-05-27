package com.saurav.agentic.prompts.composers;

import com.saurav.agentic.prompts.modules.AssertionRules;
import com.saurav.agentic.prompts.modules.CoreRules;
import com.saurav.agentic.prompts.modules.ProjectConfig;
import com.saurav.agentic.prompts.modules.RecoveryRules;
import com.saurav.agentic.prompts.modules.SeleniumRules;

/**
 * ScriptPromptComposer - Composes prompts for TestNG test class generation
 * Uses only relevant modules — no unnecessary rules
 */
public class ScriptPromptComposer {

    private ScriptPromptComposer() {}

    public static String systemPrompt() {
        return """
                You are a senior Selenium automation engineer.
                Generate a complete TestNG test class for the given component.
                Respond with PURE Java code only — no explanation, no markdown, no backticks.
                Start your response directly with: package generated.ui;
                
                """
                + ProjectConfig.getBase() + "\n"
                + ProjectConfig.getTestImports() + "\n"
                + SeleniumRules.get() + "\n"
                + AssertionRules.get() + "\n"
                + CoreRules.get() + "\n"
                + RecoveryRules.get();
    }

    public static String userPrompt(String component, String className,
                                     String pageUrl, String testCasesText,
                                     String pomCode, String pageMetadata) {
        return """
                Generate a complete Selenium TestNG test class for the '%s' component.
                
                Target URL: %s
                Class Name: %s
                
                PAGE BEHAVIOR (read before writing assertions):
                %s
                
                THE PAGE OBJECT CLASS — use ONLY methods defined here:
                %s
                
                Test cases to implement:
                %s
                
                Requirements:
                - Each test case becomes one @Test method
                - ONLY call methods that exist in the Page Object above
                - If a method is missing — add TODO comment, do not invent
                - Declare WebDriverWait as class field, initialize in @BeforeMethod
                - Always wait before asserting
                - Add @Description and @Severity to every @Test
                - Remove all unused imports
                
                Start response with: package generated.ui;
                """.formatted(component, pageUrl, className,
                              pageMetadata, pomCode, testCasesText);
    }
}