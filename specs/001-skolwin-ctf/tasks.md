---
description: "Task list for Skolwin Capture The Flag"
---

# Tasks: Skolwin Capture The Flag

**Input**: Design documents from `/specs/001-skolwin-ctf/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: Included. [research.md](./research.md) and [quickstart.md](./quickstart.md) require automated checks with no live keys (first-path, budgets, verify payload, redaction, prompt interpolation, runner ignores argv). Not TDD-first unless a task says so.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Single Maven module at repository root (`pl.tomaszko:s03e05`). Java sources under `src/main/java/pl/tomaszko/s03e05/`. Tests under `src/test/java/pl/tomaszko/s03e05/`. JDK `C:\tools\jdk-23.0.2`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Maven module, package layout, committed config skeletons, and secret placeholders

- [x] T001 Create `pom.xml` at repository root: parent `org.springframework.boot:spring-boot-starter-parent:4.1.0`, `groupId` `pl.tomaszko`, `artifactId` `s03e05`, packaging `jar`, `maven.compiler.release` 23, import `org.springframework.ai:spring-ai-bom:2.0.0`, dependencies `spring-boot-starter` (no `spring-boot-starter-web`), RestClient support without Tomcat, `spring-ai-starter-model-openai`, `spring-boot-starter-test` (JUnit 5, Mockito)
- [x] T002 Create package directories `src/main/java/pl/tomaszko/s03e05/{config,hub,tools,session,prompt,logging,runner}` and matching `src/test/java/pl/tomaszko/s03e05/{tools,hub,prompt,logging,runner}` per [plan.md](./plan.md)
- [x] T003 [P] Create `.env.example` listing empty `OPENROUTER_API_KEY=` and `HUB_API_KEY=` only
- [x] T004 [P] Create `.gitignore` ignoring `target/`, `logs/`, `.env`, IDE metadata; do not ignore `.env.example`
- [x] T005 [P] Create `src/main/resources/application.yaml` with `spring.main.web-application-type: none`, `spring.ai.openai.api-key: ${OPENROUTER_API_KEY}`, `spring.ai.openai.base-url: ${app.openrouter.base-url}`, `spring.ai.openai.chat.options.model: ${app.llm.model}`, and `app.*` defaults from [data-model.md](./data-model.md) (hub URL `https://hub.ag3nts.org`, toolsearch `/api/toolsearch`, verify `/verify`, task `savethem`, OpenRouter `https://openrouter.ai/api/v1`, model `inclusionai/ling-3.0-flash`, discover-limit 30, verify-limit 10, briefing destination `Skolwin`, map 10×10, starting fuel/food 10)
- [x] T006 [P] Create `src/main/resources/logback-spring.xml` with CONSOLE and FILE appenders writing `logs/s03e05.log`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Configuration, secrets fail-fast, in-memory session, hub HTTP, ChatClient wiring, and redacted logging — required before any user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T007 Implement `src/main/java/pl/tomaszko/s03e05/config/AppProperties.java` as `@ConfigurationProperties(prefix = "app")` with nested hub (apiKey, baseUrl, toolsearchPath, verifyPath, task), openrouter.baseUrl, llm.model, tools.discoverLimit / verifyLimit, optional prompt.system, and nested briefing (destination, mapWidth, mapHeight, startingFuel, startingFood) per [data-model.md](./data-model.md); validate non-blank keys/URLs/destination and limits/map/resources > 0
- [x] T008 Enable configuration properties on `src/main/java/pl/tomaszko/s03e05/S03e05Application.java` (`@SpringBootApplication`, `@EnableConfigurationProperties`) and bind `app.hub.api-key: ${HUB_API_KEY}` in `src/main/resources/application.yaml`
- [x] T009 Implement startup fail-fast in `src/main/java/pl/tomaszko/s03e05/config/SecretsValidator.java` so blank or missing `OPENROUTER_API_KEY` or `HUB_API_KEY` fails the context before any hub or OpenRouter call; process exit code 2 per [contracts/operator-console.md](./contracts/operator-console.md)
- [x] T010 [P] Implement `src/main/java/pl/tomaszko/s03e05/logging/SecretRedactor.java` that strips `HUB_API_KEY`, `OPENROUTER_API_KEY`, and JSON `apikey` field values from any string
- [x] T011 [P] Implement `src/main/java/pl/tomaszko/s03e05/tools/ToolBudget.java` starting at configured discover/verify limits; decrement on each attempt; derived exhausted flags; do not decrement when remaining is already 0
- [x] T012 [P] Implement `src/main/java/pl/tomaszko/s03e05/session/GameSession.java` with toolsearchCompleted, flag (verify-only), stopReason, lastDiscoverQuery/path, lastVerifyAnswer/outcome, and states STARTED / PLANNING / FLAG_ACQUIRED / DISCOVER_LIMIT / VERIFY_LIMIT / STARTUP_FAILED from [data-model.md](./data-model.md)
- [x] T013 [P] Implement `src/main/java/pl/tomaszko/s03e05/session/RunOutcome.java` with exitCode (0 flag, 1 limit/planning failure, 2 startup), optional flag, operator-facing stopReason
- [x] T014 [P] Implement hub DTOs `src/main/java/pl/tomaszko/s03e05/hub/DiscoverRequest.java`, `DiscoverResponse.java`, `VerifyRequest.java`, `VerifyResponse.java` matching [contracts/hub-discover.md](./contracts/hub-discover.md) and [contracts/hub-verify.md](./contracts/hub-verify.md)
- [x] T015 Implement `src/main/java/pl/tomaszko/s03e05/config/HubClientConfig.java` RestClient bean (timeouts, JSON) and `src/main/java/pl/tomaszko/s03e05/hub/HubClient.java`: POST `{baseUrl}{path}` with `{apikey, query}` for discover and POST `{baseUrl}{verifyPath}` with `{apikey, task, answer}` for verify; inject apikey and task from `AppProperties`, never from callers that originated in the model; treat empty, unreadable, timed-out, and unreachable responses as failed attempts returning error text
- [x] T016 Implement `src/main/java/pl/tomaszko/s03e05/config/ChatClientConfig.java` `ChatClient` bean pointed at OpenRouter via Spring AI OpenAI starter (`base-url` includes `/v1`); do not register tools yet
- [x] T017 [P] Implement `src/main/java/pl/tomaszko/s03e05/logging/ModelCommunicationAdvisor.java` logging system prompt, tool definitions, user prompt, and model response after `SecretRedactor`
- [x] T018 Implement `src/main/java/pl/tomaszko/s03e05/logging/ToolExecutionLogger.java` logging each tool name, parameters, and result after `SecretRedactor` (depends on T010)

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 1 - Obtain The Flag By Reaching Skolwin (Priority: P1) 🎯 MVP

