---
name: backend-pr-followup
description: Work/follow-up lane skill for one open backend PR. Addresses review comments, CI failures, rebuttals, restacks, replacement PRs, and guarded merge cleanup when explicitly allowed. Use when the user asks to address PR feedback or CI, or when the autonomous delivery runner dispatches a follow-up turn. Do not start new issue implementation or perform independent review.
---

# Backend PR Followup

Apply this skill to drive a PR from "open with feedback" to "merged" — one review-cycle round per invocation. The skill never carries state from the implementer's session; everything it needs lives in the PR (diff, threads, CI status, labels).

## Working Tree

Always run in a fresh worktree at the PR's current HEAD, even if the branch already exists locally from earlier work. The PR is the source of truth; reusing stale local state is the most common way followup pushes drift.

```
git worktree add <path> <pr-branch>
git fetch origin <pr-branch> && git reset --hard origin/<pr-branch>
```

If the PR is from a fork, fetch the fork's branch into a local tracking ref before the worktree add.

## Stale-Branch Sweep

Before substantive work, list local branches whose remote counterpart is already merged into `main`. For each, if a worktree still exists, remove the worktree and delete the local branch. This is defense-in-depth: it catches branches merged via the GitHub UI or by a prior agent run that crashed before its own cleanup completed.

## Inputs

Read:

- PR title, body, current head SHA, CI status, labels, agent-state comment, unresolved review threads, review submissions (including their top-level bodies), and comments created since the last reviewed SHA when that can be determined. A `🚨 [BLOCKING — ...]` finding in a review body or top-level comment is actionable feedback equal to an unresolved thread, even when no thread exists — address or rebut it before clearing `needs-changes` (reply via a normal PR comment when there is no thread to answer in).
- The current PR diff only as needed to understand the unresolved/new feedback and failing checks.
- Linked issue (body + `Refinement Notes` comment) and linked spec only when the unresolved/new feedback or CI failure depends on acceptance scope, contract, auth/authz, persistence semantics, or domain behavior.
- `AGENTS.md`, `CODING_CONVENTIONS.md`, and the relevant segments of `.agents/shared/references/backend-workflow-spine.md` for any code path the threads target.
- Root `CONTEXT.md`. Title-scan `docs/adr/*.md`; open ADRs whose topic appears in the diff or in a thread.
- `docs/agents-support/decision-candidates.md` **only on demand** — when a thread cites an entry by date.

Do not read resolved review threads or all PR comments by default. Read resolved threads only when:

- an unresolved/new comment references them,
- the coordinator passes their URLs as part of the follow-up scope,
- the agent-state comment is inconsistent,
- a prior rebuttal may be invalidated by the current head,
- a human explicitly asks to revisit them.

## Agent State Comment

All cross-invocation state lives in a single PR comment owned by the agent skills. The first line is an HTML marker (invisible when rendered, used by agents to find the comment); the rest renders as a readable status panel humans can glance at.

Exact body format:

```
<!-- agent-state v1 -->
🤖 **Agent state** — managed by the delivery coordinator and the `backend-pr-followup`/reviewer skills. Do not edit; the agent will overwrite manual changes on the next run.

- cycle: <N>
- round: <N>
- escalated: <yes|no>
- last_reviewed_sha: <sha-or-(none yet)>
- last_deep_reviewed_sha: <sha-or-(none yet)>
```

Find it with one call:

```
gh pr view <pr> --json comments \
  --jq '.comments | map(select(.body | startswith("<!-- agent-state v1 -->"))) | last'
```

Parse the bullet lines with a simple `^- (\w+): (.+)$` regex. If missing, create the comment with defaults (`cycle: 1`, `round: 0`, `escalated: no`, `last_reviewed_sha: (none yet)`, `last_deep_reviewed_sha: (none yet)`) via `gh pr comment <pr> -b '<body>'`. If present, update it in place via `gh api -X PATCH /repos/{owner}/{repo}/issues/comments/<id> -f body='<new body>'` — where `<id>` is the **numeric** comment id from `gh api repos/{owner}/{repo}/issues/<pr>/comments` (the `id` that `gh pr view --json comments` returns is a GraphQL node id the REST endpoint rejects). Preserve the header line and any existing `last_deep_reviewed_sha` value across updates unless the current round pushes a new commit, in which case set it back to `(none yet)`. Never post additional state-tracking comments.

