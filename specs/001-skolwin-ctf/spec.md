# Feature Specification: Skolwin Capture The Flag

**Feature Branch**: `001-skolwin-ctf`

**Created**: 2026-08-15

**Status**: Draft

**Input**: User description: "I'm building application that's solely purpose is to win capture the flag game. Game is to guide player to destination map tile - city Skolwin. Player can use any type of vehicle available, can continue by foot as well. Each type of movement uses fuel (except travelling by foot) and food. Map consist of different types of tiles which differ in fuel consumption or movement possibilities. At the beginning nothing is known. All rules, map, vehicles, and costs must be discovered. Application uses a discovery service (English queries, up to 3 results each, 30 uses) and a verification service (trip plan in, flag or error out, 10 uses). On verification error, inspect, revise the plan, and resubmit. Application shuts down immediately after the flag is acquired or any tool limit is reached."

## Clarifications

### Session 2026-08-15

- Q: After a trip check fails, may the planner ask more discovery questions before submitting the next trip? → A: Yes — after a failed check, the planner may ask more discovery questions, then submit a revised trip
- Q: When should the planner submit the first trip for verification? → A: After a minimum set: Skolwin's location, one legal path, at least one travel method, and resource costs for that path
- Q: May the planner use the briefing facts (10 by 10 map, 10 fuel, 10 food, destination named Skolwin, walking uses food but not fuel) before discovery confirms them? → A: Yes — use them as working defaults; discovery may override them
- Q: Must the planner learn how to write a trip plan from discovery before the first check? → A: Discovery must show the allowed commands before the first check; the plan may use only those commands
- Q: How much of the session must the operator see in the program's messages? → A: Each discovery question, each trip check outcome, remaining uses, and the final flag or stop reason

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Obtain The Flag By Reaching Skolwin (Priority: P1)

An operator starts the program once. The program, acting as the trip planner, begins with no map, no vehicle list, and no movement-cost table. It discovers until it has Skolwin's location, one legal path, at least one travel method, resource costs for that path, and the allowed commands for writing a trip. It then builds a trip that reaches Skolwin without exhausting fuel or food, submits that trip for verification, and prints the flag when verification succeeds.

**Why this priority**: Winning the game is the only product purpose. A run that can discover enough, submit a valid trip, and surface the flag is the minimum valuable outcome.

**Independent Test**: Start the program with required secrets already present in the environment. Observe that it proceeds without further operator input and either prints a flag of the form `{FLG:...}` or a clear failure reason, then exits.

**Acceptance Scenarios**:

1. **Given** the program has just started and knows nothing about the game world, **When** the planner has Skolwin's location, one legal path, at least one travel method, costs for that path, and the allowed commands, and then submits a trip that reaches Skolwin with remaining fuel and food, **Then** verification returns the flag and the program prints it and exits.
2. **Given** a successful flag result, **When** the program handles that result, **Then** it writes the flag to standard output and shuts down immediately without further discovery or verification.
3. **Given** the operator launches the program with no command-line arguments, **When** the run begins, **Then** the planner proceeds autonomously and does not wait for interactive input.
4. **Given** the session is running, **When** the planner sends a discovery question or submits a trip check, **Then** the operator sees that question or check outcome, the remaining uses, and later the flag or a stop reason, without needing the full service replies.

---

### User Story 2 - Discover The Unknown World Iteratively (Priority: P2)

The planner must invent English questions or keywords for the discovery service. Each answer returns at most three best matches, not the full knowledge set. The planner changes later questions based on gaps so it can learn the map to Skolwin, the map legend, allowed commands, vehicle types, and movement costs by tile and travel method.

**Why this priority**: Nothing required to plan the trip is known at start. Discovery is the only way to learn the world, and incomplete answers make naive single-question discovery fail.

**Independent Test**: Run discovery in isolation with a sequence of different English queries. Confirm each response has at most three results, later queries differ when prior results were incomplete, and the planner may stop discovery for the first trip check once it has Skolwin's location, one legal path, at least one travel method, costs for that path, and the allowed commands (or when the discovery limit is reached).

**Acceptance Scenarios**:

1. **Given** no prior knowledge, **When** the planner sends an English discovery query, **Then** it receives up to three matching results and records them.
2. **Given** a discovery answer that omits needed facts (for example only part of the map or only some vehicles), **When** the planner continues, **Then** it sends a different query rather than repeating the same one unchanged.
3. **Given** the planner has already used 30 discovery queries, **When** it would send another, **Then** the program shuts down immediately and reports that the discovery limit was reached.
4. **Given** a query not written in English, **When** discovery is considered, **Then** the planner does not send it; all discovery queries are English.
5. **Given** the planner already knows Skolwin's location, one legal path, at least one travel method, resource costs for that path, and the allowed commands, **When** it has not yet submitted a trip, **Then** it submits the first verification instead of waiting for a full 10 by 10 map or a complete vehicle catalog.

