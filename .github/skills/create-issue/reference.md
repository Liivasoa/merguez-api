# Reference — create-issue

## Output format

Each issue is a standalone Markdown file with the following sections:

| Section | Required | Description |
|---|---|---|
| `# Title` | ✅ | Concise title of the issue. Include the impacted module name when the feature spans multiple modules. |
| `**Contexte**` | ✅ | Functional context explaining *why* the issue exists and *who* it concerns. |
| `**Critères d'acceptation**` | ✅ | A Gherkin `Feature:` block with 1–N `Scenario:` entries covering the happy path and edge cases. |
| `**Notes**` | ❌ | Optional. Any additional references, constraints, or open questions. |

## Domain to technical derivation rules (Hexagonal)

- The `domain` issue captures business behavior in detail and is the source of truth.
- `application` and `infrastructure` issues are technical derivations and must not mirror every domain scenario one-by-one by default.
- If several domain scenarios follow the same technical path, group them into one technical scenario and reference all related domain IDs.
- Use this mandatory traceability line under each technical scenario header: `Ref Domain: D<id>[, D<id>...]`.
- A strict 1:1 mapping from domain to technical scenarios is allowed only when there is a real technical difference (different endpoint contract, different validation rule, different adapter behavior, etc.).

## Scope discipline (mandatory)

- Cover all explicit business rules from the prompt.
- Do not add acceptance criteria for unstated concerns.
- Do not preemptively include future-phase scenarios.
- If a potentially relevant concern is out of current scope, mention it only in `**Notes**` as out-of-scope.
- When the request says to implement only the prompt, keep scenarios minimal and strictly bounded to provided rules.

## File naming convention