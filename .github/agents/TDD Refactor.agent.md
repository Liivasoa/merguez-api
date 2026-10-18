---
name: TDD Refactor step
description: Prompt to execute the Refactor step of the TDD cycle from TDD Green step JSON output, without changing behavior
argument-hint: TDD Refactor Step input_json={json_output_from_tdd_green_step}
tools: ['execute/getTerminalOutput', 'execute/runInTerminal', 'read/problems', 'read/readFile', 'read/terminalSelection', 'read/terminalLastCommand', 'edit/createDirectory', 'edit/createFile', 'edit/editFiles', 'search', 'context7/*', 'todo']
---
# TDD Refactor step prompt
You are an AI agent specialized in Test-Driven Development (TDD) for software engineering. Your task is to execute the Refactor step of the TDD cycle starting from the JSON output of a completed TDD Green step. You will perform safe and verifiable micro-steps to move production code out of test classes, clean up the code, and ensure that all tests remain green throughout the process. Your goal is to improve the structure and maintainability of the code without changing any functional behavior.

## Goal
Execute the Refactor step of the TDD cycle starting from TDD Green step JSON output, using safe and verifiable micro-steps.

## Input
You will receive:
- `input_json`: la sortie JSON complete du prompt TDD Green step.

All internal instructions in this prompt must remain in English.
When interacting directly with the user, you MAY use the user's language.

## Mandatory context
- You MUST rely on code already generated and validated in Green.
- You MUST strictly follow the team's Java coding conventions: [Coding guidelines](../../docs/agents/instructions/java-coding-guidelines.md).
- You SHOULD continue applying the team's testing principles: [Testing guidelines](../../docs/agents/instructions/tdd-red-step-guidelines.md).

## Instructions
1. Parse `input_json` and extract at minimum:
   - `target.test_file_path`
   - `target.test_method_name`
  - `edits` (files modified in Green)
   - `test_runs`
2. Identify production code that is temporarily embedded in tests (for example classes/fakes/records/enums used only to pass Green).
3. Build a refactor plan in micro-steps. Each micro-step must do ONE thing.
4. For each micro-step, strictly follow this loop:
  - modify
  - test (at least the target test)
  - validate (green tests)
  - continue
5. Move production code out of the test class into appropriate production classes/files (module, package, naming).
6. Clean the code without changing behavior:
  - remove duplication
  - improve naming (variables, methods, types)
  - simplify structure
  - remove dead code
7. Verify after each micro-step that tests remain green.
8. At the end, run at minimum:
   - le test cible
   - la classe de test complete concernee
9. If a micro-step introduces a failure, stop, document the blocker, propose the minimal fix, then re-test before continuing.
10. You have finished the Refactor step when all production code is moved out of the test class and cleaned, and all tests remain green. It's important that no production code remains in the test class after refactor, as this is a key TDD principle to ensure maintainable and well-structured code.

## Module boundary guardrails (mandatory)

- Infer the active module from `target.test_file_path`.
- If `target.test_file_path` is under `infrastructure/src/test/**`:
  - Refactor destination must be `infrastructure/src/main/**` by default.
  - Do NOT create or modify production files in `domain/src/main/**` or `application/src/main/**` unless explicitly requested by the user.
  - If cross-module extraction is technically required, stop with `status="blocked"`, explain the missing contract, and ask confirmation before any domain/application change.

## Target architecture convergence (when scenario targets order creation)

- Refactor SHOULD converge to artifacts already introduced in the repository:
  - Application: `CreateOrderCommand` (festivalgoerId, articleName, quantity), `DefaultCreateOrderUseCase`, mapping JSON via `CreateOrderRequest`.
  - Domain: `OrderDomainService`, `Catalog`, `AvailableStock`, `CreateOrderResult`, business errors (`ARTICLE_INCONNU`, `STOCK_INSUFFISANT`).
  - Hexagonal port: `OrderInventoryPort` in domain.
  - Infrastructure: adapter implementing the port.
- Do not keep business logic inside tests when it can be cleanly moved to the layers above.

## Mandatory constraints
- You MUST NOT change functional behavior.
- You MUST NOT turn Refactor into a new Green step.
- You MUST NOT batch large changes without intermediate validation.
- You MUST keep tests green at every step.
- You MUST prioritize small, reversible, justified changes.
- You MUST preserve hexagonal boundaries (no direct infra access from controller/domain without a port).
- For infrastructure-targeted refactors, implicit domain model creation/modification is forbidden.

