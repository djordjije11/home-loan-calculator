---
name: to-prd
description: Turn the current conversation context into a local Spec under `specs/` for user review. Use when user wants to create a Spec from the current context.
---

This skill takes the current conversation context and codebase understanding and produces a local Spec. Synthesize from the existing grilling/context by default; do not restart the interview phase.

If the available context is too thin, contradictory, or not specific enough to produce a Spec that can safely feed implementation issues and production-bound code, stop and ask the minimum necessary clarification questions before writing the Spec.

Do not hide implementation-critical uncertainty in an assumptions section. Unresolved points that affect behavior, API contracts, data model, permissions, compliance, or production risk must be resolved before the Spec is committed or used for issue generation.

The skill name is historical. In this project, the output artifact is called a **Spec**, even though this skill is named `to-prd`.

Read `AGENTS.md` and the domain docs before writing the Spec.

## Process

1. Explore the repo to understand the current state of the codebase, if you haven't already. Use the project's domain glossary vocabulary throughout the Spec, and respect any ADRs in the area you're touching.

2. Sketch out the major modules you will need to build or modify to complete the implementation. Actively look for opportunities to extract deep modules that can be tested in isolation.

A deep module (as opposed to a shallow module) is one which encapsulates a lot of functionality in a simple, testable interface which rarely changes.

If these modules or testing expectations are unclear from prior context, ask the user before writing the Spec. Otherwise, include the inferred decisions in the Spec for review.

3. Write the Spec using the template below and save it directly under `specs/<domain>/<slice-id>-<feature>.md`.

4. Stop after creating or updating the local Spec. Do not create GitHub issues, do not publish to the implementation repositories, and do not apply triage labels. Issue creation is handled later by the separate `to-issues` skill after the Spec is reviewed, accepted, committed, and explicitly approved for issue generation.

<spec-template>

## Problem Statement

The problem that the user is facing, from the user's perspective.

## Solution

The solution to the problem, from the user's perspective.

## User Stories

A LONG, numbered list of user stories. Each user story should be in the format of:

1. As an <actor>, I want a <feature>, so that <benefit>

<user-story-example>
1. As a mobile bank customer, I want to see balance on my accounts, so that I can make better informed decisions about my spending
</user-story-example>

This list of user stories should be extensive and cover all important aspects of the slice.

## Implementation Decisions

A list of implementation decisions that were made. This can include:

- The modules that will be built/modified
- The interfaces of those modules that will be modified
- Technical clarifications from the developer
- Architectural decisions
- Schema changes
- API contracts
- Specific interactions

Do NOT include specific file paths or code snippets. They may end up being outdated very quickly.

Exception: if a prototype produced a snippet that encodes a decision more precisely than prose can (state machine, reducer, schema, type shape), inline it within the relevant decision and note briefly that it came from a prototype. Trim to the decision-rich parts — not a working demo, just the important bits.

## Testing Decisions

A list of testing decisions that were made. Include:

- A description of what makes a good test (only test external behavior, not implementation details)
- Which modules will be tested
- Prior art for the tests (i.e. similar types of tests in the codebase)

## Out of Scope

A description of the things that are out of scope for this Spec.

## Assumptions and Open Questions

Minor assumptions or non-blocking questions, if any. Do not include unresolved points that would make implementation issues ambiguous.

## Further Notes

Any further notes about the slice.

</spec-template>
