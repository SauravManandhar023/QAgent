package com.saurav.agentic.config;

import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.utils.ConfigReader;

/**
 * FrameworkConfig - Typed configuration object
 * Reads from ConfigReader and provides strongly-typed getters
 * Use this class everywhere instead of reading properties directly
 *
 */
public class FrameworkConfig {

    private static FrameworkConfig instance;
    private final ConfigReader reader;

    private FrameworkConfig() {
        this.reader = ConfigReader.getInstance();
    }

    public static FrameworkConfig getInstance() {
        if (instance == null) {
            instance = new FrameworkConfig();
        }
        return instance;
    }

    // ===== BROWSER =====

    public String getBrowser() {
        return reader.get("browser", FrameworkConstants.BROWSER_CHROME);
    }

    public boolean isHeadless() {
        return reader.getBoolean("headless");
    }

    // ===== GROQ AI =====

    public String getGroqApiKey() {
        // If using Ollama, we don't need a Groq API key
        if ("ollama".equalsIgnoreCase(getLlmProvider())) {
            return ""; // Return empty string - won't be used for Ollama requests
        }
        return reader.getGroqApiKey();
    }

    public String getGroqModel() {
        // Return appropriate model based on provider
        if ("ollama".equalsIgnoreCase(getLlmProvider())) {
            return reader.get("ollama.model", "llama3"); // Default Ollama model
        }
        return reader.get("groq.model", FrameworkConstants.GROQ_DEFAULT_MODEL);
    }

    public int getGroqMaxTokens() {
        return reader.getInt("groq.max.tokens");
    }

    public double getGroqTemperature() {
        return Double.parseDouble(reader.get("groq.temperature", "0.3"));
    }

    // ===== LLM PROVIDER CONFIGURATION =====

    /**
     * Get LLM provider: "groq" or "ollama"
     * Defaults to "groq" for backward compatibility
     */
    public String getLlmProvider() {
        return reader.get("llm.provider", "groq");
    }

    /**
     * Get Ollama base URL
     * Defaults to local Ollama instance
     */
    public String getOllamaBaseUrl() {
        return reader.get("ollama.base.url", "http://localhost:11434");
    }

    /**
     * Get FCC Admin base URL.
     * Defaults to http://localhost:8082
     */
    public String getFccBaseUrl() {
        return reader.get("fcc.base.url", "http://localhost:8082");
    }

    /**
     * Get FCC Admin API key.
     * Defaults to "freecc"
     */
    public String getFccApiKey() {
        return reader.get("fcc.api.key", "freecc");
    }

    // ===== BASE URL =====

    public String getBaseUrl() {
        return reader.get("base.url");
    }

    // ===== OUTPUT PATHS =====

    public String getUiExcelOutputPath() {
        return reader.get("output.excel.ui", FrameworkConstants.OUTPUT_EXCEL_UI);
    }
    
    public String getApiBaseUrl() {
        return reader.get("api.base.url", "https://automationexercise.com");
    }

    public String getApiExcelOutputPath() {
        return reader.get("output.excel.api", FrameworkConstants.OUTPUT_EXCEL_API);
    }

    public String getUiScriptsOutputPath() {
        return reader.get("output.scripts.ui", FrameworkConstants.GENERATED_UI_DIR);
    }

    public String getApiScriptsOutputPath() {
        return reader.get("output.scripts.api", FrameworkConstants.GENERATED_API_DIR);
    }

    public String getScreenshotsPath() {
        return reader.get("output.screenshots", FrameworkConstants.OUTPUT_SCREENSHOTS_DIR);
    }

    // ===== WAIT SETTINGS =====

    public int getImplicitWait() {
        return reader.getInt("implicit.wait");
    }

    public int getExplicitWait() {
        return reader.getInt("explicit.wait");
    }

    public int getPageLoadTimeout() {
        return reader.getInt("page.load.timeout");
    }

    public int getScraperWaitSeconds() {
        return reader.getInt("scraper.wait.seconds");
    }

    // ===== SCRAPER =====

    public boolean isScraperHeadless() {
        return reader.getBoolean("scraper.headless");
    }

    // ===== PRINT CONFIG (for debugging) =====

    // ===== PIPELINE MODE =====

    /**
     * Whether the pipeline should pause for human validation between Agent 1 and Agent 2.
     * Set pipeline.interactive=false in config.properties for CI/CD mode.
     * Defaults to true for local development.
     */
    /**
     * Direct access to ConfigReader for ad-hoc properties not covered by typed getters.
     */
    public ConfigReader getConfigReader() {
        return reader;
    }

    public boolean isInteractive() {
        return reader.getBoolean("pipeline.interactive", true);
    }

    // ===== PRINT CONFIG (for debugging) =====

    public void printConfig() {
        System.out.println("\n📋 FRAMEWORK CONFIGURATION:");
        System.out.println(FrameworkConstants.LOG_SEPARATOR);
        System.out.println("  Browser      : " + getBrowser());
        System.out.println("  Headless     : " + isHeadless());
        System.out.println("  Base URL     : " + getBaseUrl());
        System.out.println("  LLM Provider : " + getLlmProvider());
        String provider = getLlmProvider();
        if ("ollama".equalsIgnoreCase(provider)) {
            System.out.println("  Ollama URL   : " + getOllamaBaseUrl());
            System.out.println("  Ollama Model : " + getGroqModel());
        } else if ("fcc".equalsIgnoreCase(provider)) {
            System.out.println("  FCC Admin URL: " + getFccBaseUrl());
            System.out.println("  FCC API Key  : " + (getFccApiKey().isEmpty() ? "(none)" : "****"));
        } else {
            System.out.println("  Groq Model   : " + getGroqModel());
            System.out.println("  Groq Tokens  : " + getGroqMaxTokens());
        }
        System.out.println("  UI Excel     : " + getUiExcelOutputPath());
        System.out.println("  UI Scripts   : " + getUiScriptsOutputPath());
        System.out.println("  Explicit Wait: " + getExplicitWait() + "s");
        System.out.println("  Interactive  : " + isInteractive());
        System.out.println(FrameworkConstants.LOG_SEPARATOR + "\n");
    }
    
}
