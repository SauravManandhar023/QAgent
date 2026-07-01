package com.saurav.agentic.llm;

import com.saurav.agentic.config.FrameworkConfig;
import com.saurav.agentic.constants.FrameworkConstants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProviderFactory — Creates and caches LLMProvider instances based on configuration.
 *
 * Selects the provider based on the "llm.provider" config property.
 * Supported values: "groq", "ollama", "claude", "openai", "gemini", "nim",
 *                    "deepseek", "fcc"
 *
 * Adding a new provider requires:
 * 1. Implement LLMProvider interface
 * 2. Register it in this factory's switch statement
 * 3. No agent code changes needed
 */
public class ProviderFactory {

    private static final Map<String, LLMProvider> providerCache = new ConcurrentHashMap<>();

    private ProviderFactory() {}

    /**
     * Get the currently configured provider based on llm.provider in config.properties.
     * Providers are cached once created.
     */
    public static LLMProvider getProvider() {
        String providerName = FrameworkConfig.getInstance().getLlmProvider();
        return getProvider(providerName);
    }

    /**
     * Get a specific provider by name. Providers are cached once created.
     *
     * @param providerName "groq", "ollama", "claude", "openai", "gemini", "nim", "deepseek", "fcc"
     */
    public static LLMProvider getProvider(String providerName) {
        return providerCache.computeIfAbsent(providerName.toLowerCase(), key -> {
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Initializing LLM provider: " + key);

            return switch (key) {
                case "groq" -> new GroqProvider();
                case "ollama" -> new OllamaProvider();
                case "claude" -> new ClaudeProvider();
                case "openai" -> new OpenAIProvider();
                case "gemini" -> new GeminiProvider();
                case "nim" -> new NvidiaNimProvider();
                case "deepseek" -> new DeepSeekProvider();
                case "fcc" -> new FCCProvider();
                default -> throw new IllegalArgumentException(
                        FrameworkConstants.LOG_ERROR +
                        " Unknown LLM provider: '" + key + "'. " +
                        "Supported: groq, ollama, claude, openai, gemini, nim, deepseek, fcc"
                );
            };
        });
    }

    /**
     * @return list of all supported provider names
     */
    public static String[] getSupportedProviders() {
        return new String[]{"groq", "ollama", "claude", "openai", "gemini", "nim", "deepseek", "fcc"};
    }

    /**
     * Reset provider cache (useful for testing or config reload).
     */
    public static void reset() {
        providerCache.clear();
    }
}
