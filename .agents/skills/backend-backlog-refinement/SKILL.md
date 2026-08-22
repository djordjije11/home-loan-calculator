---
name: backend-backlog-refinement
description: Refine the open backend backlog — per-issue `Refinement Notes` comment + readiness label, cross-cutting decisions appended to `docs/agents-support/decision-candidates.md`, chat-only gap report. No code edits. Invoke when the user says "refine" or asks for a backlog pass.
---

# Backend Backlog Refinement

Run this skill to prepare the backend backlog for autonomous implementation. It looks at the **whole open backlog** in one pass — issues generated from specs *and* one-off issues — and leaves each ready issue self-sufficient for `backend-issue-work` to pick up from a cold context.

Invoke manually when there is new spec output, new issues from `to-issues`, or the backlog has drifted. Not a watcher, not a hook.

## Posture

- **Holistic, not per-issue.** A refinement run sees the backlog the way a sprint planning session does: ordering, gaps, cross-cutting decisions, recurring themes in `decision-candidates.md` that should be promoted.
- **Spec fidelity over issue wording.** Generated issues are lossy routing artifacts, not authority. If an issue narrows, weakens, or drops a committed Spec behavior, refinement must catch that drift before labels change.
- **E2E capability thinking.** For spec-driven work, reason in user-visible capabilities across repos and layers, not only backend classes. If a dealer story says they upload photos, the issue graph needs an owned upload path; metadata registration is not a substitute unless the Spec explicitly says another system owns byte upload.
- **Decide, don't sleepwalk.** On local judgement calls (helper name, internal struct shape, error code choice within an already-decided envelope) the skill decides and captures the *why* in the issue's `Refinement Notes` comment — local choices stay on the issue, not in `decision-candidates.md`. On cross-cutting or material decisions (new convention, API envelope, persistence shape across issues) the skill asks the user before deciding, then logs the outcome in `decision-candidates.md`.
- **Surface material judgement calls.** The agent may decide local details, but it must share important assumptions with the user during the refinement session before encoding them: scope reductions, ownership assumptions, missing companion work, spec/issue contradictions, e2e blockers, or decisions that make a product capability partial.
- **Routing labels are not consolation prizes.** If an issue is missing facts, route it to `needs-info` / `ready-for-human` with a concrete naming of the gap. Do not partially-refine and promote.

## Inputs

Read freely:

- Open issues in this repository, body + comments + labels + linked PRs.
- Linked Specs from the issues (via `github.com/smg-automotive/quantum-docs` per `docs/agents-support/external-sources.md`).
- `CONTEXT.md`, `docs/adr/`, `CODING_CONVENTIONS.md`.
- `docs/agents-support/harness-blueprint.md` when refining `[HARNESS]` issues — its layer model, ownership model, and value/occurrence brake govern mechanize-or-leave-to-review calls and each statement's authoritative home.
- `docs/agents-support/decision-candidates.md` (the candidates queue this skill curates).
- Sibling repos listed in `docs/agents-support/external-sources.md`, when a pattern lookup helps.

Do not run grilling for routine refinement — invoke `grill-with-docs` only for promotion (see below) or when the user explicitly asks for a durable terminology / ADR pass.

## Pass Shape

1. **Backlog scan.** Enumerate every open issue that is not `ready-for-agent` and not `wontfix`: includes `needs-triage`, `needs-info`, `ready-for-human`, and unlabeled issues. Group by spec (when linked) and by topic. Note out-of-spec issues separately.
2. **Spec fidelity scan.** For each linked Spec, build a compact capability map from user stories, implementation decisions, testing decisions, and out-of-scope text. Compare the issue set against that map. Look for dropped verbs, downgraded behavior, missing persistence/integration boundaries, and assumptions that another repo/issue owns part of the e2e flow.
3. **Per-issue refinement.** For each issue in scope: read body, prior comments, linked Spec, counterpart frontend/backend issues; identify decisions needed; decide locally or ask cross-cutting; check hard gates (below). For `needs-info` and `ready-for-human` issues, re-check whether the original blocker has been resolved before re-refining.
4. **Material-decision checkpoint.** Before writing labels/comments, tell the user about significant findings that would affect product scope, e2e completeness, repo ownership, or spec compatibility. Keep it brief and explicit; do not wait for the final gap report to reveal a material assumption.
5. **Write or update artifacts.** Refinement Notes comment per issue (minimal deltas only). Body edits for structural corrections. Append cross-cutting decisions to the decision-candidates file. Set the right readiness label.
6. **Gap detection.** Across the backlog: missing issues the spec implies (e.g. reference data, migrations, feature flags, upload/storage paths), ordering implications not yet expressed via `Depends on:`, parallelizable groups, blocked chains.
7. **Promotion scan.** Read `decision-candidates.md` end to end. Surface entries that appear in three or more issues or that read like project-wide conventions as **promotion candidates** in the gap report. Do not promote automatically.
8. **End-of-pass report.** Chat-only gap report (next section).

