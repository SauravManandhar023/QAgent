package com.saurav.agentic.agents;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.ApiEndpoint;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;

/**
 * ApiDiscoveryAgent - Agent 5A
 *
 * Discovers and probes API endpoints without any AI calls.
 * Hits each endpoint, records response, extracts schema info.
 * Outputs List<ApiEndpoint> for Agent 5B (test case generation).
 *
 * Zero AI tokens used — pure HTTP probing.
 */
public class ApiDiscoveryAgent {

    private final String baseUrl;
    private final Gson gson = new Gson();

    // Known endpoints for automationexercise.com API
    // These will be discovered dynamically for unknown APIs
    private static final List<EndpointDefinition> KNOWN_ENDPOINTS = List.of(
        new EndpointDefinition("/api/productsList",   "GET",  null, false),
        new EndpointDefinition("/api/brandsList",     "GET",  null, false),
        new EndpointDefinition("/api/searchProduct",  "POST",
            "search_product=top", true),
        new EndpointDefinition("/api/verifyLogin",    "POST",
            "email=test@test.com&password=test123", true),
        new EndpointDefinition("/api/createAccount",  "POST",
            "name=Test&email=test@test.com&password=Test1234&" +
            "firstname=Test&lastname=User&address1=123 St&" +
            "city=NYC&state=NY&zipcode=10001&country=United States&" +
            "mobile_number=1234567890", true),
        new EndpointDefinition("/api/getUserDetailByEmail", "GET",
            null, true),
        new EndpointDefinition("/api/deleteAccount",  "DELETE",
            "email=test@test.com&password=test123", true),
        new EndpointDefinition("/api/updateAccount",  "PUT",
            "name=Test&email=test@test.com&password=Test1234&" +
            "firstname=Test&lastname=User&address1=123 St&" +
            "city=NYC&state=NY&zipcode=10001&country=United States&" +
            "mobile_number=1234567890", true)
    );

    public ApiDiscoveryAgent(String baseUrl) {
        this.baseUrl = baseUrl;
        RestAssured.baseURI = baseUrl;
        RestAssured.useRelaxedHTTPSValidation();
    }

    /**
     * Main entry point — discovers and probes all endpoints
     */
    public List<ApiEndpoint> discover() {
        System.out.println("\n" + FrameworkConstants.LOG_INFO +
                " ============================================");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Agent 5A: API Discovery Started");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Base URL: " + baseUrl);
        System.out.println(FrameworkConstants.LOG_INFO +
                " ============================================");

        List<ApiEndpoint> discovered = new ArrayList<>();

        for (int i = 0; i < KNOWN_ENDPOINTS.size(); i++) {
            EndpointDefinition def = KNOWN_ENDPOINTS.get(i);
            System.out.println("\n" + FrameworkConstants.LOG_INFO +
                    " Probing " + (i + 1) + "/" + KNOWN_ENDPOINTS.size() +
                    ": " + def.method + " " + def.path);

            ApiEndpoint endpoint = probeEndpoint(def);
            discovered.add(endpoint);

            System.out.println(FrameworkConstants.LOG_SUCCESS +
                    " " + endpoint);

            // Brief pause between requests - set to 0 for faster execution (use with caution)
            sleep(0);
        }

        printSummary(discovered);
        return discovered;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENDPOINT PROBING
    // ─────────────────────────────────────────────────────────────────────────

    private ApiEndpoint probeEndpoint(EndpointDefinition def) {
        ApiEndpoint endpoint = new ApiEndpoint(baseUrl + def.path, def.method);
        endpoint.setAuthType("NONE"); // automationexercise uses no auth header

        try {
            Response response = sendRequest(def);

            endpoint.setStatusCode(response.getStatusCode());
            endpoint.setContentType(response.getContentType());
            endpoint.setWorking(response.getStatusCode() == 200 ||
                                response.getStatusCode() == 201);

            String body = response.getBody().asString();
            // Trim large responses
            String trimmedBody = body.length() > 500
                    ? body.substring(0, 500) + "..."
                    : body;
            endpoint.setResponseBody(trimmedBody);

            // Analyze response structure
            analyzeResponse(endpoint, body);

            // Add suggested test types
            suggestTestTypes(endpoint, def);

        } catch (Exception e) {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " Failed to probe: " + def.path + " — " + e.getMessage());
            endpoint.setWorking(false);
            endpoint.setStatusCode(-1);
        }

