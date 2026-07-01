# QAgent Pipeline — Complete Architecture & Structure

> **QAgent** (Quality Agent) is an AI-powered Selenium test generator. It scrapes websites, generates test cases using an LLM or direct authoring, creates Page Object Model (POM) classes and TestNG test scripts, compiles them, fixes errors, and runs the tests with Allure reporting.

---

## Pipeline Overview

```
URL Input
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  METADATA EXTRACTOR (single browser launch for all agents)  │
│  PageMetadataExtractor → PageMetadata                       │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  AGENT 1: Test Case Generation                              │
│  LLM (Ollama/Groq) or Claude manual                         │
│  Output: Excel (.xlsx) with test cases                      │
└─────────────────────────────────────────────────────────────┘
    │  Optional: Human validation step
    ▼
┌─────────────────────────────────────────────────────────────┐
│  AGENT 2: Script Generation                                 │
│  LLM → POM class + TestNG test class per component          │
│  Output: src/test/java/pages/*.java + generated/ui/*.java   │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  AGENT 3: Compilation Verification                          │
│  JavaCompilerUtil compiles all generated files              │
│  Output: List<CompileResult> (pass/fail per file)           │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  AGENT 4: Smart Code Fixer                                  │
│  Rules first (0 tokens) → LLM only if needed               │
│  Re-compiles to verify fixes                                │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  AGENT 5: Test Execution + Allure Report                    │
│  Runs mvn test → parses Surefire results → Allure report    │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  AGENTS 5A/5B: API Pipeline (optional)                      │
│  API Discovery (REST Assured) → API Test Case Generation    │
│  Output: api-test-cases.xlsx                                │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  CLAUDE MODE (when local LLM unavailable/weak)              │
│  Claude writes Excel + POM + test files directly            │
│  Skips Agents 1–4, runs Agent 5 to execute tests           │
└─────────────────────────────────────────────────────────────┘
```

---

## How to Run

### Full pipeline (using LLM agents):
```bash
mvn exec:java -Dexec.mainClass="com.saurav.agentic.runners.Main"
```

### Claude-driven mode (no LLM needed):
1. Run `GenerateTestData` to populate the Excel:
   ```bash
   mvn exec:java -Dexec.mainClass="com.saurav.agentic.utils.GenerateTestData"
   ```
2. Set `CLAUDE_MODE = true` and `SKIP_AGENT1 = true` in `Main.java`
3. Run:
   ```bash
   mvn exec:java -Dexec.mainClass="com.saurav.agentic.runners.Main"
   ```
   Or step-by-step:
   ```bash
   mvn test-compile   # compile everything
   mvn test           # run all tests
   mvn allure:report  # generate Allure report
   ```

### Run only generated UI tests (no unit tests):
```bash
mvn test -Dtest="HomePagePart1Test,HomePagePart2Test,HomePagePart3Test"
```

### Mutation testing (validates test quality):
```bash
mvn pitest:mutationCoverage -P mutation-testing
```

---

## Project Structure

