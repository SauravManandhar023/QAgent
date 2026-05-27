package com.saurav.agentic.runners;

import com.saurav.agentic.agents.ScriptGeneratorAgent;
import com.saurav.agentic.agents.TestCaseGeneratorAgent;
import com.saurav.agentic.config.FrameworkConfig;
import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.TestCase;

import com.saurav.agentic.agents.Agent3_Compiler;
import com.saurav.agentic.models.CompileResult;

import com.saurav.agentic.agents.Agent4_Reviewer;

import java.util.List;


public class Main {

    public static void main(String[] args) throws Exception {

        FrameworkConfig config = FrameworkConfig.getInstance();
        config.printConfig();

        String url = config.getBaseUrl();

        // ── Agent 1 ──────────────────────────────────────────────
        System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
        System.out.println(FrameworkConstants.LOG_INFO + " Starting Agent 1...");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);

        TestCaseGeneratorAgent agent1 = new TestCaseGeneratorAgent();
        List<TestCase> testCases = agent1.run(url);

        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Agent 1 complete! " + testCases.size() + " test cases generated.");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Excel: " + config.getUiExcelOutputPath());

        // ── Agent 2 ──────────────────────────────────────────────
        System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
        System.out.println(FrameworkConstants.LOG_INFO + " Starting Agent 2...");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);

        String pageAnalysis = agent1.getLastPageAnalysis();
        String excelPath    = config.getUiExcelOutputPath();

        ScriptGeneratorAgent agent2 = new ScriptGeneratorAgent();
        agent2.run(excelPath, url, pageAnalysis);

        // ── Done ─────────────────────────────────────────────────
        System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
        System.out.println(FrameworkConstants.LOG_SUCCESS + " QAgent pipeline complete!");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Test classes : src/test/java/generated/ui/");
        System.out.println(FrameworkConstants.LOG_INFO +
                " POM classes  : src/test/java/pages/");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);
        
    	// ── Agent 3 — Compile Verification ───────────────────────────────
        System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
        System.out.println(FrameworkConstants.LOG_INFO + " Starting Agent 3...");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);

        Agent3_Compiler agent3 = new Agent3_Compiler();
        List<CompileResult> compileResults = agent3.run();

        long failedCount = compileResults.stream()
                .filter(r -> !r.isSuccess()).count();

        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Agent 3 complete! Failed: " + failedCount + "/" +
                compileResults.size() + " files");
        
     // ── Agent 4 — Fix & Review ────────────────────────────────────────
        System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
        System.out.println(FrameworkConstants.LOG_INFO + " Starting Agent 4...");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);

        Agent4_Reviewer agent4 = new Agent4_Reviewer();
        int fixedCount = agent4.run(compileResults);

        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Agent 4 complete! Fixed: " + fixedCount +
                " files");
    }
}