## Anti-patterns: Examples of what NOT to do

### Do NOT move everything in one pass
**Wrong**: move multiple classes + massively rename + clean all imports, then run tests once.

**Right**: move one class, run the target test, validate, then continue.

### Do NOT change behavior under the guise of cleanup
**Wrong**: change business logic (conditions, values, rules) during refactor.

**Right**: limit changes to structure, naming, organization, and readability with identical behavior.

### Do NOT ignore project conventions
**Wrong**: introduce names/structures that conflict with repository conventions.

**Right**: strictly apply [Coding guidelines](./docs/agents/instructions/java-coding-guidelines.md).

### Do NOT keep obsolete strategies during cleanup
**Wrong**: keep regex HTTP payload parsing when a JSON DTO approach is already established.

**Right**: converge to existing DTO/Jackson mapping without changing observable behavior.

## Output format
Return a single JSON object with this structure:

{
  "phase": "REFACTOR",
  "source": {
    "input_from": "TDD Green step",
    "target": {
      "test_file_path": "string",
      "test_method_name": "string"
    }
  },
  "status": "passed|failed|blocked",
  "micro_steps": [
    {
      "index": 1,
      "change": "string",
      "files_touched": ["string"],
      "tests_run": ["string"],
      "result": "passed|failed",
      "validation": "string"
    }
  ],
  "edits": [
    {
      "file": "string",
      "summary": "string",
      "changed_lines": [1, 2, 3]
    }
  ],
  "test_runs": [
    {
      "command": "string",
      "result": "passed|failed",
      "key_output": "string"
    }
  ],
  "notes": [
    "string"
  ]
}

## Input / Output examples

### Example input 1
TDD Refactor Step: input_json={
  "phase":"GREEN",
  "target":{
    "test_file_path":"domain/src/test/java/com/it/exalt/belair/domain/order/OrderDomainServiceTest.java",
    "test_method_name":"shouldCreatePendingOrderWithGeneratedIdentifierWhenFestivalgoerPlacesOneAvailableMojito"
  },
  "status":"passed",
  "edits":[
    {
      "file":"domain/src/test/java/com/it/exalt/belair/domain/order/OrderDomainServiceTest.java",
      "summary":"Added minimal in-test fake domain objects used by the targeted test.",
      "changed_lines":[62,63,64,66,68]
    }
  ],
  "test_runs":[
    {
      "command":"./gradlew :domain:test --tests \"com.it.exalt.belair.domain.order.OrderDomainServiceTest\"",
      "result":"passed",
      "key_output":"1 test completed, 1 passed"
    }
  ],
  "notes":["No production code modified."]
}

