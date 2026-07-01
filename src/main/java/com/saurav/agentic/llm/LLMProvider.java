package com.saurav.agentic.llm;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * LLMProvider - Provider-agnostic interface for all LLM providers.
 *
 * Every provider (Groq, Ollama, Claude, OpenAI, Gemini, DeepSeek, Nvidia NIM, etc.)
 * must implement this interface. Agents call LLMService, which delegates to the
 * selected provider — agents never reference providers directly.
 *
 * Adding a new provider = implement this interface + register in ProviderFactory.
 * No agent code changes needed.
 */
public interface LLMProvider {

    /**
     * Send a chat prompt and receive a free-text response.
     * Used for simple interactions where no structured output is needed.
     *
     * @param systemPrompt system-level instructions
     * @param userPrompt   user query
     * @param model        model identifier (e.g. "llama-3.3-70b-versatile", "claude-sonnet-4-20250514")
     * @param temperature  sampling temperature (0.0 - 1.0)
     * @param maxTokens    maximum tokens in response
     * @return response text from the LLM
     */
    String chat(String systemPrompt, String userPrompt,
                String model, double temperature, int maxTokens) throws IOException;

    /**
     * Send a prompt with tool/function-calling for structured output.
     * Returns a JsonObject matching the supplied JSON Schema.
     * If the provider does not support tool calling natively, the implementation
     * should fall back to parsing the free-text response for valid JSON.
     *
     * @param toolName        the function name the LLM should call
     * @param toolDescription description of the function
     * @param parameters      JSON Schema object describing expected output shape
     * @param systemPrompt    system-level instructions
     * @param userPrompt      user query
     * @param model           model identifier
     * @param temperature     sampling temperature
     * @param maxTokens       maximum tokens in response
     * @return JsonObject matching the supplied schema
     */
    JsonObject chatWithTools(
            String toolName, String toolDescription,
            Map<String, Object> parameters,
            String systemPrompt, String userPrompt,
            String model, double temperature, int maxTokens) throws IOException;

    /**
     * @return true if this provider supports tool/function-calling natively
     */
    boolean supportsToolCalling();

    /**
     * @return true if this provider supports streaming responses
     */
    boolean supportsStreaming();

    /**
     * @return true if this provider supports vision/image inputs
     */
    boolean supportsVision();

    /**
     * @return the canonical provider name, e.g. "groq", "ollama", "claude", "openai"
     */
    String getProviderName();
}
