# Contract: Operator console

No command-line parameters. Launch is a bare Maven / JAR start.

## Standard output

After each discover or verify action, print:

- the discovery question, or that a trip check was submitted
- the check outcome (flag found / error summary, not the full hub dump)
- remaining discover uses and remaining verify uses

At the end, print `{FLG:...}` when present, or a plain-language stop reason
when the run ends without a flag (discover limit, verify limit, or planning
ended). Progress, remaining uses, the flag, and that stop reason all go to
stdout (constitution Principle IV).

## Standard error

Startup failures (missing secrets, invalid blank config) and unexpected
errors. No secrets. Limit-reached and planning-ended stop reasons are not
stderr; they are stdout.

## Exit codes

| Code | Meaning |
|------|---------|
| 0 | Flag acquired |
| 1 | Discover or verify limit reached, or planning ended without a flag |
| 2 | Missing `OPENROUTER_API_KEY` or `HUB_API_KEY` (or invalid blank config) |

## Files

Logback also writes the same redacted traces to `logs/s03e05.log`.
