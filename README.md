# QAgent 🤖

> **Agentic AI QA Automation Framework** — From a web application to risk-aware, executable test automation.

QAgent is an AI-powered QA automation framework that analyzes a web application, builds an understanding of its functionality, identifies testing risks, generates traceable test cases, produces Selenium automation code, and validates the generated code before execution.

The goal is not simply to generate test scripts with an LLM, but to build a **reasoning-driven QA pipeline** where each stage contributes a specific testing decision.

---

## 🧠 What is QAgent?

Traditional AI test generators often follow a simple pattern:

```text
URL
 ↓
LLM
 ↓
Test Cases
 ↓
Test Scripts

QAgent takes a more structured approach:

┌──────────────────────────┐
│      Web Application     │
│           URL            │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ Phase 1                  │
│ Page Metadata &          │
│ Application Understanding│
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ Phase 2                  │
│ Functional Model         │
│ Construction             │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ Phase 3                  │
│ Risk-Based Test Strategy │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ Phase 4                  │
│ Test Case Generation     │
│ + Traceability           │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ Script Generation        │
│ Page Objects + TestNG    │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ Phase 5                  │
│ Compilation Validation   │
│ + Static Code Review     │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ Generated Test Suite     │
│ Ready for Execution      │
└──────────────────────────┘

The important distinction is that risk and testing decisions are represented explicitly instead of being hidden inside one large LLM prompt.

🎯 Project Goals

QAgent is being developed around several core objectives:

Understand a web application's structure and functionality
Identify important application components and workflows
Generate tests based on testing risk rather than only UI elements
Maintain traceability from risk → scenario → test case → automation
Generate maintainable Selenium Page Objects and TestNG tests
Validate generated Java code before execution
Prefer deterministic code fixes before using an LLM
Minimize unnecessary LLM usage
Provide CI validation for both the framework and the complete pipeline
Gradually evolve toward a production-oriented QA automation system
🏗️ Architecture

QAgent is organized as a multi-stage QA pipeline.

                         WEB APPLICATION
                               │
                               ▼
                    ┌────────────────────┐
                    │ Page Metadata      │
                    │ & Scraping         │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │ Application        │
                    │ Understanding      │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │ Functional Model   │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │ Risk-Based         │
                    │ Strategy           │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │ Scenario           │
                    │ Generation         │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │ Test Case          │
                    │ Generation         │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │ Script Generation  │
                    │ POM + TestNG       │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │ Compilation        │
                    │ Validation         │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │ Static Code Review │
                    │ & Deterministic    │
                    │ Fixes              │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │ Runtime Evaluation │
                    │ & Test Execution   │
                    └────────────────────┘
🔬 Core Pipeline
Phase 1 — Page Metadata & Application Understanding

The pipeline begins by inspecting the target web application.

The initial analysis collects information such as:

Page structure
UI elements
Forms
Buttons
Inputs
Links
Labels
Attributes
Locators
Navigation information
Relevant page content

This information becomes the foundation for later reasoning.

Phase 2 — Functional Model

Raw page information is transformed into a more meaningful representation of application functionality.

Instead of treating every UI element independently, QAgent attempts to understand:

Components
User interactions
Functional relationships
Workflows
Preconditions
Actions
Expected outcomes

The objective is to move from:

"I found an input field."

toward:

"This input participates in a login workflow."
⚠️ Phase 3 — Risk-Based Test Strategy

One of the main differences between QAgent and a basic AI test generator is the introduction of risk-based testing decisions.

The system evaluates identified functionality and produces scenarios based on factors such as:

Functional importance
Failure impact
User interaction
Negative paths
Boundary conditions
Business risk
Automation feasibility
Confidence

Each scenario can contain information such as:

Scenario ID
Scenario Description
Priority
Risk Score
Confidence
Risk Reasoning
Given State
When Action
Then Expected Result

Example:

SCN-001

Description:
Verify that a user can log in with valid credentials.

Risk:
High

Risk Score:
9

Reasoning:
Authentication is a critical application workflow and failure
directly prevents users from accessing the system.

Given:
The login page is displayed.

When:
The user enters valid credentials and submits the form.

Then:
The user should be successfully authenticated.

This makes the testing strategy explainable instead of treating generated tests as unexplained LLM output.

🧪 Phase 4 — Test Case Generation

The risk-based scenarios are transformed into executable test cases.

A major focus of this phase is traceability.

Each generated test case can retain information from the scenario that produced it.

Risk-Based Scenario
        │
        ▼
┌──────────────────────┐
│ Test Case             │
├──────────────────────┤
│ scenarioId            │
│ scenarioDescription   │
│ riskScore             │
│ confidence            │
│ riskReasoning         │
│ givenState             │
│ whenAction             │
│ thenExpect             │
│ testType               │
│ priority               │
└──────────────────────┘

This allows questions such as:

Which tests were generated from P0/P1 scenarios?
Which high-risk scenarios are covered?
Which high-risk scenarios are missing?
Why was a particular test case generated?
What risk reasoning led to the test?
What scenario does an automated test represent?
Test Type

The framework supports test classifications such as:

Positive
Negative
Edge
Accessibility

The exact classification depends on the generated testing strategy.

🧩 Script Generation

After test cases are generated, QAgent converts them into Selenium automation code.

The generated automation follows the Page Object Model.

Test Cases
    │
    ├──────────────┐
    │              │
    ▼              ▼
Page Object      Test Class
    │              │
    │              │
    └──────┬───────┘
           ▼
     Selenium Tests

Generated files are organized into:

src/test/java/pages/

and:

src/test/java/generated/ui/

The goal is to generate automation that is:

Maintainable
Structured
Reusable
Compileable
Traceable back to its test case
🔧 Phase 5 — Compilation Validation & Static Code Review

Generated code cannot simply be assumed to be correct.

QAgent therefore introduces a validation layer between generation and execution.

Agent 3 — Compiler

The compiler stage validates generated Java source code.

It checks:

Page Object compilation
Generated test compilation
Compilation failures
Newly generated files
Existing generated files

The result is represented through structured compilation results.

Generated Java
      │
      ▼
Java Compiler
      │
      ├──────────────► SUCCESS
      │
      └──────────────► FAILURE
                         │
                         ▼
                    Agent 4
🛠️ Agent 4 — Smart Reviewer

Agent 4 reviews generated code using a rules-first strategy.

Compilation Failure
        │
        ▼
Deterministic Rules
        │
        ├──── Fixed ────► Recompile
        │
        └──── Not Fixed
                │
                ▼
          LLM Assistance
                │
                ▼
             Recompile

The philosophy is:

Use deterministic fixes whenever the problem is known. Use an LLM only when deterministic rules cannot solve it.

This reduces unnecessary LLM calls and makes common fixes predictable.

Deterministic Fix Examples

The reviewer can handle known patterns such as:

Missing imports
Selenium Duration migration
WebDriverWait conversion
JUnit → TestNG migration
Invalid Selenium patterns
Missing Selenium support imports
PageFactory
FindBy
ExpectedConditions
Java utility imports
Other known compilation-related patterns

For example:

new WebDriverWait(driver, 10);

can be transformed into:

new WebDriverWait(driver, Duration.ofSeconds(10));

with the required import.

The reviewer then recompiles the modified source.

🧠 LLM Usage Philosophy

QAgent does not treat the LLM as the solution to every problem.

The architecture intentionally separates:

Reasoning
    +
Deterministic Validation
    +
Deterministic Fixes
    +
LLM Assistance

This provides several advantages:

Lower token usage
More predictable behavior
Easier debugging
Better reproducibility
Reduced dependence on model availability
Better CI reliability
Easier testing of individual components

The LLM is used where reasoning is valuable rather than for problems that can be solved reliably with conventional code.

🔗 Traceability

Traceability is a central part of the current architecture.

The intended relationship is:

Application
     │
     ▼
Functional Model
     │
     ▼
Risk
     │
     ▼
Scenario
     │
     ▼
Test Case
     │
     ▼
Generated Script
     │
     ▼
Compilation Result
     │
     ▼
Execution Result

This allows QAgent to move toward answering:

Why does this test exist?
        ↓
Which scenario generated it?
        ↓
What risk justified the scenario?
        ↓
What automation represents the test?
        ↓
Did the generated code compile?
        ↓
Did the test execute successfully?
🧪 Testing Strategy

QAgent itself is tested as a software system.

The project includes unit tests for important components, including:

Test case generation
Traceability field population
Compilation validation
Deterministic code fixing
Missing import detection
Selenium code transformations
Edge cases
Empty input handling
Compilation failure handling

Example:

Developer Change
       │
       ▼
GitHub Actions
       │
       ▼
Maven Build
       │
       ▼
Unit Tests
       │
       ▼
PASS / FAIL

The generated automation pipeline is validated separately because it requires real browser and LLM interaction.

🚀 CI/CD

QAgent uses GitHub Actions to continuously validate the project.

The CI architecture intentionally separates fast deterministic validation from full pipeline validation.

Fast CI

Workflow:

.github/workflows/ci.yml

Runs on:

Pushes to main
Pull requests targeting main

The fast CI pipeline focuses on deterministic project validation.

Checkout
   ↓
Setup Java
   ↓
Maven Build
   ↓
Unit Tests
   ↓
Test Results
Characteristics
No external LLM dependency
Unit tests use mocked boundaries where appropriate
Faster feedback
Suitable for every code change
Helps prevent regressions

The goal is to answer:

"Did my code change break the framework?"

🌙 Full Pipeline Validation

Workflow:

.github/workflows/nightly.yml

The full pipeline validates the complete QAgent workflow using real components.

It can be:

Scheduled
Manually triggered

The pipeline includes:

Repository
    ↓
Build
    ↓
Real LLM Provider
    ↓
Web Application
    ↓
Browser
    ↓
QAgent Pipeline
    ↓
Test Generation
    ↓
Script Generation
    ↓
Compilation
    ↓
Review / Fixing
    ↓
Reports & Artifacts

This workflow is intentionally separated from normal PR CI because it is:

Slower
More resource intensive
Dependent on external services
Potentially affected by LLM availability and rate limits
Dependent on a real browser environment

The goal is to answer:

"Does the complete QAgent pipeline still work end-to-end?"

🔐 CI Secrets

Secrets must never be committed to the repository.

For example:

GROQ_API_KEY

can be provided through GitHub Actions repository secrets when the Groq provider is used.

Provider configuration is intentionally separated from the core pipeline so that different LLM backends can be used without redesigning the QA architecture.

🤖 LLM Provider Architecture

QAgent is designed around an LLM provider abstraction rather than tightly coupling the framework to one model.

Conceptually:

                 ┌───────────────┐
                 │   LLMService  │
                 └───────┬───────┘
                         │
              ┌──────────┼──────────┐
              │          │          │
              ▼          ▼          ▼
            Groq        FCC       Ollama

This allows the underlying provider to change without rewriting the agents that consume the LLM service.

The exact model can therefore evolve independently from the QA pipeline.

📁 Project Structure

The project structure is organized around the agentic pipeline and supporting infrastructure.

QAgent/
│
├── .github/
│   └── workflows/
│       ├── ci.yml
│       ├── nightly.yml
│       └── mutation-test.yml
│
├── memory/
│   ├── MEMORY.md
│   └── phase-* documentation
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── saurav/
│   │               └── agentic/
│   │                   ├── agents/
│   │                   ├── config/
│   │                   ├── constants/
│   │                   ├── models/
│   │                   ├── runners/
│   │                   └── utils/
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── saurav/
│                   └── agentic/
│                       └── agents/
│
├── src/test/java/pages/
│   └── Generated Page Objects
│
├── src/test/java/generated/ui/
│   └── Generated UI Tests
│
├── test-output/
│   └── Generated artifacts
│
├── pom.xml
├── config.properties
├── config.local.properties
├── .gitignore
└── README.md
🧱 Technology Stack
Technology	Purpose
Java 21	Core framework
Maven	Build and dependency management
Selenium WebDriver	Browser automation and application inspection
TestNG	Automated test execution
Apache POI	Excel processing
REST Assured	API testing infrastructure
JSoup	HTML processing
OkHttp	HTTP communication
Gson	JSON processing
Allure	Test reporting
Git	Version control
GitHub Actions	CI/CD
LLM Providers	Application understanding, reasoning and generation
📊 Quality Engineering Approach

QAgent is being developed with a QA engineering mindset rather than treating AI generation as the final output.

The framework emphasizes:

Risk-Based Testing

Tests should be prioritized according to risk and importance rather than simply generating one test per UI element.

Traceability

Every important testing decision should be traceable back to its source.

Deterministic Validation

Known problems should be detected and fixed through deterministic rules whenever possible.

Compilation as a Quality Gate

Generated source code must be compileable before it is considered ready for execution.

Automated Regression Protection

Changes to the framework should be validated through CI.

Controlled LLM Usage

LLMs should be used where they provide reasoning value, not where conventional automation is more reliable.

📈 Current Development Roadmap

QAgent is being developed incrementally.

Core Reasoning Pipeline
 Page metadata extraction
 Application understanding
 Functional modeling
 Risk-based strategy
 Scenario generation
 Test case generation
 Test case traceability
 Selenium script generation
 Page Object generation
 Compilation validation
 Deterministic code review/fixing
 Advanced runtime evaluation
 Iterative debugging based on execution results
 Risk-based coverage metrics
QA Infrastructure
 Unit testing
 GitHub Actions Fast CI
 Full Pipeline Validation workflow
 Test result artifacts
 Generated output artifacts
 Allure reporting infrastructure
 Mutation testing foundation
 Stronger quality gates
 Coverage tracking
 Failure trend analysis
Product Infrastructure

The core QA reasoning pipeline is being developed first. Supporting product infrastructure will be introduced around the stable core.

Planned areas include:

 Database layer
 Persistent project storage
 User/account management
 Authentication and authorization
 Security hardening
 API layer
 Project configuration management
 Job/pipeline management
 Frontend interface
 Test result dashboard
 Execution history
 Notifications and integrations

The intention is to avoid building a large application shell around an unstable core engine.

🔮 Future Architecture

The long-term direction is to evolve QAgent from a test generator into a complete AI-assisted QA engineering platform.

                         QAgent Platform
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
    Web Application       API Application       Existing Tests
          │                    │                    │
          └────────────────────┼────────────────────┘
                               ▼
                     Application Understanding
                               │
                               ▼
                       Functional Modeling
                               │
                               ▼
                       Risk-Based Strategy
                               │
                               ▼
                    Scenario / Test Generation
                               │
                    ┌──────────┴──────────┐
                    ▼                     ▼
               UI Automation         API Automation
                    │                     │
                    └──────────┬──────────┘
                               ▼
                     Quality Validation
                               │
                               ▼
                        Test Execution
                               │
                               ▼
                       Runtime Evaluation
                               │
                               ▼
                     Reports / Analytics
🧪 Example Workflow

Given:

https://the-internet.herokuapp.com/login

QAgent should progressively transform the application into a structured QA representation.

Application
Login Page
Functional Model
Login Form
 ├── Username
 ├── Password
 └── Login Action
Risk Strategy
SCN-001
Valid login

SCN-002
Invalid username

SCN-003
Invalid password

SCN-004
Empty username

SCN-005
Empty password
Test Cases

Each test case retains its scenario and risk information.

Scenario
   ↓
Test Case
   ↓
Page Object
   ↓
TestNG Test
Validation
Generated Java
      ↓
Compiler
      ↓
Static Review
      ↓
Deterministic Fixes
      ↓
LLM Fix if Required
      ↓
Recompile
      ↓
Executable Test
📦 Generated Artifacts

Depending on the pipeline stage, QAgent can produce artifacts such as:

Test Cases
    ↓
Excel / Structured Data

Generated Page Objects
    ↓
*.java

Generated TestNG Tests
    ↓
*.java

Compilation Results
    ↓
Validation Information

Test Execution
    ↓
Test Results

Allure
    ↓
Test Report
🔍 Why QAgent?

The problem QAgent is trying to solve is not simply:

"How can AI write Selenium code?"

Modern LLMs can already generate Selenium code.

The harder QA engineering problems are:

What should actually be tested?
Which functionality is risky?
Why was this test generated?
Are important risks covered?
Can generated code be trusted?
Does the generated code compile?
Can predictable failures be fixed without another LLM call?
What happens when the generated test fails?
How can the entire process be validated automatically?
How can the system evolve into a maintainable QA platform?

QAgent is designed around these questions.

📌 Current Focus

The current development focus is on strengthening the core QA reasoning and validation pipeline.

The priority is:

Reliable Core
     ↓
Reliable Generated Tests
     ↓
Reliable Validation
     ↓
Reliable CI
     ↓
Runtime Evaluation
     ↓
Platform Infrastructure
     ↓
Frontend

This allows the system to mature from the inside out rather than building a frontend around an unreliable automation engine.

👨‍💻 Author

Saurav Manandhar

QA Engineer / Computer Science Graduate

QAgent is being developed as a portfolio and engineering project focused on the intersection of:

Quality Engineering
Test Automation
Agentic AI
Risk-Based Testing
Software Architecture
CI/CD
AI-assisted Code Generation
📄 License

This project is licensed under the MIT License.

⭐ Project Vision

QAgent is not intended to be another "AI that writes Selenium scripts."

The long-term goal is to build an agentic QA engineering system that can understand an application, reason about what matters, generate traceable tests, validate its own output, recover from failures, and continuously improve the quality of the generated test suite.

Understand
    ↓
Reason
    ↓
Prioritize
    ↓
Generate
    ↓
Validate
    ↓
Fix
    ↓
Execute
    ↓
Evaluate
    ↓
Improve

That's the direction of QAgent
