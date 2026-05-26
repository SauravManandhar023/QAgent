package com.saurav.agentic.prompts.modules;

/**
 * RecoveryRules - Error recovery and fallback rules
 * Prevents hallucination when generation is ambiguous
 */
public class RecoveryRules {

    private RecoveryRules() {}

    public static String get() {
        return """
                ERROR RECOVERY RULES:
                1. If a required POM method does not exist — add a TODO comment:
                   // TODO: POM missing method — add getSuccessMessage() to page class
                   NEVER invent a method that is not in the provided POM
                
                2. If a locator cannot be determined from available information:
                   // TODO: Verify locator — element not found in page analysis
                   NEVER guess a locator
                
                3. If a test step cannot be automated with available methods:
                   // TODO: Manual verification required — automation not possible
                   DO NOT skip the test — add it with TODO comments
                
                4. If an import is uncertain — omit it rather than guessing
                   Unused or wrong imports cause compile errors
                
                5. If generation is completely impossible for a test case:
                   Generate the method signature and add:
                   // TODO: Test case cannot be automated — requires: [reason]
                   throw new org.testng.SkipException("Not yet automated");
                """;
    }
}