## Spec Fidelity And E2E Coverage

For every spec-driven issue group, refinement must answer these before promoting:

- **Capability preservation:** Does the issue graph preserve the Spec's user-visible capability, not just related backend nouns? Example smell: the Spec says users upload photos/documents, but the issue only records upload metadata.
- **Owned execution path:** For each user action, is there an issue owning the executable path end to end enough for implementation to work (frontend trigger, backend route, storage/integration boundary, persistence/read model), or is a missing owner named in the gap report?
- **Boundary accuracy:** If a capability is assigned to another repo/module/system, is that supported by the Spec or durable docs? Assumptions such as "Listing Publishing will handle this later" are material and must be surfaced.
- **Out-of-scope compatibility:** Does any issue use "out of scope" language to drop behavior that the Spec included? If so, route or correct it; do not silently compensate with a comment.
- **Counterpart consistency:** Read linked frontend/backend counterpart issues when available. If the frontend issue expects a capability that the backend issue no longer provides, or vice versa, flag the mismatch before marking either side ready.

A generated issue that degraded the Spec is not ready merely because the issue body is internally coherent. If the missing behavior is necessary for the issue's own user-visible workflow, correct the issue body or route the issue away from `ready-for-agent`. If the behavior belongs in a companion issue, that owner must already exist or be surfaced as a blocking backlog gap before promotion.

## Material Decision Checkpoint

During refinement, share important decisions or assumptions with the user before persisting them. This is not a full transcript; include only material items such as:

- A Spec capability is missing from the issue graph.
- An issue is being marked ready while a companion issue/repo must still implement part of the e2e flow.
- The refinement comment will override or narrow the issue body.
- The issue body conflicts with the Spec, counterpart issue, `CONTEXT.md`, ADRs, or current code.
- A repo boundary or ownership call is being made without an explicit Spec sentence.
- A user-facing API route, status model, storage strategy, or integration responsibility is being chosen.

If the user pushes back, stop mutating GitHub artifacts and reassess from the Spec. Do not auto-create "repair" issues for discovered gaps unless the user explicitly asks for issue creation; when asked to create them, derive the issue from the Spec/counterpart gap rather than from a repo-local implementation guess.

## Refinement Notes Comment

One comment per issue, edited in place across passes. Match by first line:

```
Refinement Notes
```

If an older comment with a different first-line header exists from a prior workflow, treat it as the same artifact: rewrite the heading to `Refinement Notes` and edit in place under the new structure rather than starting a fresh comment.

It **complements** the issue body. Omit any section that does not apply.

```
## Decisions               ← only deltas resolved during refinement
- <one-line decision> — <one-line why, link decision-candidates.md date when cross-cutting>
- ...

## Open questions          ← only if blocking and not yet resolved
- <question> — routed to `needs-info` / `ready-for-human`.

## Notes                   ← parallelizability, log pointers, non-obvious context
- Parallelizable with #N (was implied sequential by linked order).
- See `decision-candidates.md` 2026-05-26 — error envelope shape.
```

Footer:

```
_Generated by backend-backlog-refinement on <YYYY-MM-DD>._
```

Aim for 5–20 lines per comment. **Never restate the issue body.** Never carry full request/response JSON — a field list is enough.

## Editing the Issue Body

The body is the source of truth for *what to build*; the comment is the source of truth for *how we decided*. Edit the body when, and only when, it is **structurally wrong**:

- A linked issue is missing from the related-issues list (e.g. `to-issues` did not pick up a `Depends on:` that refinement determined).
- A linked issue is wrong and refinement removed it.
- An API path, method, or field name in the body is wrong against the Spec or `openapi.yaml`.
- A title is misleading.
- The generated issue degraded committed Spec behavior by omitting or weakening an in-scope user-visible capability.

Never edit the body to encode a decision (chosen approach, tradeoff). Decisions live in the comment + log.

## Ordering

`Depends on:` lines in the body + `to-issues`-generated linking are the source of truth. Refinement adds ordering information to the **comment** only when one of these holds:

1. Refinement changed the order (e.g. links imply A → B, refinement decided B can ship first behind a flag).
2. A dependency is not link-expressible — typically a shared cross-cutting decision captured in `decision-candidates.md` that both issues must respect. Cite the candidates entry date.
3. `Parallelizable with: #N` overrides what the existing links imply.

