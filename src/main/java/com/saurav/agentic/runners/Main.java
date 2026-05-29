package com.saurav.agentic.runners;

import com.saurav.agentic.agents.Agent3_Compiler;
import com.saurav.agentic.agents.Agent4_Reviewer;
import com.saurav.agentic.agents.ScriptGeneratorAgent;
import com.saurav.agentic.agents.TestCaseGeneratorAgent;
import com.saurav.agentic.config.FrameworkConfig;
import com.saurav.agentic.config.ModelConfig;
import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.CompileResult;
import com.saurav.agentic.scraper.PageMetadataExtractor;
import com.saurav.agentic.models.PageMetadata;

import java.io.File;
import java.util.List;

public class Main {

    // Set to true to skip Agent 1 and reuse existing Excel
    // Useful when testing Agent 2/3/4 changes without re-scraping
    // WARNING: Make sure Excel matches current URL before enabling
    private static final boolean SKIP_AGENT1 = false;

    public static void main(String[] args) throws Exception {

        FrameworkConfig config = FrameworkConfig.getInstance();
        config.printConfig();
        ModelConfig.getInstance().printModelConfig();

        String url       = config.getBaseUrl();
        String excelPath = config.getUiExcelOutputPath();
        String pageAnalysis = "";

        // Smart skip — only skip if manual flag set AND excel exists
        boolean shouldSkip = SKIP_AGENT1 && new File(excelPath).exists();

        // ── Agent 1 ──────────────────────────────────────────────────────────
        if (!shouldSkip) {
            System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
            System.out.println(FrameworkConstants.LOG_INFO + " Starting Agent 1...");
            System.out.println(FrameworkConstants.LOG_SEPARATOR);

            TestCaseGeneratorAgent agent1 = new TestCaseGeneratorAgent();
            agent1.run(url);
            PageMetadataExtractor extractor = new PageMetadataExtractor();
            PageMetadata metadata = extractor.extract(url);
            pageAnalysis = metadata.toPromptString();

            System.out.println(FrameworkConstants.LOG_INFO +
                    " Page type detected: " + metadata.getPageType());
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Elements found    : " + metadata.getAllElements().size());

            System.out.println(FrameworkConstants.LOG_SUCCESS +
                    " Agent 1 complete!");
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Excel: " + excelPath);

        } else {
            System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
            System.out.println(FrameworkConstants.LOG_INFO +
                    " [SKIP] Agent 1 skipped — reusing existing Excel: " + excelPath);
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " Make sure Excel matches current URL: " + url);
            System.out.println(FrameworkConstants.LOG_SEPARATOR);

            PageMetadataExtractor extractor = new PageMetadataExtractor();
            PageMetadata metadata = extractor.extract(url);
            pageAnalysis = metadata.toPromptString();
        }

        // ── Agent 2 ──────────────────────────────────────────────────────────
        System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
        System.out.println(FrameworkConstants.LOG_INFO + " Starting Agent 2...");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);

        ScriptGeneratorAgent agent2 = new ScriptGeneratorAgent();
        agent2.run(excelPath, url, pageAnalysis);

        // ── Agent 3 ──────────────────────────────────────────────────────────
        System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
        System.out.println(FrameworkConstants.LOG_INFO + " Starting Agent 3...");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);

        Agent3_Compiler agent3 = new Agent3_Compiler();
        List<CompileResult> compileResults = agent3.run();

        long failedCount = compileResults.stream()
                .filter(r -> !r.isSuccess()).count();

        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Agent 3 complete! Failed: " + failedCount +
                "/" + compileResults.size() + " files");

        // ── Agent 4 ──────────────────────────────────────────────────────────
        System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
        System.out.println(FrameworkConstants.LOG_INFO + " Starting Agent 4...");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);

        Agent4_Reviewer agent4 = new Agent4_Reviewer();
        int fixedCount = agent4.run(compileResults);

        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Agent 4 complete! Fixed: " + fixedCount + " files");

        // ── Done ─────────────────────────────────────────────────────────────
        System.out.println("\n" + FrameworkConstants.LOG_SEPARATOR);
        System.out.println(FrameworkConstants.LOG_SUCCESS + " QAgent pipeline complete!");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Test classes : src/test/java/generated/ui/");
        System.out.println(FrameworkConstants.LOG_INFO +
                " POM classes  : src/test/java/pages/");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);
    }
}