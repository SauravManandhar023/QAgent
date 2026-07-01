package com.saurav.agentic.agents;

import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.ApiEndpoint;
import com.saurav.agentic.models.ApiTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * ApiTestCaseGeneratorAgent - Agent 5B
 *
 * Takes discovered endpoints from Agent 5A
 * Generates structured API test cases using templates
 * Zero AI tokens — pure template-based generation
 *
 * Per endpoint generates:
 * - Positive: valid request → expected response
 * - Negative: missing/invalid params → error response
 * - Edge: empty params, boundary values
 * - Schema: response structure validation
 * - Performance: response time check
 */
public class ApiTestCaseGeneratorAgent {

    private int testCounter = 1;

    public List<ApiTestCase> generate(List<ApiEndpoint> endpoints) {

        System.out.println("\n" + FrameworkConstants.LOG_INFO +
                " ============================================");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Agent 5B: API Test Case Generator Started");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Strategy: Template-based (0 tokens)");
        System.out.println(FrameworkConstants.LOG_INFO +
                " ============================================");

        List<ApiTestCase> allTestCases = new ArrayList<>();

        for (ApiEndpoint endpoint : endpoints) {
            System.out.println(FrameworkConstants.LOG_INFO +
                    " Generating test cases for: " +
                    endpoint.getMethod() + " " + endpoint.getPath());

            List<ApiTestCase> cases = generateForEndpoint(endpoint);
            allTestCases.addAll(cases);

            System.out.println(FrameworkConstants.LOG_SUCCESS +
                    "   Generated " + cases.size() + " test cases");
        }

        printSummary(allTestCases);
        return allTestCases;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEMPLATE ENGINE — generates test cases per endpoint
    // ─────────────────────────────────────────────────────────────────────────

