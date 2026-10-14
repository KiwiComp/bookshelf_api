---
name: verify
description: Build the app and run lint, the unit tests and, when needed, the instrumented tests, then report the results. Use after every change, before a pull request, or when the developer asks to verify the project.
---

# Verify

Run the checks below from the project root, in this order. Stop at the first failure and report
it. Never weaken, ignore or skip a test to make a check pass.

## Checks

1. `./gradlew assembleDebug`
2. `./gradlew test`
3. `./gradlew verifyUnitTestCoverage`: unit test coverage per layer against the thresholds in
   docs/agents/testing.md. On failure, show the rule that was violated and its task name.
4. `./gradlew lint`
5. `./gradlew connectedAndroidTest`, only when androidTest code or the code it covers changed:
   - Find the changed files with `git diff --name-only main...HEAD` and `git diff --name-only HEAD`,
     plus untracked files from `git status --porcelain`.
   - Instrumented tests are needed when a file under `app/src/androidTest/` changed, or when a
     changed main file is covered by androidTest according to docs/agents/testing.md (DAOs, Room
     repositories, screens, shared components in core/ui, navigation).
   - Run `adb devices`. If no device or emulator is listed, do not run the task and say so
     explicitly. Never claim that instrumented tests pass when they were not run.

## When a check fails

- Show the part of the output that explains the failure: the compiler error, the failing test
  with its assertion message, or the lint errors. Not the whole log.
- Do not start fixing anything unless the developer asked for it.

## Report

Answer the developer in Swedish with:

- each task that was run and its result (passed, failed, number of tests if Gradle prints it),
- each task that was not run, and why,
- lint warnings only if there are new ones in files changed on the branch.