**Goal**: One-shot console run that discovers enough to submit a Skolwin trip, prints `{FLG:...}` on success, and exits without operator input

**Independent Test**: Start with `OPENROUTER_API_KEY` and `HUB_API_KEY` set and no argv. The process proceeds alone and either prints `{FLG:...}` (exit 0) or a stop reason (exit 1), then exits. Missing keys still fail at startup with exit 2 (T009).

### Tests for User Story 1

- [x] T019 [P] [US1] Unit-test first discover path is forced to configured toolsearch in `src/test/java/pl/tomaszko/s03e05/tools/DiscoverToolFirstPathTest.java` using MockRestServiceServer or a fake `HubClient`
- [x] T020 [P] [US1] Contract-test verify JSON uses configured `task`, injects apikey in the client, and never accepts apikey from the model in `src/test/java/pl/tomaszko/s03e05/hub/VerifyPayloadTest.java`
- [x] T021 [P] [US1] Unit-test `SystemPromptFactory` interpolation of limits, hub URL, toolsearch path, destination, and briefing defaults in `src/test/java/pl/tomaszko/s03e05/prompt/SystemPromptFactoryTest.java`
- [x] T022 [P] [US1] Unit-test `SecretRedactor` strips both env keys and `apikey` values in `src/test/java/pl/tomaszko/s03e05/logging/SecretRedactorTest.java`
- [x] T023 [P] [US1] Test `PlannerRunner` / `ApplicationRunner` ignores command-line args in `src/test/java/pl/tomaszko/s03e05/runner/PlannerRunnerIgnoresArgsTest.java`

### Implementation for User Story 1

