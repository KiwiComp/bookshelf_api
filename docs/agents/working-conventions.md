You are helping me build an Android app. Follow these working conventions in every task. The other prompts in this project (project setup, structure, navigation, code conventions, state and errors, testing) describe what the code should look like; this one describes how you work.

## Language

- All code, identifiers, comments, KDoc, commit messages and branch names are in English.
- Answer me in Swedish.

## Before writing code

- Read the existing code in the area you are changing, and follow its patterns.
- If existing code differs from the prompts, point out the difference. Do not silently follow either one, and do not rewrite unrelated code to make it match.
- For a new feature, or a change that touches several files, start with a short plan: which files you will create or change, and in what order. Wait for my approval before writing code.
- For a small fix in one place, go ahead without a plan.

## Scope

- One feature per branch. If a feature is large, split it into steps that each leave the projectbuilding with all tests passing.
- Change only what the task requires. If you notice something else that should be fixed, mention it in your summary instead of changing it.

## Ask before

- Adding a dependency or a plugin.
- Deviating from any of the prompts.
- Changing the database schema (it requires a migration).
- Deleting files.
- Making a change that affects several features at once.

Decide on your own anything the prompts already answer, such as names, file placement and how a test is written.

## Git

- Create a branch for each task from an up-to-date main, named after what it adds: feature/<name>, fix/<name>, refactor/<name>, chore/<name>, build/<name>, ci/<name> or docs/<name>.
- Never commit without asking me first. Leave changes uncommitted so I can review the diff. Committing, pushing and opening a pull request are done through the /pull-request skill, which describes the commit rules.
- Never push, merge, rebase onto main, or force push without asking me first.

## Explaining choices

- When you make a choice that does not follow directly from the prompts, explain it briefly: what you chose and why.
- Do not explain what the prompts already state.

## Definition of done

A task is done when:

- the project builds,
- the code and its tests follow the prompts,
- all tests you could run pass,
- and you have written a summary.

The summary contains:

- what was changed, file by file at a high level,
- the choices you made that were not given by the prompts,
- what I should test manually in the app,
- anything you could not verify (for example instrumented tests without an emulator),
- anything you noticed outside the task that should be addressed.

## When unsure

If a requirement is unclear, a rule seems wrong for the case at hand, or you cannot verify something (a library version, an API, a behavior), say so and ask. Never guess and present the guess as fact.