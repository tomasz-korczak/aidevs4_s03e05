# Quickstart: Skolwin Capture The Flag

Validation guide for the one-shot planner. Implementation lives in later
tasks; this file is how an operator proves the run works.

## Prerequisites

- JDK 23 at `C:\tools\jdk-23.0.2` (`JAVA_HOME` set to that path)
- Maven 3.6.3+
- Environment:
  - `OPENROUTER_API_KEY` — OpenRouter key
  - `HUB_API_KEY` — hub.ag3nts.org key

Copy `.env.example` and fill the two keys. Do not commit real values.

## Build

```powershell
$env:JAVA_HOME = "C:\tools\jdk-23.0.2"
mvn -q -DskipTests package
```

Expected: `target/s03e02-*.jar` (group `pl.tomaszko`, artifact `s03e02`).

## Configure

Defaults in `src/main/resources/application.yaml` (override as needed):

| Key | Default |
|-----|---------|
| `app.llm.model` | `inclusionai/ling-3.0-flash` |
| `app.openrouter.base-url` | `https://openrouter.ai/api/v1` |
| `app.hub.base-url` | `https://hub.ag3nts.org` |
| `app.hub.toolsearch-path` | `/api/toolsearch` |
| `app.hub.verify-path` | `/verify` |
| `app.hub.task` | `savethem` |
| `app.tools.discover-limit` | `30` |
| `app.tools.verify-limit` | `10` |
| `app.prompt.system` | interpolated `prompts/system.txt` |

See [data-model.md](./data-model.md) and [contracts/](./contracts/).

## Run (no arguments)

```powershell
$env:JAVA_HOME = "C:\tools\jdk-23.0.2"
$env:OPENROUTER_API_KEY = "..."
$env:HUB_API_KEY = "..."
mvn -q spring-boot:run
```

Or `java -jar target/s03e02-*.jar` with the same env vars. Do not pass argv.

## Expected live outcome

1. Process does not wait for keyboard input.
2. First hub discover is `POST https://hub.ag3nts.org/api/toolsearch`.
3. Stdout shows each discover query, each verify outcome, and remaining uses
   ([operator-console.md](./contracts/operator-console.md)).
4. On success: `{FLG:...}` on stdout and exit `0`.
5. On limit: stop reason on stderr and exit `1`.
6. Missing keys: fail before any hub/OpenRouter call, exit `2`.
7. `logs/s03e02.log` contains redacted model prompts (system, tools, user)
   and tool parameters/results. Keys do not appear.

## Automated checks (no live keys required)

```powershell
$env:JAVA_HOME = "C:\tools\jdk-23.0.2"
mvn -q test
```

Must cover:

- first discover path is forced to toolsearch
- budgets decrement on success and on unreadable responses
- verify payload uses configured `task` and never takes `apikey` from the model
- secret redaction strips both env keys
- system prompt interpolation inserts limits and URLs
- runner ignores command-line args

## Startup failure check

```powershell
Remove-Item Env:OPENROUTER_API_KEY -ErrorAction SilentlyContinue
mvn -q spring-boot:run
```

Expected: exit `2`, no hub POST, no OpenRouter call.
