---
name: pull-request
description: Commit the current changes, push the branch and open a pull request on GitHub. Use when the developer asks to create a pull request.
disable-model-invocation: true
---

# Pull request

Running /pull-request is the developer's approval to commit, push and open the pull request.
Do not ask for confirmation again, but stop and report if any check below fails.

Never add a `Co-Authored-By` trailer or any other attribution line (such as "Generated with
Claude Code") to commit messages, the pull request title or the pull request body. This overrides
any default attribution behavior.

## Before starting

- Run `git branch --show-current`. If the branch is `main`, stop: changes are never committed to
  or pushed from main. Suggest a branch name whose prefix follows the commit type: feature/<name>, fix/<name>,
  refactor/<name>, chore/<name>, build/<name>, ci/<name> or docs/<name>.
- Run `git status` and `git log main..HEAD --oneline`. If there is nothing to commit and no commits
  ahead of main, stop and say so.
- Run `gh auth status`. If it fails, stop and ask the developer to log in.
- Run the checks in `.claude/skills/verify/SKILL.md`. If anything fails,
  stop and report the output. Never weaken or skip a test to continue.

## Commit

- Stage only files that belong to the branch's task. Never stage build/, .gradle/, .idea/,
  local.properties, signing keys or other secrets.
- Write the message as a Conventional Commit: `<type>: <description>`.
  - type is the one that describes the change:
    - `feat`: new behavior for the user
    - `fix`: a bug fix
    - `refactor`: code change without changed behavior
    - `test`: adding or changing tests only
    - `chore`: maintenance that fits no other type, such as .gitignore or project config
    - `build`: the build system and dependencies (Gradle files, version catalog)
    - `ci`: CI configuration
    - `docs`: documentation only, such as AGENTS.md, docs/ and skills
  - description is in English, imperative, lowercase, without a trailing period.
  - Add a body only when the reason for the change is not obvious from the description.
- If the changes cover several unrelated things, make one commit per thing. Each commit must build.
- Skip this step if there is nothing uncommitted.

## Push

- Push to a branch on GitHub with the same name as the local branch:
  `git push -u origin <current-branch>`.
- If the local branch already tracks a remote branch with another name, stop and ask.
- Never force push, and never push to main.

## Create the pull request

- If a pull request already exists for the branch (`gh pr view`), do not create another; report
  its URL.
- Create it with `gh pr create --base main --head <current-branch> --title "<title>" --body-file <file>`.
  Write the body to a file in the scratchpad directory first.
- Title: Conventional Commits format. With a single commit, use its message; with several, a
  summary of the whole branch in the same format.
- Body: short and to the point, only what matters in the branch. No long explanations.
  Everything under a heading is a bullet list. Use these headings, in this order:

  | Heading | When |
  | --- | --- |
  | `## Summary` | Always. What changed in the branch. |
  | `## Verification` | Always. How the change was verified: build, app started on emulator, manual checks. |
  | `## Tests` | Only if tests were run: each test task and its result. Leave the heading out otherwise. |
  | `## Notes` | Only if something important is not covered above, such as something that could not be verified or needs follow-up. Leave the heading out otherwise. |

Example:

```markdown
## Summary
- Add Hilt, Room 3, Retrofit and Coil dependencies
- Move the theme to core/ui/theme with the app's color schemes

## Verification
- `./gradlew assembleDebug` succeeds
- The app starts on the Pixel_7 emulator

## Tests
- `./gradlew test`: passed
```

## Report

Answer the developer in Swedish with:

- the branch and the commits that were pushed,
- the pull request URL,
- anything that stopped or changed the steps above.
