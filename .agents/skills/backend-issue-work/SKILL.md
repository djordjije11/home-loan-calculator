---
name: backend-issue-work
description: Implementation-lane skill for exactly one assigned backend issue or explicit backend code change. Reads the issue body and Refinement Notes, routes underspecified work back to refinement, implements production code and tests, runs targeted validation, and hands off to backend-pr-publish. Use when the user targets a specific issue, or when the autonomous delivery runner dispatches an implement turn. Do not use for coordination, independent review, PR follow-up, CI repair, or multi-issue selection.
---

# Backend Issue Implementation Lane

Use this skill inside an implementation lane for one assigned backend issue. It owns implementation through PR creation, but it does not coordinate separate
review threads, monitor multiple PRs, or resolve post-review follow-up after the PR is open. If the issue is under-specified, **stop and route back to
refinement** rather than guessing.

## First Moves

1. Read the issue: body, labels, linked PRs, blockers, and the `Refinement Notes` comment. **The issue body is the primary task description.** The Refinement
   Notes comment carries the deltas resolved during refinement — clarifications, decisions, and overrides — and wins on any topic where it contradicts the body.
   Do not use linked specs, docs, or counterpart issues to reconstruct missing task context — that is refinement's job. You may read linked specs to verify
   contradictions or confirm details already cited by the issue/comment. Check linked PRs only to avoid duplicate or in-flight work.
2. Read `AGENTS.md` and `CODING_CONVENTIONS.md`.
3. Read `.agents/shared/references/backend-workflow-spine.md` and load only the segment references for the execution path you will touch.
4. Read root `CONTEXT.md`. Title-scan `docs/adr/*.md` filenames and open the bodies of ADRs whose title topic appears in the diff, the issue body, or the
   `Refinement Notes` comment. Consult `docs/agents-support/decision-candidates.md` **only on demand**: when the `Refinement Notes` comment cites an entry by
   date, or when you are about to append and want to check for a duplicate near the same topic. Do not scan the whole file. If a cited entry is missing, treat
   it as promoted — the durable record lives in `docs/adr/` or `CONTEXT.md`.
5. Inspect the touched package and neighbors before choosing a pattern. When local patterns are thin or ambiguous, consult
   `docs/agents-support/external-sources.md` and read the relevant sibling repo read-only.

For an explicit chat task (no issue), skip step 1; inspect local code and clarify only when the expected observable behavior or owning boundary is genuinely
ambiguous.

Prefer documented repository conventions over prototype or scaffold code when they conflict.

## Stop and Route

Before editing, stop if any of these holds. Remove `ready-for-agent`, add the routing label, and leave one comment naming the exact gap. Then end the run.

- Issue touches HTTP but the API contract is missing or incomplete across body + comment (route, method, auth, request fields, response fields, status codes) →
  `needs-info`. Comment must start with `Refinement gap:` so refinement can resume on the specific gap without redoing the full pass.
- Acceptance criteria are missing from both body and comment → `needs-triage`. Comment with the missing item.
- A required product decision is missing → `ready-for-human`. Comment with the specific decision.
- External factual content is missing (reporter info, Spec section) → `needs-info`. Comment with the concrete item.
- Sources conflict between body, comment, Spec, related issue, or code, and the comment does not say which wins → `ready-for-human`. Comment with the
  contradiction.

Do not derive public API contracts from neighboring code or the Spec alone. If the contract is not present in the body and not added by the Refinement Notes
comment, route back.

## Picking An Issue

Use this section only for an explicitly requested single-thread/fallback implementation run that is not given a specific issue. Normal issue selection belongs
to the autonomous delivery runner (`agent-host/run-delivery.sh`).

- From open `ready-for-agent` issues in this repository with no open linked PR, active claim comment, or unresolved blocker, pick the lowest-numbered.
- Skip issues still labeled `needs-triage`, `needs-info`, `ready-for-human`, or `wontfix`.
- Do not work on more than one issue per run.
- Before editing, recheck for an active claim. Then leave: `Agent run started on branch <engine>/<issue-number>-<short-slug>.` Use the invoking engine's prefix:
  `codex` for Codex Desktop, `claude` for Claude Code, future engines pick their own short slug. Create or use that branch.
