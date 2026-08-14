# Formal Readiness Checklist: Skolwin Capture The Flag

**Purpose**: Author gate that the spec, plan, and contracts are complete, consistent, and measurable for hub/tool contracts, secrets/logging, and run lifecycle before `/speckit-tasks`
**Created**: 2026-08-15
**Feature**: [spec.md](../spec.md)

**Note**: This checklist tests requirement quality, not implementation. Mark an item `[x]` only when the written docs already answer it unambiguously.

**Review 2026-08-15**: 50/50 items closed after readiness-gate decisions were written into the spec and contracts.

## Requirement Completeness

- [x] CHK001 Are hub discover request fields, method, and path rules specified in the spec as well as in the plan contract (first path vs later paths, body has query + injected key only)? [Completeness, Spec §FR-004, Plan contracts/hub-discover.md]
- [x] CHK002 Is the first-discover path rule (`toolsearch` before any other discover path) stated as a requirement in the spec, not only in the plan? [Gap, Plan contracts/hub-discover.md]
- [x] CHK003 Are verify request fields specified as requirements: injected key, fixed task name, `answer` as an ordered command list with optional leading vehicle? [Completeness, Spec §FR-014, Plan contracts/hub-verify.md]
- [x] CHK004 Is the verify task identifier documented in the spec (or explicitly declared plan-only configuration)? [Gap, Plan contracts/hub-verify.md]
- [x] CHK005 Are both required secrets named in the spec (`OPENROUTER_API_KEY` and `HUB_API_KEY`), including that they are env/uncommitted only? [Gap, Spec §FR-021]
- [x] CHK006 Are file-log requirements specified (what is logged, redaction, that logs MUST NOT contain secrets) in the spec, or explicitly scoped to the plan only? [Gap, Spec §FR-018, Plan logging]
- [x] CHK007 Are process exit statuses specified in the spec for flag, limit/planning failure, and missing secrets? [Gap, Plan contracts/operator-console.md]
- [x] CHK008 Is it specified that after a stop condition (flag or either limit) no further discover or verify request may be sent, including in-flight planner loops? [Completeness, Spec §FR-007, §FR-015, §FR-017, §SC-004]
- [x] CHK009 Are HTTP timeout, retry, and unreachable-hub behaviors specified, or explicitly excluded as out of scope? [Gap, Spec Edge Cases]
- [x] CHK010 Is English-only discovery specified as reject, rewrite, or other handling when the planner would send a non-English query? [Completeness, Spec §FR-005, User Story 2 scenario 4]

## Requirement Clarity

- [x] CHK011 Is "allowed commands" defined so an author can tell when FR-023's first-verify gate is met (what in a discover result counts as a command list)? [Clarity, Spec §FR-023, Clarifications]
- [x] CHK012 Is "one legal path" defined with enough detail to decide the first verify is allowed (connected tiles vs any mention of Skolwin)? [Clarity, Spec §FR-023]
- [x] CHK013 Is "trip-check outcome" in operator messages specified (flag / error class / short error text) versus forbidden full hub dumps? [Clarity, Spec §FR-019, §SC-007]
- [x] CHK014 Is "shut down immediately" defined as process exit with no additional tool uses, including leftover budget on the other tool? [Clarity, Spec §SC-004, Edge Cases]
- [x] CHK015 Is "failed use" for empty or unreadable responses specified as consuming exactly one discover or verify unit? [Clarity, Spec Edge Cases, Plan data-model ToolBudget]
- [x] CHK016 Is "hide secrets" / "MUST NOT reveal secrets" defined as covering stdout, stderr, and file logs, including `apikey` JSON fields? [Clarity, Spec §FR-018, §FR-019, §FR-021]
- [x] CHK017 Is the discover `path` argument specified as ignored on the first call and required/optional on later calls? [Clarity, Plan contracts/llm-tools.md]

## Requirement Consistency

- [x] CHK018 Does Clarifications Q2 (first verify without mentioning commands) match FR-023 and User Story 1 (commands required), or is Q2 marked superseded? [Conflict, Spec Clarifications vs §FR-023]
- [x] CHK019 Are discover/verify limits specified as fixed 30/10 in the spec while also parametrized in the plan, and is the spec's normative default vs override rule stated? [Conflict, Spec §FR-007, §FR-017, Plan AppProperties]
- [x] CHK020 Do FR-003 (commands unknown at start) and FR-024 (briefing defaults) agree that command lists and map contents are never briefing defaults? [Consistency, Spec §FR-003, §FR-024, Assumptions]
- [x] CHK021 Do FR-013 (disband and continue on foot) and the assumption "any mix of discovered vehicles" agree on whether vehicle-to-vehicle change without walking is allowed? [Conflict, Spec §FR-013, Assumptions]
- [x] CHK022 Do FR-015 / constitution (flag to stdout, failures to stderr) and FR-019 (session messages) assign each message type to stdout vs stderr without overlap? [Consistency, Spec §FR-015, §FR-018, §FR-019]
- [x] CHK023 Does the spec's "discovery/verification service" language match the plan's two named tools and hub URLs, or is a glossary mapping required? [Consistency, Spec §FR-004, §FR-014, Plan contracts/llm-tools.md]

## Acceptance Criteria Quality

