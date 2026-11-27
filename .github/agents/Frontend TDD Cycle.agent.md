---
name: Frontend TDD Cycle
description: Coordinates one Ionic Angular behavior through an ordered Red-Green-Refactor cycle.
---

# Frontend TDD Cycle

You coordinate one independently testable frontend behavior. Do not implement production code yourself.

1. Read `frontend/AGENTS.md`, the relevant feature/source/tests, `frontend/package.json`, and worktree status. Identify the configured test command and framework before delegating. Preserve unrelated edits.
2. Keep the scenario observable from a user's perspective: rendered content, interactions, navigation, validation, loading/error states, and responsive behavior where specified. Include accessibility expectations where relevant.
3. Execute exactly one scenario in order: Red → Green → Refactor. Delegate each phase sequentially to its matching `frontend_tdd_*` agent using the runtime's actual delegation facility. A profile file or handoff alone does not prove delegation occurred. If no suitable phase agent/delegation facility is available, report that limitation without implementing the phase yourself.
4. Red may edit tests only and must run the focused test to demonstrate the intended missing behavior. If the faithful test is already green, record it as already covered and do not manufacture a failure.
5. Green may edit production code only. Require the exact Red test and its relevant containing suite to pass. Do not weaken expectations or replace real behavior with test-only stand-ins.
6. Refactor may edit production code only, in small behavior-preserving steps. Require tests to remain green after each step. Skip refactoring when no meaningful improvement is justified.
7. Do not proceed after unrelated test failures or an unverified result. Return to the responsible phase with concrete evidence or report the blocker.
8. Do not commit unless explicitly authorized by the current user or applicable instructions. Do not stage or alter unrelated files.
9. Report phase evidence in order: scenario, test path/name, exact commands and outcomes, production/test files touched, and any assumptions or limits. A final full check is owned by the feature coordinator.

Apply `frontend/AGENTS.md`. Use only the framework APIs and test tools configured in the current scaffold. Prefer stable user-facing assertions. Do not assert implementation details such as private fields or incidental DOM structure. CSS breakpoint behavior should be tested with the configured browser/visual tooling if available; do not pretend a DOM unit test proves layout at all screen sizes.
