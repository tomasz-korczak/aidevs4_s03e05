# Contract: Hub discover

## Request

`POST {app.hub.base-url}{path}`

`Content-Type: application/json`

```json
{
  "apikey": "<HUB_API_KEY>",
  "query": "I need notes about movement rules and terrain"
}
```

| Field | Required | Notes |
|-------|----------|-------|
| apikey | yes | Environment `HUB_API_KEY`. Injected by `HubClient`. |
| query | yes | English question or keywords from the model. |

### Path

| Call | Path | Who chooses it |
|------|------|----------------|
| First discover in the process | `{app.hub.toolsearch-path}` default `/api/toolsearch` | Application, always |
| Later discovers | Path returned by toolsearch / notes (example `/api/...`) | Model argument, after first call |

Full first URL with defaults: `https://hub.ag3nts.org/api/toolsearch`.

## Response

Hub JSON or text, up to three best matches for the query. The application
forwards the body to the model as the tool result. It MUST NOT assume a fixed
schema beyond: non-empty readable body vs empty/unreadable.

Each HTTP attempt counts as one discover use, including failures. Timeout and
unreachable hub are the same as empty/unreadable.

A `{FLG:...}` string is not expected on discover; the application MUST NOT
treat a discover body as flag acquisition.

## Errors

| Condition | Tool result to the model | Budget |
|-----------|--------------------------|--------|
| Budget already 0 | Limit-reached message; session stop | No extra decrement |
| HTTP / empty / unreadable / timeout / unreachable | Error text; retry with a different query if uses remain | Decrement |
| Success | Raw body (secrets redacted in logs) | Decrement |
