package com.saurav.agentic.models;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for CompileResult model — used by Agent 3 and Agent 4.
 */
public class CompileResultTest {

    private CompileResult result;

    @BeforeMethod
    public void setUp() {
        result = new CompileResult("/path/to/TestFile.java", "TestFile");
    }

    @Test
    public void testConstructor() {
        assertEquals(result.getFilePath(), "/path/to/TestFile.java");
        assertEquals(result.getClassName(), "TestFile");
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().isEmpty());
        assertNull(result.getSourceCode());
    }

    @Test
    public void testSetSuccess() {
        assertFalse(result.isSuccess());
        result.setSuccess(true);
        assertTrue(result.isSuccess());
    }

    @Test
    public void testAddError() {
        result.addError("Line 42: cannot find symbol");
        assertEquals(result.getErrors().size(), 1);
        assertEquals(result.getErrors().get(0), "Line 42: cannot find symbol");
    }

    @Test
    public void testAddMultipleErrors() {
        result.addError("Error 1");
        result.addError("Error 2");
        result.addError("Error 3");

        assertEquals(result.getErrors().size(), 3);
        assertTrue(result.hasErrors());
    }

    @Test
    public void testSetSourceCode() {
        String code = "package generated.ui;\npublic class TestFile {}";
        result.setSourceCode(code);
        assertEquals(result.getSourceCode(), code);
    }

    @Test
    public void testHasErrorsFalseWhenEmpty() {
        assertFalse(result.hasErrors());
    }

    @Test
    public void testHasErrorsTrueAfterAdding() {
        result.addError("Some error");
        assertTrue(result.hasErrors());
    }

    @Test
    public void testGetErrorSummaryNoErrors() {
        assertEquals(result.getErrorSummary(), "No errors");
    }

    @Test
    public void testGetErrorSummaryWithErrors() {
        result.addError("Line 10: missing semicolon");
        result.addError("Line 15: unreachable statement");

        String summary = result.getErrorSummary();
        assertTrue(summary.contains("Error 1:"));
        assertTrue(summary.contains("Line 10: missing semicolon"));
        assertTrue(summary.contains("Error 2:"));
        assertTrue(summary.contains("Line 15: unreachable statement"));
    }

    @Test
    public void testToStringCompiledOk() {
        result.setSuccess(true);
        String str = result.toString();
        assertTrue(str.contains("COMPILED OK"));
        assertTrue(str.contains("TestFile"));
    }

    @Test
    public void testToStringCompiledFailed() {
        result.addError("Error 1");
        String str = result.toString();
        assertTrue(str.contains("FAILED"));
        assertTrue(str.contains("1 error(s)"));
    }

    @Test
    public void testToStringMultipleErrors() {
        result.addError("Error 1");
        result.addError("Error 2");
        result.addError("Error 3");
        String str = result.toString();
        assertTrue(str.contains("FAILED"));
        assertTrue(str.contains("3 error(s)"));
    }

    @Test
    public void testDefaultConstructorState() {
        CompileResult defaultResult = new CompileResult("", "");
        assertEquals(defaultResult.getFilePath(), "");
        assertEquals(defaultResult.getClassName(), "");
        assertFalse(defaultResult.isSuccess());
        assertTrue(defaultResult.getErrors().isEmpty());
    }
}
