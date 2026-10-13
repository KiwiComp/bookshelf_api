---
name: next-step
description: Find the next step in docs/roadmap.md, create its branch and propose a plan for it. Use when the developer asks to start the next step.
disable-model-invocation: true
---

# Next step

Running /next-step means: find the step to work on and plan it. It is not approval to write code.

## Find the step

- Read AGENTS.md and docs/roadmap.md with the Read tool, including the Decisions section, which
  can move content between steps.
- If a step is `In progress`, that is the step. Switch to its branch (`git checkout <branch>`) and
  continue it instead of starting a new one. Run `git status` and `git log main..HEAD --oneline`
  to see what is already done.
- Otherwise the step is the first one that is `Not started`. If every step is `Done`, say so and
  stop.

## Check before starting a new step

Stop and report if any of these fail:

- The step before it is `Done`.
- The working tree is clean (`git status`).
- main is up to date: `git checkout main` and `git pull --ff-only`.
- No pull request is open for an earlier step (`gh pr list --state open`). An open one means the
  earlier step is not merged yet.

Then create the branch named in the roadmap table from main: `git checkout -b <branch>`.

## Plan

- Read the files in docs/agents/ that the step's content requires, as listed in AGENTS.md, with
  the Read tool. working-conventions.md and testing.md always apply.
- Read the existing code the step builds on.
- Enter plan mode and propose a plan: which files are created or changed, in what order, and
  which tests come with them. Point out anything in the step that the rules do not answer, or
  where the rules and the code disagree.
- Wait for the developer's approval. Never write code before the plan is approved.

## After approval

- Set the step's status to `In progress` in docs/roadmap.md, in the step's branch.
- Implement the plan. Verify with the checks in `.claude/skills/verify/SKILL.md`.
- When the step is complete and verified, set its status to `Done` in the same branch.
- Do not commit. Finish with the summary required by the definition of done in AGENTS.md, and
  remind the developer that /pull-request commits and opens the pull request.
