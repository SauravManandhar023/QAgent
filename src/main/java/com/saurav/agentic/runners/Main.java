package com.saurav.agentic.runners;

import com.saurav.agentic.agents.TestCaseGeneratorAgent;
import com.saurav.agentic.config.FrameworkConfig;
import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.TestCase;

import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {

        // Load config
        FrameworkConfig config = FrameworkConfig.getInstance();
        config.printConfig();

        // Get URL
        String url = config.getBaseUrl();

        // Run Agent 1
        TestCaseGeneratorAgent agent1 = new TestCaseGeneratorAgent();
        List<TestCase> testCases = agent1.run(url);

        System.out.println("\n" + FrameworkConstants.LOG_SUCCESS +
                " Agent 1 complete! Total: " + testCases.size() + " test cases generated.");
        System.out.println("Check Excel at: " + config.getUiExcelOutputPath());
    }
}