        return endpoint;
    }

    private Response sendRequest(EndpointDefinition def) {
        switch (def.method.toUpperCase()) {
            case "GET" -> {
                return given()
                        .header("Accept", "application/json")
                        .when()
                        .get(def.path)
                        .then()
                        .extract()
                        .response();
            }
            case "POST" -> {
                if (def.formBody != null) {
                    // Parse form params
                    var spec = given()
                            .header("Accept", "application/json")
                            .contentType("application/x-www-form-urlencoded");
                    for (String param : def.formBody.split("&")) {
                        String[] kv = param.split("=", 2);
                        if (kv.length == 2) {
                            spec = spec.formParam(kv[0], kv[1]);
                        }
                    }
                    return spec.when()
                            .post(baseUrl + def.path)
                            .then()
                            .extract()
                            .response();
                } else {
                    return given()
                            .header("Accept", "application/json")
                            .when()
                            .post(baseUrl + def.path)
                            .then()
                            .extract()
                            .response();
                }
            }
            case "PUT" -> {
                var spec = given()
                        .header("Accept", "application/json")
                        .contentType("application/x-www-form-urlencoded");
                if (def.formBody != null) {
                    for (String param : def.formBody.split("&")) {
                        String[] kv = param.split("=", 2);
                        if (kv.length == 2) {
                            spec = spec.formParam(kv[0], kv[1]);
                        }
                    }
                }
                return spec.when()
                        .put(baseUrl + def.path)
                        .then()
                        .extract()
                        .response();
            }
            case "DELETE" -> {
                var spec = given()
                        .header("Accept", "application/json")
                        .contentType("application/x-www-form-urlencoded");
                if (def.formBody != null) {
                    for (String param : def.formBody.split("&")) {
                        String[] kv = param.split("=", 2);
                        if (kv.length == 2) {
                            spec = spec.formParam(kv[0], kv[1]);
                        }
                    }
                }
                return spec.when()
                        .delete(baseUrl + def.path)
                        .then()
                        .extract()
                        .response();
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported method: " + def.method);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESPONSE ANALYSIS
    // ─────────────────────────────────────────────────────────────────────────

    private void analyzeResponse(ApiEndpoint endpoint, String body) {
        if (body == null || body.isBlank()) {
            endpoint.setResponseType("EMPTY");
            return;
        }

        try {
            JsonElement element = JsonParser.parseString(body);

            if (element.isJsonObject()) {
                endpoint.setResponseType("OBJECT");
                JsonObject obj = element.getAsJsonObject();

                // Extract top-level keys
                List<String> keys = new ArrayList<>();
                for (String key : obj.keySet()) {
                    keys.add(key);
                }
                endpoint.setResponseKeys(keys);

                // Check for automationexercise responseCode pattern
                if (obj.has("responseCode")) {
                    endpoint.setHasResponseCode(true);
                    int code = obj.get("responseCode").getAsInt();
                    endpoint.setStatusCode(code);
                }

            } else if (element.isJsonArray()) {
                endpoint.setResponseType("ARRAY");
                JsonArray arr = element.getAsJsonArray();

                // Extract keys from first element
                if (!arr.isEmpty() && arr.get(0).isJsonObject()) {
                    JsonObject first = arr.get(0).getAsJsonObject();
                    List<String> keys = new ArrayList<>();
                    for (String key : first.keySet()) {
                        keys.add(key);
                    }
                    endpoint.setResponseKeys(keys);
                }
            } else {
                endpoint.setResponseType("PRIMITIVE");
            }

        } catch (Exception e) {
            // Not JSON — might be HTML or plain text
            endpoint.setResponseType("NON_JSON");
            if (body.contains("<html")) {
                endpoint.setResponseType("HTML");
            }
        }
    }

    private void suggestTestTypes(ApiEndpoint endpoint, EndpointDefinition def) {
        // Always suggest positive test
        endpoint.addSuggestedTestType("positive_200");

        // Suggest negative tests based on method
        if (def.hasFormParams) {
            endpoint.addSuggestedTestType("negative_missing_params");
            endpoint.addSuggestedTestType("negative_invalid_params");
        }

        // Suggest response schema validation
        if (!endpoint.getResponseKeys().isEmpty()) {
            endpoint.addSuggestedTestType("schema_validation");
        }

        // Suggest response time test
        endpoint.addSuggestedTestType("response_time");

        // Suggest content type validation
        endpoint.addSuggestedTestType("content_type_validation");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void printSummary(List<ApiEndpoint> endpoints) {
        long working = endpoints.stream().filter(ApiEndpoint::isWorking).count();
        long failing = endpoints.size() - working;

        System.out.println("\n" + FrameworkConstants.LOG_INFO +
                " ============================================");
        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Agent 5A Complete!");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Endpoints discovered : " + endpoints.size());
        System.out.println(FrameworkConstants.LOG_INFO +
                " Working              : " + working);
        System.out.println(FrameworkConstants.LOG_INFO +
                " Failing              : " + failing);
        System.out.println(FrameworkConstants.LOG_INFO +
                " ============================================\n");

        System.out.println(FrameworkConstants.LOG_INFO +
                " Endpoint Summary:");
        for (ApiEndpoint ep : endpoints) {
            System.out.println("   " + ep);
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INNER CLASS — Endpoint Definition
    // ─────────────────────────────────────────────────────────────────────────

    private record EndpointDefinition(
            String path,
            String method,
            String formBody,
            boolean hasFormParams
    ) {}
}