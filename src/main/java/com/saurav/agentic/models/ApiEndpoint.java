package com.saurav.agentic.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ApiEndpoint - Represents a single discovered API endpoint
 * Used by Agent 5A to pass structured API info to Agent 5B/5C
 */
public class ApiEndpoint {

    private String url;             // full URL
    private String path;            // just the path e.g. /api/productsList
    private String method;          // GET, POST, PUT, DELETE
    private int statusCode;         // actual response status code
    private String responseBody;    // raw response body (trimmed)
    private String contentType;     // response content type
    private String authType;        // NONE, BEARER, API_KEY, BASIC
    private boolean requiresAuth;   // does endpoint need auth?
    private boolean isWorking;      // did it return 200/201?

    // Request info
    private Map<String, String> headers     = new HashMap<>();
    private Map<String, String> queryParams = new HashMap<>();
    private Map<String, String> formParams  = new HashMap<>();
    private String requestBody;

    // Response schema info
    private List<String> responseKeys       = new ArrayList<>(); // top-level JSON keys
    private String responseType;    // OBJECT, ARRAY, STRING, UNKNOWN
    private boolean hasResponseCode; // has "responseCode" field (automationexercise pattern)

    // Test generation hints
    private String description;
    private List<String> suggestedTestTypes = new ArrayList<>(); // positive, negative, edge

    public ApiEndpoint() {}

    public ApiEndpoint(String url, String method) {
        this.url    = url;
        this.method = method;
        // Extract path from URL
        try {
            this.path = new java.net.URL(url).getPath();
        } catch (Exception e) {
            this.path = url;
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getUrl()                      { return url; }
    public String getPath()                     { return path; }
    public String getMethod()                   { return method; }
    public int getStatusCode()                  { return statusCode; }
    public String getResponseBody()             { return responseBody; }
    public String getContentType()              { return contentType; }
    public String getAuthType()                 { return authType; }
    public boolean isRequiresAuth()             { return requiresAuth; }
    public boolean isWorking()                  { return isWorking; }
    public Map<String, String> getHeaders()     { return headers; }
    public Map<String, String> getQueryParams() { return queryParams; }
    public Map<String, String> getFormParams()  { return formParams; }
    public String getRequestBody()              { return requestBody; }
    public List<String> getResponseKeys()       { return responseKeys; }
    public String getResponseType()             { return responseType; }
    public boolean isHasResponseCode()          { return hasResponseCode; }
    public String getDescription()              { return description; }
    public List<String> getSuggestedTestTypes() { return suggestedTestTypes; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setUrl(String url)                          { this.url = url; }
    public void setPath(String path)                        { this.path = path; }
    public void setMethod(String method)                    { this.method = method; }
    public void setStatusCode(int statusCode)               { this.statusCode = statusCode; }
    public void setResponseBody(String responseBody)        { this.responseBody = responseBody; }
    public void setContentType(String contentType)          { this.contentType = contentType; }
    public void setAuthType(String authType)                { this.authType = authType; }
    public void setRequiresAuth(boolean requiresAuth)       { this.requiresAuth = requiresAuth; }
    public void setWorking(boolean working)                 { this.isWorking = working; }
    public void setRequestBody(String requestBody)          { this.requestBody = requestBody; }
    public void setResponseKeys(List<String> responseKeys)  { this.responseKeys = responseKeys; }
    public void setResponseType(String responseType)        { this.responseType = responseType; }
    public void setHasResponseCode(boolean hasResponseCode) { this.hasResponseCode = hasResponseCode; }
    public void setDescription(String description)          { this.description = description; }
    public void addHeader(String key, String value)         { this.headers.put(key, value); }
    public void addQueryParam(String key, String value)     { this.queryParams.put(key, value); }
    public void addFormParam(String key, String value)      { this.formParams.put(key, value); }
    public void addSuggestedTestType(String type)           { this.suggestedTestTypes.add(type); }

    /**
     * Generates a structured summary for AI prompts
     */
    public String toPromptString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ENDPOINT: ").append(method).append(" ").append(path).append("\n");
        sb.append("URL     : ").append(url).append("\n");
        sb.append("STATUS  : ").append(statusCode).append("\n");
        sb.append("WORKING : ").append(isWorking).append("\n");
        sb.append("AUTH    : ").append(authType).append("\n");

        if (!responseKeys.isEmpty()) {
            sb.append("RESPONSE KEYS: ").append(responseKeys).append("\n");
        }
        if (responseType != null) {
            sb.append("RESPONSE TYPE: ").append(responseType).append("\n");
        }
        if (hasResponseCode) {
            sb.append("NOTE: Response contains 'responseCode' field\n");
        }
        if (!formParams.isEmpty()) {
            sb.append("FORM PARAMS: ").append(formParams.keySet()).append("\n");
        }
        if (description != null) {
            sb.append("DESCRIPTION: ").append(description).append("\n");
        }
        if (!suggestedTestTypes.isEmpty()) {
            sb.append("SUGGESTED TESTS: ").append(suggestedTestTypes).append("\n");
        }

        // Trimmed response body for context
        if (responseBody != null && !responseBody.isEmpty()) {
            String trimmed = responseBody.length() > 300
                    ? responseBody.substring(0, 300) + "..."
                    : responseBody;
            sb.append("SAMPLE RESPONSE:\n").append(trimmed).append("\n");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return method + " " + path + " [" + statusCode + "] " +
               (isWorking ? "✅" : "❌");
    }
}