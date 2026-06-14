package base;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.BeforeClass;

import static io.restassured.RestAssured.given;

/**
 * BaseApiTest - Base class for all AI-generated API test classes
 *
 * Provides:
 * - Base URL configuration
 * - Common request specification (headers, content type, filters)
 * - Allure reporting integration
 * - Reusable GET/POST/PUT/DELETE helpers
 */
public class BaseApiTest {

    protected static RequestSpecification requestSpec;
    protected static String BASE_URL;

    @BeforeClass
    public void setupApi() {
        BASE_URL = System.getProperty("api.base.url",
                "https://automationexercise.com");

        RestAssured.baseURI = BASE_URL;

        requestSpec = given()
                .filter(new AllureRestAssured())       // Allure logging
                .filter(new RequestLoggingFilter())    // Console request log
                .filter(new ResponseLoggingFilter())   // Console response log
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    // ── Reusable HTTP methods ─────────────────────────────────────────────────

    protected Response get(String endpoint) {
        return given(requestSpec)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }

    protected Response post(String endpoint, Object body) {
        return given(requestSpec)
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .extract()
                .response();
    }

    protected Response put(String endpoint, Object body) {
        return given(requestSpec)
                .body(body)
                .when()
                .put(endpoint)
                .then()
                .extract()
                .response();
    }

    protected Response delete(String endpoint) {
        return given(requestSpec)
                .when()
                .delete(endpoint)
                .then()
                .extract()
                .response();
    }

    protected Response postForm(String endpoint, java.util.Map<String, String> params) {
        RequestSpecification spec = given(requestSpec)
                .contentType(ContentType.URLENC);
        params.forEach(spec::formParam);
        return spec.when()
                .post(endpoint)
                .then()
                .extract()
                .response();
    }

    // ── Common assertions ─────────────────────────────────────────────────────

    protected void assertStatusCode(Response response, int expected) {
        org.testng.Assert.assertEquals(response.getStatusCode(), expected,
                "Status code mismatch");
    }

    protected void assertResponseContains(Response response, String key) {
        org.testng.Assert.assertNotNull(response.jsonPath().get(key),
                "Response missing key: " + key);
    }

    protected void assertResponseCode(Response response, int expectedCode) {
        int actualCode = response.jsonPath().getInt("responseCode");
        org.testng.Assert.assertEquals(actualCode, expectedCode,
                "Response code mismatch");
    }
}