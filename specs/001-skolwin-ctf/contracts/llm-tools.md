# Contract: LLM tools

Spring AI `@Tool` methods exposed to OpenRouter via `ChatClient.tools(...)`.
The model never receives API keys.

## discoverTool

| Item | Value |
|------|-------|
| Java | `DiscoverTool.discover` |
| Name | `discoverTool` |
| Purpose | Discover game rules, paths, map, notes, vehicles, costs, commands |

Parameters:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| query | string | yes | English question or keywords |
| path | string | no | Hub path after the first call. Ignored on the first call. |

First invocation always POSTs to `{base-url}{toolsearch-path}`.

## verifyTool

| Item | Value |
|------|-------|
| Java | `VerifyTool.verify` |
| Name | `verifyTool` |
| Purpose | Submit a trip. Success body contains `{FLG:...}`. |

Parameters:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| answer | string[] | yes | Commands only. Optional leading vehicle name. |

`apikey` and `task` are not tool parameters.

If `answer` is missing, empty, or not a list of strings, the tool MUST return
an error to the model, MUST NOT call the hub, and MUST NOT decrement the
verify budget.

## Stop behavior

When a tool finds a flag or exhausts its budget it sets `GameSession` and
returns with `returnDirect = true` so `ToolCallingAdvisor` does not continue
the model loop.

## Logging

Every tool invocation logs name, parameters, and result after secret
redaction. Model turns log system prompt, tool definitions, user prompt, and
response after secret redaction.
