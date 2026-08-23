---
name: backend-pr-deep-review
description: Opt-in adversarial drift hunt on backend PRs — Public API field shape, internal patterns, durable decisions, sibling-repo conventions, stacked-PR contracts; curates incorrect existing review comments. Invoke when the user explicitly says "deep review" or when the delivery coordinator has first told the user the concrete deep-review reason.
---

# Backend PR Deep Review

Independent, ruthless reviewer. Not a polite second opinion. The default question is *"is this change necessary, and is it the simplest shape we already know
how to build?"* — not *"does this implement the issue?"*.

Use this skill only on canonical or cross-repo-sensitive PRs when the user explicitly asks for deep review or the coordinator has already told the user the
concrete deep-review reason. The standard `backend-pr-review` skill is the default for routine PRs, including ordinary stacked PRs.

## Posture

- Adversarial but specific. Every finding cites either a **binding source** (an ADR, a `CONTEXT.md` definition, an openapi field, an internal reference, a
  sibling-repo file, a prior PR's contract) or **citeable working memory** (a `decision-candidates.md` entry). No vibes-only findings.
- Treat **existing internal patterns as the source of truth**, external docs as secondary. If a shared reference (such as
  `.agents/shared/references/backend-outbound-adapters.md` or `backend-integration-clients.md`) or a sibling repo already establishes a pattern, that supersedes
  prose from external documentation.
- **Entries in `docs/adr/` and `CONTEXT.md` are binding** — do not re-litigate; flag anything that contradicts them as a high-severity finding. Entries in
  `docs/agents-support/decision-candidates.md` are **citeable working memory, not binding**: the PR should either comply, explicitly cite why the entry is wrong
  (and propose updating or deleting it), or propose promotion to ADR/CONTEXT. Anything not yet captured in any of those is fair game to challenge from scratch.
- Bias toward subtraction. New abstractions, helpers, config knobs, and fields need to justify their existence against the issue/spec.
- One reply per thread when curating existing comments. No back-and-forth — if the original commenter pushes back, that's a human call.

## Drift Sources

This skill is structured around drift detection. Each drift source is loaded **only when the diff makes it relevant** — most PRs touch only one or two.

| Drift source                                                                                                            | Load when                                                                                                                                                                                                                                                                                                         |
|-------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `docs/agents-support/decision-candidates.md` — rolling cross-cutting decisions                                          | **TOC scan only on every run** (the `##` header lines). Open the body of a specific entry only when its topic visibly matches the diff, the `Refinement Notes` comment cites it, or an existing PR comment references it. Do not grep the body of every entry.                                                    |
| `docs/adr/` and `CONTEXT.md` — durable decisions and domain vocabulary                                                  | Title-scan `docs/adr/*.md` filenames first; open the body of ADRs whose title topic appears in the diff, the linked issue, or `Refinement Notes`. Read `CONTEXT.md` when the diff touches domain term boundaries (architecture, error envelopes, persistence rules).                                              |
| Linked issue (body + `Refinement Notes` comment), linked spec                                                           | Always.                                                                                                                                                                                                                                                                                                           |
| `.agents/shared/references/backend-review-rubric.md`                                                                    | Always.                                                                                                                                                                                                                                                                                                           |
| `.agents/shared/references/backend-outbound-adapters.md`                                                                | Diff adds or changes an outbound port, a provider or in-process adapter, or lets module code reach an external system or another module.                                                                                                                                                                          |
| `.agents/shared/references/backend-integration-clients.md`                                                              | Diff adds or changes an external/internal-service client, OAuth/m2m flow, retry/timeout config, or transport boilerplate.                                                                                                                                                                                         |
| `djordjije11-api-specs/openapi.yaml` (via `docs/agents-support/external-sources.md`)                                    | Diff touches Public-API-adjacent surface — controllers, DTOs, contract fixtures, or fields whose concepts overlap the Public API (vehicle, make, model, version, category, VIN, fuel, transmission, body, equipment, etc.). Skip for pure persistence, internal helpers, test scaffolding, config, and refactors. |
| Sibling repo from `docs/agents-support/external-sources.md` (listing-service, catalogue-service, auction-service, etc.) | Diff introduces a new feature-module shape, package layout, controller/service/repo naming pattern, or canonical test layout. Pick the sibling repo whose surface most overlaps the PR's topic. Read via local hint if present, otherwise `gh`; cite as `github.com/smg-automotive/<repo>/blob/main/<path>`.      |
| `CODING_CONVENTIONS.md`                                                                                                 | A suspected finding needs a written-policy backstop.                                                                                                                                                                                                                                                              |
| Prior merged PRs in the same stacked-PR series                                                                          | The current PR is part of a stacked series and the diff touches a contract, error shape, or field semantics an earlier slice established.                                                                                                                                                                         |

If none of these are relevant, this is probably the wrong skill — use `backend-pr-review` instead.

## Drift Types To Hunt

Findings should map to one of these. If a finding doesn't, question whether it's actually a finding or just preference.

1. **Public API field/value drift.** Routes can diverge — Quantum owns Vehicle vocabulary. Field names, enum values, types, optionality, date formats should
   match `openapi.yaml` where concepts overlap. Every intentional divergence should be captured in `docs/adr/` or `docs/agents-support/decision-candidates.md`;
   flag any divergence that isn't.
2. **Internal-pattern drift.** External-API calls that don't use the documented outbound-adapter and integration-client pattern (port in `domain`, adapter in
   `infra`, client under `io.github.djordjije11.integration`; auth, HTTP client shape, error mapping, retries). The agent reading external docs is exactly when
   this gets missed.