---

### User Story 3 - Revise Failed Trips Until Success Or Limit (Priority: P3)

When verification rejects a trip, the planner reads the error. It may ask more discovery questions to fill gaps the error revealed, then changes the plan (route, vehicle, walking, or resource pacing) and submits again. Faster travel spends more fuel; slower travel spends more food. The planner may abandon a vehicle and continue on foot. Each attempt starts with 10 fuel units and 10 food rations.

**Why this priority**: A first plan may fail. Recovery from verification errors is how the program still wins within the remaining checks.

**Independent Test**: Feed a rejected trip and its error into the planner. Confirm the next submitted plan is different and addresses the reported problem, and that a tenth failed check causes immediate shutdown.

**Acceptance Scenarios**:

1. **Given** verification returns an error (for example the trip never reaches Skolwin, or fuel or food runs out), **When** unused verification and discovery attempts remain, **Then** the planner inspects the error, may ask more discovery questions, changes the trip, and resubmits.
2. **Given** the same trip was just rejected, **When** the planner submits again, **Then** the new trip is not an unchanged copy of the rejected one.
3. **Given** 10 verification attempts have been used, **When** the flag has not been obtained, **Then** the program shuts down immediately and reports that the verification limit was reached.
4. **Given** a trip that still has a vehicle the planner no longer needs, **When** the planner revises the plan, **Then** it may disband that vehicle and continue the remaining legs on foot.

---

### Edge Cases

- Discovery returns no matches: the planner rephrases the query instead of treating the world as empty and quitting, unless the discovery limit is reached.
- Discovery returns three matches that are not the needed facts: the planner asks again with a different question or keywords.
- Verification reports the trip is invalid or resources are exhausted: the planner may ask more discovery questions, then revises route, speed, or travel method rather than resubmitting the same plan.
- The program reaches 30 discovery uses or 10 verification uses: it exits at once even if the other service still has remaining uses.
- The flag arrives on a verification response: the program prints it and exits without spending leftover discovery or verification uses.
- Required secrets are missing at start: the program stops immediately, reports the failure, and does not call game services.
- A service response is empty or unreadable: the program treats it as a failed use of that service, reports the problem, and either retries with a changed request (if uses remain) or exits if the limit is reached.
- A tile cannot be crossed by the current travel method (water, rock, or similar): the planner chooses another path or another travel method learned from discovery.
- Fuel would be spent on foot travel: the planner MUST NOT assume foot travel consumes fuel; foot travel consumes food only, unless discovery later states otherwise.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The program MUST be a console application that accepts no command-line arguments, flags, or positional parameters.
- **FR-002**: After start, the program MUST run a single autonomous session and MUST NOT wait for operator input to advance discovery, planning, or verification.
- **FR-003**: At session start the planner MUST treat map contents, the vehicle catalog, allowed commands, and detailed movement-cost tables as unknown. Those facts MUST NOT be hard-coded as authoritative. The planner MAY start from the briefing defaults in FR-024.
- **FR-004**: The planner MUST learn game rules only through the discovery service, including the path to Skolwin, map legend, possible commands, vehicle types, and movement costs by tile and travel method.
- **FR-005**: Every discovery request MUST be in English. The planner MUST invent the question or keywords from current knowledge gaps.
- **FR-006**: The program MUST treat each discovery response as at most three best matches, not a complete dump of game knowledge, and MUST issue further distinct queries when more facts are needed.
- **FR-007**: The program MUST NOT send more than 30 discovery requests in one run. Reaching 30 without the flag MUST shut the program down immediately.
- **FR-008**: The planner MUST produce a trip plan whose goal is to move the player to the city of Skolwin.
- **FR-023**: The planner MUST NOT submit the first verification until it has Skolwin's location, one legal path to that city, at least one travel method, resource costs for that path, and the allowed commands for writing a trip. It MUST NOT delay that first check solely to finish a full 10 by 10 map or a complete vehicle catalog.
- **FR-009**: A trip plan MUST account for available travel methods (any discovered vehicle and travel on foot), tile movement restrictions, and resource use. Every submitted trip MUST use only commands returned by discovery.
- **FR-010**: For each verification attempt the planner MUST assume the player starts with 10 fuel units and 10 food rations unless discovery later overrides those starting amounts.
- **FR-011**: The planner MUST treat vehicle movement as consuming both fuel and food, and foot travel as consuming food but not fuel, unless discovery later overrides that rule.
- **FR-012**: The planner MUST consider that faster travel costs more fuel and slower travel costs more food, and MUST pace the trip so neither resource reaches zero before Skolwin.
- **FR-013**: The planner MUST be able to disband a vehicle at any plan step and continue later steps on foot.
- **FR-014**: The program MUST submit trip plans only through the verification service.
- **FR-015**: When verification succeeds, the response contains the flag as a `{FLG:...}` string. The program MUST show that flag to the operator and shut down immediately.
- **FR-016**: When verification fails, the program MUST inspect the error. While unused discovery attempts remain, it MAY send further discovery requests to fill gaps the error revealed. It MUST then change the trip plan and resubmit while unused verification attempts remain.
- **FR-017**: The program MUST NOT send more than 10 verification requests in one run. Reaching 10 without the flag MUST shut the program down immediately.
- **FR-018**: On shutdown after a limit or unrecoverable error, the program MUST report a clear failure reason to the operator and MUST NOT reveal secrets.
- **FR-019**: The program's messages MUST include each discovery question sent, each trip-check outcome, the remaining discovery and verification uses after each of those actions, and the final flag or stop reason. Messages MUST hide secrets. Full discovery answers and full submitted trip text are not required in those messages.
- **FR-020**: The map the planner works from MUST be treated as 10 by 10 tiles and MAY include rivers, trees, rocks, and other terrain learned from discovery.
- **FR-021**: Required runtime secrets (planner access and any game-service credentials) MUST be read from the environment or an uncommitted local secrets file at startup. Missing secrets MUST stop the program immediately with a failure status.
- **FR-022**: Automated planning MUST exist only to invent discovery queries, interpret answers, draft trip plans, and revise plans after errors. The program MUST NOT offer a general chat experience.
- **FR-024**: The planner MAY use these briefing defaults before discovery confirms them, and MUST accept discovery overrides: the map is 10 by 10 tiles; each trip attempt starts with 10 fuel units and 10 food rations; the destination city is named Skolwin; walking consumes food but not fuel.

