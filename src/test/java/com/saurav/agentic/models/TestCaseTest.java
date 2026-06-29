package com.saurav.agentic.models;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for TestCase model — getters, setters, toString.
 */
public class TestCaseTest {

    private TestCase tc;

    @BeforeMethod
    public void setUp() {
        tc = new TestCase();
    }

    @Test
    public void testDefaultValues() {
        assertNull(tc.getTestCaseId());
        assertNull(tc.getTestCaseName());
        assertNull(tc.getDescription());
        assertNull(tc.getPreconditions());
        assertNull(tc.getTestSteps());
        assertNull(tc.getTestData());
        assertNull(tc.getExpectedResult());
        assertNull(tc.getTestType());
        assertNull(tc.getPriority());
        assertNull(tc.getComponent());
        assertFalse(tc.isAutomationFeasible());
    }

    @Test
    public void testSettersAndGetters() {
        tc.setTestCaseId("TC_001");
        tc.setTestCaseName("Verify valid login");
        tc.setDescription("Test that user can log in");
        tc.setPreconditions("User registered");
        tc.setTestSteps("1. Enter username\n2. Enter password\n3. Click Login");
        tc.setTestData("username: admin, password: pass123");
        tc.setExpectedResult("User is logged in");
        tc.setTestType("Positive");
        tc.setPriority("High");
        tc.setComponent("Login Form");
        tc.setAutomationFeasible(true);

        assertEquals(tc.getTestCaseId(), "TC_001");
        assertEquals(tc.getTestCaseName(), "Verify valid login");
        assertEquals(tc.getDescription(), "Test that user can log in");
        assertEquals(tc.getPreconditions(), "User registered");
        assertEquals(tc.getTestSteps(), "1. Enter username\n2. Enter password\n3. Click Login");
        assertEquals(tc.getTestData(), "username: admin, password: pass123");
        assertEquals(tc.getExpectedResult(), "User is logged in");
        assertEquals(tc.getTestType(), "Positive");
        assertEquals(tc.getPriority(), "High");
        assertEquals(tc.getComponent(), "Login Form");
        assertTrue(tc.isAutomationFeasible());
    }

    @Test
    public void testToString() {
        tc.setTestCaseId("TC_001");
        tc.setTestCaseName("Verify login");
        tc.setTestType("Positive");
        tc.setPriority("High");

        String result = tc.toString();
        assertTrue(result.contains("TC_001"));
        assertTrue(result.contains("Verify login"));
        assertTrue(result.contains("Positive"));
        assertTrue(result.contains("High"));
    }

    @Test
    public void testAutomationFeasibleDefaultFalse() {
        assertFalse(tc.isAutomationFeasible());
    }

    @Test
    public void testAutomationFeasibleSetTrue() {
        tc.setAutomationFeasible(true);
        assertTrue(tc.isAutomationFeasible());
    }

    @Test
    public void testAllNullFields() {
        tc.setTestData(null);
        assertNull(tc.getTestData());
    }

    @Test
    public void testEmptyStringValues() {
        tc.setTestCaseId("");
        tc.setTestCaseName("");
        tc.setTestType("");

        assertEquals(tc.getTestCaseId(), "");
        assertEquals(tc.getTestCaseName(), "");
        assertEquals(tc.getTestType(), "");
    }

    @Test
    public void testComponentAssignment() {
        tc.setComponent("Checkout Page");
        assertEquals(tc.getComponent(), "Checkout Page");

        tc.setComponent("");
        assertEquals(tc.getComponent(), "");
    }
}
