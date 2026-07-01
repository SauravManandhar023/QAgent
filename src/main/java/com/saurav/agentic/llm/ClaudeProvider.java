package com.saurav.agentic.llm;

import com.google.gson.JsonObject;
import com.saurav.agentic.constants.FrameworkConstants;

import java.io.IOException;
import java.util.Map;

/**
 * ClaudeProvider — LLM provider implementation for Claude (Anthropic) API.
 *
 * Uses the Anthropic Messages API to interact with Claude models.
 * Supports tool-calling natively via the Anthropic tool_use format.
 *
 * STUB: This provider is registered but not yet fully implemented.
 * It will work once the Anthropic SDK or REST client is integrated.
 */
public class ClaudeProvider implements LLMProvider {

    public ClaudeProvider() {
        System.out.println(FrameworkConstants.LOG_INFO +
                " ClaudeProvider initialized (stub — ready for integration)");
    }

    @Override
    public String chat(String systemPrompt, String userPrompt,
                       String model, double temperature, int maxTokens) throws IOException {
        // TODO: Implement Claude API call via Anthropic Messages API
        // Endpoint: https://api.anthropic.com/v1/messages
        // Auth: x-api-key header
        // Model: e.g. "claude-sonnet-4-20250514", "claude-3-5-haiku-latest"
        throw new IOException(
                FrameworkConstants.LOG_ERROR + " ClaudeProvider not yet implemented. " +
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
                FrameworkConstants.LOG_ERROR + " ClaudeProvider not yet implemented. " +
                "Set llm.provider=groq or llm.provider=ollama to use existing providers."
        );
    }

    @Override
    public boolean supportsToolCalling() {
        return true;
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public boolean supportsVision() {
        return true;
    }

    @Override
    public String getProviderName() {
        return "claude";
    }
}
