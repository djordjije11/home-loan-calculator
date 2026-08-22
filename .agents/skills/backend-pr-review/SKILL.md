---
name: backend-pr-review
description: Independent review-lane skill for one backend PR. Supports full first-pass review and focused re-review after follow-up pushes; publishes actionable findings and review-state labels by default. Use when the user asks to review a PR directly, or when the autonomous review runner dispatches a review turn. Do not edit code or use implementation-thread reasoning as evidence.
---

# Backend PR Review

Use this skill for an independent backend PR review pass. Do not edit code.

## Review Modes

Use **full review** when:

- this is the first review pass for the PR,
- the PR number changed or a replacement PR was opened,
- the base branch changed,
- the prior review state is missing or inconsistent,
- the coordinator explicitly asks for full review,
- the follow-up range is large enough that focused review would be unreliable.

Use **focused re-review** after follow-up pushes on the same PR when the coordinator provides the current head and a prior reviewed SHA. In focused mode, read only:

- the PR title/body and compact status,
- the agent-state comment,
- unresolved review threads and threads changed since the prior reviewed SHA,
- the diff from the prior reviewed SHA to the current head,
- files touched by that diff,
- CI status for the current head.

In focused mode, reload the linked issue/spec/rubric only when the follow-up diff changes API contract, acceptance scope, auth/authz, persistence semantics, or domain behavior. You may use prior review results from this same review thread to avoid reloading the whole PR. Do not use implementation-thread notes or reasoning as evidence.

## Inputs

For full review, read:

- pull request title, body, diff, comments, and review threads,
- linked issue and issue comments (including the `Refinement Notes` comment when present),
- linked spec,
- `.agents/shared/references/backend-review-rubric.md`.

Consult `docs/agents-support/decision-candidates.md` **only on demand**: when the `Refinement Notes` comment or an existing PR comment cites an entry. Do not scan the file before forming findings — durable rules live in `docs/adr/` and `CONTEXT.md`.

Read `CODING_CONVENTIONS.md` only when the PR changes conventions or when a suspected finding needs confirmation against durable implementation policy.
Read `.github/copilot-instructions.md` or `.github/instructions/**/*.instructions.md` only when the PR changes Copilot instructions.

For segment-specific guidance, follow the segment reference map in `backend-review-rubric.md` and load only the references that match the changed path.

Use GitHub-hosted spec links. Sibling-repo lookups follow `docs/agents-support/external-sources.md` — local hint when present, otherwise `gh`. Cite as `github.com/smg-automotive/<repo>/blob/main/<path>` regardless of how you read it.

## Decision Candidates Are Read-Only Here

This skill never writes to `docs/agents-support/decision-candidates.md`. When the candidates file already contains an entry that the PR violates or confirms, **cite the entry** (`see docs/agents-support/decision-candidates.md 2026-05-26 — error envelope shape`) in the PR comment. Adding a new entry belongs to the worker (during implementation) or to `backend-pr-followup` (during review-resolution), not to the reviewer.

Start full review from the issue/spec and the review rubric, then review the diff against the expected execution path and package shape.
Do not treat existing probe/scaffold controllers as feature-module templates.
Do not run local tests by default when CI and PR validation are already present; run them only when they answer a specific review question or CI is missing, stale, or failing.

## Review Focus

Prioritize:

- auth/authz regressions and missing `@PreAuthorize`,
- sensitive data in logs, fixtures, config, or errors,
- validation bypasses or validation moved away from the owning layer,
- transaction or retry changes that could duplicate external operations,
- persistence query changes without integration coverage,
- OpenAPI-visible REST/API changes without contract and controller coverage,
- REST/API changes that do not follow the documented feature-module package and test shape,
- missing `<ControllerName>IntegrationTest` coverage for new or changed REST controllers,
- `WebMvcTest`, security-only tests, or contract tests being used as a substitute for controller integration coverage,
- weakened or overly mocked tests, missing or meaningless assertions (U1 is entirely review-owned), unfinished Mockito verification chains (C4 is entirely review-owned), and weakened or disabled test-governance checks,
- business logic in the wrong layer,
- missing validation results in the PR description.

