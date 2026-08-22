---
name: inspect-prototype
description: Drive a UI prototype in the browser to recover the designer's intent by observing what it actually does — reach a screen or state, check a field or behaviour, confirm a flow. Use during discovery, grilling, or speccing whenever a prototype URL is available and a concrete question about the intended UI would be settled faster by looking than by asking (e.g. "what happens when you click New lead?", "what states does the inbox have?"), even if the user doesn't frame it as "inspect" or "walk". For answering questions against a live prototype — not for verifying a locally-running build, implementing UI from it, or producing a standing product-reference catalogue.
---

<what-to-do>

The prototype is the designer's intent made concrete — the skill's job is to recover that intent: what a screen, field, or flow is meant to do. Treat it as a source you can query. When a question about the intended UI comes up and the prototype could answer it, drive it in the Codex in-app browser to reach the screen or state, observe what happens, and report the answer — look instead of asking the user. Ad-hoc lookup, not a full walk-through: go only as far as the question needs.

</what-to-do>

<supporting-info>

Check what's already written down first — a catalogue of the prototype often exists under `docs/product-reference/`. Look live for what the docs can't answer: actual behaviour, a state, a detail. Answer and report; grilling, classification, and Spec text stay with the skills that own them (`grill-with-docs`, `build-product-reference`, `to-prd`).

The designer's intent is a proposal to ratify, never a contract. The prototype cannot author domain rules, validation, wire format, or error semantics — treat what you observe as input for the user to confirm, not a settled decision.

## What you need

- **URL** — from the user or project context (`CONTEXT.md`, product-reference). Don't guess one; if you can't find it, ask. Confirm it loads.
- **Question** — usually you already have it; it's why you're looking. Ask only if you don't know what would answer it.

Within the question, drive freely — it sets the boundary, you choose the path.

## Driving it

Use the native Codex in-app browser for all navigation and actions. Nothing to build, start, authenticate, or tear down — just open the URL. Do not use Playwright / `playwright-cli`, install browser drivers, or write automation scripts. If the in-app browser can't reach the URL, that's a setup blocker to report, not a cue for a different tool.

Act on concrete targets — click by visible text or role, type into named fields, select, scroll. Prefer the accessibility tree / page text over pixel-clicking. Use a viewport matching the prototype's form factor; desktop by default. If a state needs data or a step you can't reach, have the user drive and report what they see.

## What to bring back

Lead with the answer. Add only what bears on the question, not an inventory:

- what happened at each step and the path to reach it;
- the fields, controls, copy, or states that answer it;
- decision-relevant behaviour — validation, record matching, status/SLA side-effects, error/empty states — the part worth grilling on that a catalogue usually misses;
- gaps: if the prototype can't show the needed state, say so; don't invent it.

Take screenshots only when asked — they're expensive. When visual fidelity matters (layout, overlap, hover, animation), a screenshot shows what text can't.

## Output

Answer plainly. Flag any terminology mismatch — where a prototype label conflicts with the project's established terms, raise it, don't adopt it. It's observation feeding a decision, not the decision. The reference holds only while the prototype stays live and aligned — a pointer, not a contract.

