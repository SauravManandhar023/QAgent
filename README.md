# QAgent 🤖

> **Agentic AI QA Automation Framework** — Give it a URL. It thinks, writes, and tests.

QAgent is an AI-powered test automation framework that takes a web page URL as input and autonomously scrapes UI elements, generates test cases using Groq AI (Llama 3.3 70B), saves them to Excel, and writes production-ready Selenium Java test scripts — all without a human writing a single line of test code.

---

## What It Does

```
URL Input
   ↓
Agent 1 — Scrapes the page → sends to Groq AI → generates test cases → saves to Excel
   ↓
Agent 2 — Reads Excel → groups by component → generates Page Object + TestNG classes
   ↓
Ready-to-run Selenium test suite
```

---

## Agents

| Agent | Status | Responsibility |
|---|---|---|
| Agent 1 — TestCaseGeneratorAgent | ✅ Done | Scrape → AI → Excel |
| Agent 2 — ScriptGeneratorAgent | ✅ Done | Excel → AI → Java test files |
| Agent 3 — ApiTestCaseAgent | 🔜 Planned | API endpoint test case generation |
| Agent 4 — ApiScriptGeneratorAgent | 🔜 Planned | REST Assured script generation |

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Core language |
| Maven | 3.9+ | Build tool |
| Selenium | 4.44.0 | Browser automation + scraping |
| WebDriverManager | 6.3.4 | Auto driver management |
| TestNG | 7.12.0 | Test framework |
| REST Assured | 5.5.7 | API testing (planned) |
| Apache POI | 5.4.0 | Excel read/write |
| OkHttp | 4.12.0 | HTTP calls to Groq API |
| Gson | 2.11.0 | JSON parsing |
| JSoup | 1.18.3 | HTML scraping |
| Allure | 2.27.0 | Test reporting (planned) |
| Groq AI | Llama 3.3 70B | AI brain (free API) |

---

## Project Structure

```
QAgent/
│
├── pom.xml
├── config.properties              ← safe to commit
├── config.local.properties        ← secrets, gitignored
│
├── src/main/java/com/saurav/agentic/
│   ├── agents/
│   │   ├── TestCaseGeneratorAgent.java     ✅ Agent 1
│   │   └── ScriptGeneratorAgent.java       ✅ Agent 2
│   ├── config/
│   │   ├── DriverFactory.java
│   │   └── FrameworkConfig.java
│   ├── constants/
│   │   └── FrameworkConstants.java
│   ├── models/
│   │   └── TestCase.java
│   ├── pages/
│   │   └── BasePage.java
│   ├── runners/
│   │   └── Main.java
│   └── utils/
│       ├── ConfigReader.java
│       ├── ExcelUtil.java
│       ├── GroqClient.java
│       ├── PromptBuilder.java
│       └── SeleniumScraper.java
│
├── src/test/java/
│   ├── base/
│   │   └── BaseTest.java
│   ├── generated/ui/              ← AI generates these
│   └── pages/                     ← AI generates these
│
└── test-output/
    └── excel/
        └── ui-test-cases.xlsx     ← Agent 1 output
```

---

## Setup

### 1. Prerequisites

- Java 21
- Maven 3.9+
- Chrome browser installed
- Groq API key (free at [console.groq.com](https://console.groq.com))

### 2. Clone the repo

```bash
git clone https://github.com/YOUR_USERNAME/QAgent.git
cd QAgent
```

### 3. Create your secrets file

Create `config.local.properties` in the project root (this file is gitignored):

```properties
groq.api.key=your_groq_api_key_here
base.url=https://the-internet.herokuapp.com/login
```

### 4. Review public config

`config.properties` (already in repo):

```properties
browser=chrome
headless=false
groq.model=llama-3.3-70b-versatile
groq.temperature=0.3
groq.max.tokens=4096
```

### 5. Install dependencies

```bash
mvn clean install -DskipTests
```

---

## Running QAgent

```bash
mvn exec:java -Dexec.mainClass="com.saurav.agentic.runners.Main"
```

Or run `Main.java` directly from Eclipse.

---

## Sample Output

Tested on `https://the-internet.herokuapp.com/login`:

```
============================================
 Agent 1: Test Case Generator Started
============================================
[INFO]  Scraping page: https://the-internet.herokuapp.com/login
[SUCCESS] Scraping complete
[INFO]  Sending request to Groq AI...
[SUCCESS] Groq AI response received!
[SUCCESS] Excel saved: test-output/excel/ui-test-cases.xlsx

 Total Test Cases  : 15
 Positive          : 6
 Negative          : 3
 Edge              : 3
 Accessibility     : 3
 Automation Feasible: 12/15

============================================
 Agent 2: Script Generator Started
============================================
[INFO]  Processing component 1/1: Login Form (12 test cases)
[SUCCESS] Generated: LoginFormPage.java + LoginFormTest.java
============================================
 Agent 2 Complete!
 File pairs generated : 1 (POM + Test per component)
 POM classes saved to : src/test/java/pages/
 Test classes saved to: src/test/java/generated/ui/
============================================
```

---

## Roadmap

- [x] Agent 1 — UI test case generation
- [x] Agent 2 — Selenium script generation
- [ ] Agent 3 — API test case generation (REST Assured)
- [ ] Agent 4 — API script generation
- [ ] Human-in-the-loop prompt injection between Agent 1 and Agent 2
- [ ] Per-component AI calls for complex sites
- [x] Allure reporting integration
- [x] GitHub Actions CI/CD
- [ ] Discord notifications via n8n webhook

---
 
## CI/CD Pipeline
 
QAgent uses GitHub Actions for continuous integration and delivery:
 
### Fast CI (PRs and Pushes to main)
- Runs on every push and pull request to `main`
- Executes only unit tests with mocked dependencies for fast feedback
- Does not require external LLM provider (tests mock the LLM boundary)
- Timeout: 15 minutes
- Workflow: `.github/workflows/ci.yml`
 
### Full Pipeline Validation
- Runs daily at 2:00 AM UTC and can be manually triggered
- Executes the full QAgent pipeline with real browser and LLM
- Uses Groq API via GitHub Secrets (can be swapped to Ollama by changing LLM_PROVIDER)
- Generates comprehensive Allure reports
- Retains artifacts for 30 days
- Timeout: 45 minutes
- Workflow: `.github/workflows/nightly.yml`
 
### Environment Variables
Workflows support these overrides via repository secrets:
- `LLM_PROVIDER`: groq (default for nightly), ollama, claude, openai, etc.
- `GROQ_API_KEY`: Required when using Groq provider
- `BASE_URL`: Test URL (defaults to the-internet.herokuapp.com/login)
 
---
 
## Security
 
- **Never commit** `config.local.properties` — it contains your Groq API key
- The `.gitignore` already excludes it
- Alternatively set your key as an environment variable: `GROQ_API_KEY=your_key`
 
---
 
## Author
 
**Saurav Manandhar** — Junior QA Engineer  
Building QAgent as a portfolio project to demonstrate agentic AI + test automation skills.
 
---
 
## License
 
MIT License — free to use, modify, and share.