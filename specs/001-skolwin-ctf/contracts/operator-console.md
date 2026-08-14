# Contract: Operator console

No command-line parameters. Launch is a bare Maven / JAR start.

## Standard output

After each discover or verify action, print:

- the discovery question, or that a trip check was submitted
- the check outcome (flag found / error summary, not the full hub dump)
- remaining discover uses and remaining verify uses

At the end, print `{FLG:...}` when present.

## Standard error

Startup failures, limit-reached stop reasons, and unexpected errors. No
secrets.

## Exit codes

| Code | Meaning |
|------|---------|
| 0 | Flag acquired |
| 1 | Discover or verify limit reached, or planning ended without a flag |
| 2 | Missing `OPENROUTER_API_KEY` or `HUB_API_KEY` (or invalid blank config) |

## Files

Logback also writes the same redacted traces to `logs/s03e02.log`.
