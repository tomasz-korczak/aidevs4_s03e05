<!--
Sync Impact Report
- Version change: (none / template placeholders) → 1.0.0
- Modified principles:
  - [PRINCIPLE_1_NAME] → I. Console Application, No Parameters
  - [PRINCIPLE_2_NAME] → II. One-Shot Autonomous Execution
  - [PRINCIPLE_3_NAME] → III. OpenRouter LLM Access
  - [PRINCIPLE_4_NAME] → IV. Flag-Centric Outcome
  - [PRINCIPLE_5_NAME] → V. Minimal Scope
- Added sections:
  - Runtime Constraints (replaces [SECTION_2_NAME])
  - Secrets And Configuration (replaces [SECTION_3_NAME])
  - Governance (filled from [GOVERNANCE_RULES])
- Removed sections: none (template slots filled, HTML example comments removed)
- Follow-up TODOs: none
-->

# S03E05 CTF Constitution

## Core Principles

### I. Console Application, No Parameters
The application MUST be a console program. It MUST NOT accept command-line
arguments, flags, or positional parameters. Invocation MUST be a bare
executable or language-runtime entry point with no user-supplied argv
contract. Required runtime values (API key, model identifier, base URL)
MUST come from the environment or an uncommitted local secrets file, never
from the command line.

### II. One-Shot Autonomous Execution
The application MUST run as a single invocation. After start it MUST
execute a series of actions, inspect each result, and continue searching
for the flag until the flag is found or a defined termination condition
is reached (exhausted attempts, unrecoverable error, or explicit stop
rule). It MUST NOT require interactive user input to advance the action
loop. A one-time run is complete when the process exits.

### III. OpenRouter LLM Access
All language-model calls MUST use the OpenRouter API. The process MUST
fail fast with a non-zero exit if the OpenRouter API key is missing or
invalid at startup. LLM usage MUST serve the flag-search goal only. The
application MUST NOT expose a general-purpose chat interface or unused
model features.

### IV. Flag-Centric Outcome
This project is a capture-the-flag exercise. Discovery of the flag is the
success condition. The process MUST write progress and the final flag, or
a clear failure reason, to stdout. Errors MUST go to stderr. API keys,
tokens, and other secrets MUST NOT be printed or logged.

### V. Minimal Scope
The codebase MUST stay limited to what the one-shot flag-search loop
requires. No CLI framework, no unused services, and no general-purpose
agent platform. New complexity MUST be justified by a concrete need on
the flag-search path.

## Runtime Constraints

- The entry point MUST ignore and MUST NOT document command-line parameters.
- Configuration MUST be read once at startup from the environment or an
  uncommitted local secrets file.
- The run MUST be sequential enough to inspect each action result before
  deciding the next action.
- The process MUST exit after the flag is found or a termination condition
  is met. It MUST NOT remain as a long-running server or daemon.
- External I/O beyond OpenRouter and the exercise's own target endpoints
  MUST NOT be added without a constitution amendment.

## Secrets And Configuration

- OpenRouter credentials MUST NOT be committed to the repository.
- A committed `.env.example` or equivalent MAY list required variable
  names with empty or placeholder values only.
- Logs, stdout, and stderr MUST redact secrets.
- Model choice, if configurable, MUST be an environment value, not a
  command-line flag.

## Governance

This constitution supersedes informal practice and prior ad-hoc decisions.
Amendments MUST update this file, bump `CONSTITUTION_VERSION` using
semantic versioning, and refresh `LAST_AMENDED_DATE`.

- MAJOR: remove or redefine a principle, or change the no-parameter,
  one-shot, or OpenRouter constraints in a backward-incompatible way.
- MINOR: add a principle or section, or materially expand guidance.
- PATCH: clarifications, wording, and typo fixes.

Reviews and implementation plans MUST check compliance with every Core
Principle. Complexity that is not required for the flag-search loop MUST
be rejected or justified in the plan. Runtime development guidance lives
in feature specs and plans derived from this constitution.

**Version**: 1.0.0 | **Ratified**: 2026-08-15 | **Last Amended**: 2026-08-15
