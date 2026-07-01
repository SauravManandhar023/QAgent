package com.saurav.agentic.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.saurav.agentic.config.FrameworkConfig;
import com.saurav.agentic.constants.FrameworkConstants;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * LlmClient - Handles all communication with LLM API
 * Now supports Ollama (local) as a free, rate-limit-free alternative to Groq
 * Uses OkHttp for HTTP calls and Gson for JSON parsing
 *
 * To use Ollama:
 * 1. Install Ollama from https://ollama.com
 * 2. Pull a model: ollama pull llama3 (or llama3:70b for better quality)
 * 3. Ollama runs locally at http://localhost:11434 by default
 */
public class GroqClient {

    private final OkHttpClient httpClient;
    private final FrameworkConfig config;
    private final Gson gson;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // In-memory cache for LLM responses (session-scoped, fast)
    private final Map<String, String> responseCache = new HashMap<>();

    // Disk-backed persistent cache (survives restarts, enables offline mode)
    private LlmCache llmCache;

    public GroqClient() {
        this.config = FrameworkConfig.getInstance();
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS) // Increased for local LLM processing
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Send prompt with default config (used by Agent 1)
     * @deprecated Use chat(systemPrompt, userPrompt, model, temperature, maxTokens) directly
     */
    @Deprecated
    public String chat(String systemPrompt, String userPrompt) throws IOException {
        // Fallback to default settings for backward compatibility
        return chat(
                systemPrompt,
                userPrompt,
                config.getGroqModel(), // This will now return Ollama model name if configured
                config.getGroqTemperature(),
                config.getGroqMaxTokens()
        );
    }

    /**
     * Send prompt with custom model settings (used by Agent 2, 3, 4)
     * Now works with both Groq and Ollama backends
     */
    public String chat(String systemPrompt, String userPrompt,
                       String model, double temperature, int maxTokens) throws IOException {
        // Create cache key
        String key = systemPrompt + "|||" + userPrompt + "|||" + model + "|||" + temperature + "|||" + maxTokens;

        // Step 1: Check disk cache (persistent, survives restarts)
        LlmCache cache = getCache();
        if (cache.isEnabled()) {
            String cached = cache.get(systemPrompt, userPrompt, model, temperature, maxTokens);
            if (cached != null) {
                // Also populate in-memory cache for even faster lookup next time
                responseCache.put(key, cached);
                return cached;
            }
            // Offline mode: no live LLM calls allowed
            if (cache.isOfflineMode()) {
                throw new IOException(
                        FrameworkConstants.LOG_ERROR + " OFFLINE MODE — no cached response found for this prompt.\n" +
                        "To fix: run once with cache.offline.mode=false to populate the cache, then\n" +
                        "re-run with cache.offline.mode=true.\n" +
                        "Model: " + model + " | Key hash: " + key.hashCode()
                );
            }
        }

        // Step 2: Check in-memory cache (session-scoped, fast)
        if (responseCache.containsKey(key)) {
            System.out.println(FrameworkConstants.LOG_INFO + " Returning cached LLM response.");
            return responseCache.get(key);
        }

        // Determine if we're using Ollama or Groq based on model name or config
        boolean useOllama = isOllamaModel(model);
        String apiUrl = useOllama ? "http://localhost:11434/api/chat"
                                  : FrameworkConstants.GROQ_API_URL;

        String apiUrlStr = useOllama ? "http://localhost:11434/api/chat" : FrameworkConstants.GROQ_API_URL;
        System.out.println(FrameworkConstants.LOG_INFO +
                " Sending request to " + (useOllama ? "Ollama AI" : "Groq AI") + " [" + model + "] at URL: " + apiUrlStr);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("temperature", temperature);
        requestBody.addProperty("max_tokens", maxTokens);
        requestBody.addProperty("stream", false); // We want complete response, not streaming

        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userPrompt);
        messages.add(userMessage);

        requestBody.add("messages", messages);

