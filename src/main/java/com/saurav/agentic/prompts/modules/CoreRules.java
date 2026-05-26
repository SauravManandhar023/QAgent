package com.saurav.agentic.prompts.modules;

/**
 * CoreRules - Base rules shared across all agents
 * Focused only on universal QA principles
 */
public class CoreRules {

    private CoreRules() {}

    public static String get() {
        return """
                CORE QA RULES (apply to all generated code):
                1. Every test must have a clear, single responsibility
                2. Test names must describe what is being tested and expected outcome
                   CORRECT: testLoginFailsWithInvalidPassword()
                   WRONG:   testLogin2()
                3. Never hardcode environment-specific values (URLs, credentials)
                   Use values provided in the test case data instead
                4. Every test must be independent — no test should depend on another
                5. Cleanup must happen in @AfterMethod — never inside @Test
                6. Comments must explain WHY, not WHAT the code does
                """;
    }
}