- [x] T024 [P] [US1] Create `src/main/resources/prompts/system.txt` from [contracts/system-prompt.md](./contracts/system-prompt.md) with `${destination}`, `${mapWidth}`, `${mapHeight}`, `${startingFuel}`, `${startingFood}`, `${hubBaseUrl}`, `${toolsearchPath}`, `${discoverLimit}`, `${verifyLimit}`
- [x] T025 [US1] Implement `src/main/java/pl/tomaszko/s03e05/prompt/SystemPromptFactory.java` interpolating `AppProperties` (hub, tools, and `app.briefing.*`) into `prompts/system.txt`, honoring optional `app.prompt.system` override; do not hardcode briefing constants (depends on T024)
- [x] T026 [US1] Implement `src/main/java/pl/tomaszko/s03e05/tools/DiscoverTool.java` `@Tool(name = "discoverTool")` with `@ToolParam` `query` (required) and `path` (optional); first invocation ignores path and POSTs to `toolsearchPath`; later calls use model path; decrement discover budget on each hub attempt; when remaining is already 0, return a limit-reached message, set `GameSession` `DISCOVER_LIMIT`, `returnDirect`, and do not call the hub or decrement again; never treat discover body as flag acquisition; return hub body (or error text) to the model per [contracts/llm-tools.md](./contracts/llm-tools.md)
- [x] T027 [US1] Implement `src/main/java/pl/tomaszko/s03e05/tools/VerifyTool.java` `@Tool(name = "verifyTool")` with `@ToolParam` `answer` string array only; inject apikey and task in `HubClient`; decrement verify budget on hub attempts; when remaining is already 0, return limit-reached, set `VERIFY_LIMIT`, `returnDirect`, no hub call, no extra decrement; on `{FLG:...}` set `GameSession.flag` and `returnDirect`
- [x] T028 [US1] Implement `src/main/java/pl/tomaszko/s03e05/session/OperatorReporter.java` printing each discovery question or that a trip check was submitted, the check outcome summary (not the full hub dump), remaining discover and verify uses, the final `{FLG:...}` when present, and the limit/planning stop reason on stdout; startup and unexpected errors on stderr; no secrets per [contracts/operator-console.md](./contracts/operator-console.md)
- [x] T029 [US1] Wire `ChatClient.tools(discoverTool, verifyTool)`, `ModelCommunicationAdvisor`, and `ToolExecutionLogger` in `src/main/java/pl/tomaszko/s03e05/config/ChatClientConfig.java` (edit after T016). This is the only place tools are registered.
- [x] T030 [US1] Implement `src/main/java/pl/tomaszko/s03e05/runner/PlannerRunner.java` as `ApplicationRunner` that ignores argv, uses the `ChatClient` bean from T029 (do not call `.tools(...)` here), sends interpolated system text plus a start-now user message, runs until flag or tool `returnDirect` / model stop, maps `GameSession` to `RunOutcome`, and does not offer a general chat UI
- [x] T031 [US1] Complete `src/main/java/pl/tomaszko/s03e05/S03e05Application.java` so `main` calls `System.exit(SpringApplication.exit(...))` with `RunOutcome.exitCode`, `web-application-type` remains none, and no CLI parser exists

**Checkpoint**: User Story 1 is a runnable one-shot planner that can obtain and print the flag

---

## Phase 4: User Story 2 - Discover The Unknown World Iteratively (Priority: P2)

**Goal**: Discovery stays English, iterative, and budget-capped: first call is toolsearch, later queries change when answers are incomplete, and exhausting discover uses stops the process immediately

**Independent Test**: Drive `DiscoverTool` with a fake hub. First POST is always toolsearch. A second call with a different query hits a later path. Unreadable/timeout bodies decrement the budget. At remaining 0 the tool returns a limit message, sets `DISCOVER_LIMIT`, uses `returnDirect`, and does not POST again.

### Tests for User Story 2

- [x] T032 [P] [US2] Unit-test discover (and verify) budgets decrement on success and on empty/unreadable/timeout/unreachable bodies in `src/test/java/pl/tomaszko/s03e05/tools/ToolBudgetConsumptionTest.java`

### Implementation for User Story 2

