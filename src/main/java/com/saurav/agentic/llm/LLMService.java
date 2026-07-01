package com.saurav.agentic.llm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.saurav.agentic.config.FrameworkConfig;
import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.utils.LlmCache;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLMService — Provider-agnostic LLM client.
 *
 * This is the drop-in replacement for GroqClient.
 *
 * Agents construct LLMService and call chat() / chatWithTools().
 * LLMService delegates to the provider selected in config.properties (llm.provider),
 * handles caching, and normalizes responses so agents never know which provider
 * is in use.
 *
 * To change providers, edit config.properties:
 *   llm.provider=groq    (default)
 *   llm.provider=ollama
 *   llm.provider=claude
 *   llm.provider=openai
 *   llm.provider=gemini
 *   llm.provider=nim
 *   llm.provider=deepseek
 *
 * No agent code changes needed.
 */
public class LLMService {

    private final LLMProvider provider;
    private final FrameworkConfig config;
    private final Gson gson;

    // Session-scoped in-memory cache (fast)
    private final Map<String, String> responseCache = new HashMap<>();

    // Disk-backed persistent cache (survives restarts)
    private LlmCache llmCache;

    public LLMService() {
        this.config = FrameworkConfig.getInstance();
        this.gson = new Gson();
        this.provider = ProviderFactory.getProvider();
        System.out.println(FrameworkConstants.LOG_INFO +
                " LLMService initialized — provider: " + provider.getProviderName());
    }

    /**
     * Send prompt with default config (used by Agent 1).
     */
    public String chat(String systemPrompt, String userPrompt) throws IOException {
        return chat(
                systemPrompt,
                userPrompt,
                config.getGroqModel(),
                config.getGroqTemperature(),
                config.getGroqMaxTokens()
        );
    }

    /**
     * Send prompt with custom model settings (used by Agent 2, 3, 4).
     * Delegates to the configured provider after checking cache.
     */
    public String chat(String systemPrompt, String userPrompt,
                       String model, double temperature, int maxTokens) throws IOException {
        String cacheKey = systemPrompt + "|||" + userPrompt + "|||"
                + model + "|||" + temperature + "|||" + maxTokens;

        // Step 1: Check disk cache
        LlmCache cache = getCache();
        if (cache.isEnabled()) {
            String cached = cache.get(systemPrompt, userPrompt, model, temperature, maxTokens);
            if (cached != null) {
                responseCache.put(cacheKey, cached);
                return cached;
            }
            if (cache.isOfflineMode()) {
                throw new IOException(
                        FrameworkConstants.LOG_ERROR +
                        " OFFLINE MODE — no cached response found.\n" +
                        "Run once with cache.offline.mode=false to populate cache.\n" +
                        "Model: " + model + " | Key hash: " + cacheKey.hashCode()
                );
            }
        }

        // Step 2: Check in-memory cache
        if (responseCache.containsKey(cacheKey)) {
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Returning cached LLM response.");
            return responseCache.get(cacheKey);
        }

        // Step 3: Delegate to the configured provider
        String result = provider.chat(systemPrompt, userPrompt, model, temperature, maxTokens);

        // Cache the result
        responseCache.put(cacheKey, result);
        if (cache.isEnabled()) {
            cache.put(systemPrompt, userPrompt, model, temperature, maxTokens, result);
        }

        return result;
    }

