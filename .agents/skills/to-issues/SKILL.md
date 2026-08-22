---
name: to-issues
description: Break a committed Spec under `specs/` into repo-scoped GitHub implementation issues for subsequent triage. Use when user wants to create frontend/backend implementation tickets from an accepted Spec.
---

# To Issues

Break a committed Spec into repo-scoped implementation issues that are ready for triage.

Before publishing, read:

- `docs/agents/delivery-flow.md` for the Spec -> committed Spec -> issues -> PR flow.
- `docs/agents/issue-tracker.md` for where specs live and which GitHub repo receives each issue.
- `docs/agents/triage-labels.md` for the label strings to apply.
- `docs/agents/domain.md` for domain docs and ADR rules.

## Process

### 1. Gather committed Spec context

Work from a committed Spec under `specs/<domain>/<slice-id>-<feature>.md`.

If the user invokes this skill without a Spec path or GitHub URL, ask which committed Spec to use or find the obvious Spec from the conversation and verify it before continuing.

Before drafting issues, verify that the Spec:

- lives under `specs/`
- is committed in `quantum-docs`
- has a GitHub URL that can be included in every issue body

If the Spec is uncommitted, outside `specs/`, or only exists in conversation context, stop before issue drafting. Ask the user to commit the Spec first.

### 2. Explore the codebase (optional)

If you have not already explored the codebase, do so to understand the current state of the code. Issue titles and descriptions should use the project's domain glossary vocabulary, and respect ADRs in the area you're touching.

### 3. Draft repo-scoped issue sets

Break the Spec into repo-scoped issues that map to the intended ownership split.

Always apply `needs-triage` to issues produced by this skill. Do not apply `ready-for-agent`, `ready-for-human`, or `needs-info` from `to-issues`; subsequent triage skills or maintainers decide execution readiness and replace `needs-triage` later.

<repo-split-rules>
- Every implementation issue must include a dedicated `Spec` section that links to the source Spec in `smg-automotive/quantum-docs`.
- Publish every issue with `needs-triage`.
- Never publish issues with `ready-for-agent` from this skill.
- Create backend/service issues in `smg-automotive/quantum-service`.
- Create frontend/UI issues in `smg-automotive/quantum-web`.
- Do not create one implementation issue that requires one agent to change both repos.
- If frontend and backend work are both needed, create separate issues and link them.
- Backend/service issues usually define or update the API/OpenAPI contract that frontend issues consume.
- A frontend issue that depends on a new or changed API contract should be blocked by the relevant backend issue unless the contract is already stable in the Spec.
- If frontend work may need contract-first enablement for a new or changed generated API contract, include that as a hint in the issue body. Do not split backend contract/bootstrap work here; leave that decision to the later repo-specific triage skill.
- Each issue should be one repo, one bounded agent job, one PR, and one clear test signal.
- Prefer many thin repo-local issues over few broad issues.
- If the Spec references a prototype, carry the prototype URL and the in-scope screen list forward into the body of each frontend/UI issue that builds user-visible UI (the `Prototype` section below), so the implementer's prototype gate fires without digging into the Spec. Omit it for backend issues and for UI issues whose Spec references no prototype.
- Omit separate issues for end-to-end tests or final verification. The implementing agents must validate each individual issue instead.
</repo-split-rules>

#### Frontend/UI Issue Split Heuristics

Use these heuristics when drafting frontend/UI issues for `smg-automotive/quantum-web`.

<frontend-issue-split-heuristics>
For frontend/UI work, split by user-visible capability first, then use this dependency order as guidance:

1. Page/route foundation
2. Data seam
3. Read-only representation
4. Display interactions
5. Write actions

This is an ordering heuristic, not a closed taxonomy. Do not create exactly one issue per step by default, and do not reject a better split when the Spec exposes a clearer user-visible capability boundary.

Each frontend issue should be independently explainable, user-visible or intentionally foundational, and have its own verification signal.

For a new frontend surface, prefer an initial page/route foundation issue when it establishes navigation, route structure, page naming, section vocabulary, or placeholders that later issues will build into. Combine it with the first read-only slice only when the surface is small enough that a separate foundation PR would be mostly ceremony.

A page/route foundation issue should own route/page entry, route-builder/navigation wiring where applicable, page title, nav label, breadcrumbs/tabs if present, minimal layout regions with stable section names, loading/empty/blocked placeholders, prototype URL and in-scope screen references where applicable, and verification that the route renders. It should not own full data rendering, display interactions beyond basic navigation affordances, write behavior, or invented domain copy not pinned by the Spec.

Create standalone data-seam issues only when they unblock multiple later issues, establish a shared frontend data boundary, or intentionally ship an honest blocked shell. Otherwise, include data wiring in the smallest meaningful user-visible slice that needs it.

For large read-only surfaces, split by meaningful page area, domain concept, data source/contract shape, state family, or prototype section. Avoid low-level component splits unless the component group is also a visible product area.