- [x] T033 [US2] In `src/main/java/pl/tomaszko/s03e05/tools/DiscoverTool.java` after a limit-reached stop (mechanics already in T026), emit the operator-facing `DISCOVER_LIMIT` stopReason via `OperatorReporter` on stdout with remaining uses 0; do not re-implement budget-zero or `returnDirect`
- [x] T034 [US2] In `src/main/java/pl/tomaszko/s03e05/tools/DiscoverTool.java` map empty/unreadable/timeout/unreachable hub results (classified by `HubClient` in T015) to error text for the model, decrement one discover use, keep `toolsearchCompleted` true after the first attempt even if that attempt failed, and call `OperatorReporter` with the query and remaining uses
- [x] T035 [US2] Update `src/main/resources/prompts/system.txt` so the planner: invents English questions or keywords only; treats each answer as at most three matches; changes the query when facts are missing; does not send non-English queries; does not wait for a full 10×10 map or complete vehicle catalog before the first verify (FR-023); starts with a discoverTool call so the first request hits toolsearch
- [x] T036 [US2] Ensure `src/main/java/pl/tomaszko/s03e05/runner/PlannerRunner.java` maps discover-limit stop to exit code 1 and prints the stop reason on stdout with no further discover or verify calls

**Checkpoint**: User Stories 1 and 2 — discovery iteration and discover-limit shutdown work without a live model

---

## Phase 5: User Story 3 - Revise Failed Trips Until Success Or Limit (Priority: P3)

**Goal**: Failed verifies return inspectable errors; invalid answers never hit the hub; the planner may discover more then submit a different trip; verify-limit or flag stops immediately; no vehicle-to-vehicle switch

**Independent Test**: Call `VerifyTool` with a rejected hub body and remaining budget — model receives the error, budget decremented, session records last answer. Call with empty/null `answer` — no hub POST, budget unchanged. Exhaust verify uses — `VERIFY_LIMIT`, `returnDirect`, exit 1. Flag body — print flag, exit 0, no further tools.

### Tests for User Story 3

- [x] T037 [P] [US3] Unit-test missing/empty/non-list `answer` is rejected locally with no hub call and no budget decrement in `src/test/java/pl/tomaszko/s03e05/tools/VerifyToolLocalRejectTest.java`
- [x] T038 [P] [US3] Unit-test flag in verify body sets session flag, `returnDirect`, and that a discover body containing `{FLG:...}` does not set the flag in `src/test/java/pl/tomaszko/s03e05/tools/FlagAcquisitionTest.java`

### Implementation for User Story 3

- [x] T039 [US3] In `src/main/java/pl/tomaszko/s03e05/tools/VerifyTool.java` reject missing, empty, or non-list `answer` locally (error text to the model, no hub call, no decrement) per FR-029
- [x] T040 [US3] In `src/main/java/pl/tomaszko/s03e05/tools/VerifyTool.java` on a rejected trip (non-flag body) return the error to the model, record `lastVerifyAnswer` / `lastVerifyOutcome`, and report via `OperatorReporter`; do not decrement again or re-implement remaining==0 / `returnDirect` (T027 already owns budget and stop)
- [x] T041 [US3] Update `src/main/resources/prompts/system.txt` planning rules: after a failed verify, read the error, optionally call discoverTool again, then submit a different `answer`; never repeat an unchanged failed answer; disband to foot only, never switch vehicle-to-vehicle; a trip may start in at most one vehicle
- [x] T042 [US3] In `src/main/java/pl/tomaszko/s03e05/runner/PlannerRunner.java` stop on flag or either tool limit with no extra wall-clock or turn cap (FR-028); exit 0 only when verify produced `{FLG:...}`; remaining fuel/food need no separate meter (SC-006)

**Checkpoint**: All three user stories are independently testable; the run can recover from rejected trips or stop at the verify limit

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Quickstart validation, constitution compliance, and leftover operator/logging gaps

- [x] T043 [P] Confirm `src/main/resources/logback-spring.xml` and `.gitignore` keep `logs/s03e05.log` local-only and that model/tool traces are redacted in both console and file
- [x] T044 Align `src/main/resources/application.yaml` with every `AppProperties` field in [data-model.md](./data-model.md) and [quickstart.md](./quickstart.md) (limits authoritative; 30/10 are defaults only)
- [x] T045 Verify constitution gates in `src/main/java/pl/tomaszko/s03e05/S03e05Application.java` and `PlannerRunner.java`: no CLI parser, argv ignored, no web server, no I/O beyond OpenRouter and hub.ag3nts.org
- [x] T046 Run `mvn test` with `JAVA_HOME=C:\tools\jdk-23.0.2` per [quickstart.md](./quickstart.md) automated checks and fix failures
- [x] T047 Manually confirm startup-failure path from [quickstart.md](./quickstart.md): unset `OPENROUTER_API_KEY`, `mvn spring-boot:run`, expect exit 2 and no hub/OpenRouter call

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - Sequential by priority if one implementer: US1 → US2 → US3 (later stories edit `DiscoverTool`, `VerifyTool`, `system.txt`, `PlannerRunner`)
  - US1 is the MVP (flag path). US2/US3 harden discovery iteration and verify recovery on those same types
