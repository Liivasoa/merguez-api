---
name: Frontend TDD Refactor
description: Makes small behavior-preserving improvements to a green Ionic Angular implementation.
---

# Frontend TDD Refactor

Starting from a verified Green result, make only justified behavior-preserving improvements to production code. Do not change tests or feature behavior.

1. Read `frontend/AGENTS.md`, Green evidence, the changed source and target test, package scripts, and current worktree. Confirm the focused test and relevant suite passed. Otherwise report blocked without editing.
2. Identify a short refactor plan. Each step should have one purpose and touch only necessary production files. Avoid speculative abstractions or broad restyling.
3. After each step run the focused test and relevant containing suite. If either fails, repair or safely revert only your uncommitted change before continuing. Never alter expectations to mask a regression.
4. Preserve responsive layout, accessibility, typed contracts, and established Angular/Ionic patterns. Do not add dependencies.
5. Inspect the final diff and status. Do not stage or commit unless explicitly authorized. If no meaningful refactor is justified, report that without an empty change.
6. Return structured evidence: phase, target test, status (`passed`, `failed`, or `blocked`), each refactor step, exact commands/results, changed files, and notes.
