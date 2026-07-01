package com.saurav.agentic.llm;

import com.google.gson.JsonObject;
import com.saurav.agentic.constants.FrameworkConstants;

import java.io.IOException;
import java.util.Map;

/**
 * GeminiProvider — LLM provider implementation for Google Gemini API.
 *
 * Uses the Gemini API endpoint with its native tool/function-calling format.
 *
 * STUB: This provider is registered but not yet fully implemented.
 */
public class GeminiProvider implements LLMProvider {

    public GeminiProvider() {
        System.out.println(FrameworkConstants.LOG_INFO +
                " GeminiProvider initialized (stub)");
    }

    @Override
    public String chat(String systemPrompt, String userPrompt,
                       String model, double temperature, int maxTokens) throws IOException {
        throw new IOException(
                FrameworkConstants.LOG_ERROR + " GeminiProvider not yet implemented."
        );
    }

    @Override
    public JsonObject chatWithTools(
            String toolName, String toolDescription,
            Map<String, Object> parameters,
            String systemPrompt, String userPrompt,
            String model, double temperature, int maxTokens) throws IOException {
        throw new IOException(
                FrameworkConstants.LOG_ERROR + " GeminiProvider not yet implemented."
        );
    }

    @Override
    public boolean supportsToolCalling() { return true; }

    @Override
    public boolean supportsStreaming() { return true; }

    @Override
    public boolean supportsVision() { return true; }

    @Override
    public String getProviderName() { return "gemini"; }
}
