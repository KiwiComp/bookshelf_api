---
name: review-conventions
description: Review the code against the project's rules in AGENTS.md and docs/agents/ and report where it deviates. Use when the developer asks to check the code against the conventions, or before a pull request.
argument-hint: "[diff | <path> | <feature>]"
disable-model-invocation: true
---

# Review conventions

Check that the code follows AGENTS.md and the rules in docs/agents/. Generic bug hunting is not
the goal here; /code-review covers that.

Follow docs/agents/reviewing.md for how to review, the scope and the report format. Read it with
the Read tool first. The scope is: $ARGUMENTS

## Read the rules

Read AGENTS.md and every file in docs/agents/ that applies to the code in scope. Each file ends
with a "Before you finish" list; treat every item in those lists as something to check.

## What to check

Use grep where a rule can be checked mechanically, then read the code to confirm each hit.

- **Dependency direction** (structure.md): imports that break `navigation -> feature -> core.data
  -> core.database / core.network`, such as a feature importing another feature, core.database or
  core.network, core.ui importing anything but core.model, or core.network and core.database
  importing core.model.
- **ViewModels** (structure.md, state-and-errors.md): depend on repository interfaces only, expose
  StateFlow<XUiState>, catch specific exceptions and map them as in the Errors table in AGENTS.md.
- **Navigation** (navigation.md): no NavController in screens, events as lambdas, destinations
  carry only the id.
- **Network and storage** (network.md): no hardcoded URLs, network models only in core.network and
  core.data, no direct `Dispatchers.IO` in classes, images saved under filesDir, a failed cover
  download never fails a save, removing a book deletes its cover.
- **Compose** (code-conventions.md): modifier parameters, hoisted state, no hardcoded colors or
  text, keys in lazy lists, side effects in LaunchedEffect, previews.
- **App behavior** (AGENTS.md): plurals for counts, Collator sorting instead of SQL ORDER BY,
  isRead false for unsaved books, the summary counts all saved books, paging rules, snackbars that
  retry the failed action.
- **Tests** (testing.md): every class with behavior has tests of the type the layer table
  requires, in the mirrored package, with sentence names (underscores in androidTest). Report
  missing tests as findings.

## When the code may be right

If the code deviates but seems to be the better choice, or two rule files disagree, report it as
a question about the rule instead of a finding in the code, so the developer can update the rule.
