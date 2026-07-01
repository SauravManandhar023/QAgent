# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 📋 Commonly Used Commands

### Build and Dependencies
- `mvn clean install` - Build the project and install dependencies
- `mvn compile` - Compile source code
- `mvn test` - Run all generated tests (TestNG)
- `mvn exec:java -Dexec.mainClass="com.saurav.agentic.runners.Main"` - Run the full QAgent pipeline

### Running the Pipeline
The QAgent pipeline consists of multiple agents that work sequentially:
1. **Agent 1** (TestCaseGeneratorAgent): Scrapes a URL, generates test cases via configured LLM, saves to Excel
2. **Agent 2** (ScriptGeneratorAgent): Reads Excel, generates Page Object Model (POM) and TestNG test classes
3. **Agent 3** (Agent3_Compiler): Compiles generated test classes to verify syntax
4. **Agent 4** (Agent4_Reviewer): Reviews and fixes generated test classes
5. **Optional API Agents** (Agent 5A/5B): For API testing (enabled via `RUN_API_AGENTS` flag in Main.java)

**All agents use the LLM provider configured in `config.properties` — no code changes needed to switch providers.**

#### To run the full pipeline:
```bash
mvn exec:java -Dexec.mainClass="com.saurav.agentic.runners.Main"
```

#### To skip Agent 1 (reuse existing Excel):
1. Edit `src/main/java/com/saurav/agentic/runners/Main.java`
2. Set `private static final boolean SKIP_AGENT1 = true;`
3. Ensure the Excel file (`test-output/excel/ui-test-cases.xlsx`) exists and matches the current URL
4. Run the pipeline command above

#### To change LLM provider:
Edit `config.properties` and set:
```properties
llm.provider=groq     # or: ollama, claude, openai, gemini, nim, deepseek, fcc
```
No code changes needed. Add per-provider config in the same file.

#### To disable API pipeline:
1. Edit `src/main/java/com/saurav/agentic/runners/Main.java`
2. Set `private static final boolean RUN_API_AGENTS = false;`
3. Run the pipeline command above

### Running Specific Tests
After generating tests with Agent 2, you can run:
- `mvn test` - Run all tests
- `mvn test -Dtest=*LoginFormTest*` - Run tests matching pattern (Surefire syntax)
- To run a single test class: `mvn test -Dtest=LoginFormTest`
- To run a specific test method: `mvn test -Dtest=LoginFormTest#testValidLogin`

### Generating Allure Reports
```bash
mvn site
```
Reports are generated in `target/site/allure-maven-plugin/` (requires test execution first)

## 🏗️ High-Level Architecture

### Core Pipeline Flow
```
URL Input
    ↓
Agent 1: TestCaseGeneratorAgent
    → Scrapes UI elements with SeleniumScraper
    → Sends analysis to LLM (via LLMService)
    → Parses LLM response into TestCase objects
    → Saves to Excel (test-output/excel/ui-test-cases.xlsx)
    ↓
Agent 2: ScriptGeneratorAgent
    → Reads Excel, groups test cases by component
    → For each component:
        • Generates Page Object Model (POM) class via LLM
        • Generates TestNG test class via LLM (batches of ≤5 test cases)
    → Saves POM to src/test/java/pages/
    → Saves test classes to src/test/java/generated/ui/
    ↓
Agent 3: Agent3_Compiler
    → Compiles all generated test classes
    → Returns compilation results (success/failure per file)
    ↓
Agent 4: Agent4_Reviewer
    → Reviews compilation failures
    → Pass 1: Deterministic rule fixes (0 tokens)
    → Pass 2: Compile — if passes, done
    → Pass 3: Error-context-only LLM fix (minimal tokens)
    → Pass 4: Re-compile to verify
    ↓
Agent 5: TestExecutionAgent
    → Runs the generated test suite via TestNG
    → Generates Allure reports
    ↓
Ready-to-run Selenium test suite
```

### LLM Provider Architecture
```
Agent
  ↓
LLMService (provider-agnostic, handles caching)
  ↓
ProviderFactory (selects provider via config)
  ↓
LLMProvider interface
  ├── GroqProvider       — Groq API (OpenAI-compatible)
  ├── OllamaProvider     — Local Ollama instance
  ├── ClaudeProvider     — Anthropic API (stub)
  ├── OpenAIProvider     — OpenAI API (stub)
  ├── GeminiProvider     — Google API (stub)
  ├── NvidiaNimProvider  — Nvidia NIM (stub)
  ├── DeepSeekProvider   — DeepSeek API (stub)
  └── FCCProvider        — FCC Admin proxy (15+ providers)
```

### Key Modules

#### LLM Layer (`com.saurav.agentic.llm`)
- `LLMProvider`: Interface all providers implement — `chat()`, `chatWithTools()`, `supportsToolCalling()`, etc.
- `LLMService`: Drop-in replacement for GroqClient — agents call this exclusively
- `ProviderFactory`: Creates and caches provider instances based on `llm.provider` config
- `GroqProvider` / `OllamaProvider` / `FCCProvider`: Fully implemented providers
- `ClaudeProvider` / `OpenAIProvider` / etc.: Stub providers ready for integration