3. **Unnecessary scope.** New abstractions, helpers, config knobs, DTO fields, or layers not required by the issue/spec/durable decisions. *"Could three lines
   have done what this 200-line subsystem does?"*
4. **Convention drift vs sibling repos.** Package shape, controller/service/repo naming, integration-test layout. Especially on canonical first-of-kind features
   that future PRs will copy.
5. **Stack-wide invariant drift.** Project-level rules captured in `docs/adr/` and `CONTEXT.md` (e.g. *Vehicle ≠ Listing*, additive evolution of existing
   routes, contract-coverage parity across slices). A later slice quietly breaking an invariant the earlier slices respected is a high-severity finding.
6. **Implementation-vs-prior-slice drift.** In stacked-PR series: a later PR changing the contract, error shape, validation, or field semantics an earlier
   merged PR established. Easy to miss because each PR looks fine in isolation.
7. **Missing or weakened proof.** Contract or controller integration tests skipped on Public-API-visible changes; `WebMvcTest` or security-only tests
   substituted for controller integration coverage; mocks used where the rubric requires real integration.

## Curating Existing Review Comments

Read all existing PR comments (Copilot, Codex, humans) **before** reviewing the diff. The goal is to short-circuit incorrect suggestions before the implementer
agent dutifully addresses them.

Reply to an existing comment **only** when one of these is true:

- It conflicts with an entry in `docs/adr/`, `CONTEXT.md`, or `docs/agents-support/decision-candidates.md`.
- It would cause Public-API field/value drift away from `openapi.yaml` (when loaded).
- It would cause internal-pattern drift away from `backend-outbound-adapters.md`, `backend-integration-clients.md`, or an established sibling-repo pattern.
- It is technically wrong — high confidence, not "I'd do it differently."
- It duplicates a thread already resolved on this PR, or a finding raised at a better anchor.

**Do not reply** for style preferences, judgment calls, "could also be done as X" suggestions, or anything where the original commenter's choice is defensible.

Reply form: one short comment, lead with the binding or citeable reference (quoted ADR / CONTEXT / `decision-candidates.md` entry, openapi path, internal
reference filename and section, sibling-repo file). No threaded back-and-forth.