The delivery coordinator may add an optional `- dispatched: <lane>@<sha>` line to mark that it dispatched a review or follow-up lane. It is coordinator-managed: this skill need not preserve it — omitting it when you rewrite the comment at the end of a round correctly signals to the coordinator that the lane finished.

## Iteration Cap

On every invocation, read the state comment and the PR's current labels in one `gh pr view <pr> --json comments,labels` call.

1. **Cycle reset check.** If the state comment shows `escalated: yes` but `needs-human-review` is **not** in the current labels, the human cleared the escalation. Bump `cycle: N+1`, set `round: 0`, set `escalated: no`. Continue.
2. **Compute** `next_round` as the existing `round` value plus 1. Keep using the existing `round` field in the existing state comment format; do not introduce separate counters.
3. **Cap check.** Apply this exact rule: `needs-changes && next_round >= 3`. If `needs-changes` is in the current labels and `next_round` is 3 or higher, do not run the round. This deliberately covers stale or already-high `round` values as well as the normal `round=2` → `next_round=3` transition. Instead:
   - Post a PR comment listing every still-blocking thread and the binding reference for each (or "no binding reference, judgment call needed").
   - Set `round: <next_round>` and `escalated: yes` in the state comment.
   - Remove `needs-changes` if set. Add `needs-human-review`.
   - End the run.
4. Otherwise set `round: <next_round>` and proceed to the per-thread work. Update the state comment at the end of the round.

The cap counts rounds since the last human handoff, not rounds since PR open. The cycle reset is detected from comment + label state alone — no timeline pagination required.

## Per-Thread Posture

Read all unresolved threads (human, Copilot, agent reviewers) and coordinator-provided recent actionable comment URLs before changing any code. Then apply two stages.

If the coordinator provided a `last_reviewed_sha` or specific thread URLs, start from that follow-up scope. Expand to older comments only when needed to classify the current feedback correctly.

### Stage 1 — short-circuit bot noise

Bots (Copilot, dependabot, automated checks) routinely produce findings that the human reviewer skills are *explicitly told not to flag* — see `backend-review-rubric.md`'s "Do Not Flag These" list. Treat any bot-authored thread whose finding falls into one of these categories as noise:

- Source/test hygiene facts that Checkstyle or the architecture governance suites mechanically enforce. Assertion presence and meaning, Mockito verification completion, branch ownership, and the other review-owned boundaries that list already names stay substantive — a bot thread on those is signal, not noise.
- Compilation failures that CI will report mechanically.
- Spring Cloud Contract verifier output for changed contract YAML when no contract is actually missing.
- Broad refactor preferences, naming taste, or alternative designs that do not create a correctness/security/maintainability risk.
- Comments on legacy or scaffold code that the PR does not rely on as a pattern.

For noise threads, do not enter the per-thread loop below. Instead, post **one** top-level PR comment listing the affected thread anchors (file:line) and a single rationale: *"These match the 'Do Not Flag These' list in `backend-review-rubric.md` and are out of scope for review-resolution."* Resolve each noise thread with a one-line reply pointing at that top-level comment. This prevents Copilot-style floods from consuming followup rounds.

### Stage 2 — classify substantive threads

For every remaining unresolved thread (human, agent-reviewer, or bot threads not short-circuited above):

- **Agree.** Apply the fix in the diff. Reply with a one-line summary of the change and the validation that confirms it. Resolve the thread.
- **Disagree on principle.** Reply with the binding reference (ADR section, `CONTEXT.md` definition, `decision-candidates.md` entry, sibling-repo file path, OpenAPI field) that supports keeping the code as is. Resolve the thread. Append to `decision-candidates.md` per the rule below — the next reviewer must not refile the same finding.
- **Need a human decision.** Reply with the specific decision required (one sentence). Leave the thread unresolved. This thread counts as still-blocking for the iteration-cap check at end of round.

