# Refactoring Goal: Transform QAgent into a Provider-Agnostic AI Test Generation Platform

## Objective

The purpose of this project is **NOT** to have Claude generate Selenium tests directly.

The purpose of this project is to build an AI-powered Selenium test generation platform that can work with **any supported LLM at runtime**, including but not limited to:

- Ollama
- Nvidia NIM
- Groq
- OpenAI-compatible APIs
- Claude API
- Gemini
- DeepSeek
- Qwen
- Future providers

The framework itself should perform all orchestration.

The LLM should only provide intelligence when requested.

---

# Current Problem

Currently the project has two execution modes.

## Normal Mode

```
Metadata Extraction

↓

Agent 1
Generate Test Cases

↓

Agent 2
Generate Java Code

↓

Agent 3
Compile

↓

Agent 4
Review / Fix

↓

Agent 5
Execute Tests
```

This is the architecture I want to preserve.

---

## Claude Mode

Currently Claude Mode works like this:

```
Claude

↓

Writes Excel

↓

Writes POM

↓

Writes Test Classes

↓

Skip Agent 1

Skip Agent 2

Skip Agent 3

Skip Agent 4

↓

Run Tests
```

This completely bypasses the architecture.

That is NOT the long-term goal.

Claude should never replace the pipeline.

Claude should become another LLM provider that participates in the pipeline exactly like Ollama or Groq.

---

# Long-Term Vision

The framework should become an AI orchestration platform.

The pipeline should never change regardless of which model is selected.

```
                User

                  │

                  ▼

             Main Runner

                  │

                  ▼

        Metadata Extraction

                  │

                  ▼

             Agent 1

                  │

                  ▼

             Agent 2

                  │

                  ▼

             Agent 3

                  │

                  ▼

             Agent 4

                  │

                  ▼

             Agent 5

                  │

                  ▼

          Generated Tests
```

None of these agents should know which LLM is being used.

---

# New LLM Architecture

Instead of

```
Agent

↓

GroqClient

↓

Ollama
```

or

```
Agent

↓

GroqClient

↓

Groq
```

I want

```
Agent

↓

LLM Service

↓

Provider Factory

↓

Provider Adapter

↓

Selected Provider
```

Example

```
Agent

↓

LLM Service

↓

Provider Factory

↓

Claude Provider
```

or

```
Agent

↓

LLM Service

↓

Provider Factory

↓

Ollama Provider
```

The Agent should never change.

Only the provider changes.

---

# Replace GroqClient

GroqClient currently contains provider-specific logic.

I want to replace it with something similar to

```
LLMService

or

LLMClient
```

that exposes only methods such as

```
chat()

chatWithTools()
```

Internally it should automatically delegate to

```
OllamaProvider

GroqProvider

NvidiaNimProvider

ClaudeProvider

OpenAIProvider

GeminiProvider

etc.
```

Agents should never know which provider is being used.

---

# Remove CLAUDE_MODE

CLAUDE_MODE should be removed completely.

Instead,

Claude should become another provider.

Instead of

```
CLAUDE_MODE

↓

Claude writes Java files
```

it should become

```
provider=claude

↓

Agent 1

↓

Claude API

↓

Agent 2

↓

Claude API

↓

Agent 3

↓

Agent 4

↓

Agent 5
```

Exactly like every other provider.

---

# Preserve the Agent Architecture

The existing architecture is one of the strengths of the project.

I want to preserve

- Agent 1
- Agent 2
- Agent 3
- Agent 4
- Agent 5

Their responsibilities should remain the same.

Only the communication with the LLM should change.

---

# Preserve Existing Components

Do NOT rewrite the framework.

Continue using the existing infrastructure whenever possible.

Preserve

- TestCase
- PageMetadata
- CompileResult
- ExcelUtil
- ApiExcelUtil
- PromptBuilder
- ModelConfig
- FrameworkConfig
- DriverFactory
- BasePage
- BaseTest
- JavaCompilerUtil
- LlmCache

