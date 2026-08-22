---
name: backend-pr-publish
description: Implementation-lane publish gate for a backend change — final validation, formatting, commit, push, and `gh pr create`. Use after `backend-issue-work`, or when the user asks to publish/open a PR for existing local implementation work. One-shot skill; terminates when the PR is opened.
---

# Backend PR Publish

Apply this skill to take a finished local change to an opened PR. The skill is one-shot: it ends at `gh pr create`. Review follow-ups, CI fixes, and merge live in `backend-pr-followup`.

## Working Tree

Run inside the worktree and branch created by `backend-issue-work` (`<engine>/<issue-number>-<short-slug>` or `<engine>/<short-slug>` for chat tasks). Do not publish from the main checkout. If no worktree was created (chat task started without one), stop and create one before editing or staging.

## Definition Of Done

Before opening the PR (or marking it ready for review), self-verify:

- [ ] All issue acceptance criteria have matching tests or observable behavior changes.
- [ ] Targeted validation passes locally; full validation (`./scripts/build.sh`) has run or its skip is explicitly documented (e.g. Docker unavailable) with the CI checks that must confirm.
- [ ] No `TODO`, debug log, or commented-out code left in production paths.
- [ ] Fixtures (Java, SQL, contract) are synchronized.
- [ ] PR description includes: linked issue with closing reference, validation commands and results, any residual CI risk.
- [ ] Any candidates entries added during implementation are summarized in the final chat turn.
- [ ] Branch is up to date with the base; no unrelated files staged.

If anything is missing, finish it before pushing. Do not open a PR with known DoD gaps.

## Validation

Use the Gradle wrapper and the commands listed in `AGENTS.md`. For final pre-PR validation, prefer `./scripts/build.sh` unless the change is narrowly scoped and a targeted run already exercises the touched segment. Do not avoid backing services with test-only auto-configuration exclusions or unrelated infrastructure mocks unless an existing local test pattern already does that for the same kind of endpoint. If full validation cannot run because Docker, credentials, or backing services are unavailable, state the blocker and which CI checks must confirm the change.

## Publish Sequence

1. Confirm the current worktree, branch, and changed files.
2. Run the relevant validation command.
3. Run the pre-PR local review gate and fix actionable findings before publishing.
4. Stage only intended files.
5. Commit with a focused message.
6. Push the current branch.
7. Open a ready-for-review PR by default. Use a draft PR only when the user explicitly asks for one or the work is intentionally incomplete.
8. If the linked issue has a comment containing a deep-review marker (`/deep-review`, `[deep-review]`, `requires`/`needs deep review`, or `deep review requested`/`required`), add a PR comment carrying the request forward: `/deep-review requested on linked issue #<issue-number>; carrying that request onto this PR.`
9. Preserve issue/spec links and validation results in the PR description.

## PR Preparation

- Confirm the change is focused on the issue and linked spec.
- Format and optimize imports for touched files before final validation.
- Confirm new files are tracked and fixtures are synchronized.
- Before opening the PR, perform a local backend review pass against the branch using the `backend-pr-review` checklist. This is a self-check gate, not a GitHub review submission.
- Fix actionable pre-PR review findings before pushing. Do not open the PR while required-test-coverage, security, contract, package-shape, or validation-result findings remain.
- Use a concise PR title that describes the change. Include a work-tracking key in square brackets when one exists.
- For issue work, link the original issue in the PR description with a closing reference:
  - `Closes #<issue-number>` for issues in this repository.
  - `Closes owner/repo#<issue-number>` for external issues.
- For explicit chat-task work without a GitHub issue, omit `Closes #...`, describe the request context briefly in the PR body, and include validation results. If the task needs durable product/spec context, stop and ask for an issue or spec instead of creating a contextless PR.
- Document commands and results in the PR description.
- State when CI must confirm Docker Compose, persistence, messaging, external API, caching, or startup behavior.
- Do not name specific agents or engines (Codex, Claude, etc.) in the PR description. The PR is engine-neutral; orchestration happens via labels, not prose instructions.

## Labels

This skill does not set review-state PR labels. Phase B (review) owns `review-passed` / `needs-changes`, and humans/orchestrators own `auto-merge`. A freshly published PR carries no agent-managed labels by default.

## Out Of Scope

- CI failure follow-up, addressing review comments, merge, post-merge cleanup. Those are `backend-pr-followup`.
- Updates to `CODING_CONVENTIONS.md`, `CONTEXT.md`, `docs/adr/`, or skills. Process-doc promotion runs through `backend-backlog-refinement` → `grill-with-docs`.

## Decision Candidates

This skill does not write to `docs/agents-support/decision-candidates.md`. Implementation-time entries are written by `backend-issue-work`; review-resolution entries are written by `backend-pr-followup`.