```
ai-selenium-test-generator/
│
├── STRUCTURE.md                           ← This file
├── pom.xml                                ← Maven build with dependencies
├── config.properties                      ← Framework configuration (safe to commit)
├── config.local.properties                ← Local overrides / secrets (DO NOT COMMIT)
├── testng.xml                             ← TestNG suite configuration
├── CLAUDE.md                              ← Instructions for Claude Code
│
├── run_compile.bat                        ← Helper: compile only
├── run_pipeline.bat                       ← Helper: run full pipeline
│
├── src/
│   └── main/java/com/saurav/agentic/
│       │
│       ├── runners/
│       │   └── Main.java                  ← Pipeline orchestrator (entry point)
│       │
│       ├── agents/
│       │   ├── TestCaseGeneratorAgent.java    ← Agent 1: UI test case generation
│       │   ├── ScriptGeneratorAgent.java      ← Agent 2: POM + TestNG generation
│       │   ├── Agent3_Compiler.java           ← Agent 3: compilation verification
│       │   ├── Agent4_Reviewer.java           ← Agent 4: code review + fixes
│       │   ├── TestExecutionAgent.java        ← Agent 5: test execution + Allure
│       │   ├── ApiDiscoveryAgent.java         ← Agent 5A: REST API discovery
│       │   └── ApiTestCaseGeneratorAgent.java ← Agent 5B: API test case generation
│       │
│       ├── models/
│       │   ├── TestCase.java              ← Data model for UI test cases
│       │   ├── ApiEndpoint.java           ← Data model for discovered API endpoints
│       │   ├── ApiTestCase.java           ← Data model for API test cases
│       │   ├── PageMetadata.java          ← Scraped page structure & elements
│       │   ├── PageElement.java           ← Single page element data
│       │   └── CompileResult.java         ← Compilation result (pass/fail/errors)
│       │
│       ├── config/
│       │   ├── FrameworkConfig.java       ← Singleton: reads config.properties
│       │   ├── ModelConfig.java           ← Singleton: per-agent model settings
│       │   └── DriverFactory.java         ← WebDriver lifecycle management
│       │
│       ├── constants/
│       │   └── FrameworkConstants.java    ← Log prefixes, paths, default values
│       │
│       ├── scraper/
│       │   ├── SeleniumScraper.java       ← Selenium-based page scraping
│       │   └── PageMetadataExtractor.java ← Builds PageMetadata from scraped data
│       │
│       ├── compiler/
│       │   └── JavaCompilerUtil.java      ← Programmatic Java compilation
│       │
│       ├── utils/
│       │   ├── GroqClient.java            ← LLM API client (Ollama + Groq)
│       │   ├── LlmCache.java              ← Persistent LLM response cache
│       │   ├── PromptBuilder.java         ← Prompt templates for all agents
│       │   ├── ConfigReader.java          ← Properties file reader
│       │   ├── ExcelUtil.java             ← Apache POI Excel read/write
│       │   ├── ApiExcelUtil.java          ← API test case Excel read/write
│       │   ├── GenerateTestData.java      ← Manually writes 20 test cases to Excel
│       │   └── SeleniumScraper.java       ← (used by scraper package)
│       │
│       ├── prompts/composers/
│       │   ├── ScriptPromptComposer.java  ← TestNG script generation prompts
│       │   └── PomPromptComposer.java     ← POM class generation prompts
│       │
│       ├── pages/
│       │   └── BasePage.java              ← Base POM with common utilities
│       │
│       └── tools/                         ← (extensible tool definitions)
│
├── src/test/java/
│   │
│   ├── base/
│   │   └── BaseTest.java                 ← TestNG base class (setUp/tearDown)
│   │
│   ├── pages/                            ← GENERATED: POM classes
│   │   └── HomePagePage.java             ← POM for automationexercise.com
│   │
│   ├── generated/ui/                     ← GENERATED: UI test classes
│   │   ├── HomePagePart1Test.java        ← TC_001–TC_006 (navigation links)
│   │   ├── HomePagePart2Test.java        ← TC_007–TC_012 (subscription, categories)
│   │   └── HomePagePart3Test.java        ← TC_013–TC_020 (brands, products, scroll)
│   │
│   ├── com/saurav/agentic/               ← UNIT TESTS for the framework
│   │   ├── models/
│   │   │   ├── TestCaseTest.java
│   │   │   ├── CompileResultTest.java
│   │   │   ├── PageElementTest.java
│   │   │   └── PageMetadataTest.java
│   │   ├── utils/
│   │   │   ├── ExcelUtilTest.java
│   │   │   ├── PromptBuilderTest.java
│   │   │   └── LlmCacheTest.java
│   │   └── constants/
│   │       └── FrameworkConstantsTest.java
│   │
│   └── com/saurav/testapp/               ← Demo app (pre-existing, unrelated)
│       └── LoginAppTest.java
│
├── test-output/
│   ├── excel/
│   │   ├── ui-test-cases.xlsx            ← Generated UI test cases (Agent 1 output)
│   │   └── api-test-cases.xlsx           ← Generated API test cases (Agent 5B output)
│   ├── logs/
│   │   └── agent.log                     ← Pipeline execution log
│   ├── cache/
│   │   └── llm-cache.json               ← Cached LLM responses
│   └── pending-components.txt            ← Components queued due to rate limits
│
├── target/
│   ├── classpath.txt                     ← Built by maven-dependency-plugin
│   ├── surefire-reports/                 ← TestNG XML/HTML test reports
│   └── site/allure-maven-plugin/         ← Allure HTML report
│
└── allure-results/                       ← Allure raw test data
```

