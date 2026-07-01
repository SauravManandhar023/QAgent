package com.saurav.agentic.llm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.saurav.agentic.config.FrameworkConfig;
import com.saurav.agentic.constants.FrameworkConstants;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * FCCProvider — LLM provider implementation for FCC Admin.
 *
 * FCC Admin is a local proxy server that manages multiple AI providers
 * (NVIDIA NIM, OpenRouter, Gemini, DeepSeek, Groq, Ollama, LM Studio,
 * llama.cpp, OpenCode, Mistral, etc.) through a single API.
 *
 * Uses the Anthropic Messages API format at /v1/messages.
 * FCC Admin handles API key management, model routing, and rate limiting.
 *
 * Endpoint: http://localhost:8082/v1/messages
 * Auth: x-api-key header (default: freecc)
 *
 * Model format (from FCC Admin /v1/models):
 *   anthropic/&lt;provider&gt;/&lt;model-name&gt;
 *   e.g. "anthropic/opencode/deepseek-v4-flash-free"
 *
 * To use: set llm.provider=fcc in config.properties
 */
public class FCCProvider implements LLMProvider {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String DEFAULT_FCC_URL = "http://localhost:8082";
    private static final String DEFAULT_API_KEY = "freecc";

    private final OkHttpClient httpClient;
    private final FrameworkConfig config;
    private final Gson gson;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;

    public FCCProvider() {
        this.config = FrameworkConfig.getInstance();
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS) // FCC Admin may have slow models
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        // Read FCC Admin connection details from config
        this.baseUrl = config.getConfigReader().get("fcc.base.url", DEFAULT_FCC_URL);
        this.apiKey = config.getConfigReader().get("fcc.api.key", DEFAULT_API_KEY);

        // Default model for chat() — can be overridden by agents
        String configuredModel = config.getConfigReader().get("fcc.model", "");
        if (configuredModel.isEmpty()) {
            // Try to discover from FCC Admin /v1/models, fall back
            this.defaultModel = discoverDefaultModel();
        } else {
            this.defaultModel = configuredModel;
        }