- Run in a dedicated worktree at that branch, not in the main checkout. Use `git worktree add <path> -b <engine>/<issue-number>-<short-slug> main` for a new
  branch, or `git worktree add <path> <branch>` to reuse an existing branch. This keeps parallel runs isolated and matches the working-tree expectation of
  `backend-pr-publish` and `backend-pr-followup`.

If no suitable issue exists, report that no executable `ready-for-agent` issue is available.

For an explicit chat task without an issue, follow the same worktree rule: pick a short slug (`<engine>/<short-slug>`) and run in a dedicated worktree.

## Execution Path Loading

Follow `.agents/shared/references/backend-workflow-spine.md` for the segment reference map and execution-path tracing. Load references for every touched
segment; for end-to-end work, compose them rather than stopping at the first adapter.

Use contract-first only as a REST/API strategy when the goal is to unblock a frontend or external consumer before behavior exists. Do not invent contract-first
modes for messaging or scheduling.

Optional implementation examples live under `.agents/shared/references/examples/`. Load only when neighboring code does not provide a clear pattern.

## Core Guardrails

- Inbound adapters: translation, validation, authorization, delegation. No business rules.
- Application services: orchestration. Business rules live in domain objects or domain services.
- Query flows: projection-oriented and consumer-driven.
- Use explicit Spring Data `Repository` interfaces, not generic `JpaRepository`, unless an existing local decision changes this.
- Use constructor injection, explicit imports, `ClockProvider` for current time, `@ConfigurationProperties` records for service config.
- Do not log tokens, credentials, secrets, or sensitive payloads.
- Do not add dependencies unless the issue requires them and the PR explains why.
- Implement production code and matching tests together at every touched segment.

## Decision Candidates Appends

Append to `docs/agents-support/decision-candidates.md` when implementation forces a **non-obvious decision** that future agents will need to know — typically
because the issue body and Refinement Notes did not pin it down, neighboring code is ambiguous, and the call you made will affect more than this one place.

Skip purely local choices (variable names, internal helper shape).

**Before appending, grep `decision-candidates.md` for the topic keyword.** If a `##` heading already covers the same topic, append your issue number to that
entry's `Seen in:` line instead of writing a new entry. Duplicate entries accumulate between refinement passes; check first.

Entry format (newest first):

```
## YYYY-MM-DD — <short topic>
<2–6 lines: what was decided and why>

Source: agent during #<issue-number>
Seen in: #<issue-number>
```

For chat-task work without a linked issue, use `Source: agent (chat task)` and either list `PR #<pr-number>` under `Seen in:` once a PR exists or omit the
`Seen in:` line until then.

If an existing entry covers the same decision, append your issue to `Seen in:` rather than writing a new one. Cite external references as
`github.com/smg-automotive/<repo>/blob/main/<path>`, never as a local path.

End the run with a one-line summary of any candidates entries you added so the user can skim them.

## Goal-Driven Execution

Before editing, state the success criterion for the touched behavior in one or two sentences (the observable thing that proves the change works — usually drawn
from acceptance criteria). For multi-step changes, briefly map each step to its verification (which test, which validation command, which manual probe). Loop
step → verify → next step rather than batching all changes and verifying at the end.

## Validation

Run targeted validation per `AGENTS.md`. Run the smallest relevant test class first; escalate to `./scripts/test-with-backing-services.sh` only when the touched
segment needs backing services. Hand off to `backend-pr-publish` for the full pre-PR validation gate.

## Handoff Checklist

Before handing off to `backend-pr-publish`, self-verify:

- [ ] Every acceptance criterion from the issue body has a matching test or observable behavior change.
- [ ] Production code and tests are implemented together at every touched segment.
- [ ] No `TODO`, debug log, or commented-out code left in production paths.
- [ ] Fixtures (Java, SQL, contract) are synchronized where the change touches them.
- [ ] Targeted validation passes locally; integration tests requiring backing services are noted if not run.
- [ ] Any non-obvious decision is captured per the "Decision Candidates Appends" rule above.

If anything is missing, finish it before handoff. Do not delegate gaps to delivery.

## Run Completion

An issue run is not complete until a PR is open against the branch. Local implementation, passing tests, and a clean working tree are **not** the terminal
state — the terminal state is a PR opened via `backend-pr-publish`. Do not end the run with "implementation finished, ready for review" or similar phrasing
while the PR does not yet exist.

The only skill-internal stop short of opening a PR is a Stop and Route condition above (label set, comment left, exit). Absent that, hand off to
`backend-pr-publish` as the final step of every run.