---

## Detailed File Descriptions

### 1. Pipeline Entry Point

#### `Main.java`
- Orchestrates all agents in sequence
- Configuration flags:
  - `SKIP_AGENT1` (boolean) — skip scraping + test case generation, reuse existing Excel
  - `CLAUDE_MODE` (boolean) — skip all LLM-dependent agents (1–4), Claude writes files directly
  - `RUN_API_AGENTS` (boolean) — enable API pipeline (Agents 5A/5B)
  - `INTERACTIVE` (boolean) — pause between Agent 1 and 2 for human Excel review
- Flow: Metadata Extract → (Agent 1 → Human Validation) → (Agent 2 → 3 → 4 in CLAUDE_MODE) → Agent 5 → API agents

---

### 2. Agents (Pipeline Stages)

#### `TestCaseGeneratorAgent.java` — Agent 1
- **Purpose**: Generate UI test cases from a scraped webpage
- **Input**: URL → SeleniumScraper scrapes the page → analysis text
- **Process**:
  1. Scrape page elements via SeleniumScraper (or reuse pre-scraped `PageMetadata`)
  2. Load existing test cases from Excel for deduplication & incremental ID assignment
  3. Call GroqClient with tool-calling API (`chatWithTools`) — sends page analysis + system prompt
  4. Parse the structured JSON response into `TestCase` objects
  5. Renumber new test cases to ensure globally unique IDs
  6. Save to Excel via `ExcelUtil.appendTestCases()`
