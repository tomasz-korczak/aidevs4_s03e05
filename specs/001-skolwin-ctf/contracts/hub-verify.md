# Contract: Hub verify

## Request

`POST {app.hub.base-url}{app.hub.verify-path}`

Default: `https://hub.ag3nts.org/verify`

`Content-Type: application/json`

```json
{
  "apikey": "<HUB_API_KEY>",
  "task": "savethem",
  "answer": ["vehicle_name", "right", "right", "up", "down", "up"]
}
```

| Field | Required | Notes |
|-------|----------|-------|
| apikey | yes | From `HUB_API_KEY`, never from the model |
| task | yes | From `app.hub.task`, default `savethem` |
| answer | yes | String array of discovered commands |

### Answer rules

- Possible commands and vehicle types come from discover, not from code.
- The player MAY start in a vehicle: vehicle type is the first command.
- Later commands are movement or disband-to-foot as discovered. Vehicle-to-vehicle
  change is not allowed.
- One attempt uses briefing resources (10 fuel, 10 food) unless discovery
  overrides.

## Response

Success: body contains `{FLG:...}`. Application copies that substring to
`GameSession.flag`, prints it, and stops the process.

Failure: error text. Application returns it to the model. The model MAY
discover more, then MUST submit a different `answer`.

Each HTTP attempt counts as one verify use, including failures.

## Errors

| Condition | Tool result | Budget |
|-----------|-------------|--------|
| Budget already 0 | Limit-reached; session stop | No extra decrement |
| HTTP / empty / unreadable / timeout / unreachable | Error text | Decrement |
| Missing, empty, or non-list `answer` | Local error to the model; no hub call | No decrement |
| Rejected trip | Error body | Decrement |
| Flag present | Body; `returnDirect` stop | Decrement |