Every substantive thread reaches a terminal reply this round — no silent skips. Bot threads that survive Stage 1 (i.e. raise an actual correctness/security/coverage point) get the same classification but with a higher bar for "agree"; default to skeptical and require an actual problem in the diff, not a generic suggestion.

Post the round's thread replies as **one** review submission, not one call per thread. A reply sent on its own becomes its own empty-bodied review event, so a six-thread round renders as six near-identical "reviewed" markers seconds apart. Batch them:

```
gh api graphql -f query='mutation($pr:ID!){ addPullRequestReview(input:{pullRequestId:$pr}){ pullRequestReview { id } } }' -f pr=<pr-node-id>
# then, per thread (thread node ids come from the reviewThreads query):
gh api graphql -f query='mutation($rev:ID!,$thread:ID!,$body:String!){ addPullRequestReviewThreadReply(input:{pullRequestReviewId:$rev,pullRequestReviewThreadId:$thread,body:$body}){ comment { id } } }' -f rev=<review-id> -f thread=<thread-id> -f body='<reply>'
# then submit once:
gh api graphql -f query='mutation($rev:ID!){ submitPullRequestReview(input:{pullRequestReviewId:$rev,event:COMMENT}){ pullRequestReview { id } } }' -f rev=<review-id>
```

Resolving threads (`resolveReviewThread`) stays a separate per-thread call — only the replies batch.

For comments created after the last reviewed SHA that are not review-thread comments, treat them like unresolved threads when they are actionable. If they are status chatter, summaries, or duplicate bot comments, ignore them unless the coordinator or a human calls them out.

## Code Changes

When a fix is needed:

- Implement the smallest change that resolves the thread. Do not bundle unrelated cleanup.
- Production code and tests change together at every touched segment.
- Follow the guardrails in `backend-issue-work` (layering, repository style, ClockProvider, no logged secrets) — those apply equally to followup edits.
- Run targeted validation per `AGENTS.md`. Escalate to `./scripts/test-with-backing-services.sh` only when the touched segment requires backing services.

## Label Hygiene Before Push

Before pushing:

1. Clear `review-passed` if set — new commits invalidate the prior clean review.
2. Clear `needs-changes` — this round's fixes are about to be on the branch; Phase B will re-evaluate.
3. Do **not** set `review-passed` from this skill. Only the reviewer agent does that.

If no code changed this round, take one of:

