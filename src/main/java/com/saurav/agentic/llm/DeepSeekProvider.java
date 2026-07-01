package com.saurav.agentic.llm;

import com.google.gson.JsonObject;
import com.saurav.agentic.constants.FrameworkConstants;

import java.io.IOException;
import java.util.Map;

/**
 * DeepSeekProvider — LLM provider implementation for DeepSeek API.
 *
 * Uses the OpenAI-compatible chat completions endpoint.
 * Supports tool/function-calling in the OpenAI-compatible format.
 *
 * STUB: This provider is registered but not yet fully implemented.
 */
public class DeepSeekProvider implements LLMProvider {

    public DeepSeekProvider() {
        System.out.println(FrameworkConstants.LOG_INFO +
                " DeepSeekProvider initialized (stub)");
    }

    @Override
    public String chat(String systemPrompt, String userPrompt,
                       String model, double temperature, int maxTokens) throws IOException {
        throw new IOException(
                FrameworkConstants.LOG_ERROR + " DeepSeekProvider not yet implemented."
        );
    }

    @Override
    public JsonObject chatWithTools(
            String toolName, String toolDescription,
            Map<String, Object> parameters,
            String systemPrompt, String userPrompt,
            String model, double temperature, int maxTokens) throws IOException {
        throw new IOException(
                FrameworkConstants.LOG_ERROR + " DeepSeekProvider not yet implemented."
        );
    }

    @Override
    public boolean supportsToolCalling() { return true; }

    @Override
    public boolean supportsStreaming() { return true; }

    @Override
    public boolean supportsVision() { return false; }

    @Override
    public String getProviderName() { return "deepseek"; }
}