- **Fallback parsers**: `parseTestCases()` (JSON in markdown) and `parseMarkdownTestCases()` (for models like qwen2.5-coder that don't support structured JSON)

#### `ScriptGeneratorAgent.java` — Agent 2
- **Purpose**: Generate POM + TestNG test classes from Excel test cases
- **Input**: Excel file path, URL, page analysis
- **Process**:
  1. Read Excel → filter automation-feasible cases → group by component (fuzzy matching)
  2. For each component:
     a. Generate POM class via LLM (`generatePomClass`)
     b. Extract public methods from POM
     c. Split test cases into batches of ≤5
     d. For each batch: generate TestNG test class via LLM (`generateTestClass`)
  3. Write to temp directory first, then swap to production on success
  4. Track pending components on rate-limit exhaustion (persisted to `test-output/pending-components.txt`)
- **Rate limiting**: Exponential backoff with max 3 consecutive failures before pausing
- **Key method**: `generateCode()` uses tool-calling API with `{"code": "string"}` schema

#### `Agent3_Compiler.java` — Agent 3
- **Purpose**: Compile all generated POM and test files
- **Process**:
  1. Compile all files in `pages/` directory
  2. Compile all files in `generated/ui/` directory
  3. Return `List<CompileResult>` — one per file, with error details
- **Uses**: `JavaCompilerUtil.compileDirectory()` — programmatic Java compilation via `javax.tools.JavaCompiler`

#### `Agent4_Reviewer.java` — Agent 4
- **Purpose**: Fix compilation errors without wasting LLM tokens
- **Strategy: Rules First → LLM Only If Needed**:
  - **Pass 1**: Apply deterministic regex fixes (0 LLM tokens used):
    - Wrong `SeverityLevel` mappings (MEDIUM→NORMAL, HIGH→CRITICAL, LOW→MINOR)
    - `WebDriverWait(driver, 10)` → `WebDriverWait(driver, Duration.ofSeconds(10))`
    - Deprecated Selenium 3 `findElementBy*` → Selenium 4 `findElement(By.*)`
    - Wrong imports (`org.openqa.selenium.Duration` → `java.time.Duration`)
    - JUnit to TestNG import migration
    - Duplicate import removal
    - Missing import auto-insertion (checks symbol usage, adds import)
    - Syntax fixes (double semicolons, empty catch blocks, wrong package separators)
  - **Pass 2**: Re-compile after rule fixes
  - **Pass 3**: For remaining failures, send ONLY error context (not full file) to LLM
  - **Pass 4**: Re-compile after LLM fix
- **Token optimization**: `extractErrorContext()` only sends lines near errors (±8 lines)

#### `TestExecutionAgent.java` — Agent 5
- **Purpose**: Run generated Selenium tests via Maven and produce Allure reports
- **Process**:
  1. Run `mvn test` via `ProcessBuilder` with real-time output streaming
  2. Parse Surefire XML reports for structured results (`TestResult` objects)
  3. Fallback: regex parsing of summary line from raw output
  4. Generate Allure HTML report via `mvn allure:report`
- **Edge cases**: Handles missing surefire-reports dir, non-zero exit codes, NoSuchElementException graceful handling

#### `ApiDiscoveryAgent.java` — Agent 5A
- **Purpose**: Probe API endpoints without any AI calls (0 tokens)
- **Process**:
  1. Hit each known endpoint (GET/POST/PUT/DELETE) via REST Assured
  2. Analyze response: status code, content type, JSON structure (keys, nesting)
  3. Suggest test types: positive, negative, schema validation, response time, content type
- **Zero AI tokens**: Pure HTTP probing with hardcoded endpoint definitions

#### `ApiTestCaseGeneratorAgent.java` — Agent 5B
- **Purpose**: Generate structured API test cases from discovered endpoints
- **Process**: Template-based generation (0 LLM tokens):
  - Positive: valid request → expected 200/201
  - Negative: missing/invalid params → error response
  - Edge: empty params, boundary values
  - Schema: response structure validation
  - Performance: response time check

---

### 3. Models (Data Classes)

#### `TestCase.java`
- 11 fields: `testCaseId`, `testCaseName`, `description`, `preconditions`, `testSteps`, `testData`, `expectedResult`, `testType`, `priority`, `component`, `automationFeasible`
- Used by: Agent 1 (generation), Agent 2 (script generation), ExcelUtil (persistence), GenerateTestData (manual authoring)

#### `ApiEndpoint.java`
- Represents a discovered REST API endpoint
- Fields: URL, HTTP method, status code, content type, response keys, response type (OBJECT/ARRAY/PRIMITIVE), auth type, working status
- Holds suggested test types list

#### `ApiTestCase.java`
- Represents a single API test case
- Fields: testCaseId, testCaseName, endpoint, httpMethod, requestHeaders, requestBody, expectedStatusCode, testType, priority

#### `PageMetadata.java`
- Container for scraped page structure
- Holds: page title, URL, page type, lists of elements (inputs, buttons, links, forms, dropdowns, checkboxes, images)
- `toPromptString()` converts the structured data into a text prompt for LLM consumption
- `getAllElements()` returns union of all element types

#### `PageElement.java`
- Single page element: tag, text, attributes (id, name, class, type, href, etc.), locator recommendations
- Boolean states: isClickable, isRequired, isDisabled
- Form context (which form an input belongs to)
- Core identity for deduplication

#### `CompileResult.java`
- Compilation result: file path, class name, source code, success status, list of error messages
- `getErrorSummary()` concatenates all errors
- `toString()` for human-readable pass/fail display

---

### 4. Configuration

#### `FrameworkConfig.java`
- Singleton pattern: reads `config.properties` + `config.local.properties` (local overrides)
- Provides: browser type, headless mode, LLM provider, Groq API key, output paths, wait settings, cache settings
- Key methods: `getBaseUrl()`, `getBrowser()`, `isHeadless()`, `getUiExcelOutputPath()`, `getApiExcelOutputPath()`

#### `ModelConfig.java`
- Singleton: per-agent model overrides
- Provides per-agent: model name, temperature, max tokens
- Keys from config: `agent1.model`, `agent1.temperature`, `agent1.max.tokens`, etc.

#### `DriverFactory.java`
- ThreadLocal WebDriver management (thread-safe for parallel test execution)
- `initDriver()`: creates Chrome/Firefox/Edge driver based on config
- `getDriver()`: returns current thread's driver
- `quitDriver()`: closes browser and cleans up ThreadLocal
- Chrome options: headless=new, no-sandbox, disable-dev-shm-usage, 1920x1080 window

#### `FrameworkConstants.java`
- All constants in one place: log prefixes, browser names, test types, priority levels, output paths, wait times, sheet names
- Key constants: `LOG_INFO/SUCCESS/WARNING/ERROR`, `BROWSER_CHROME/FIREFOX/EDGE`, `SHEET_SUMMARY`, `GROQ_API_URL`

---

### 5. Utilities

#### `GroqClient.java`
- HTTP client for LLM APIs (supports both Groq Cloud and Ollama local)
- **Two API modes**:
  1. `chat()` — sends system + user prompt, returns free-form text
  2. `chatWithTools()` — sends system + user prompt + tool definition (JSON Schema), returns structured `JsonObject` from `tool_calls[0].function.arguments`
- **Provider detection**: `isOllamaModel()` checks config and model name patterns
- **Retry**: OkHttp with 300s read timeout, exponential backoff in callers
- **Caching**: Two-tier (in-memory `HashMap` + disk-backed `LlmCache`)
- **Tool schema helpers**: `jsonSchema()`, `stringProperty()`, `integerProperty()`, `booleanProperty()`, `arrayProperty()` — build JSON Schema definitions programmatically
- **Response fallback**: If tool_calls is null (model doesn't support structured output), falls back to content parsing with 5 strategies:
  1. Content starts with `[` → wrap as `{"testCases": [...]}`
  2. Content starts with `{` → use as-is
  3. Search for `[...]` anywhere in text → validate + extract
  4. Search for `{...}` anywhere → validate + extract
  5. No JSON found → wrap as `{"code": "..."}` for caller fallback

#### `LlmCache.java`
- Persistent disk-backed cache for LLM responses
- Uses SHA-256 hash of (systemPrompt + userPrompt + model + temperature + maxTokens) as key
- LRU eviction when cache exceeds 500 entries (default)
- Thread-safe with `ReadWriteLock`
- Offline mode: when `cache.offline.mode=true`, throws if no cache hit — never calls LLM
- File format: JSON array of `{key, response, timestamp}` objects

#### `PromptBuilder.java`
- Static methods returning prompt strings for each agent
- **Agent 1**: `uiTestCaseSystemPrompt()`, `uiTestCaseUserPrompt()` — detailed rules for test case generation
- **Agent 2**: `seleniumScriptSystemPrompt()`, `seleniumScriptUserPrompt()` — Selenium coding rules
- **Agent 2 POM**: `pomSystemPrompt()`, `pomUserPrompt()` — POM generation rules
- **API Agents**: `apiTestCaseSystemPrompt()`, `apiTestCaseUserPrompt()` — API test rules
- **Helper**: `toPascalCase()` — converts component names to Java class names
- Prompts encode 20+ rules covering locator strategy, assertion patterns, error handling, Selenium 4 conventions

#### `ExcelUtil.java`
- Apache POI wrapper for test case Excel read/write
- `writeTestCases()`: creates a Summary sheet + per-component sheets with headers (Test Case ID, Name, Description, Preconditions, Test Steps, Test Data, Expected Result, Test Type, Priority, Automation Feasible)
- `readTestCases()`: reads all component sheets back into `TestCase` objects
- `appendTestCases()`: incremental mode — reads existing, deduplicates by name, appends new ones
- Sheet name sanitization: removes invalid Excel characters, truncates to 31 chars

#### `ApiExcelUtil.java`
- Same pattern as ExcelUtil but for `ApiTestCase` objects
- Writes/reads API test cases: endpoint, HTTP method, request details, expected status codes

#### `GenerateTestData.java`
- **Purpose**: Manual test data generation for CLAUDE_MODE
- Writes 20 hand-authored test cases (TC_001–TC_020) to Excel via `ExcelUtil.writeTestCases()`
- Test coverage: navigation (6), subscription (3), categories (3), brands (1), products (1), scroll (1), visibility (3), accessibility (1), featured products (1)
- Statically defines all fields: 17 Positive, 1 Negative, 1 Edge, 1 Accessibility
- All marked automationFeasible = true

#### `ConfigReader.java`
- Reads properties files from classpath and filesystem
- Supports default values: `get(key, defaultValue)`

#### `SeleniumScraper.java`
- Opens a browser via `DriverFactory`, navigates to URL, scrapes all interactive elements
- Detects: inputs, buttons, links, dropdowns, checkboxes, forms, images
- Captures attributes: id, name, class, type, href, placeholder, aria-label, etc.
- Returns structured text analysis for LLM consumption

#### `PageMetadataExtractor.java`
- Higher-level scraper: builds `PageMetadata` object from SeleniumScraper output
- Extracts page type (login form, product listing, etc.), overall structure
- `extract(url)`: single browser launch, returns `PageMetadata`
- `toPromptString()`: formats for LLM consumption

---

### 6. Prompts (Script + POM Generation)

#### `ScriptPromptComposer.java`
- Builds Agent 2 prompts for test script generation
- `systemPrompt()`: Coding rules (Selenium 4 API, TestNG conventions, Allure annotations, locator priorities, assertion patterns)
- `userPrompt()`: Component name, target URL, POM methods, test cases to implement

#### `PomPromptComposer.java`
- Builds Agent 2 prompts for POM class generation
- `systemPrompt()`: POM conventions (@FindBy, PageFactory, locator priorities)
- `userPrompt()`: Component name, class name, URL, scraped page elements

---

### 7. Compilation

#### `JavaCompilerUtil.java`
- Programmatic Java compilation using `javax.tools.JavaCompiler`
- `compileDirectory(path)`: compiles all `.java` files in a directory, returns `List<CompileResult>`
- `compile(filePath)`: compiles a single file
- Builds classpath from `target/classpath.txt` (generated by maven-dependency-plugin)
- Custom `DiagnosticCollector` captures line numbers and error messages

---

### 8. Test Infrastructure

#### `BaseTest.java`
- All generated tests extend this class
- `setUp()`: calls `DriverFactory.initDriver()` → creates WebDriver
- `tearDown()`: calls `DriverFactory.quitDriver()` → closes browser + cleans ThreadLocal
- `driver` field is `protected` so test classes can access it

#### `BasePage.java`
- Optional base class for POMs with shared utilities
- Currently minimal — provides common helper methods

#### `HomePagePage.java` (in `pages/`)
- Generated POM for automationexercise.com homepage
- Elements mapped by `@FindBy`: navigation links (Home, Products, Cart, Signup/Login, Test Cases, Contact us), subscription form (email input + subscribe button), category sidebar (WOMEN, MEN, KIDS), brands (POLO), featured products (View Product links), scroll-up button
- Methods: click* (with WebDriverWait for clickable), is*Visible (visibility checks), urlContains (waits for URL), scrollToBottom (JS), getSubscriptionPlaceholder, getFeaturedProductCount, areCategoryLinksPresent

#### `HomePagePart1Test.java`
- Tests TC_001–TC_006: All 6 navigation links
- Each test clicks the nav link and asserts the URL contains the expected path

#### `HomePagePart2Test.java`
- Tests TC_007–TC_012: Subscription (valid, empty, invalid email) + category links (WOMEN, MEN, KIDS)
- Subscription tests scroll to footer, enter email, click subscribe

#### `HomePagePart3Test.java`
- Tests TC_013–TC_020: Brand navigation, product details, scroll-up button, nav visibility, featured products, subscription visibility, placeholder accessibility, category visibility

---

### 9. Configuration Files

#### `config.properties`
- Checked into git — safe defaults
- Browser: chrome, headless: true
- LLM provider: ollama (default) or groq
- All model overrides per agent
- Cache: enabled, offline mode: false
- Pipeline: interactive: false, run tests: true

#### `testng.xml`
- Suite definition for running generated UI tests
- Configured to run only: `HomePagePart1Test`, `HomePagePart2Test`, `HomePagePart3Test`
- Run via `mvn test` (Surefire reads this file)

---

### 10. How Agents Communicate

| Step | Who Writes | Who Reads | Medium |
|------|-----------|-----------|--------|
| Page analysis | PageMetadataExtractor | Agent 1 | `PageMetadata.toPromptString()` (in-memory) |
| Test cases | Agent 1 (or GenerateTestData) | Agent 2 | Excel file (`ui-test-cases.xlsx`) |
| POM classes | Agent 2 | Agent 3 | Java files (`pages/*.java`) |
| Test classes | Agent 2 | Agent 3 | Java files (`generated/ui/*.java`) |
| Compile results | Agent 3 | Agent 4 | `List<CompileResult>` (in-memory) |
| Fixed source | Agent 4 | Agent 3 (re-compile) | Java files (overwritten on disk) |
| Test results | Agent 5 | human | Surefire XML + Allure HTML |

---

### 11. CLAUDE_MODE vs Normal Mode

| Aspect | Normal Mode | CLAUDE_MODE |
|--------|-----------|-------------|
| Who writes Excel | Agent 1 → LLM (Groq/Ollama) | `GenerateTestData.java` manually |
| Who writes Java files | Agent 2 → LLM (Groq/Ollama) | Claude (this AI) writes directly |
| Compilation | Agent 3 compiles | Run `mvn test-compile` manually |
| Error fixes | Agent 4 → rules + LLM | Manually fix in-editor |
| Test execution | Agent 5 runs `mvn test` | Agent 5 runs `mvn test` |
| API pipeline | Agents 5A/5B (0 tokens) | Agents 5A/5B (0 tokens) |
| LLM calls | Heavy — one per agent step | Zero — Claude writes everything |

---

### 12. Key Dependencies

| Library | Version | Used For |
|---------|---------|----------|
| Selenium | 4.44.0 | Browser automation, page scraping |
| WebDriverManager | 6.3.4 | Automatic driver binary management |
| TestNG | 7.12.0 | Test framework (parallel, annotations, reporting) |
| REST Assured | 5.5.7 | API endpoint probing (Agents 5A/5B) |
| Apache POI | 5.4.0 | Excel read/write for test cases |
| OkHttp | 4.12.0 | HTTP client for LLM API calls |
| Gson | 2.11.0 | JSON parsing for LLM responses |
| JSoup | 1.18.3 | HTML parsing during scraping |
| Allure | 2.27.0 | Test reporting (annotations + HTML report) |
| SLF4J + Logback | 2.0.17 / 1.5.18 | Logging framework |

---

### 13. File Types Overview

| Extension | Purpose | Git Status |
|-----------|---------|------------|
| `.java` (src/main) | Framework code | ✅ Tracked + committed |
| `.java` (src/test/pages) | Generated POM classes | ❌ Gitignored |
| `.java` (src/test/generated) | Generated test classes | ❌ Gitignored |
| `.java` (src/test/com/...) | Unit tests + LoginAppTest | ✅ Tracked + committed |
| `.xlsx` (test-output/excel) | Test case data | ❌ Gitignored |
| `.json` (test-output/cache) | LLM response cache | ❌ Gitignored |
| `.properties` | Configuration | ✅ Tracked (base) / ❌ ignored (local) |
| `.html` (target/site) | Allure reports | ❌ Gitignored |
| `.xml` (testng.xml) | TestNG suite | ✅ Tracked + committed |
| `.log` | Execution logs | ❌ Gitignored |

---

### 14. Error Handling Patterns

| Issue | How It's Handled |
|-------|-----------------|
| LLM API 429 (rate limited) | Exponential backoff, max 3 retries, then pause + persist pending components to disk |
| LLM API timeout (slow model) | OkHttp 300s read timeout, caller retries once |
| Scraping failure | 3 retry attempts with 2s delay between |
| Compilation error | Agent 4: deterministic regex fixes first (0 tokens), then LLM if needed |
| Truncated LLM response | String boundary recovery in `cleanJsonResponse()`, content-length validation |
| No tool_calls in response | Fallback to 5-tier JSON extraction strategies |
| Model returns markdown/backticks | Post-processing strips fences, extracts JSON/Java code |
| Null or empty test cases | Pass through with defaults + skip to next |
| Test execution failure | Non-zero exit code reported but pipeline continues |
| Missing Allure results | Graceful skip with warning message |
