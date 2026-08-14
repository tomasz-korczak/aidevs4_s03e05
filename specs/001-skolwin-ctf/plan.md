# Implementation Plan: Skolwin Capture The Flag

**Branch**: `001-skolwin-ctf` | **Date**: 2026-08-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-skolwin-ctf/spec.md`

## Summary

One-shot Spring Boot console app that uses Spring AI tool calling against OpenRouter
to discover game rules via the hub `discoverTool` flow, then submit a Skolwin trip
via `verifyTool` until a `{FLG:...}` string arrives or a tool budget is exhausted.
The process starts with no command-line parameters, fails fast without secrets,
logs model traffic and tool I/O (secrets redacted), prints the flag or a stop
reason, and exits.

## Technical Context

**Language/Version**: Java 23 (JDK `C:\tools\jdk-23.0.2`)

**Primary Dependencies**: Spring Boot 4.1.0, Spring AI 2.0.0
(`spring-ai-starter-model-openai`, `@Tool` / `@ToolParam`, `ChatClient`,
`ToolCallingAdvisor`), Spring RestClient, Logback

**Storage**: N/A (in-memory session only; file logging)

**Testing**: JUnit 5, Spring Boot Test, MockRestServiceServer, Mockito

**Target Platform**: Windows / any JVM console; `JAVA_HOME=C:\tools\jdk-23.0.2`

**Project Type**: Console application (Spring Boot, `web-application-type=none`)

**Performance Goals**: Single autonomous run; sequential tool loop; exit as soon
as the flag arrives or a configured tool limit is hit

**Constraints**: No CLI args; OpenRouter only for LLM; hub HTTP only for game
tools; configured discover/verify limits (defaults 30 and 10, config
authoritative); first discover path forced to `/api/toolsearch`; secrets from
env; logs must redact keys; no vehicle-to-vehicle switch; no extra time cap

**Scale/Scope**: One Maven module (`pl.tomaszko:s03e02`); two tools; one planner
conversation; 10×10 map briefing defaults

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|-----------|--------|------------------------|
| I. Console Application, No Parameters | PASS | `ApplicationRunner` ignores argv; no CLI parser; config from env / `application.yaml` |
| II. One-Shot Autonomous Execution | PASS | `web-application-type=none`; runner starts the ChatClient tool loop and `SpringApplication.exit` |
| III. OpenRouter LLM Access | PASS | OpenAI-compatible starter pointed at OpenRouter; `OPENROUTER_API_KEY` required at startup |
| IV. Flag-Centric Outcome | PASS | Flag to stdout; failure reason to stderr; secrets never logged or printed |
| V. Minimal Scope | PASS | No web API, no extra agent platform; only ChatClient, two `@Tool` methods, hub client, logging |

Post-design re-check: still PASS. File logging and advisors exist only to satisfy
operator follow-along (FR-019) and the requested model/tool traces. No extra
external I/O beyond OpenRouter and hub.ag3nts.org.

## Project Structure

### Documentation (this feature)

```text
specs/001-skolwin-ctf/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── hub-discover.md
│   ├── hub-verify.md
│   ├── llm-tools.md
│   ├── operator-console.md
│   └── system-prompt.md
└── tasks.md
```

### Source Code (repository root)

```text
pom.xml
.env.example
src/main/java/pl/tomaszko/s03e02/
├── S03e02Application.java
├── config/
│   ├── AppProperties.java
│   ├── ChatClientConfig.java
│   └── HubClientConfig.java
├── hub/
│   ├── HubClient.java
│   ├── DiscoverRequest.java
│   ├── DiscoverResponse.java
│   ├── VerifyRequest.java
│   └── VerifyResponse.java
├── tools/
│   ├── DiscoverTool.java
│   ├── VerifyTool.java
│   └── ToolBudget.java
├── session/
│   ├── GameSession.java
│   └── RunOutcome.java
├── prompt/
│   └── SystemPromptFactory.java
├── logging/
│   ├── ModelCommunicationAdvisor.java
│   ├── ToolExecutionLogger.java
│   └── SecretRedactor.java
└── runner/
    └── PlannerRunner.java
src/main/resources/
├── application.yaml
├── logback-spring.xml
└── prompts/system.txt
src/test/java/pl/tomaszko/s03e02/
├── tools/
├── hub/
├── prompt/
├── logging/
└── runner/
```

**Structure Decision**: Single Maven module at the repository root. Spring Boot
with `web-application-type=none` keeps the constitution's console, one-shot
constraint. Packages split only where the flag-search loop needs a seam
(config, hub HTTP, tools, session, logging, runner).

## Complexity Tracking

> No constitution violations requiring justification.