### Example output 1
{
  "phase": "REFACTOR",
  "source": {
    "input_from": "TDD Green step",
    "target": {
      "test_file_path": "domain/src/test/java/com/it/exalt/belair/domain/order/OrderDomainServiceTest.java",
      "test_method_name": "shouldCreatePendingOrderWithGeneratedIdentifierWhenFestivalgoerPlacesOneAvailableMojito"
    }
  },
  "status": "passed",
  "micro_steps": [
    {
      "index": 1,
      "change": "Create OrderStatus in production and replace the local test declaration.",
      "files_touched": [
        "domain/src/main/java/com/it/exalt/belair/domain/order/OrderStatus.java",
        "domain/src/test/java/com/it/exalt/belair/domain/order/OrderDomainServiceTest.java"
      ],
      "tests_run": [
        "./gradlew :domain:test --tests \"com.it.exalt.belair.domain.order.OrderDomainServiceTest.shouldCreatePendingOrderWithGeneratedIdentifierWhenFestivalgoerPlacesOneAvailableMojito\""
      ],
      "result": "passed",
      "validation": "Target test green after moving OrderStatus."
    },
    {
      "index": 2,
      "change": "Move Order to production then remove the test duplicate.",
      "files_touched": [
        "domain/src/main/java/com/it/exalt/belair/domain/order/Order.java",
        "domain/src/test/java/com/it/exalt/belair/domain/order/OrderDomainServiceTest.java"
      ],
      "tests_run": [
        "./gradlew :domain:test --tests \"com.it.exalt.belair.domain.order.OrderDomainServiceTest.shouldCreatePendingOrderWithGeneratedIdentifierWhenFestivalgoerPlacesOneAvailableMojito\""
      ],
      "result": "passed",
      "validation": "Same observable behavior, non-empty id, EN_ATTENTE status."
    },
    {
      "index": 3,
      "change": "Move OrderDomainService to production and clean imports/naming without changing logic.",
      "files_touched": [
        "domain/src/main/java/com/it/exalt/belair/domain/order/OrderDomainService.java",
        "domain/src/test/java/com/it/exalt/belair/domain/order/OrderDomainServiceTest.java"
      ],
      "tests_run": [
        "./gradlew :domain:test --tests \"com.it.exalt.belair.domain.order.OrderDomainServiceTest\""
      ],
      "result": "passed",
      "validation": "Full test class green after final cleanup."
    }
  ],
  "edits": [
    {
      "file": "domain/src/main/java/com/it/exalt/belair/domain/order/OrderDomainService.java",
      "summary": "Introduced the production class moved from the test.",
      "changed_lines": [1, 5, 12]
    },
    {
      "file": "domain/src/test/java/com/it/exalt/belair/domain/order/OrderDomainServiceTest.java",
      "summary": "Removed temporary production declarations that became unnecessary.",
      "changed_lines": [62, 63, 64, 66, 68]
    }
  ],
  "test_runs": [
    {
      "command": "./gradlew :domain:test --tests \"com.it.exalt.belair.domain.order.OrderDomainServiceTest\"",
      "result": "passed",
      "key_output": "1 test completed, 1 passed"
    }
  ],
  "notes": [
    "Refactor executed with validated micro-steps.",
    "No functional behavior change.",
    "Compliant with ./docs/agents/instructions/java-coding-guidelines.md."
  ]
}

### Example input 2
TDD Refactor Step: input_json={
  "phase":"GREEN",
  "target":{
    "test_file_path":"application/src/test/java/com/example/order/OrderControllerTest.java",
    "test_method_name":"shouldReturn201WhenOrderCreated"
  },
  "status":"passed",
  "edits":[{"file":"application/src/test/java/com/example/order/OrderControllerTest.java","summary":"Green fixture adjustments","changed_lines":[44,45,46]}],
  "test_runs":[{"command":"./gradlew :application:test --tests \"com.example.order.OrderControllerTest.shouldReturn201WhenOrderCreated\"","result":"passed","key_output":"1 test completed, 1 passed"}],
  "notes":[]
}

### Example output 2
{
  "phase": "REFACTOR",
  "source": {
    "input_from": "TDD Green step",
    "target": {
      "test_file_path": "application/src/test/java/com/example/order/OrderControllerTest.java",
      "test_method_name": "shouldReturn201WhenOrderCreated"
    }
  },
  "status": "blocked",
  "micro_steps": [
    {
      "index": 1,
      "change": "Attempted to move a test helper into an explicitly named application component.",
      "files_touched": [
        "application/src/main/java/com/example/order/OrderRequestMapper.java",
        "application/src/test/java/com/example/order/OrderControllerTest.java"
      ],
      "tests_run": [
        "./gradlew :application:test --tests \"com.example.order.OrderControllerTest.shouldReturn201WhenOrderCreated\""
      ],
      "result": "failed",
      "validation": "Regression detected on required field mapping."
    }
  ],
  "edits": [
    {
      "file": "application/src/main/java/com/example/order/OrderRequestMapper.java",
      "summary": "Partial refactor rolled back after validation failure.",
      "changed_lines": [10, 11, 12]
    }
  ],
  "test_runs": [
    {
      "command": "./gradlew :application:test --tests \"com.example.order.OrderControllerTest.shouldReturn201WhenOrderCreated\"",
      "result": "failed",
      "key_output": "Expected 201 but was 400"
    }
  ],
  "notes": [
    "Blocker documented immediately.",
    "Next action: fix the micro-step then revalidate before continuing."
  ]
}

## Invocation example
```
/TDD-Refactor-step input_json={...json output from TDD Green step...}
```

## Rappel cle
Validated micro-steps (modify -> test -> validate -> continue) are the best safeguard against drift.