- **Polish (Phase 6)**: Depends on the user stories you intend to ship (MVP can polish after US1)

### User Story Dependencies

- **User Story 1 (P1)**: After Foundational — no story dependencies. Delivers discover + verify + runner + prompt + operator output
- **User Story 2 (P2)**: After US1 recommended (same `DiscoverTool` / prompt / runner files). Independently testable with a fake hub
- **User Story 3 (P3)**: After US1 recommended (same `VerifyTool` / prompt / runner files). Independently testable with a fake hub

### Within Each User Story

- Tests listed in a story may be written beside or just after the matching production class; they MUST fail until that behavior exists
- Prompt file before `SystemPromptFactory`
- Hub client and budget before tools
- Tools and prompt before `ChatClientConfig` tool wiring (T029)
- Wired `ChatClient` before `PlannerRunner` (T030)
- `PlannerRunner` before `System.exit` wiring
- Story complete before moving to the next priority when files overlap

### Parallel Opportunities

- Phase 1: T003, T004, T005, T006 after T001/T002 layout exists
- Phase 2: T010–T014 in parallel; T017 after T010; T015 after T014; T016 after T007/T008
- US1 tests T019–T023 in parallel once the classes they target exist (or as failing shells)
- US1 T024 can run in parallel with tool work; T025 depends on T024
- US3 tests T037–T038 in parallel
- Polish T043 can run beside T044

---

## Parallel Example: User Story 1

```text
# Tests (after production types exist, or as failing shells):
Task: "Unit-test first discover path in src/test/java/pl/tomaszko/s03e05/tools/DiscoverToolFirstPathTest.java"
Task: "Contract-test verify payload in src/test/java/pl/tomaszko/s03e05/hub/VerifyPayloadTest.java"
Task: "Unit-test prompt interpolation in src/test/java/pl/tomaszko/s03e05/prompt/SystemPromptFactoryTest.java"
Task: "Unit-test secret redaction in src/test/java/pl/tomaszko/s03e05/logging/SecretRedactorTest.java"
Task: "Test runner ignores argv in src/test/java/pl/tomaszko/s03e05/runner/PlannerRunnerIgnoresArgsTest.java"

# Prompt template (no Java dependency):
Task: "Create src/main/resources/prompts/system.txt from contracts/system-prompt.md"
```

---

## Parallel Example: User Story 3

```text
Task: "Unit-test local reject in src/test/java/pl/tomaszko/s03e05/tools/VerifyToolLocalRejectTest.java"
Task: "Unit-test flag acquisition in src/test/java/pl/tomaszko/s03e05/tools/FlagAcquisitionTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: `mvn test` plus a live run from [quickstart.md](./quickstart.md) if keys are present
5. Demo: flag on stdout or a clear stop reason, then process exit

### Incremental Delivery

1. Setup + Foundational → console app boots, missing secrets → exit 2
2. User Story 1 → autonomous discover/verify loop can win and print `{FLG:...}`
3. User Story 2 → iterative discovery + discover-limit shutdown
4. User Story 3 → revise failed trips, local answer reject, verify-limit shutdown
5. Polish → quickstart automated and startup-failure checks

### Parallel Team Strategy

With multiple developers, after Foundational:

- Developer A: US1 tools + runner (T026–T031)
- Developer B: US1 tests + prompt (T019–T025) — coordinate on `DiscoverTool` / `VerifyTool` file ownership
- Prefer one owner per overlapping file (`DiscoverTool.java`, `VerifyTool.java`, `system.txt`, `PlannerRunner.java`)

US2 and US3 should wait on those files rather than true parallel edits.

---

## Notes

- [P] tasks = different files, no dependencies on incomplete work
- [Story] label maps the task to spec.md user stories
- Do not hard-code map, vehicles, costs, or commands; briefing defaults only (10×10, 10 fuel, 10 food, Skolwin, walking uses food not fuel)
- Configuration is authoritative for tool limits; 30 and 10 are defaults
- Commit after each task or logical group
- Stop at any checkpoint to validate the story independently
- Avoid: CLI flags, Spring Shell, embedded web server, treating discover bodies as flags, vehicle-to-vehicle switches
