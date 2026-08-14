# Research: Skolwin Capture The Flag

## Decision: Spring Boot 4.1.0 + Java 23

- **Decision**: Use Spring Boot `4.1.0` (parent POM) and Java 23
  (`maven.compiler.release=23`, JDK `C:\tools\jdk-23.0.2`).
- **Rationale**: Latest stable Boot as of 2026-08-15. Boot 4.1 supports Java 17–26.
  Matches the requested JDK and annotation-first Spring style
  (`@SpringBootApplication`, `@ConfigurationProperties`, `@Tool`).
- **Alternatives considered**: Boot 4.0.7 (older train); Boot 3.5.x (OSS ended
  2026-06-30, incompatible with Spring AI 2.0).

## Decision: Spring AI 2.0.0 with OpenAI-compatible OpenRouter

- **Decision**: Import `org.springframework.ai:spring-ai-bom:2.0.0` and
  `spring-ai-starter-model-openai`. Point the OpenAI client at OpenRouter.
- **Rationale**: Spring AI 2.0.0 is the latest GA and requires Boot 4.0/4.1.
  OpenRouter is OpenAI-compatible. Spring AI 2.0 uses the official OpenAI Java
  SDK, so the default base URL includes `/v1`:
  `https://openrouter.ai/api/v1`.
- **Alternatives considered**: Spring AI 1.1.x (Boot 3.5 only); a custom HTTP
  OpenRouter client (rejects `@Tool` / `ChatClient` / `ToolCallingAdvisor`).

## Decision: OpenRouter configuration properties

- **Decision**:
  - `spring.ai.openai.api-key=${OPENROUTER_API_KEY}`
  - `spring.ai.openai.base-url=${app.openrouter.base-url}` (default
    `https://openrouter.ai/api/v1`)
  - `spring.ai.openai.chat.options.model=${app.llm.model}` (default
    `inclusionai/ling-3.0-flash`)
- **Rationale**: Model name and HTTP address are parametrized as required.
  Key stays in the environment. Fail startup if the key is blank.
- **Alternatives considered**: Hard-coded model (violates parametrization);
  `https://openrouter.ai/api` without `/v1` (correct for Spring AI 1.x path
  joining, wrong for the 2.0 official SDK).

## Decision: Maven coordinates

- **Decision**: `groupId=pl.tomaszko`, `artifactId=s03e02`, packaging `jar`.
- **Rationale**: Requested build identity. Application class
  `pl.tomaszko.s03e02.S03e02Application`.
- **Alternatives considered**: Matching the folder name `s03e05` (rejected;
  user set the artifact name).

## Decision: Console one-shot runtime, not a web server

- **Decision**: `spring.main.web-application-type=none`. Use
  `spring-boot-starter` + RestClient support, not an embedded Tomcat API.
  `PlannerRunner` implements `ApplicationRunner`. `main` calls
  `System.exit(SpringApplication.exit(...))`.
- **Rationale**: Constitution forbids a long-running server and CLI args.
  `ApplicationRunner` receives args and MUST ignore them.
- **Alternatives considered**: `spring-boot-starter-web` on port 8080
  (daemon); Spring Shell (CLI framework, forbidden).

## Decision: `@Tool` methods + auto `ToolCallingAdvisor`

- **Decision**: `DiscoverTool` and `VerifyTool` are Spring beans with `@Tool`
  / `@ToolParam`. `ChatClient.prompt().system(...).user(...).tools(...).call()`
  lets `ToolCallingAdvisor` run the loop until the model stops calling tools
  or a tool signals stop (`returnDirect` when flag found or budget exhausted).
- **Rationale**: User asked for Spring AI annotations. 2.0 owns the tool loop
  in the advisor chain. Tools enforce budgets and the first-path rule in Java
  so the model cannot bypass them.
- **Alternatives considered**: Manual `ToolCallingManager` loop (more code);
  MCP server annotations (wrong direction: we consume hub HTTP, we do not
  expose MCP).

## Decision: Hub HTTP via Spring RestClient

- **Decision**: One `HubClient` bean using `RestClient`. Discover POSTs to
  `{hub.base-url}{path}` with `{apikey, query}`. Verify POSTs to
  `{hub.base-url}{verify-path}` with `{apikey, task, answer}`.
- **Rationale**: Addresses and task name are configuration. The API key is
  injected in the client, never accepted from the model.
- **Alternatives considered**: WebClient (reactive, unused); raw HttpClient
  (no Spring annotations).

## Decision: First discover call forced to `/api/toolsearch`

- **Decision**: `DiscoverTool` keeps a session flag. The first invocation
  ignores any model-supplied path and uses `app.hub.toolsearch-path`
  (`/api/toolsearch`). Later calls use the model-supplied path if present.
- **Rationale**: User rule: the very first call must hit toolsearch so other
  paths can be learned. Enforcement in code, not only in the prompt.
- **Alternatives considered**: Prompt-only first-path (model can skip it).

## Decision: Tool budgets in process memory

- **Decision**: `ToolBudget` starts at `app.tools.discover-limit=30` and
  `app.tools.verify-limit=10`. Each tool attempt (including empty/unreadable
  HTTP) decrements. At zero the tool returns an error, sets session stop, and
  uses `returnDirect=true` so the advisor does not keep calling the model.
- **Rationale**: Spec FR-007 / FR-017. Limits are parametrized.
- **Alternatives considered**: Trust the model to stop (unsafe).

## Decision: Logging — console + file, plus two advisors

- **Decision**: Logback CONSOLE and FILE (`logs/s03e02.log`).
  `ModelCommunicationAdvisor` sits inside the tool loop and logs system
  prompt, tool definitions, user prompt, and model response.
  `ToolExecutionLogger` logs each tool name, parameters, and result.
  `SecretRedactor` strips `HUB_API_KEY`, `OPENROUTER_API_KEY`, and `apikey`
  field values.
- **Rationale**: Requested traces plus constitution secret rules. FR-019
  operator messages stay on stdout; detailed traces go to the logger.
- **Alternatives considered**: `SimpleLoggerAdvisor` only (does not guarantee
  tool-definition and per-tool parameter coverage).

## Decision: System prompt as a string template

- **Decision**: `src/main/resources/prompts/system.txt` is a template with
  placeholders for discover limit, verify limit, hub base URL, toolsearch
  path, verify path, task name, destination, briefing defaults. Bound through
  `app.prompt.system` override (optional) or the file default.
  `SystemPromptFactory` interpolates `AppProperties`.
- **Rationale**: Prompt is parametrized and includes tool-use rules. File
  keeps YAML readable; `app.prompt.system` still allows a full override.
  Full template: [contracts/system-prompt.md](./contracts/system-prompt.md).
- **Alternatives considered**: Prompt only in Java (harder to edit); prompt
  only in YAML (awkward multiline edits).

## Decision: Secrets and fail-fast

- **Decision**: `.env.example` lists `OPENROUTER_API_KEY=` and `HUB_API_KEY=`.
  A startup validator fails the context with a non-zero exit if either is
  blank. Keys are never constructor-logged.
- **Rationale**: Constitution III and FR-021.
- **Alternatives considered**: Lazy failure on first HTTP call (wastes a
  discover use).

## Decision: Testing approach

- **Decision**: Unit-test budget, first-path enforcement, prompt
  interpolation, and redaction without a live model. Contract-test hub JSON
  with `MockRestServiceServer`. Runner tests use a fake `ChatClient` or a
  stub `ChatModel`.
- **Rationale**: Live OpenRouter/hub calls are not reproducible in CI.
- **Alternatives considered**: Only end-to-end live runs (fragile).
