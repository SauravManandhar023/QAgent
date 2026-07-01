package com.saurav.agentic.config;

import com.saurav.agentic.utils.ConfigReader;

/**
 * ModelConfig - Per-agent model selection
 *
 * Agent 1 uses heavy 70B model for reasoning quality
 * Agent 2 uses fast 8B model for boilerplate code generation
 * Agent 3/4 use fast 8B model for compilation/review tasks
 *
 * Models can be overridden in config.local.properties
 */
public class ModelConfig {

    private static ModelConfig instance;
    private final ConfigReader configReader;

    // ── Default Models ────────────────────────────────────────────────────────

    // Heavy model — best reasoning, used for test case generation
    private static final String DEFAULT_AGENT1_MODEL = "llama-3.3-70b-versatile";

    // Fast model — best for code generation, much higher rate limits
    private static final String DEFAULT_AGENT2_MODEL = "llama-3.1-8b-instant";

    // Fast model — used for compilation feedback and review
    private static final String DEFAULT_AGENT3_MODEL = "llama-3.1-8b-instant";
    private static final String DEFAULT_AGENT4_MODEL = "llama-3.1-8b-instant";

    // ── Temperature per agent ─────────────────────────────────────────────────

    // Low temperature = consistent, deterministic output
    private static final double DEFAULT_AGENT1_TEMPERATURE = 0.2;
    private static final double DEFAULT_AGENT2_TEMPERATURE = 0.1;
    private static final double DEFAULT_AGENT3_TEMPERATURE = 0.1;
    private static final double DEFAULT_AGENT4_TEMPERATURE = 0.1;

    // ── Max tokens per agent ──────────────────────────────────────────────────

    // Agent 1 needs more tokens for test case JSON
    private static final int DEFAULT_AGENT1_MAX_TOKENS = 4096;

    // Agent 2 needs more tokens for full Java class generation
    private static final int DEFAULT_AGENT2_MAX_TOKENS = 4096;

    // Agent 3/4 need fewer tokens — just errors and feedback
    private static final int DEFAULT_AGENT3_MAX_TOKENS = 2048;
    private static final int DEFAULT_AGENT4_MAX_TOKENS = 2048;

    private ModelConfig() {
        this.configReader = ConfigReader.getInstance();
    }

    public static ModelConfig getInstance() {
        if (instance == null) {
            instance = new ModelConfig();
        }
        return instance;
    }

    // ── Agent 1 ───────────────────────────────────────────────────────────────

    public String getAgent1Model() {
        return configReader.get("agent1.model", DEFAULT_AGENT1_MODEL);
    }

    public double getAgent1Temperature() {
        String val = configReader.get("agent1.temperature", "");
        return val.isEmpty() ? DEFAULT_AGENT1_TEMPERATURE : Double.parseDouble(val);
    }

    public int getAgent1MaxTokens() {
        String val = configReader.get("agent1.max.tokens", "");
        return val.isEmpty() ? DEFAULT_AGENT1_MAX_TOKENS : Integer.parseInt(val);
    }

    // ── Agent 2 ───────────────────────────────────────────────────────────────

    public String getAgent2Model() {
        return configReader.get("agent2.model", DEFAULT_AGENT2_MODEL);
    }

    public double getAgent2Temperature() {
        String val = configReader.get("agent2.temperature", "");
        return val.isEmpty() ? DEFAULT_AGENT2_TEMPERATURE : Double.parseDouble(val);
    }

    public int getAgent2MaxTokens() {
        String val = configReader.get("agent2.max.tokens", "");
        return val.isEmpty() ? DEFAULT_AGENT2_MAX_TOKENS : Integer.parseInt(val);
    }

    // ── Agent 3 ───────────────────────────────────────────────────────────────

    public String getAgent3Model() {
        return configReader.get("agent3.model", DEFAULT_AGENT3_MODEL);
    }

    public double getAgent3Temperature() {
        String val = configReader.get("agent3.temperature", "");
        return val.isEmpty() ? DEFAULT_AGENT3_TEMPERATURE : Double.parseDouble(val);
    }

    public int getAgent3MaxTokens() {
        String val = configReader.get("agent3.max.tokens", "");
        return val.isEmpty() ? DEFAULT_AGENT3_MAX_TOKENS : Integer.parseInt(val);
    }

    // ── Agent 4 ───────────────────────────────────────────────────────────────

    public String getAgent4Model() {
        return configReader.get("agent4.model", DEFAULT_AGENT4_MODEL);
    }

    public double getAgent4Temperature() {
        String val = configReader.get("agent4.temperature", "");
        return val.isEmpty() ? DEFAULT_AGENT4_TEMPERATURE : Double.parseDouble(val);
    }

    public int getAgent4MaxTokens() {
        String val = configReader.get("agent4.max.tokens", "");
        return val.isEmpty() ? DEFAULT_AGENT4_MAX_TOKENS : Integer.parseInt(val);
    }
    
    public String getAgent2PomModel() {
        return configReader.get("agent2.pom.model", "llama-3.1-8b-instant");
    }

    public String getAgent2TestModel() {
        return configReader.get("agent2.test.model", "llama-3.3-70b-versatile");
    }

    // ── Debug ─────────────────────────────────────────────────────────────────

    public void printModelConfig() {
        System.out.println("\n[INFO] MODEL CONFIGURATION:");
        System.out.println("  Agent 1 : " + getAgent1Model() +
                " (temp=" + getAgent1Temperature() +
                ", tokens=" + getAgent1MaxTokens() + ")");
        System.out.println("  Agent 2 POM : " + getAgent2PomModel() +
                " (temp=" + getAgent2Temperature() +
                ", tokens=" + getAgent2MaxTokens() + ")");
        System.out.println("  Agent 2 Test: " + getAgent2TestModel() +
                " (temp=" + getAgent2Temperature() +
                ", tokens=" + getAgent2MaxTokens() + ")");
        System.out.println("  Agent 3 : " + getAgent3Model() +
                " (temp=" + getAgent3Temperature() +
                ", tokens=" + getAgent3MaxTokens() + ")");
        System.out.println("  Agent 4 : " + getAgent4Model() +
                " (temp=" + getAgent4Temperature() +
                ", tokens=" + getAgent4MaxTokens() + ")");
    }
}