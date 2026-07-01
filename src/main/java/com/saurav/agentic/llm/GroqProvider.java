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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * GroqProvider — LLM provider implementation for Groq API.
 *
 * Uses the OpenAI-compatible chat completions endpoint at
 * https://api.groq.com/openai/v1/chat/completions
 *
 * Supports tool/function-calling natively (OpenAI-compatible format).
 */
public class GroqProvider implements LLMProvider {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final FrameworkConfig config;
    private final Gson gson;

    public GroqProvider() {
        this.config = FrameworkConfig.getInstance();
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt,
                       String model, double temperature, int maxTokens) throws IOException {
        return doChat(systemPrompt, userPrompt, model, temperature, maxTokens, null, null, null);
    }

    @Override
    public JsonObject chatWithTools(
            String toolName, String toolDescription,
            Map<String, Object> parameters,
            String systemPrompt, String userPrompt,
            String model, double temperature, int maxTokens) throws IOException {

        String responseBody = doChat(systemPrompt, userPrompt, model, temperature, maxTokens,
                toolName, toolDescription, parameters);

        // Parse tool_calls from the Groq/OpenAI response format
        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

        JsonArray choices = jsonResponse.getAsJsonArray("choices");
        if (choices != null && choices.size() > 0) {
            JsonObject choiceMsg = choices.get(0).getAsJsonObject()
                    .getAsJsonObject("message");
            if (choiceMsg != null && choiceMsg.has("tool_calls")
                    && !choiceMsg.get("tool_calls").isJsonNull()) {
                JsonArray toolCalls = choiceMsg.getAsJsonArray("tool_calls");
                if (toolCalls != null && toolCalls.size() > 0) {
                    String argumentsJson = toolCalls.get(0).getAsJsonObject()
                            .getAsJsonObject("function")
                            .get("arguments").getAsString();
                    return gson.fromJson(argumentsJson, JsonObject.class);
                }
            }
        }

        // Fallback: parse content as JSON
        String content = jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject()
                .getAsJsonObject("message").get("content").getAsString();
        return LLMService.parseFallbackJson(content, gson);
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
        return false;
    }

    @Override
    public String getProviderName() {
        return "groq";
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    /**
     * Core HTTP call to the Groq API. If tool parameters are provided, includes
     * tool definitions and forces tool_choice.
     */
    private String doChat(String systemPrompt, String userPrompt,
                          String model, double temperature, int maxTokens,
                          String toolName, String toolDescription,
                          Map<String, Object> toolParameters) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("temperature", temperature);
        requestBody.addProperty("max_tokens", maxTokens);
        requestBody.addProperty("stream", false);

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

        // Optional tool definition
        if (toolName != null) {
            JsonArray tools = new JsonArray();
            JsonObject toolObj = new JsonObject();
            toolObj.addProperty("type", "function");

            JsonObject functionObj = new JsonObject();
            functionObj.addProperty("name", toolName);
            functionObj.addProperty("description", toolDescription);
            functionObj.add("parameters", mapToJsonObject(toolParameters));
            toolObj.add("function", functionObj);
            tools.add(toolObj);
            requestBody.add("tools", tools);

            JsonObject toolChoice = new JsonObject();
            toolChoice.addProperty("type", "function");
            JsonObject funcChoice = new JsonObject();
            funcChoice.addProperty("name", toolName);
            toolChoice.add("function", funcChoice);
            requestBody.add("tool_choice", toolChoice);
        }

        String jsonRequest = gson.toJson(requestBody);
        System.out.println(FrameworkConstants.LOG_INFO +
                " Groq request [" + model + "]: " + jsonRequest);

        RequestBody body = RequestBody.create(jsonRequest, JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + config.getGroqApiKey())
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
                        FrameworkConstants.LOG_ERROR + " Groq API failed: " +
                        response.code() + " - " + response.message() +
                        "\nResponse body: " + responseBody
                );
            }
            String responseBody = response.body().string();
            System.out.println(FrameworkConstants.LOG_SUCCESS +
                    " Groq response received!");
            return responseBody;
        }
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