The goal is architectural improvement rather than rewriting everything.

---

# Prompt Layer

Currently PromptBuilder generates prompts.

I want to improve this into something more modular.

Example

```
Agent

↓

Prompt Composer

↓

Prompt Validator

↓

LLM Service

↓

Response Parser

↓

Provider
```

This allows prompts to be adjusted slightly for different providers.

Example

Claude may require one format.

Qwen may require another.

Ollama may require another.

The Agent should never care.

---

# Response Parsing

Different providers return different formats.

The framework should normalize responses into a common internal structure.

The framework should automatically recover from

- markdown
- code fences
- malformed JSON
- missing tool calls
- partial JSON
- provider-specific formatting

The agents should always receive a normalized response.

---

# Provider Abstraction

I want a provider interface.

Example

```
interface LLMProvider

chat()

chatWithTools()

supportsToolCalling()

supportsStreaming()

supportsVision()

getProviderName()
```

Each provider should implement this interface.

Examples

```
OllamaProvider

GroqProvider

ClaudeProvider

OpenAIProvider

GeminiProvider

NvidiaNimProvider
```

Adding a new provider should require implementing only this interface.

No agent should require modification.

---

# Keep Agent 4

The existing Agent 4 strategy is excellent.

```
Rules

↓

Compile

↓

LLM

↓

Compile
```

Please preserve this architecture.

Do not replace it.

If possible, improve it.

---

# Improve Agent 2

Instead of one large generation step

```
Generate POM

Generate Test Class
```

consider internally splitting responsibilities into smaller components such as

```
Planner

↓

POM Generator

↓

Test Generator

↓

Locator Validator

↓

Formatter
```

This should remain transparent to the rest of the framework.

---

# Better Use of Metadata

Currently PageMetadata is mainly used by Agent 1.

I want metadata to also be available to

- Agent 2
- Agent 4

This will improve

- locator quality
- component understanding
- validation
- repair decisions

---

# Caching

Continue using LlmCache.

Improve it where possible.

Caching should remain provider-independent.

---

# Retry Logic

Retry logic should remain centralized.

Providers should not duplicate retry logic.

---

# Error Recovery

Continue improving automatic recovery for

- malformed JSON
- truncated responses
- missing fields
- invalid Java
- compilation failures

The framework should recover automatically whenever possible.

---

# Configuration

The framework should allow selecting a provider entirely through configuration.

Example

```
provider=ollama

provider=groq

provider=claude

provider=nim

provider=openai
```

Changing providers should not require code changes.

---

# Final Architecture

The final architecture should resemble

```
                     Main

                      │

         Metadata Extraction

                      │

                 Agent 1

                      │

                 Agent 2

                      │

                 Agent 3

                      │

                 Agent 4

                      │

                 Agent 5

──────────────────────────────────────────────

                 LLM Service

                      │

             Provider Factory

                      │

 ┌──────────┬──────────┬──────────┬──────────┐
 │          │          │          │
Ollama    Groq       NIM      Claude
 │          │          │          │
 └──────────┴──────────┴──────────┘

──────────────────────────────────────────────

Prompt Composer

Prompt Validator

Response Parser

Cache

Retry

JSON Repair

Tool Calling

Validation
```

---

# Refactoring Requirements

Do NOT perform a massive rewrite.

Refactor incrementally.

The project should remain buildable after each stage.

Each refactoring step should preserve backward compatibility whenever possible.

---

# Important

This project is an AI orchestration framework.

It is NOT a Claude-generated Selenium project.

Claude's responsibility is to improve the framework itself.

Claude should never replace the framework.

The framework should remain responsible for:

- generating test cases
- generating POM classes
- generating test scripts
- compiling
- repairing
- executing

through whichever LLM provider is selected at runtime.

The end goal is a reusable AI-powered Selenium framework that is completely independent of any single LLM provider.