Otherwise the comment is silent on ordering.

## Hard Gates Before `ready-for-agent`

All must hold across **body + comment combined**:

1. If refinement resolved any gap or decision, the `Refinement Notes` comment captures it.
2. Spec-driven work preserves the linked Spec's relevant capability scope; any deliberate partial slice has explicit companion ownership or is reported as a gap.
3. If the issue touches HTTP, the API contract is complete across body + comment: route, method, auth/ownership, request field list, response field list, status codes.
4. Acceptance criteria — in body, comment, or both — list observable behavior the worker can test.
5. No conflicting facts across body, comment, Spec, and counterpart issues. Where the comment overrides the body, it says so explicitly.
6. Spec-driven feature work links the committed Spec.
7. E2E dependencies are named: if implementation needs another issue/repo/system to make the user workflow work, the dependency or gap is explicit before the label changes.

A blocker is allowed: a fully-specified issue may be `ready-for-agent` with a `Depends on:` line — the worker filters blocked issues itself.

Bug fixes and small maintenance issues may be `ready-for-agent` without a Spec when the issue and code provide enough context. The API Contract gate still applies if the bug touches HTTP.

## Readiness Labels

Existing canon. Set one canonical state at a time; remove the previous one.

- `needs-triage` — default for incoming generated issues; refinement not yet done.
- `needs-info` — waiting on reporter, Spec, or factual content. Comment names the missing item.
- `ready-for-agent` — passes the hard gates above.
- `ready-for-human` — a human decision or action is required before implementation is safe.
- `wontfix` — closed without doing.

## Candidates Appends

Append to `docs/agents-support/decision-candidates.md` when a refinement decision is cross-cutting (will affect more than one issue or sets a convention). Skip purely issue-local decisions — those live in the `Refinement Notes` comment only.

Entry format (newest first in the file):

```
## YYYY-MM-DD — <short topic>
<2–6 lines: what was decided and why>

Source: refinement
Seen in: #N, #N
```

When an existing entry applies to a new issue, append the issue number to `Seen in:` rather than writing a new entry. Citation form for external sources is `github.com/smg-automotive/<repo>/blob/main/<path>` — never local paths.

## Promotion Candidates

End each run by scanning `decision-candidates.md` for promotion candidates:

- Entries with three or more `Seen in:` references.
- Entries that read like a project-wide convention rather than a one-off (envelope shapes, layering rules, naming conventions).
- Entries that conflict with stale wording in `CONTEXT.md` or `docs/adr/`.

List them under `## Promotion candidates` in the gap report. **Do not promote automatically.** When the user picks one to promote, run `grill-with-docs` to write the ADR or update `CONTEXT.md`, then **delete the original entry from `decision-candidates.md`** — the durable doc becomes the source of truth.

## Gap Report (chat-only, end of run)

Shape:

```
## Backlog gaps
- <missing issue / migration / reference-data / feature flag>, implied by Spec <link> § <section>.
- <spec capability degraded by generated issue>, Spec <link> § <section>; affected issues: #N, #N.
- <missing e2e owner>, e.g. frontend trigger, backend route, storage/upload boundary, provider integration, or read-model exposure.

## Material decisions surfaced
- <decision/assumption shared during the run> — <where it was persisted or why it was routed>.
- ...

## Ordering nits
- #71 must merge before #72 (cross-cutting decision in decision-candidates.md 2026-05-26). Body link missing — added.
- #80 and #81 are parallelizable; existing link implied sequential.

## Promotion candidates
- 2026-05-26 — VIN draft: error envelope shape (Seen in: #71, #72, #74). Reads like a project-wide convention.

## Routed issues
- #82 → `ready-for-human`: needs a product decision on draft expiry.
- #84 → `needs-info`: Spec link is broken.

## Summary
- <N> issues touched, <N> promoted to ready-for-agent, <N> routed.
```

The gap report is **not persisted** — it is for the user to act on. Missing issues are not auto-created; the user files them or explicitly asks the agent to create them from the Spec-backed gap.

## When to Route, Not Promote

- Missing reporter or factual content → `needs-info`, comment naming the missing item.
- Required product decision missing, or sources conflict → `ready-for-human`, comment with the specific decision or contradiction.
- Issue is already in flight (open linked PR, active claim) → leave alone unless labels are misleading.

## Out Of Scope

- Code edits, branches, PRs.
- Running grilling for routine refinement (use only for promotion or on explicit user request).
- Auto-creating missing issues. The gap report surfaces them; the user files them.
- Writing to `CONTEXT.md` or `docs/adr/` directly — that is `grill-with-docs`'s job during promotion.