- **All threads and body-level blocking findings terminally resolved, none classified as "Need a human decision"**: Clear `needs-changes`. Do not push, do not set `review-passed` (still reviewer's prerogative). The PR is now in the "ready for fresh review" state; Phase B will re-evaluate and either set `review-passed` or re-set `needs-changes`. This is the path when every blocking finding was successfully rebutted with a binding reference and no edit was needed.
- **At least one thread is "Need a human decision", or the iteration cap fired**: Leave `needs-changes` set for ordinary human-decision blockers. If the iteration cap fired, follow the cap path's label changes instead (`needs-changes` removed, `needs-human-review` added). End the run.

## CI Failures

When CI is red:

- Treat the GitHub PR checks/status view as the default CI signal. Do not query CircleCI logs for routine green or pending PRs.
- When a CircleCI build, test, or deployment check fails and GitHub does not expose enough detail to diagnose it, use the approved CircleCI MCP or API-token path to inspect the failed job, test metadata, artifacts, or logs.
- Identify the failing check, command, and the smallest useful log excerpt before editing.
- Reproduce the failure locally with the smallest matching validation command when practical; if local reproduction is not possible, state the reason before relying on CI-only evidence.
- Apply the smallest reasonable fix.
- Rerun the relevant local validation command when possible.
- If the failure is environmental (Docker on CI flaked, transient network), rerun the check rather than patching code. Document the rerun decision in the PR if the failure pattern recurs.
- If CircleCI details are unavailable, run `./scripts/build.sh` locally before editing. It executes the same `clean build` gate as CI; if it fails, treat that failure as the root cause. If it passes, say so explicitly and continue with the GitHub check name/status.

CI fixes count as part of the current round, not a new one.

## Decision Candidates On Review Resolution

This is the write surface for review-resolution decisions. Append to `docs/agents-support/decision-candidates.md` in the same commit that carries the fix when:

- **Fix involves a new decision or assumption** ("we'll always return 422 for validation errors") — fix the code **and** append a candidates entry citing the PR comment.
- **Review comment rejected on principle** (we want this divergence) — append the rationale so the next reviewer does not refile it.

Skip the append when the fix is a straight correction with no new decision.

Entry format (newest first):

```
## YYYY-MM-DD — <short topic>
<2–6 lines: what was decided and why>

Source: pr-followup during PR #<pr-number>
Seen in: #<issue-number>
```

For chat-task PRs with no linked issue, list `PR #<pr-number>` under `Seen in:` instead.

**Before appending, grep `decision-candidates.md` for the topic keyword.** If a `##` heading already covers the same topic, append your PR/issue number to that entry's `Seen in:` line rather than writing a duplicate.

End the round with a one-line note of any candidates entries added.

## Engine-Neutral PR Surface

Do not name specific agents or engines (Codex, Claude, etc.) in PR replies or commit messages. The PR is the handoff surface; orchestration uses labels, not prose instructions to "start a new Codex session" or similar.

## Guarded Auto-Merge Terminal Step

After the per-thread loop, if all of the following hold, merge:

- The PR has the `auto-merge` label. Agents do not add this label unless the user or automation prompt explicitly asks.
- CI is green.
- `needs-changes` and `needs-human-review` are both unset.
- `review-passed` is set.
- The state comment's `last_reviewed_sha` matches the PR's current HEAD SHA. If not, an unreviewed push slipped in since the last Phase B run. Clear `review-passed`, leave the PR open, and report "review stale; re-run Phase B before merging." Do not add `needs-changes` — there is nothing for followup to do.
- No deep review is pending. If a deep-review request exists — an issue or PR comment carrying the deep-review marker (`/deep-review`, `[deep-review]`, `requires`/`needs deep review`, `deep review requested`/`required`) — then the state comment's `last_deep_reviewed_sha` must equal the PR's current HEAD SHA. If a deep review is requested but `last_deep_reviewed_sha` != HEAD, do **not** merge: leave the PR open and report "deep review pending; the review lane will deep-review this head before merge." Do not add `needs-changes` — there is nothing for followup to do; the read-only review lane will run deep review and stamp `last_deep_reviewed_sha`.
- The branch is up to date or mergeable without conflict.

Merge synchronously with `gh pr merge --admin --squash --delete-branch` (or the repo's configured merge strategy). The `--admin` flag bypasses the required-approval branch-protection rule per the Repository State note in `AGENTS.md` — all gates above must still pass; `--admin` is *only* for the self-approval blocker.

Do not use `gh pr merge --auto`. In solo-author mode it would stall waiting for an approval that cannot arrive. If CI is not yet green when this skill runs, leave the PR open with `auto-merge` set; the next invocation re-evaluates and merges then.

If any gate fails, leave the PR open and report which gate blocked.

## Post-Merge Cleanup

After a synchronous merge in this run:

1. Verify the merge SHA is on `main` on the remote.
2. `git worktree remove <worktree>`.
3. `git branch -D <pr-branch>` locally.
4. `git push origin --delete <pr-branch>` if the remote branch was not auto-deleted by the merge.
5. Report cleanup completion in one line.

## Out Of Scope

- Issue picking, implementation of new features. Those are `backend-issue-work`.
- Opening a PR. That is `backend-pr-publish`.
- Updates to `CODING_CONVENTIONS.md`, `CONTEXT.md`, `docs/adr/`, or skills during followup. Process-doc promotion runs through `backend-backlog-refinement` → `grill-with-docs`.

## Output

When invoked by a delivery coordinator, end with:

```text
HANDOFF:
- state: done|blocked|needs-review
- pr: #<number>
- branch: <branch>
- base: <base>
- head: <sha>
- labels: <labels changed or observed>
- checks: green|red|pending|unknown
- validation: <commands/results in one line>
- unresolved: <count plus short anchors, or none>
- next: <one sentence>
```