### Key Entities

- **Map**: A grid of tiles, 10 by 10 unless discovery overrides. Includes the destination city Skolwin and other terrain such as rivers, trees, and rocks. Tile contents are unknown until discovered.
- **Tile**: One map cell. Has a terrain type that affects whether a travel method is allowed and how much fuel or food a step costs.
- **Vehicle**: A discovered travel method with its own fuel use. May be abandoned so the player continues on foot.
- **Resources**: Fuel units and food rations available for one trip attempt. Default starting amounts are 10 and 10.
- **Trip Plan**: An ordered sequence of moves toward Skolwin, written only with commands returned by discovery, including travel method, optional vehicle disband steps, and implied resource spend.
- **Discovery Query**: An English question or keyword set sent to the discovery service. Returns up to three best matches.
- **Verification Result**: Either a flag (`{FLG:...}`) for a trip that reaches Skolwin with resources remaining, or an error describing why the trip failed.
- **Flag**: The game prize string returned only by a successful verification.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An operator can start the program and, without typing anything else, receive either the flag or a plain-language stop reason before the process ends.
- **SC-007**: After a finished run, the operator can recount every discovery question, every trip-check outcome, and the remaining uses at each of those steps from the program's messages alone.
- **SC-002**: A winning run obtains the flag after at most 30 discovery questions and at most 10 trip checks.
- **SC-003**: After a rejected trip, the next submitted trip differs from the rejected one, reflects the rejection reason, and may be informed by additional discovery questions asked after that rejection.
- **SC-004**: The process ends on the same turn the flag arrives or a usage limit is hit; no further discovery questions or trip checks occur after that moment.
- **SC-005**: When a discovery answer is incomplete, the planner asks a different question and can obtain facts that were missing from the earlier (at most three) matches.
- **SC-006**: In a winning trip, the player arrives at Skolwin with fuel and food still above zero.

## Assumptions

- The operator places required secrets in the environment or an uncommitted local secrets file before launch.
- Briefing defaults (10 by 10 map, 10 fuel, 10 food, destination named Skolwin, walking uses food but not fuel) may be used immediately and remain in force until discovery overrides them.
- Starting location, allowed commands, vehicle list, map legend, tile contents, and exact per-tile costs are available through discovery; they are not briefing defaults.
- A full map and a complete vehicle list are not required before the first trip check; the minimum set in FR-023 is enough, and that set includes the allowed commands.
- The planner must not invent trip-plan syntax; it may submit only commands discovery has returned.
- Each verification submission is one trip attempt with a fresh 10 fuel and 10 rations, unless discovery states otherwise.
- Discovery queries may be full questions or keywords; both are valid as long as they are English.
- The destination name is Skolwin and the prize is returned only by the verification service.
- The program does not need to keep state across separate process launches; each launch is a new one-shot run.
- Foot travel consumes food and not fuel unless discovery later gives a different rule.
- The planner may use any mix of discovered vehicles and walking, including disbanding a vehicle mid-trip.
- Game services other than discovery, verification, and the project language-model connection are out of scope.
- Interactive menus, command-line options, and a long-running server mode are out of scope.
- Operator messages do not need to reprint full discovery answers or the full trip text; questions, check outcomes, remaining uses, and the final result are enough.
