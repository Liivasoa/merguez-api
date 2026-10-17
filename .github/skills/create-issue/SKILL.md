--- 
name: create-issue
description: Create an issue in the form of a markdown file with title, description, implementation plan, and Gherkin test scenarios from a functional request. Use when needing structured, testable issues.
---

# Instructions
1. Use right thinking and structured analysis before writing anything.
2. Extract context, explicit rules, and success criteria from the request.
3. Build a rule coverage checklist before writing anything. You MUST list every business rule found in the request and map each one to at least one acceptance criterion or note.
4. Ask clarifying questions only if a blocker prevents writing valid scenarios. If the request is actionable, do not ask extra questions and proceed directly.
5. Identify impacted modules.
6. If `domain` is impacted, it MUST be processed first and completed before starting `application` or `infrastructure`.
7. Generate issues in this mandatory order:
    1. `domain` first (mandatory when impacted):
        1. Summarize business context and invariants.
        2. Identify complete success criteria for all business scenarios.
        3. Generate Gherkin scenarios that are exhaustive for explicit rules only (happy path + explicit edge cases + explicit forbidden/invalid cases).
        4. Create `docs/features/{feature_name}/domain_{issue_title}.md`.
    2. `application` second (if impacted):
        1. Derive scenarios from the completed domain scenarios only.
        2. Focus on API contract, mapping, validation, orchestration, and error exposure.
        3. Group domain scenarios when they share the same technical treatment in the application layer.
        4. Avoid 1:1 domain-to-application scenario mirroring unless technically justified.
        5. Do not invent new business rules not present in domain outputs.
        6. Create `docs/features/{feature_name}/application_{issue_title}.md`.
    3. `infrastructure` third (if impacted):
        1. Derive scenarios from the completed domain scenarios only.
        2. Focus on persistence, adapters, integration mechanics, and technical failure handling.
        3. Group domain scenarios when they share the same technical treatment in adapters/persistence.
        4. Avoid 1:1 domain-to-infrastructure scenario mirroring unless technically justified.
        5. Do not invent new business rules not present in domain outputs.
        6. Create `docs/features/{feature_name}/infrastructure_{issue_title}.md`.
8. For each generated module issue:
    1. Keep traceability to the originating domain scenario IDs/headings.
    2. For `application` and `infrastructure` scenarios, add an explicit reference line directly under each numbered scenario header using this exact format: `Ref Domain: D<id>[, D<id>...]`.
    3. Generate a concise title and structured description.
    4. Produce 1..N Gherkin scenarios following the mandatory presentation format below.
    5. Use the `templates/issue.md` template.
    6. Use the `examples.md` file to provide an example of the generated issue for a given request.
    7. Validate the issue by running `scripts/validate_issue_format.py`.
    8. Perform a completeness pass.

# Strict Scope Guard (Mandatory)

- Implement only what is explicitly requested in the prompt and rule bullets.
- Do not add speculative capabilities, future phases, or "nice to have" scenarios.
- Do not infer unrelated technical concerns unless explicitly requested (examples: authentication/authorization, 404 handling, retries, scheduler/batch, migration, observability, rate limiting).
- "Exhaustive" means exhaustive coverage of explicit rules, not exhaustive coverage of a full imagined product.
- If a rule is not stated, do not create an acceptance criterion for it.
- If an unstated area seems important, add it under `**Notes**` as out-of-scope instead of turning it into a scenario.
- If the user asks to "just implement the prompt", skip optional expansion and stay minimal.

# Mandatory Domain-First Orchestration