Do not duplicate findings that Checkstyle, the architecture governance suites, compilation, or Spring Cloud Contract will mechanically catch.

## Publishing Review Feedback

When the user asks to review a GitHub PR by number or URL, publish actionable findings to the PR by default, unless the
user explicitly asks for a local-only or dry-run review.

- Treat GitHub as the handoff surface between reviewer and worker agents. Do not keep actionable PR feedback only in the Codex thread when a PR can be commented on.
- Use `REQUEST_CHANGES` when there are blocking correctness, security, contract, required-test-coverage, or review-only convention findings.
- Use `COMMENT` when findings are non-blocking, advisory, or need maintainer judgment.
- Use `APPROVE` only when the user explicitly asks to approve or the workflow explicitly expects approval.
- Post every blocking finding as an inline review comment so it creates a resolvable thread — the followup lane's work queue is unresolved threads, and a finding that lives only in the review body is invisible to it. Anchor on the changed line when one exists; for missing coverage, missing validation, or architectural gaps, anchor on the most relevant line the diff does contain (e.g. the test class that should hold the missing case, or the entry point of the gap).
- If no diff hunk offers any sensible anchor, post the finding as a top-level PR comment (`gh pr comment`) instead — never only in the review body.
- Keep the review body a pure index: one line per finding (severity tag, `file:line`, short title), plus review scope and the CI note. Do not restate a finding's explanation, evidence, or required resolution there — that text belongs in the thread it anchors to. Repeating it doubles what a reader and the followup lane have to reconcile, and the two copies drift.
- Still summarize the submitted review in the final chat response.

### Publishing mechanics that bite

- **Self-authored PRs can't be `REQUEST_CHANGES`-ed.** GitHub rejects with `422 "Review Can not request changes on your own pull request"`. On solo-dev repos this is the common case. Fall back to `COMMENT` and lead each blocking finding with an explicit severity tag in the body (`🚨 [BLOCKING — ...]`) so the signal is in the prose, not the event type. Note the intent in the top-level body (e.g. *"Treat the BLOCKING findings below as request-changes equivalents."*).
- **Inline comments must anchor inside a diff hunk, not just on a line in the file.** GitHub returns `422 "Line could not be resolved"` otherwise. For modified files, run `gh pr diff <pr> --patch | grep -E "^(diff --git|@@)"` and only anchor on lines within `@@ -A,B +C,D @@` ranges (lines `C` through `C+D-1` in the new file). For new files (`@@ -0,0 +1,N @@`), every line is valid. If a finding is structurally about a line outside any hunk, raise it as a top-level PR comment (`gh pr comment`) instead — not the review body.
- **Reviews are submitted via `POST /repos/{owner}/{repo}/pulls/{pull_number}/reviews`** with `commit_id` (head SHA), `event`, `body`, and a `comments` array of `{path, line, body}`. Bundle everything into one review per PR — don't drip-feed inline comments via the separate review-comments endpoint. Each drip-fed comment becomes its own empty-bodied review event, so one round of findings renders as a stutter of near-identical "reviewed" markers.

## Setting Review-State Labels

After submitting the review, update PR labels per the "Setting Review-State Labels" section in `.agents/shared/references/backend-review-rubric.md`. That section is the single source of truth shared with `backend-pr-deep-review`.

When a focused re-review finds no actionable issues and the current head is fully reviewed, update the agent-state comment's `last_reviewed_sha` to the current head SHA if the workflow owns that comment.

## Output

Use code-review style:

1. Findings first, ordered by severity, with file and line references when possible.
2. Open questions or assumptions.
3. Brief summary only after findings.

If there are no actionable findings, say so and mention any residual test or CI risk.

When invoked by a delivery coordinator, end with:

```text
HANDOFF:
- state: done|blocked
- pr: #<number>
- branch: <branch-or-none>
- base: <base>
- head: <sha>
- labels: <labels changed or observed>
- checks: green|red|pending|unknown
- validation: <local tests run, or "not run">
- unresolved: <count plus short anchors, or none>
- next: <one sentence>
```
