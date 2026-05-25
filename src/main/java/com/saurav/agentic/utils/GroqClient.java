package com.saurav.agentic.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.saurav.agentic.config.FrameworkConfig;
import com.saurav.agentic.constants.FrameworkConstants;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * GroqClient - Handles all communication with Groq AI API
 * Uses OkHttp for HTTP calls and Gson for JSON parsing
 *
 */
public class GroqClient {

    private final OkHttpClient httpClient;
    private final FrameworkConfig config;
    private final Gson gson;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public GroqClient() {
        this.config = FrameworkConfig.getInstance();
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Send a prompt to Groq AI and get response
     *
     * @param systemPrompt - Instructions for the AI
     * @param userPrompt   - The actual content/question
     * @return AI response as String
     */
    public String chat(String systemPrompt, String userPrompt) throws IOException {
        System.out.println(FrameworkConstants.LOG_INFO + " Sending request to Groq AI...");

        // Build request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.getGroqModel());
        requestBody.addProperty("temperature", config.getGroqTemperature());
        requestBody.addProperty("max_tokens", config.getGroqMaxTokens());

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

        // Build HTTP request
        RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);
        Request request = new Request.Builder()
                .url(FrameworkConstants.GROQ_API_URL)
                .addHeader("Authorization", "Bearer " + config.getGroqApiKey())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        // Execute request
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(
                    FrameworkConstants.LOG_ERROR + " Groq API failed: " +
                    response.code() + " - " + response.message()
                );
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            String result = jsonResponse
                    .getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            System.out.println(FrameworkConstants.LOG_SUCCESS + " Groq AI response received!");
            return result;
        }
    }
}