#### Agents (`com.saurav.agentic.agents`)
- `TestCaseGeneratorAgent`: Agent 1 - UI test case generation
- `ScriptGeneratorAgent`: Agent 2 - Selenium script generation (POM + TestNG)
- `Agent3_Compiler`: Agent 3 - Java compilation validator
- `Agent4_Reviewer`: Agent 4 - Code review and fixer
- `TestExecutionAgent`: Agent 5 - Runs generated tests
- `ApiDiscoveryAgent`: Agent 5A - API endpoint discovery
- `ApiTestCaseGeneratorAgent`: Agent 5B - API test case generation

#### Utilities (`com.saurav.agentic.utils`)
- `GroqClient`: Legacy client — kept for backward compat. New code should use `LLMService`.
- `SeleniumScraper`: Handles web scraping with Selenium/WebDriverManager
- `ExcelUtil` / `ApiExcelUtil`: Read/write Excel files (Apache POI)
- `PromptBuilder`: Builds base prompts for AI agents
- `PomPromptComposer` / `ScriptPromptComposer`: Component-specific prompt composition
- `ConfigReader`: Reads properties files
- `DriverFactory`: Manages WebDriver lifecycle (Chrome/Firefox)
- `LlmCache`: Persistent disk-backed cache for LLM responses (provider-independent)

#### Configuration (`com.saurav.agentic.config`)
- `FrameworkConfig`: Singleton for application properties (config.properties)
- `ModelConfig`: Singleton for AI model parameters (temperature, max tokens, etc.)

#### Models (`com.saurav.agentic.models`)
- `TestCase`: Data model for a single test case
- `PageMetadata` / `PageElement`: Page structure analysis
- `CompileResult`: Compilation result with errors
- `ApiEndpoint` / `ApiTestCase`: API-specific models

#### Constants (`com.saurav.agentic.constants`)
- `FrameworkConstants`: Log prefixes, separators, and default values

### Important Directories
- `src/main/java/` - Core framework code
- `src/test/java/` - 
  - `base/` - BaseTest class (TestNG setup/teardown)
  - `pages/` - Generated POM classes (by Agent 2)
  - `generated/ui/` - Generated test classes (by Agent 2)
- `test-output/excel/` - UI test cases Excel file (Agent 1 output)
- `test-output/pending-components.txt` - Tracks components skipped due to rate limits
- `allure-results/` - Allure test results (after test execution)

### Technology Stack
- **Java 21** - Core language
- **Selenium 4.44.0** - Browser automation and scraping
- **WebDriverManager 6.3.4** - Automatic driver management
- **TestNG 7.12.0** - Test framework
- **Apache POI 5.4.0** - Excel read/write
- **OkHttp 4.12.0** - HTTP client
- **Gson 2.11.0** - JSON parsing
- **JSoup 1.18.3** - HTML parsing (used in scraping)
- **Allure 2.27.0** - Test reporting
- **LLM providers**: Groq, Ollama, FCC Admin (implemented); Claude, OpenAI, Gemini, NIM, DeepSeek (stubs)

### Key Design Decisions
1. **Provider-agnostic agents**: Agents never know which LLM provider is in use. `LLMService` handles delegation.
2. **Configuration-driven**: Changing providers requires only editing `config.properties` — no code changes.
3. **Caching is provider-independent**: `LlmCache` caches at the service level, not per-provider.
4. **Incremental refactoring**: Legacy `GroqClient` is preserved for backward compatibility.
5. **Agent 4 rules-first strategy**: Deterministic fixes applied before LLM calls (80% of fixes need 0 tokens).

### Extension Points
1. **Adding a new LLM provider**: Implement `LLMProvider` interface, register in `ProviderFactory`, add config section to `config.properties`. No agent code changes needed.
2. **Adding new AI agents**: Create a new agent class in `agents/` package and orchestrate in `Main.java`
3. **Modifying prompts**: Edit prompt composer classes in `utils/prompts/composers/`
4. **Changing AI models**: Adjust values in `config.properties` or `ModelConfig.java`
5. **Adding new test types**: Extend `TestCase` model and update prompt composers
6. **API testing**: Enable `RUN_API_AGENTS` flag and configure `config.properties` with API base URL

### FCC Admin Provider

[FCC Admin](https://github.com/freecc/fcc-admin) is a local proxy server that manages 15+ AI providers through a single API endpoint. Configure it:

```properties
llm.provider=fcc
fcc.base.url=http://localhost:8082
fcc.api.key=freecc
```

**How it works:**
- FCC Admin proxies requests to any configured backend provider (NVIDIA NIM, OpenRouter, Gemini, DeepSeek, Mistral, Groq, OpenCode, Ollama, LM Studio, llama.cpp, and more)
- Uses **Anthropic Messages API format** at `{baseUrl}/v1/messages`
- Always responds via Server-Sent Events (SSE), even for non-streaming requests
- Model IDs use the format `anthropic/<provider>/<model-name>` (e.g., `anthropic/opencode/deepseek-v4-flash-free`)
- On startup, `FCCProvider` auto-discovers the first available model via `GET /v1/models`
- API keys are managed inside FCC Admin — not in `config.properties`
- Override the default model: `fcc.model=anthropic/opencode/deepseek-v4-flash-free`

### Rate Limit Handling
Rate limits vary by provider:
- **Groq**: Public API has rate limits (~30 req/min for 70B models). Framework implements exponential backoff.
- **Ollama**: Local — no rate limits. Runs at hardware speed.
- **FCC Admin**: Proxy-managed — rate limits depend on the underlying provider selected
- **Claude/OpenAI/Gemini/etc.**: Rate limits depend on API tier.
The framework persists pending components to `test-output/pending-components.txt` for automatic resumption.
