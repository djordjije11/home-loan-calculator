---
name: build-product-reference
description: Research product inputs and create durable product-reference documentation. Use when the user gives notes, screenshots, URLs, prototypes, idea dumps, competitor/reference products, or prior research and asks Codex to extract product information, modules, features, workflows, fields, data, states, screenshots, or gaps into docs/product-reference for future agents.
---

# Build Product Reference

Turn messy product evidence into reusable reference docs under `docs/product-reference/`. The output should help future agents understand and replicate the product experience before writing Specs or implementation issues.

## Workflow

1. Read local operating context first:
   - `AGENTS.md`
   - `CONTEXT.md` when present
   - `docs/agents/domain.md`
   - existing files under `docs/product-reference/`

2. Identify the reference target:
   - Product or prototype name.
   - Source inputs: URLs, current browser page, screenshots, notes, documents, repositories, or prior product references.
   - Target modules, if the user names them.
   - Existing comparison style, especially a product the user calls out as the template.

3. Gather evidence from every available input:
   - For URLs or live prototypes, use the Browser skill when available. Inspect visible pages, route changes, nav items, modals, dialogs, forms, tabs, filters, menus, buttons, empty states, alerts, and seeded records.
   - For screenshots, use image inspection and capture concrete UI text, visible layout, data, states, and implied workflows.
   - For notes or docs, extract product language, objects, decisions, open questions, and implied behavior.
   - For local prototype code or seed data, inspect routes, fixtures, component labels, and data objects when available.

4. Explore like a product researcher:
   - Map information architecture and routes.
   - Open key records and detail pages.
   - Trigger non-destructive modals and action surfaces.
   - Capture screenshots when the visual state matters.
   - Prefer specific evidence over generic summaries.
   - Do not perform destructive or externally visible actions without user approval.

5. Separate evidence levels:
   - `Visible`: implemented visibly with concrete UI or data.
   - `Action surface`: button, route, or affordance exists, but deeper behavior is not exposed.
   - `Inferred`: strongly implied by connected screens, seeded data, or copy.
   - `Open`: needed for product/spec work but not answered by the evidence.

## Output Shape

Create or update a folder under:

```text
docs/product-reference/<product-slug>/
```

For a broad product or prototype, produce:

- `<product-slug>-prototype.md`: product overview, modules, route map, workflows, object model, UI patterns, screenshots, and open gaps.
- `<product-slug>-<module>.md`: one deep dive per priority module.
- `<product-slug>-module-feature-inventory.md`: checklist-style inventory across priority modules.
- Screenshot files with stable numbered names when visual evidence is useful.

When the user says "in BME fashion", mirror the BME reference style:

- Evidence sources and screenshots.
- Module surfaces and route table.
- Seeded records and example data.
- Product objects and relationships.
- Workflows as named flows.
- Field inventory grouped by area.
- State/lifecycle model.
- Feature checklist.
- Open gaps and implementation clues.

Keep these documents as product reference, not Specs, PRDs, or GitHub issue drafts. Do not create implementation issues from this skill.

## Coverage Checklist

For every target module, try to capture:

- Module purpose and users.
- Routes, pages, tabs, panels, and modals.
- Primary workflows and alternate flows.
- Buttons/actions and whether they are wired or only visible.
- Forms, fields, defaults, placeholders, options, and validation hints.
- Tables, cards, filters, sorting, search, exports, and bulk actions.
- Record types, IDs, seeded data, statuses, owners, dates, amounts, and metrics.
- Object relationships and cross-module links.
- Lifecycle states, queue states, publishing states, task states, and error states.
- Notifications, reminders, SLAs, permissions, audit trails, and external integrations.
- Visual patterns future UI agents should preserve.
- Prototype-only behavior, simulated state, and external integrations that are not wired.
- Product decisions still needed before a Spec.

## Writing Standards

- Start each reference doc with `Last researched: YYYY-MM-DD`.
- Link companion docs from the overview.
- Use the repo's domain vocabulary from `CONTEXT.md` where available.
- Use concrete prototype text and data, but summarize rather than copying large source passages.
- Prefer tables for field inventories, states, seeded records, routes, and feature matrices.
- Call out uncertainty plainly instead of presenting inferred behavior as fact.
- Keep unrelated repo files untouched.

## Validation

Before finishing:

- Check that every created reference doc is linked from the overview or an obvious index.
- Compare depth against the requested style/reference product when one exists.
- Run `rg` for obvious typo drift and non-ASCII surprises if the repo prefers ASCII.
- Run `wc -l` or heading scans as a quick breadth check, but judge completeness by coverage, not line count.
- Run `git status --short` and report only relevant created/modified files.
