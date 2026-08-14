# Data Model: Skolwin Capture The Flag

In-memory only. Nothing is persisted across process launches.

## AppProperties

Configuration bound once at startup (`app.*` plus Spring AI OpenAI keys).

| Field | Type | Default | Source |
|-------|------|---------|--------|
| hub.apiKey | string | (required) | `HUB_API_KEY` |
| hub.baseUrl | URI | `https://hub.ag3nts.org` | `app.hub.base-url` |
| hub.toolsearchPath | string | `/api/toolsearch` | `app.hub.toolsearch-path` |
| hub.verifyPath | string | `/verify` | `app.hub.verify-path` |
| hub.task | string | `savethem` | `app.hub.task` |
| openrouter.baseUrl | URI | `https://openrouter.ai/api/v1` | `app.openrouter.base-url` |
| llm.model | string | `inclusionai/ling-3.0-flash` | `app.llm.model` |
| tools.discoverLimit | int | 30 | `app.tools.discover-limit` |
| tools.verifyLimit | int | 10 | `app.tools.verify-limit` |
| prompt.system | string | file `prompts/system.txt` | `app.prompt.system` |
| briefing.destination | string | `Skolwin` | `app.briefing.destination` |
| briefing.mapWidth | int | 10 | `app.briefing.map-width` |
| briefing.mapHeight | int | 10 | `app.briefing.map-height` |
| briefing.startingFuel | int | 10 | `app.briefing.starting-fuel` |
| briefing.startingFood | int | 10 | `app.briefing.starting-food` |

Validation: api keys and URLs MUST be non-blank; limits, map dimensions, and starting resources MUST be > 0; destination MUST be non-blank.
Configuration is authoritative for discover and verify limits (defaults 30 and 10).
`SystemPromptFactory` interpolates briefing fields from this object, not from hardcoded constants.

## ToolBudget

| Field | Type | Rules |
|-------|------|-------|
| discoverRemaining | int | Starts at discoverLimit; decrement on every discover attempt |
| verifyRemaining | int | Starts at verifyLimit; decrement on every verify attempt |
| discoverExhausted | derived | remaining == 0 |
| verifyExhausted | derived | remaining == 0 |

A call that gets an empty, unreadable, timed-out, or unreachable HTTP body
still consumes one use. A verify invocation rejected locally for a missing,
empty, or non-list `answer` does not consume a use.

## GameSession

| Field | Type | Rules |
|-------|------|-------|
| toolsearchCompleted | boolean | False until the first discover attempt finishes |
| flag | string? | Set when a **verify** hub body contains `{FLG:...}` |
| stopReason | string? | Flag acquired, discover limit, verify limit, missing secrets, unreadable response after last use |
| lastDiscoverQuery | string? | Last English query sent |
| lastDiscoverPath | string? | Path actually used (forced toolsearch on first call) |
| lastVerifyAnswer | list of string? | Last submitted command array |
| lastVerifyOutcome | string? | success / error text (no secrets) |

State transitions:

```text
STARTED
  -> PLANNING          (ChatClient loop running)
  -> FLAG_ACQUIRED     (verify body contains {FLG:...}) -> exit 0
  -> DISCOVER_LIMIT    -> exit 1
  -> VERIFY_LIMIT      -> exit 1
  -> STARTUP_FAILED    (missing secrets) -> exit 2
```

## DiscoverRequest / DiscoverResponse

Request (hub JSON):

| Field | Type | Rules |
|-------|------|-------|
| apikey | string | From config, never from the model |
| query | string | English; required; from the model |

Path is not in the JSON body. It is the URL path. First call uses
`toolsearchPath`. Later calls use the model-supplied path.

Response: opaque hub JSON. Treat as a string for the model. Count as readable
vs empty. Do not treat a discover body as flag acquisition.

## VerifyRequest / VerifyResponse

Request (hub JSON):

| Field | Type | Rules |
|-------|------|-------|
| apikey | string | From config |
| task | string | From config (`savethem`) |
| answer | string[] | Commands only; first element MAY be a vehicle name |

Response: opaque hub JSON/text. Success if it contains `{FLG:...}`. Otherwise
an error the model may read.

## TripPlan

Not stored as a first-class table. The live representation is the `answer`
string array passed to `verifyTool`.

| Constraint | Rule |
|------------|------|
| Commands | Only commands previously returned by discovery |
| Vehicle | Optional first element: discovered vehicle type |
| Disband | Allowed if discovery lists a disband/walk command. No vehicle-to-vehicle change. |
| Resources | Planner assumes 10 fuel and 10 food per attempt unless discovery overrides |

## RunOutcome

Returned by `PlannerRunner` to set the process exit code.

| Field | Type |
|-------|------|
| exitCode | 0 flag, 1 limit/planning failure, 2 startup |
| flag | optional `{FLG:...}` |
| stopReason | operator-facing sentence |

## Relationships

```text
AppProperties --> ToolBudget (initial limits)
AppProperties --> HubClient (URLs, key, task)
AppProperties --> SystemPromptFactory
ToolBudget <--> DiscoverTool
ToolBudget <--> VerifyTool
DiscoverTool --> HubClient
VerifyTool --> HubClient
HubClient --> GameSession (flag / last outcomes)
PlannerRunner --> ChatClient + GameSession + RunOutcome
```
