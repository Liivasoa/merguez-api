# Ionic Angular Frontend Instructions

These instructions apply to the Ionic Angular application under `frontend/`. The frontend has not been scaffolded yet; when it is added, keep its package manifest, source, tests, and build configuration inside this directory.

## Boundaries

- Keep UI presentation, navigation, and client-side interaction in this application. The Spring API remains under `backend/` and owns server-side business rules and persistence.
- Call the API through explicit HTTP client services and typed frontend models. Do not access backend source code or MongoDB directly.
- Keep API base URLs in the frontend's environment/runtime configuration; do not hardcode machine-specific URLs in feature components.
- Keep Ionic/Angular dependencies and scripts in `frontend/package.json`. Do not add frontend dependencies to the Maven build.
- Keep web and native-target concerns within the Ionic/Capacitor frontend configuration; do not make the API serve the compiled UI unless the user requests that deployment design.

## Implementation and quality

- Organize UI code by user-facing capability, then by Angular responsibility. Avoid empty layers and generic catch-all folders.
- Use the Angular and Ionic APIs supported by the versions in `frontend/package.json`. Prefer standalone Angular components and typed inputs/outputs when supported by the established app conventions. Keep components focused, use dependency injection for collaborators, and avoid duplicated state or unnecessary abstraction; apply SOLID in proportion to the feature.
- Keep TypeScript strict and models explicit. Avoid `any`, unsafe casts, non-null assertions, and duplicated API types where a shared contract already exists. Keep templates declarative and move nontrivial behavior into focused component logic or services.
- Design mobile-first for touch and narrow viewports, then verify that content, navigation, forms, and actions remain usable on tablet and desktop widths. Use responsive layout primitives and existing design tokens; do not encode a phone-only fixed canvas.
- Preserve accessibility: semantic controls, accessible names, visible focus, sufficient contrast, keyboard operation, and appropriate reduced-motion behavior. Use Ionic controls where they fit the interaction and existing design system.
- Follow the Angular and Ionic versions and conventions established when the frontend is scaffolded; do not assume a test runner or lint command that is not configured.
- Add or update frontend tests for behavior when the user asks for testing or the selected feature workflow requires them. Report exact commands and results.
- Keep frontend-specific decisions and instructions here. Put cross-application API contract decisions in root documentation or the relevant backend feature issue.

## Frontend agent profiles

- For a new or substantial Ionic Angular UI feature, use `.codex/agents/frontend-feature-implementation.toml` (also available as `.github/agents/Frontend Feature Implementation.agent.md`) to coordinate the ordered workflow.
- The coordinator uses `.codex/agents/frontend-tdd-cycle.toml` and its Red, Green, and Refactor phase profiles. Red edits tests only, Green edits production code only, and Refactor makes behavior-preserving production changes. Follow the actual runtime's available delegation tools and do not claim a phase ran unless its evidence is reported.
- These frontend profiles do not require issue folders under `docs/features/`, which are for backend feature workflows. Do not commit unless the user or applicable instructions authorize commits.