Display interactions reveal, hide, select, preview, or navigate existing UI state: modal/menu/drawer, expand/collapse, show more, tabs, row selection, local step navigation, detail preview. They usually belong with the read-only area they reveal or manipulate. Split them separately only when they span multiple areas, are complex enough to verify independently, or are central to the flow.

Write actions mutate backend or durable app state: submit, save, create, update, delete, confirm, publish, upload, assign, persist step progress, backend validation rendering, pending/success/error states. They should usually be separate from read/display work because they involve side effects, contract certainty, and mutation-specific tests.

Do not split frontend issues by production component architecture during `to-issues`. Preserve prototype URL and in-scope screens/states where relevant; let implementation/refinement decide production component boundaries.
</frontend-issue-split-heuristics>

### 4. Quiz the user

Present the proposed breakdown as a numbered list. For each issue, show:

- **Title**: short descriptive name
- **Target repo**: `smg-automotive/quantum-service` or `smg-automotive/quantum-web`
- **Spec**: source Spec path and link
- **Triage label**: `needs-triage`
- **Blocked by**: which other issues (if any) must complete first
- **Linked counterpart**: related frontend/backend issue, if any
- **Contract impact**: whether this creates, changes, or consumes an API/OpenAPI contract
- **Contract-first enablement hint**: whether frontend may need early generated-contract availability before full backend behavior
- **User stories covered**: which user stories this addresses (if the source material has them)
- **Issue-specific acceptance criteria**: the checks that prove this bounded job is done

Ask the user:

- Does the granularity feel right? (too coarse / too fine)
- Are the dependency relationships correct?
- Are the frontend/backend split and target repos correct?
- Are contract-producing backend issues linked to contract-consuming frontend issues?
- Are the contract-first enablement hints accurate, without splitting the backend work at this stage?
- Should any issues be merged or split further?

Iterate until the user approves the breakdown.

### 5. Publish the issues to the issue tracker

For each approved issue, publish a new issue to the correct GitHub repo. Use the issue body template below and apply only the `needs-triage` label.

Publish issues in dependency order (blockers first) so you can reference real issue identifiers in the "Blocked by" and "Related issues" fields.

Every published implementation issue must contain a `Spec` section. Include both the repo-relative path and a GitHub URL to the source Spec in `smg-automotive/quantum-docs`. If the Spec is not committed yet, stop before issue creation and ask the user to commit it first.

Do not create or require a parent/tracking issue by default. The committed Spec is the parent artifact. Create a parent/tracking issue only when the user explicitly asks for GitHub-native progress tracking.

For split frontend/backend work:

1. Publish backend/service contract-producing issues first.
2. Publish frontend/UI contract-consuming issues after their backend issue exists.
3. Link related issues in issue bodies or comments using full GitHub references, e.g. `smg-automotive/quantum-service#123`.
4. After both sides exist, add a short cross-link comment to each issue if the relationship is important and not already obvious from the body.

Formal GitHub issue links are optional. Use them only when the repo setup supports the right relationship and the tool flow makes it convenient.

<issue-template>

## Spec

- Repo: `smg-automotive/quantum-docs`
- Path: `specs/<domain>/<slice-id>-<feature>.md`
- Link: GitHub URL to the Spec

## Target repo

`smg-automotive/quantum-service` or `smg-automotive/quantum-web`.

## Prototype

Include this section only for a frontend/UI issue whose Spec references a prototype; omit it entirely otherwise.

- URL: prototype URL carried forward from the Spec
- In-scope screens: the screen(s)/state(s) this issue builds
- Build the UI from this reference rather than from prose. The Spec carries the in-scope / out-of-scope classification; the frontend pipeline turns this into a component outline before implementation.

## What to build

A concise description of this repo-local implementation job. Describe externally visible behavior and boundaries, not a file-by-file task list.

Avoid specific file paths or code snippets — they go stale fast. Exception: if a prototype produced a snippet that encodes a decision more precisely than prose can (state machine, reducer, schema, type shape), inline it here and note briefly that it came from a prototype. Trim to the decision-rich parts — not a working demo, just the important bits.

## Contract

- Produces/changes/consumes API contract: yes/no
- Contract source: Spec section, OpenAPI fragment, backend issue, or existing API
- Related contract issue: GitHub reference, if applicable
- Contract-first enablement candidate: yes/no
- If yes: briefly explain why frontend may need early generated-contract availability. Do not split backend contract/bootstrap work in this issue; leave that to later triage.

## Acceptance criteria

Replace this section with acceptance criteria specific to this bounded implementation issue. Do not copy the entire Spec.

- [ ] Reference the relevant Spec section or user story.
- [ ] Verify the repo-local behavior this issue owns.
- [ ] Include the expected test, contract, or UI evidence where applicable.

## Related issues

- Frontend/backend counterpart: GitHub reference, if applicable
- Parent/tracking issue: GitHub reference, only if the user explicitly requested one

## Blocked by

- A reference to the blocking ticket (if any)

Or "None - can start immediately" if no blockers.

</issue-template>

If a parent/tracking issue exists because the user explicitly requested one, do not close or modify it unless the user asks.