    private List<ApiTestCase> generateForEndpoint(ApiEndpoint endpoint) {
        List<ApiTestCase> cases = new ArrayList<>();

        switch (endpoint.getMethod().toUpperCase()) {
            case "GET"    -> cases.addAll(generateGetTests(endpoint));
            case "POST"   -> cases.addAll(generatePostTests(endpoint));
            case "PUT"    -> cases.addAll(generatePutTests(endpoint));
            case "DELETE" -> cases.addAll(generateDeleteTests(endpoint));
        }

        return cases;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET TEMPLATES
    // ─────────────────────────────────────────────────────────────────────────

    private List<ApiTestCase> generateGetTests(ApiEndpoint endpoint) {
        List<ApiTestCase> cases = new ArrayList<>();
        String path = endpoint.getPath();
        String name = extractName(path);

        // 1. Positive — basic status code
        cases.add(build(
            "Verify GET " + name + " returns 200",
            path, "GET", null, null,
            "200", null, "STATUS_CODE",
            "Positive", "High",
            "Send GET request to " + path + " and verify HTTP 200 response"
        ));

        // 2. Response time
        cases.add(build(
            "Verify GET " + name + " response time is acceptable",
            path, "GET", null, null,
            "200", "responseTime < 2000ms", "RESPONSE_TIME",
            "Edge", "Medium",
            "Verify GET " + path + " responds within 2000 milliseconds"
        ));

        // 3. Content type
        cases.add(build(
            "Verify GET " + name + " returns JSON content type",
            path, "GET", null, null,
            "200", "application/json", "CONTENT_TYPE",
            "Positive", "Medium",
            "Verify response Content-Type is application/json"
        ));

        // 4. Response structure if we have keys
        if (!endpoint.getResponseKeys().isEmpty()) {
            String firstKey = endpoint.getResponseKeys().get(0);
            cases.add(build(
                "Verify GET " + name + " response contains " + firstKey,
                path, "GET", null, null,
                "200", firstKey, "FIELD_EXISTS",
                "Positive", "High",
                "Verify response body contains field: " + firstKey
            ));
        }

        // 5. Response type array check
        if ("ARRAY".equals(endpoint.getResponseType())) {
            cases.add(build(
                "Verify GET " + name + " returns non-empty list",
                path, "GET", null, null,
                "200", "list.size() > 0", "FIELD_VALUE",
                "Positive", "High",
                "Verify response array is not empty"
            ));
        }

        // 6. responseCode field check (automationexercise pattern)
        if (endpoint.isHasResponseCode()) {
            cases.add(build(
                "Verify GET " + name + " responseCode is 200",
                path, "GET", null, null,
                "200", "responseCode=200", "FIELD_VALUE",
                "Positive", "High",
                "Verify JSON body responseCode field equals 200"
            ));
        }

        return cases;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST TEMPLATES
    // ─────────────────────────────────────────────────────────────────────────

    private List<ApiTestCase> generatePostTests(ApiEndpoint endpoint) {
        List<ApiTestCase> cases = new ArrayList<>();
        String path = endpoint.getPath();
        String name = extractName(path);

        // 1. Positive — with valid params
        String validParams = buildValidParams(endpoint);
        cases.add(build(
            "Verify POST " + name + " with valid params returns 200",
            path, "POST", null, validParams,
            "200", null, "STATUS_CODE",
            "Positive", "High",
            "Send POST to " + path + " with valid parameters and verify 200"
        ));

        // 2. Negative — missing required params
        cases.add(build(
            "Verify POST " + name + " with missing params returns 400",
            path, "POST", null, null,
            "400", null, "STATUS_CODE",
            "Negative", "High",
            "Send POST to " + path + " without required parameters and verify 400"
        ));

        // 3. Negative — invalid param values
        cases.add(build(
            "Verify POST " + name + " with invalid params returns error",
            path, "POST", null, "invalid_param=invalid_value",
            "400", null, "STATUS_CODE",
            "Negative", "Medium",
            "Send POST to " + path + " with invalid parameter values"
        ));

        // 4. Response time
        cases.add(build(
            "Verify POST " + name + " response time is acceptable",
            path, "POST", null, validParams,
            "200", "responseTime < 3000ms", "RESPONSE_TIME",
            "Edge", "Low",
            "Verify POST " + path + " responds within 3000 milliseconds"
        ));

        // 5. Content type
        cases.add(build(
            "Verify POST " + name + " returns JSON content type",
            path, "POST", null, validParams,
            "200", "application/json", "CONTENT_TYPE",
            "Positive", "Medium",
            "Verify POST response Content-Type is application/json"
        ));

        // 6. responseCode check
        if (endpoint.isHasResponseCode()) {
            cases.add(build(
                "Verify POST " + name + " responseCode is 200 with valid params",
                path, "POST", null, validParams,
                "200", "responseCode=200", "FIELD_VALUE",
                "Positive", "High",
                "Verify JSON body responseCode field equals 200"
            ));
        }

        return cases;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT TEMPLATES
    // ─────────────────────────────────────────────────────────────────────────

    private List<ApiTestCase> generatePutTests(ApiEndpoint endpoint) {
        List<ApiTestCase> cases = new ArrayList<>();
        String path = endpoint.getPath();
        String name = extractName(path);
        String validParams = buildValidParams(endpoint);

        // 1. Positive
        cases.add(build(
            "Verify PUT " + name + " with valid params returns 200",
            path, "PUT", null, validParams,
            "200", null, "STATUS_CODE",
            "Positive", "High",
            "Send PUT to " + path + " with valid parameters and verify 200"
        ));

        // 2. Negative — missing params
        cases.add(build(
            "Verify PUT " + name + " with missing params returns 400",
            path, "PUT", null, null,
            "400", null, "STATUS_CODE",
            "Negative", "High",
            "Send PUT to " + path + " without required parameters"
        ));

        // 3. Response time
        cases.add(build(
            "Verify PUT " + name + " response time is acceptable",
            path, "PUT", null, validParams,
            "200", "responseTime < 3000ms", "RESPONSE_TIME",
            "Edge", "Low",
            "Verify PUT " + path + " responds within 3000 milliseconds"
        ));

        return cases;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE TEMPLATES
    // ─────────────────────────────────────────────────────────────────────────

    private List<ApiTestCase> generateDeleteTests(ApiEndpoint endpoint) {
        List<ApiTestCase> cases = new ArrayList<>();
        String path = endpoint.getPath();
        String name = extractName(path);
        String validParams = buildValidParams(endpoint);

        // 1. Positive
        cases.add(build(
            "Verify DELETE " + name + " with valid params returns 200",
            path, "DELETE", null, validParams,
            "200", null, "STATUS_CODE",
            "Positive", "High",
            "Send DELETE to " + path + " with valid params and verify 200"
        ));

        // 2. Negative — missing params
        cases.add(build(
            "Verify DELETE " + name + " with missing params returns 400",
            path, "DELETE", null, null,
            "400", null, "STATUS_CODE",
            "Negative", "High",
            "Send DELETE to " + path + " without required parameters"
        ));

        // 3. Response time
        cases.add(build(
            "Verify DELETE " + name + " response time is acceptable",
            path, "DELETE", null, validParams,
            "200", "responseTime < 3000ms", "RESPONSE_TIME",
            "Edge", "Low",
            "Verify DELETE " + path + " responds within 3000 milliseconds"
        ));

        return cases;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private ApiTestCase build(
            String name, String endpoint, String method,
            String requestBody, String requestParams,
            String expectedStatus, String expectedField,
            String assertionType, String testType,
            String priority, String description) {

        ApiTestCase tc = new ApiTestCase();
        tc.setTestCaseId("TC_API_" + String.format("%03d", testCounter++));
        tc.setTestCaseName(name);
        tc.setEndpoint(endpoint);
        tc.setMethod(method);
        tc.setRequestBody(requestBody != null ? requestBody : "");
        tc.setRequestParams(requestParams != null ? requestParams : "");
        tc.setExpectedStatusCode(expectedStatus);
        tc.setExpectedResponseField(expectedField != null ? expectedField : "");
        tc.setAssertionType(assertionType);
        tc.setTestType(testType);
        tc.setPriority(priority);
        tc.setDescription(description);
        tc.setAutomationFeasible(true);
        return tc;
    }

    /**
     * Extracts readable name from path
     * /api/productsList → productsList
     */
    private String extractName(String path) {
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }

    /**
     * Builds valid params string from endpoint form params
     */
    private String buildValidParams(ApiEndpoint endpoint) {
        if (!endpoint.getFormParams().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            endpoint.getFormParams().forEach((k, v) ->
                sb.append(k).append("=").append(v).append("&"));
            return sb.toString();
        }
        return "";
    }

    private void printSummary(List<ApiTestCase> cases) {
        long positive = cases.stream()
                .filter(c -> "Positive".equals(c.getTestType())).count();
        long negative = cases.stream()
                .filter(c -> "Negative".equals(c.getTestType())).count();
        long edge = cases.stream()
                .filter(c -> "Edge".equals(c.getTestType())).count();

        System.out.println("\n" + FrameworkConstants.LOG_INFO +
                " ============================================");
        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Agent 5B Complete!");
        System.out.println(FrameworkConstants.LOG_INFO +
                " Total API test cases : " + cases.size());
        System.out.println(FrameworkConstants.LOG_INFO +
                " Positive             : " + positive);
        System.out.println(FrameworkConstants.LOG_INFO +
                " Negative             : " + negative);
        System.out.println(FrameworkConstants.LOG_INFO +
                " Edge                 : " + edge);
        System.out.println(FrameworkConstants.LOG_INFO +
                " LLM tokens used      : 0");
        System.out.println(FrameworkConstants.LOG_INFO +
                " ============================================\n");
    }
}