- [x] CHK024 Can FR-007 and FR-017 be accepted by counting discover and verify requests in one run against the documented limits (including failed HTTP)? [Measurability, Spec §FR-007, §FR-017, §SC-002]
- [x] CHK025 Can FR-023 be accepted with a yes/no checklist of the five minimum facts before the first verify (location, path, travel method, costs, commands)? [Measurability, Spec §FR-023]
- [x] CHK026 Can FR-019 / SC-007 be accepted by reconstructing every discover query, every verify outcome, and remaining uses from operator messages alone? [Measurability, Spec §FR-019, §SC-007]
- [x] CHK027 Can FR-015 / SC-001 be accepted by presence of a `{FLG:...}` substring on stdout and process end with no further tool uses? [Measurability, Spec §FR-015, §SC-001]
- [x] CHK028 Can FR-021 missing-secrets startup be accepted as: no hub calls, no planner-model calls, failure status, no secret values in output? [Measurability, Spec §FR-021]
- [x] CHK029 Can SC-003 be accepted by comparing consecutive verify `answer` lists for inequality plus a documented link to the prior error? [Measurability, Spec §SC-003, §FR-016]
- [x] CHK030 Can SC-006 be accepted only via successful verify (flag implies resources remained), and is that indirection stated so it is not an untestable resource meter? [Measurability, Spec §SC-006, §FR-015]

## Scenario Coverage

- [x] CHK031 Are primary-flow requirements complete for: start → forced first discover → minimum-set first verify → flag → stop? [Coverage, Spec User Story 1, Plan contracts/hub-discover.md]
- [x] CHK032 Are alternate-flow requirements complete for: incomplete three-match discover → different query → later verify? [Coverage, Spec User Story 2, §FR-006, §SC-005]
- [x] CHK033 Are recovery-flow requirements complete for: verify error → optional more discover → different trip → success or verify limit? [Coverage, Spec User Story 3, §FR-016]
- [x] CHK034 Are exception-flow requirements complete for: missing secrets, empty/unreadable hub body, discover limit with verify remaining, verify limit with discover remaining? [Coverage, Spec Edge Cases, §FR-007, §FR-017, §FR-021]
- [x] CHK035 Are non-functional requirements complete for operator observability (console vs file) and secret protection, or are file traces explicitly extra-spec? [Coverage, Spec §FR-019, Plan logging]

## Edge Case Coverage

- [x] CHK036 Is a `{FLG:...}` string appearing on a discover response specified (treat as flag and stop, ignore, or impossible)? [Gap, Plan data-model DiscoverResponse]
- [x] CHK037 Are zero-match discover results specified as rephrase-and-retry unless the discover limit is reached? [Coverage, Spec Edge Cases]
- [x] CHK038 Is model-supplied discover path before toolsearch specified as ignored (application forces the first path)? [Coverage, Plan contracts/llm-tools.md]
- [x] CHK039 Is verify with an empty or non-array `answer` specified as a consumed failed use or a rejected tool input that does not call the hub? [Gap, Plan contracts/hub-verify.md]
- [x] CHK040 Are leftover uses after flag specified as unused (no extra discover or verify)? [Coverage, Spec Edge Cases, §SC-004]

## Non-Functional Requirements

- [x] CHK041 Are secret-redaction requirements specified for every output channel (stdout, stderr, file logs, tool-parameter logs, model-prompt logs)? [Completeness, Spec §FR-018, Plan contracts/llm-tools.md]
- [x] CHK042 Is a run-duration or planner-turn cap specified, or explicitly omitted in favor of tool-budget stop only? [Gap, Spec Success Criteria]
- [x] CHK043 Are logging requirements for model system prompt, tool definitions, user prompt, and model response specified as mandatory, including redaction? [Completeness, Plan logging vs Spec §FR-019]

## Dependencies & Assumptions

- [x] CHK044 Is hub availability (discover + verify) documented as a required external dependency with no offline fallback? [Dependency, Spec Assumptions]
- [x] CHK045 Is the OpenRouter/planner connection documented as a required dependency whose missing key is a startup failure, not a tool-budget use? [Dependency, Spec §FR-021]
- [x] CHK046 Are briefing defaults listed as assumptions that discovery may override, with command lists and tile contents excluded from that list? [Assumption, Spec §FR-024, Assumptions]
- [x] CHK047 Is "each verify attempt resets 10 fuel and 10 food" stated as an assumption unless discovery overrides? [Assumption, Spec §FR-010]

## Ambiguities & Conflicts

- [x] CHK048 Does any remaining `[NEEDS CLARIFICATION]` or TODO remain in spec, plan, or contracts? [Ambiguity]
- [x] CHK049 If plan-only rules (first discover path, task name, exit codes, file logs, parametrized limits) stay out of the spec, is that exclusion written so `/speckit-tasks` does not treat them as optional? [Conflict, Spec vs Plan]
- [x] CHK050 Is FR-022 (no general chat) consistent with a single autonomous planner conversation that only uses the two game tools? [Consistency, Spec §FR-022, Plan Summary]

## Notes

- Check items off as completed: `[x]`
- Closed from docs on 2026-08-15: CHK003, 006, 008, 010, 013–017, 020, 022–029, 031–035, 037–038, 040–041, 043–048, 050
- Closed after readiness-gate answers: CHK001, 002, 004, 005, 007, 009, 011, 012, 018, 019, 021, 030, 036, 039, 042, 049
- Related: [requirements.md](./requirements.md) (generic spec-quality pass); this list is the formal cross-cutting gate