        System.out.println(FrameworkConstants.LOG_INFO +
                " FCCProvider initialized — URL: " + baseUrl +
                ", default model: " + defaultModel);
    }

    @Override
    public String chat(String systemPrompt, String userPrompt,
                       String model, double temperature, int maxTokens) throws IOException {
        String resolvedModel = resolveModel(model);
        String responseBody = doChat(resolvedModel, systemPrompt, userPrompt,
                temperature, maxTokens, null, null, null);
        return extractTextContent(responseBody);
    }

    @Override
    public JsonObject chatWithTools(
            String toolName, String toolDescription,
            Map<String, Object> parameters,
            String systemPrompt, String userPrompt,
            String model, double temperature, int maxTokens) throws IOException {

        String resolvedModel = resolveModel(model);
        String responseBody = doChat(resolvedModel, systemPrompt, userPrompt,
                temperature, maxTokens, toolName, toolDescription, parameters);

        // Try to extract tool_use from Anthropic-style response
        String toolJson = extractToolUseContent(responseBody);
        if (toolJson != null) {
            return gson.fromJson(toolJson, JsonObject.class);
        }

        // Fallback: extract text content and parse as JSON
        String text = extractTextContent(responseBody);
        if (text != null && !text.isEmpty()) {
            return LLMService.parseFallbackJson(text, gson);
        }

        return new JsonObject();
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
        return true; // Some FCC-routed models support vision
    }

    @Override
    public String getProviderName() {
        return "fcc";
    }

    // ── Core API Call ───────────────────────────────────────────────────────

    /**
     * Send a request to FCC Admin's /v1/messages endpoint using the
     * Anthropic Messages API format. Always receives SSE response even
     * when stream=false — we parse it to get the final text.
     */
    private String doChat(String model, String systemPrompt, String userPrompt,
                          double temperature, int maxTokens,
                          String toolName, String toolDescription,
                          Map<String, Object> toolParameters) throws IOException {

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("max_tokens", maxTokens);
        requestBody.addProperty("stream", false);

        // Temperature mapping (Anthropic uses top_p/ temperature similarly)
        requestBody.addProperty("temperature", temperature);

        // System prompt
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonArray systemArr = new JsonArray();
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("type", "text");
            sysMsg.addProperty("text", systemPrompt);
            systemArr.add(sysMsg);
            requestBody.add("system", systemArr);
        }

        // Messages
        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");

        JsonArray content = new JsonArray();
        JsonObject textBlock = new JsonObject();
        textBlock.addProperty("type", "text");
        textBlock.addProperty("text", userPrompt);
        content.add(textBlock);
        userMessage.add("content", content);
        messages.add(userMessage);
        requestBody.add("messages", messages);

        // Tools (Anthropic tool_use format)
        if (toolName != null) {
            JsonArray tools = new JsonArray();
            JsonObject toolObj = new JsonObject();
            toolObj.addProperty("name", toolName);
            toolObj.addProperty("description", toolDescription);

            // Convert JSON Schema parameters to Anthropic "input_schema" format
            toolObj.add("input_schema", mapToJsonObject(toolParameters));
            tools.add(toolObj);
            requestBody.add("tools", tools);
        }

        String jsonRequest = gson.toJson(requestBody);
        System.out.println(FrameworkConstants.LOG_INFO +
                " FCC request [" + model + "]: " + jsonRequest);

        RequestBody body = RequestBody.create(jsonRequest, JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/v1/messages")
                .addHeader("Content-Type", "application/json")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = "";
                try {
                    responseBody = response.body().string();
                } catch (Exception e) {
                    responseBody = "Unable to read response body";
                }
                throw new IOException(
                        FrameworkConstants.LOG_ERROR + " FCC Admin API failed: " +
                        response.code() + " - " + response.message() +
                        "\nResponse body: " + responseBody
                );
            }

            // Read the SSE response body
            StringBuilder rawResponse = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    rawResponse.append(line).append("\n");
                }
            }

            System.out.println(FrameworkConstants.LOG_SUCCESS +
                    " FCC response received! (" + rawResponse.length() + " chars)");
            return rawResponse.toString();
        }
    }

    // ── SSE Parsing ─────────────────────────────────────────────────────────

    /**
     * Extract the text content from an Anthropic SSE response.
     * Looks for content_block_delta events with text_delta type.
     */
    private String extractTextContent(String sseResponse) {
        StringBuilder text = new StringBuilder();
        boolean inTextDelta = false;

        for (String line : sseResponse.split("\n")) {
            line = line.trim();

            // Detect text_delta start
            if (line.startsWith("data: ") && line.contains("\"type\":\"text_delta\"")) {
                inTextDelta = true;
                try {
                    String json = line.substring(5).trim(); // strip "data: "
                    JsonObject obj = gson.fromJson(json, JsonObject.class);
                    if (obj.has("delta")) {
                        String delta = obj.getAsJsonObject("delta")
                                .get("text").getAsString();
                        text.append(delta);
                    }
                } catch (Exception ignored) {}
                continue;
            }

            // Detect text block start (initial content)
            if (line.startsWith("data: ") && line.contains("\"type\":\"content_block_start\"")) {
                try {
                    String json = line.substring(5).trim();
                    JsonObject obj = gson.fromJson(json, JsonObject.class);
                    JsonObject block = obj.getAsJsonObject("content_block");
                    if (block != null && "text".equals(block.get("type").getAsString())) {
                        String initialText = block.get("text").getAsString();
                        if (initialText != null && !initialText.isEmpty()) {
                            text.append(initialText);
                        }
                        inTextDelta = true;
                    }
                } catch (Exception ignored) {}
                continue;
            }

            // End of text block
            if (inTextDelta && line.contains("content_block_stop")) {
                inTextDelta = false;
            }
        }

        return text.toString().trim();
    }

    /**
     * Extract tool_use content from an Anthropic SSE response.
     * Returns the arguments JSON if a tool_use block is found, null otherwise.
     */
    private String extractToolUseContent(String sseResponse) {
        String currentBlock = "";
        boolean inToolUse = false;

        for (String line : sseResponse.split("\n")) {
            line = line.trim();

            if (!line.startsWith("data: ")) continue;

            try {
                String json = line.substring(5).trim();
                JsonObject obj = gson.fromJson(json, JsonObject.class);

                // content_block_start with tool_use type
                if (line.contains("\"type\":\"content_block_start\"")) {
                    JsonObject block = obj.getAsJsonObject("content_block");
                    if (block != null && "tool_use".equals(block.get("type").getAsString())) {
                        inToolUse = true;
                        currentBlock = "";
                    } else if (block != null && "text".equals(block.get("type").getAsString())) {
                        inToolUse = false;
                    }
                    continue;
                }

                // content_block_delta for tool_use
                if (inToolUse && line.contains("\"type\":\"content_block_delta\"")) {
                    JsonObject delta = obj.getAsJsonObject("delta");
                    if (delta != null && "input_json_delta".equals(delta.get("type").getAsString())) {
                        String partial = delta.get("partial_json").getAsString();
                        if (partial != null) {
                            currentBlock += partial;
                        }
                    }
                    continue;
                }

                // content_block_stop for tool_use
                if (inToolUse && line.contains("content_block_stop")) {
                    inToolUse = false;
                    if (!currentBlock.isEmpty()) {
                        return currentBlock;
                    }
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    // ── Model Discovery ─────────────────────────────────────────────────────

    /**
     * Query FCC Admin's /v1/models endpoint to find a default model.
     * Uses the first available model (preferring non-thinking variants).
     */
    private String discoverDefaultModel() {
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "/v1/models")
                    .addHeader("x-api-key", apiKey)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    JsonArray data = json.getAsJsonArray("data");

                    if (data != null && data.size() > 0) {
                        // Prefer a model without "no thinking" suffix, not starting with "claude-3-freecc"
                        for (int i = 0; i < data.size(); i++) {
                            String modelId = data.get(i).getAsJsonObject().get("id").getAsString();
                            if (!modelId.contains("no-thinking") && !modelId.startsWith("claude-3-freecc")) {
                                return modelId;
                            }
                        }
                        // Fallback to first model
                        return data.get(0).getAsJsonObject().get("id").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " Could not discover FCC models: " + e.getMessage());
        }

        // Ultimate fallback
        return "anthropic/opencode/deepseek-v4-flash-free";
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Resolve model name. If model starts with "anthropic/" it's an FCC model.
     * If agent provides a short name like "groq/llama3", prefix with "anthropic/".
     * If it's already a full FCC model ID, use as-is.
     */
    private String resolveModel(String model) {
        if (model == null || model.isEmpty()) {
            return defaultModel;
        }
        // Already an FCC model ID
        if (model.startsWith("anthropic/") || model.startsWith("claude-3-freecc")) {
            return model;
        }
        // Prefix with anthropic/ for FCC routing
        return "anthropic/" + model;
    }

    @SuppressWarnings("unchecked")
    private JsonObject mapToJsonObject(Map<String, Object> map) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            obj.add(entry.getKey(), objectToJsonElement(entry.getValue()));
        }
        return obj;
    }

    @SuppressWarnings("unchecked")
    private JsonElement objectToJsonElement(Object value) {
        if (value == null) return JsonNull.INSTANCE;
        if (value instanceof Map) return mapToJsonObject((Map<String, Object>) value);
        if (value instanceof List) {
            JsonArray arr = new JsonArray();
            for (Object item : (List<Object>) value) arr.add(objectToJsonElement(item));
            return arr;
        }
        if (value instanceof String) return new com.google.gson.JsonPrimitive((String) value);
        if (value instanceof Number) return new com.google.gson.JsonPrimitive((Number) value);
        if (value instanceof Boolean) return new com.google.gson.JsonPrimitive((Boolean) value);
        return new com.google.gson.JsonPrimitive(value.toString());
    }
}
