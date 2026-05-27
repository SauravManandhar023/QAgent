package com.saurav.agentic.models;

import java.util.ArrayList;
import java.util.List;

/**
 * CompileResult - Holds the result of compiling a single Java file
 * Used by Agent 3 to pass structured error info to Agent 4
 */
public class CompileResult {

    private String filePath;
    private String className;
    private boolean success;
    private List<String> errors;
    private String sourceCode;

    public CompileResult(String filePath, String className) {
        this.filePath  = filePath;
        this.className = className;
        this.errors    = new ArrayList<>();
        this.success   = false;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getFilePath()  { return filePath; }
    public String getClassName() { return className; }
    public boolean isSuccess()   { return success; }
    public List<String> getErrors() { return errors; }
    public String getSourceCode() { return sourceCode; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setSuccess(boolean success)     { this.success = success; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
    public void addError(String error)          { this.errors.add(error); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean hasErrors() { return !errors.isEmpty(); }

    public String getErrorSummary() {
        if (errors.isEmpty()) return "No errors";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            sb.append("Error ").append(i + 1).append(": ")
              .append(errors.get(i)).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "[" + className + "] " +
               (success ? "COMPILED OK" : "FAILED — " + errors.size() + " error(s)");
    }
}