    /**
     * Send a prompt with tool/function-calling for structured output.
     * Delegates to the configured provider and normalizes the response.
     */
    public JsonObject chatWithTools(
            String toolName, String toolDescription,
            Map<String, Object> parameters,
            String systemPrompt, String userPrompt,
            String model, double temperature, int maxTokens) throws IOException {

        System.out.println(FrameworkConstants.LOG_INFO +
                " Sending tool-call request via " + provider.getProviderName() +
                " [" + model + "] tool=" + toolName);

        JsonObject result = provider.chatWithTools(
                toolName, toolDescription, parameters,
                systemPrompt, userPrompt, model, temperature, maxTokens
        );

        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Tool-call response received! Provider: " + provider.getProviderName());

        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // JSON SCHEMA HELPERS — used by agents to build tool-calling schemas
    // These were previously static methods on GroqClient.
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Build a JSON Schema object for a tool's parameters in the standard
     * JSON Schema format expected by OpenAI-compatible APIs.
     */
    public static Map<String, Object> jsonSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    /**
     * Build a JSON Schema property definition for a string field.
     */
    public static Map<String, Object> stringProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "string");
        prop.put("description", description);
        return prop;
    }

    /**
     * Build a JSON Schema property definition for an integer field.
     */
    public static Map<String, Object> integerProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "integer");
        prop.put("description", description);
        return prop;
    }

    /**
     * Build a JSON Schema property definition for a boolean field.
     */
    public static Map<String, Object> booleanProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "boolean");
        prop.put("description", description);
        return prop;
    }

    /**
     * Build a JSON Schema property definition for an array of objects.
     */
    public static Map<String, Object> arrayProperty(Map<String, Object> itemProperties,
                                                     List<String> required,
                                                     String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "array");
        prop.put("description", description);

        Map<String, Object> items = new HashMap<>();
        items.put("type", "object");
        items.put("properties", itemProperties);
        items.put("required", required);
        prop.put("items", items);

        return prop;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FALLBACK JSON PARSING — for providers/models that don't support
    // tool-calling properly. Used internally by providers.
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Attempt to parse free-text content as JSON when tool-calling fails or
     * is unavailable. Handles markdown fences, truncated JSON, and
     * embedded JSON objects/arrays.
     */
    public static JsonObject parseFallbackJson(String content, Gson gson) {
        if (content == null || content.trim().isEmpty()) {
            return new JsonObject();
        }

        content = content.trim();

        // Strip markdown code fences
        content = content.replaceAll("```[a-zA-Z]*\\s*", "").trim();
        content = content.replace("```", "").trim();

        String argumentsJson = null;

        // Strategy 1: Content starts with [ → JSON array
        if (content.startsWith("[")) {
            argumentsJson = "{\"testCases\":" + content + "}";
        }
        // Strategy 2: Content starts with { → JSON object
        else if (content.startsWith("{")) {
            argumentsJson = content;
        }
        // Strategy 3: Search for JSON array anywhere in the text
        else {
            int arrayStart = content.indexOf('[');
            int arrayEnd = content.lastIndexOf(']');
            if (arrayStart != -1 && arrayEnd > arrayStart) {
                String jsonArray = content.substring(arrayStart, arrayEnd + 1);
                String trimmed = jsonArray.trim();
                if (trimmed.startsWith("[{") || trimmed.startsWith("[[")) {
                    try {
                        gson.fromJson(jsonArray, JsonArray.class);
                        argumentsJson = "{\"testCases\":" + jsonArray + "}";
                    } catch (Exception ignored) {}
                }
            }
        }

        // Strategy 4: Search for JSON object anywhere
        if (argumentsJson == null) {
            int objStart = content.indexOf('{');
            if (objStart != -1) {
                int objEnd = content.lastIndexOf('}');
                if (objEnd > objStart) {
                    String jsonObj = content.substring(objStart, objEnd + 1);
                    if (jsonObj.trim().startsWith("{\"") && jsonObj.contains(":")) {
                        try {
                            gson.fromJson(jsonObj, JsonObject.class);
                            argumentsJson = jsonObj;
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        // Strategy 5: No JSON found → wrap as raw text
        if (argumentsJson == null) {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " No JSON found in response — wrapping as raw text.");
            String escaped = content
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            argumentsJson = "{\"code\":\"" + escaped + "\"}";
        }

        return gson.fromJson(argumentsJson, JsonObject.class);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DELEGATED METHODS
    // ═══════════════════════════════════════════════════════════════════════

    public boolean supportsToolCalling() {
        return provider.supportsToolCalling();
    }

    public boolean supportsStreaming() {
        return provider.supportsStreaming();
    }

    public boolean supportsVision() {
        return provider.supportsVision();
    }

    public String getProviderName() {
        return provider.getProviderName();
    }

    public LLMProvider getProvider() {
        return provider;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CACHE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════

    private LlmCache getCache() {
        if (llmCache == null) {
            llmCache = new LlmCache();
        }
        return llmCache;
    }
}