        String jsonRequest = gson.toJson(requestBody);
        System.out.println(FrameworkConstants.LOG_INFO + " Request JSON: " + jsonRequest);
        RequestBody body = RequestBody.create(jsonRequest, JSON);
        Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl)
                .addHeader("Content-Type", "application/json")
                .post(body);

        // Only add Authorization header for Groq (Ollama running locally doesn't need it)
        if (!useOllama) {
            requestBuilder.addHeader("Authorization", "Bearer " + config.getGroqApiKey());
        }

        Request request = requestBuilder.build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = "";
                try {
                    responseBody = response.body().string();
                } catch (Exception e) {
                    responseBody = "Unable to read response body";
                }
                throw new IOException(
                        FrameworkConstants.LOG_ERROR + " LLM API failed: " +
                        response.code() + " - " + response.message() + "\nResponse body: " + responseBody
                );
            }
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            String result;
            if (useOllama) {
                // Ollama response format: {"model": "...", "created_at": "...", "message": {"role": "...", "content": "..."}, ...}
                result = jsonResponse
                        .getAsJsonObject("message")
                        .get("content").getAsString();
            } else {
                // Groq/OpenAI format: {"choices": [{"message": {"role": "...", "content": "..."}}], ...}
                result = jsonResponse
                        .getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
            }

            System.out.println(FrameworkConstants.LOG_SUCCESS +
                    " LLM response received!");
            // Cache the result in memory
            responseCache.put(key, result);
            // Cache the result to disk (if enabled)
            if (getCache().isEnabled()) {
                getCache().put(systemPrompt, userPrompt, model, temperature, maxTokens, result);
            }
            return result;
        }
    }

    /**
     * Send a prompt with tool/function-calling for structured output.
     *
     * Instead of returning free-form text (which needs fragile parsing like
     * cleanJsonResponse / cleanJavaCode), the LLM returns a typed tool_calls
     * array where the arguments field contains valid JSON matching the schema.
     *
     * Works with both Ollama and Groq (both support OpenAI-compatible tools).
     *
     * @param toolName        the function name the LLM should call
     * @param toolDescription description of what the function does
     * @param parameters      JSON Schema object describing the expected output shape
     * @param systemPrompt    system-level instructions
     * @param userPrompt      user query
     * @return JsonObject matching the supplied schema (from tool_calls[0].function.arguments)
     */
    public JsonObject chatWithTools(
            String toolName, String toolDescription,
            Map<String, Object> parameters,
            String systemPrompt, String userPrompt,
            String model, double temperature, int maxTokens) throws IOException {

        // Determine provider
        boolean useOllama = isOllamaModel(model);
        String apiUrl = useOllama ? "http://localhost:11434/api/chat"
                                  : FrameworkConstants.GROQ_API_URL;

        System.out.println(FrameworkConstants.LOG_INFO +
                " Sending tool-call request to " + (useOllama ? "Ollama AI" : "Groq AI")
                + " [" + model + "] tool=" + toolName);

        // Build the request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("temperature", temperature);
        requestBody.addProperty("max_tokens", maxTokens);
        requestBody.addProperty("stream", false);

        // Messages array
        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userPrompt);
        messages.add(userMessage);

        requestBody.add("messages", messages);

        // Tools array — define the single function the LLM can call
        JsonArray tools = new JsonArray();
        JsonObject toolObj = new JsonObject();
        toolObj.addProperty("type", "function");

        JsonObject functionObj = new JsonObject();
        functionObj.addProperty("name", toolName);
        functionObj.addProperty("description", toolDescription);

        // Convert the Java Map parameters to a Gson JsonObject
        // (the parameters map already follows JSON Schema format)
        JsonObject paramsJson = mapToJsonObject(parameters);
        functionObj.add("parameters", paramsJson);

        toolObj.add("function", functionObj);
        tools.add(toolObj);
        requestBody.add("tools", tools);

        // Force the LLM to call our function
        JsonObject toolChoice = new JsonObject();
        toolChoice.addProperty("type", "function");
        JsonObject funcChoice = new JsonObject();
        funcChoice.addProperty("name", toolName);
        toolChoice.add("function", funcChoice);
        requestBody.add("tool_choice", toolChoice);

        // Build and send the HTTP request
        String jsonRequest = gson.toJson(requestBody);
        System.out.println(FrameworkConstants.LOG_INFO + " Tool-call request: " + jsonRequest);

        RequestBody body = RequestBody.create(jsonRequest, JSON);
        Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl)
                .addHeader("Content-Type", "application/json")
                .post(body);

        if (!useOllama) {
            requestBuilder.addHeader("Authorization", "Bearer " + config.getGroqApiKey());
        }

        Request request = requestBuilder.build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = "";
                try {
                    responseBody = response.body().string();
                } catch (Exception e) {
                    responseBody = "Unable to read response body";
                }
                throw new IOException(
                        FrameworkConstants.LOG_ERROR + " LLM API failed: " +
                        response.code() + " - " + response.message() + "\nResponse body: " + responseBody
                );
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            // Parse tool_calls from the response
            String argumentsJson = null;

            if (useOllama) {
                // Ollama response: { "message": { "role": "assistant", "content": null, "tool_calls": [...] } }
                JsonObject msgObj = jsonResponse.getAsJsonObject("message");
                if (msgObj != null && msgObj.has("tool_calls") && !msgObj.get("tool_calls").isJsonNull()) {
                    JsonArray toolCalls = msgObj.getAsJsonArray("tool_calls");
                    if (toolCalls != null && toolCalls.size() > 0) {
                        JsonElement argsEl = toolCalls.get(0).getAsJsonObject()
                                .getAsJsonObject("function")
                                .get("arguments");
                        // Ollama may return arguments as a JSON object directly (newer versions)
                        // or as a JSON string (older versions)
                        if (argsEl.isJsonObject()) {
                            argumentsJson = argsEl.getAsJsonObject().toString();
                        } else {
                            argumentsJson = argsEl.getAsString();
                        }
                    }
                }
            } else {
                // Groq/OpenAI format: { "choices": [{ "message": { "tool_calls": [...] } }] }
                JsonArray choices = jsonResponse.getAsJsonArray("choices");
                if (choices != null && choices.size() > 0) {
                    JsonObject choiceMsg = choices.get(0).getAsJsonObject()
                            .getAsJsonObject("message");
                    if (choiceMsg != null && choiceMsg.has("tool_calls") && !choiceMsg.get("tool_calls").isJsonNull()) {
                        JsonArray toolCalls = choiceMsg.getAsJsonArray("tool_calls");
                        if (toolCalls != null && toolCalls.size() > 0) {
                            argumentsJson = toolCalls.get(0).getAsJsonObject()
                                    .getAsJsonObject("function")
                                    .get("arguments").getAsString();
                        }
                    }
                }
            }

            // Fallback: if tool_calls was null or empty, try parsing the message content as JSON
            // This handles models that don't support tool-calling properly
            if (argumentsJson == null) {
                String content = useOllama
                    ? jsonResponse.getAsJsonObject("message").get("content").getAsString()
                    : jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject()
                            .getAsJsonObject("message").get("content").getAsString();
                System.out.println(FrameworkConstants.LOG_WARNING +
                        " No tool_calls in response — falling back to content parsing.");
                content = content.trim();

                // Strip ALL markdown code fences anywhere in the content (not just at start)
                // Handle ```json, ```java, ```, etc.
                content = content.replaceAll("```[a-zA-Z]*\\s*", "").trim();
                content = content.replace("```", "").trim();

                // Strategy 1: Content starts with [ — it's a JSON array
                if (content.startsWith("[")) {
                    argumentsJson = "{\"testCases\":" + content + "}";
                }
                // Strategy 2: Content starts with { — it's a JSON object
                else if (content.startsWith("{")) {
                    argumentsJson = content;
                }
                // Strategy 3: Search for JSON array [...] anywhere in the text
                // (handles "Here is the JSON:\n[...]" style responses)
                // Validates the extracted content is actually valid JSON before using it.
                else {
                    int arrayStart = content.indexOf('[');
                    int arrayEnd = content.lastIndexOf(']');
                    if (arrayStart != -1 && arrayEnd > arrayStart) {
                        String jsonArray = content.substring(arrayStart, arrayEnd + 1);
                        // Validate: JSON arrays must start with [{ or [[
                        if (jsonArray.trim().startsWith("[{") || jsonArray.trim().startsWith("[[")) {
                            try {
                                gson.fromJson(jsonArray, JsonArray.class);
                                System.out.println(FrameworkConstants.LOG_INFO +
                                        " Extracted valid JSON array from content (offset " + arrayStart + ").");
                                argumentsJson = "{\"testCases\":" + jsonArray + "}";
                            } catch (Exception e) {
                                System.out.println(FrameworkConstants.LOG_INFO +
                                        " Skipped non-JSON array at offset " + arrayStart + " (not valid JSON).");
                            }
                        } else {
                            System.out.println(FrameworkConstants.LOG_INFO +
                                    " Skipped bracket pair at offset " + arrayStart + " (not a JSON array).");
                        }
                    }
                }

                // Strategy 4: Search for JSON object {...} anywhere in the text
                // ONLY accept if it parses as valid JSON — protects against Java code
                // containing { } being falsely detected as JSON.
                if (argumentsJson == null) {
                    int objStart = content.indexOf('{');
                    if (objStart != -1) {
                        int objEnd = content.lastIndexOf('}');
                        if (objEnd > objStart) {
                            String jsonObj = content.substring(objStart, objEnd + 1);
                            // Quick validation: must start with {" and have properly paired quotes
                            if (jsonObj.trim().startsWith("{\"") && jsonObj.contains(":")) {
                                try {
                                    gson.fromJson(jsonObj, JsonObject.class);
                                    System.out.println(FrameworkConstants.LOG_INFO +
                                            " Extracted valid JSON object from content (offset " + objStart + ").");
                                    argumentsJson = jsonObj;
                                } catch (Exception e) {
                                    System.out.println(FrameworkConstants.LOG_INFO +
                                            " Skipped non-JSON object at offset " + objStart + " (not valid JSON).");
                                }
                            }
                        }
                    }
                }

                // Strategy 5: No JSON found at all — model doesn't support structured output.
                // Fall through to wrapping as raw text so the caller can try fallback parsing.
                if (argumentsJson == null) {
                    System.out.println(FrameworkConstants.LOG_WARNING +
                            " Model returned non-JSON content (no JSON found anywhere in response).\n" +
                            "  Response starts with: \"" + content.substring(0, Math.min(200, content.length())) + "...\"\n" +
                            "  Wrapping as raw text for caller fallback parsing.");
                    String escaped = content
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r")
                            .replace("\t", "\\t");
                    argumentsJson = "{\"code\":\"" + escaped + "\"}";
                }
            }

            JsonObject arguments = gson.fromJson(argumentsJson, JsonObject.class);

            System.out.println(FrameworkConstants.LOG_SUCCESS +
                    " Tool-call response received! Arguments: " + arguments.toString());

            // Cache the raw text response for offline mode
            responseCache.put(systemPrompt + "|||" + userPrompt + "|||" + model + "|||" + temperature + "|||" + maxTokens, responseBody);
            if (getCache().isEnabled()) {
                getCache().put(systemPrompt, userPrompt, model, temperature, maxTokens, responseBody);
            }

            return arguments;
        }
    }

    /**
     * Convert a Map&lt;String, Object&gt; (nested, supporting List and Map values)
     * into a Gson JsonObject for use as a tool parameters schema.
     */
    @SuppressWarnings("unchecked")
    private JsonObject mapToJsonObject(Map<String, Object> map) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            obj.add(key, objectToJsonElement(value));
        }
        return obj;
    }

    @SuppressWarnings("unchecked")
    private JsonElement objectToJsonElement(Object value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        } else if (value instanceof Map) {
            return mapToJsonObject((Map<String, Object>) value);
        } else if (value instanceof List) {
            JsonArray arr = new JsonArray();
            for (Object item : (List<Object>) value) {
                arr.add(objectToJsonElement(item));
            }
            return arr;
        } else if (value instanceof String) {
            return new com.google.gson.JsonPrimitive((String) value);
        } else if (value instanceof Number) {
            return new com.google.gson.JsonPrimitive((Number) value);
        } else if (value instanceof Boolean) {
            return new com.google.gson.JsonPrimitive((Boolean) value);
        } else {
            return new com.google.gson.JsonPrimitive(value.toString());
        }
    }

    /**
     * Build a JSON Schema object for a tool's parameters in the standard
     * JSON Schema format expected by OpenAI-compatible APIs.
     *
     * @param properties a map of field name → JSON Schema fragment
     * @param required   list of required field names
     * @return a Map representing the full JSON Schema object
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
     *
     * @param itemProperties the JSON Schema for each item in the array
     * @param required       required fields within each item
     * @return a Map representing the array property
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

    /**
     * Lazy-initialize and return the disk-backed LlmCache.
     * Created on first use to avoid I/O during construction.
     */
    private LlmCache getCache() {
        if (llmCache == null) {
            llmCache = new LlmCache();
        }
        return llmCache;
    }

    /**
     * Determines if a model name refers to an Ollama model
     * Ollama model names typically don't contain special Groq identifiers
     * We can also check config for explicit Ollama usage
     */
    private boolean isOllamaModel(String model) {
        // Check if explicitly configured to use Ollama
        String ollamaConfig = config.getLlmProvider();
        if ("ollama".equalsIgnoreCase(ollamaConfig)) {
            return true;
        }

        // Heuristic: Ollama models often have specific naming patterns
        // Groq models often have identifiers like "llama-3.3-70b-versatile", "mixtral-8x7b-32768"
        // Ollama models are typically just "llama3", "mistral", "codellama", etc.
        // But this is fuzzy - better to rely on explicit config

        // For now, if model contains "groq" or specific Groq patterns, treat as Groq
        // Otherwise assume Ollama for simplicity (user can configure explicitly)
        return !(model.contains("groq") ||
                model.contains("-") &&
                (model.contains("70b") || model.contains("8x7b") || model.contains("32768")));
    }
}