- `domain` is the source of truth for business behavior.
- `application` and `infrastructure` are downstream issues and MUST be derived from `domain`.
- If multiple modules are impacted and `domain` is not generated first, stop and fix the order.
- If only technical modules are requested but business rules are ambiguous, force creation of a domain issue first (or ask clarifying questions).
- Every `application` and `infrastructure` scenario must reference at least one domain scenario.
- `application` and `infrastructure` issues may split or regroup domain scenarios for technical clarity, but they must preserve the same business intent.
- `application` and `infrastructure` must be written from a technical-responsibility perspective (contract/orchestration/validation/mapping for application; adapters/persistence/integration failure handling for infrastructure), not as a business-case-by-business-case rewrite.
- If multiple domain scenarios are handled by the same technical flow, they MUST be grouped under one technical scenario with multiple domain references.
- 1:1 mapping between domain scenarios and technical scenarios is a last resort and requires explicit technical justification.
- The reference format is mandatory and strict: `Ref Domain: D<id>[, D<id>...]`.

In particular for the `domain` module:

- If the request contains pricing, tariff, cost, token-consumption, free-item, or premium/standard rules, the domain issue MUST contain explicit acceptance criteria for the pricing policy itself, not only aggregate order totals.
- The domain issue MUST make the business invariant visible as a first-class concern. For example, a tariff matrix must appear as dedicated scenarios or an explicitly named section of acceptance criteria.
- A mixed-order scenario is not sufficient to cover pricing rules by itself when the pricing matrix contains multiple distinct cases.

Before finalizing, ask yourself these mandatory checks:

- Does every rule bullet from the feature appear in at least one scenario or note?
- If the feature defines a pricing matrix, is that matrix explicitly represented in the domain issue?
- If the feature defines state-dependent behavior, are the allowed and forbidden transitions both covered?
- If the feature defines refunds or pooled contributions, are their effects represented explicitly?
- **Is there at least one happy-path ("nominal"/"baseline") scenario that covers the simplest successful case?** Even if not explicitly mentioned in the spec, add a basic scenario showing the happy path. This ensures robust baseline coverage and catches regressions.
- If `application` and/or `infrastructure` issues were generated, do all their scenarios map back to at least one completed domain scenario?
- Are `application` and `infrastructure` scenarios technical derivations (contract, validation, mapping, adapter, persistence, integration failures) rather than new business-rule definitions?
- Did I avoid unnecessary 1:1 mirroring of domain scenarios in `application` and `infrastructure`?
- Where domain scenarios share the same technical processing path, did I aggregate them into a single technical scenario with multiple `Ref Domain` IDs?
- Do all `application` and `infrastructure` scenarios include a strict `Ref Domain: D<id>[, D<id>...]` line?

# Mandatory Gherkin Presentation
For readability and consistency, every generated issue MUST present acceptance criteria using this structure:

1. A single `Feature:` block at the beginning of `**Critères d'acceptation**` (or `**Acceptance Criteria**`).
2. Numbered scenario headings in markdown, one per scenario:
   - `1. Scenario Header`
   - `2. Scenario Header`
3. For `application` and `infrastructure` issues, add `Ref Domain: D<id>[, D<id>...]` directly below each numbered scenario heading.
4. Immediately under each heading (and after `Ref Domain` when required), add a real Gherkin block starting with `Scenario:` and then `Given`, `When`, `Then` (and optional `And`).
5. Keep one blank line between scenarios.

Expected shape:

```markdown
**Critères d'acceptation**
Feature: <feature name>

1. <short scenario header>
Ref Domain: D1
Scenario: <gherkin scenario title>
Given <precondition>
When <action>
Then <expected result>
And <optional additional assertion>

2. <short scenario header>
Ref Domain: D2, D3
Scenario: <gherkin scenario title>
Given <precondition>
When <action>
Then <expected result>
```

Do not output plain prose-only acceptance criteria. Every scenario must be expressed in Gherkin syntax.
    
# Note
- This skill is intended to create manageable issue. Typically, it should not span more than one module. 
- If the request is too broad, propose the user to break it down per module
- Completeness of business rules matters more than minimizing the number of scenarios. Prefer a few extra precise scenarios over a concise but incomplete issue.
- Never introduce features not yet managed by the current scope. Keep issues aligned with the requested implementation phase only.