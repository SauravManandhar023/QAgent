package com.saurav.agentic.llm;

import com.google.gson.JsonObject;
import com.saurav.agentic.constants.FrameworkConstants;

import java.io.IOException;
import java.util.Map;

/**
 * OpenAIProvider — LLM provider implementation for OpenAI API.
 *
 * Uses the OpenAI chat completions endpoint.
 * Supports tool/function-calling natively (same format as Groq).
 *
 * STUB: This provider is registered but not yet fully implemented.
 */
public class OpenAIProvider implements LLMProvider {

    public OpenAIProvider() {
        System.out.println(FrameworkConstants.LOG_INFO +
                " OpenAIProvider initialized (stub)");
    }

    @Override
    public String chat(String systemPrompt, String userPrompt,
                       String model, double temperature, int maxTokens) throws IOException {
        throw new IOException(
                FrameworkConstants.LOG_ERROR + " OpenAIProvider not yet implemented. " +
                "Set llm.provider=groq or llm.provider=ollama to use existing providers."
        );
    }

    @Override
    public JsonObject chatWithTools(
            String toolName, String toolDescription,
            Map<String, Object> parameters,
            String systemPrompt, String userPrompt,
            String model, double temperature, int maxTokens) throws IOException {
        throw new IOException(
                FrameworkConstants.LOG_ERROR + " OpenAIProvider not yet implemented."
        );
    }

    @Override
    public boolean supportsToolCalling() { return true; }

    @Override
    public boolean supportsStreaming() { return true; }

    @Override
    public boolean supportsVision() { return true; }

    @Override
    public String getProviderName() { return "openai"; }
}