If a finding I would otherwise raise as a top-level comment is already raised by an existing reviewer comment, do not duplicate it — acknowledge the existing
thread in the review summary instead. When acknowledging, name the reviewer ("Copilot + Codex P1 — ...") and add the *binding* reference if they only described
the symptom (e.g. they flagged a race condition; you add that the unique constraint is at `v1.xml:109` and that resilience4j is configured to retry but the
service isn't `@Retryable`). That elevates a symptom report into a binding finding without producing a duplicate.

## Input Order

1. PR title, body, diff, comments, review threads, CI status.
2. `docs/agents-support/decision-candidates.md` — TOC scan (just the `##` headers); open specific entries only when they visibly match the diff or are cited by
   an existing comment.
3. Linked issue (body + `Refinement Notes` comment) and spec.
4. `backend-review-rubric.md`.
5. Conditional drift-source loads (table above) — only the ones the diff actually triggers.
6. Existing PR comments — include the deep-review request comment, then decide which comments to curate before forming top-level findings.
7. The diff itself.

## Decision Candidates Are Read-Only Here

This skill never writes to `docs/agents-support/decision-candidates.md`. When the candidates file already contains an entry that the PR violates or confirms,
**cite the entry** (`see docs/agents-support/decision-candidates.md 2026-05-26 — error envelope shape`) in the PR comment. Adding new entries is the worker's
job during implementation, or `backend-pr-followup`'s during review-resolution.

## Publishing

When invoked with a PR number or URL, publish findings to the PR by default. Local-only or dry-run only when the user asks for it.

- `REQUEST_CHANGES` for blocking drift findings (Public-API drift, internal-pattern drift, scope creep that adds a new abstraction, broken durable decision,
  missing required proof).
- `COMMENT` for advisory findings, questions on Closed-Decision boundaries, or curation-only reviews.
- `APPROVE` only when the user explicitly asks.
- Inline comments for findings anchored to changed lines. For missing coverage, missing validation, architectural gaps, or stack-wide invariant findings, anchor
  on the most relevant line the diff does contain (e.g. the test class that should hold the missing case, or the entry point of the gap) — an inline comment
  creates the resolvable thread the followup lane works from.
- Top-level PR comment (`gh pr comment`) only when no diff hunk offers any sensible anchor, or for curation-summary remarks.
- Replies to existing comment threads for curation, per the rules above.
- Keep the review body a pure index: one line per finding (severity tag, `file:line`, short title), plus review scope and the CI note. Do not restate a
  finding's explanation, evidence, or required resolution there — that text belongs in the thread it anchors to.

### Publishing mechanics that bite

- **Self-authored PRs can't be `REQUEST_CHANGES`-ed.** GitHub rejects with `422 "Review Can not request changes on your own pull request"`. On solo-dev repos
  this is the common case. Fall back to `COMMENT` and lead each blocking finding with an explicit severity tag in the body (`🚨 [BLOCKING — ...]`) so the signal
  is in the prose, not the event type. Note the intent in the top-level body (e.g. *"Treat the BLOCKING findings below as request-changes equivalents."*).
- **Inline comments must anchor inside a diff hunk, not just on a line in the file.** GitHub returns `422 "Line could not be resolved"` otherwise. For modified
  files, run `gh pr diff <pr> --patch | grep -E "^(diff --git|@@)"` and only anchor on lines within `@@ -A,B +C,D @@` ranges (lines `C` through `C+D-1` in the
  new file). For new files (`@@ -0,0 +1,N @@`), every line is valid. If a finding is structurally about a line outside any hunk, raise it as a top-level review
  comment instead.
- **Reviews are submitted via `POST /repos/{owner}/{repo}/pulls/{pull_number}/reviews`** with `commit_id` (head SHA), `event`, `body`, and a `comments` array of
  `{path, line, body}`. Bundle everything into one review per PR — don't drip-feed inline comments via the separate review-comments endpoint, that produces
  "pending review" state and a worse reading experience.

## Setting Review-State Labels

After submitting the review, update PR labels per the "Setting Review-State Labels" section in `.agents/shared/references/backend-review-rubric.md`. That
section is the single source of truth shared with `backend-pr-review`.

When the deep review has zero blocking findings and marks the PR `review-passed`, set `last_deep_reviewed_sha: <current-head-sha>` in the PR's
`<!-- agent-state v1 -->` comment. Leave `last_reviewed_sha` untouched — that field is owned by `backend-pr-review`, the baseline review that always runs first
(deep review only runs on a head that has already passed baseline). Patch the comment with the **numeric** REST comment id (see `backend-pr-followup`'s "Agent
State Comment" section). If the state comment does not exist, create it using that format with `last_reviewed_sha: (none yet)` and
`last_deep_reviewed_sha: <current-head-sha>` — do not claim a baseline pass you did not perform. This prevents the autonomous review lane from re-running deep
review on the same commit.

## Output

1. Findings, severity-ordered, with file and line references and the binding source for each.
2. Curated existing-comment threads (which I replied to and why).
3. Open questions or assumptions worth surfacing — kept tight.
4. Short summary only after findings.

If there are zero actionable findings, say so once and stop. No padding, no "everything looks good" closers.

## Out Of Scope

- Code edits. This skill never edits.
- Running tests locally when CI is present and green; run only when CI is missing, stale, failing, or a finding hinges on observed behavior.
- Findings Checkstyle, the architecture governance suites, compilation, or Spring Cloud Contract will mechanically catch.
- Re-litigating entries in `docs/adr/` or `CONTEXT.md` (durable). `decision-candidates.md` entries are *citeable working memory*: flag if a PR ignores or
  casually overrides one without addressing it, but they may still be challenged with a binding reason (and propose updating, deleting, or promoting the entry).
- Polite-baseline coverage — that's `backend-pr-review`'s job, not this one.
