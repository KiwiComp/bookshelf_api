These rules apply to every review skill in this project (review-conventions, review-ui, review-docs). The skills say what to look for; this file says how to review and how to report.

## Read only

- A review never changes files, creates branches or commits. It only reads and reports.
- Findings the developer wants fixed become separate tasks, each in its own branch.

## Before reviewing

- Read AGENTS.md and the files in docs/agents/ that apply to the code under review, with the Read tool, every time.
- Never suggest something that breaks a rule, such as SQL ORDER BY instead of a Collator, or passing a NavController to a screen. If you think a rule is wrong for the case at hand, report that as a question about the rule, not as a finding in the code.

## Scope

The skill's argument sets the scope:

| Argument | Scope |
| --- | --- |
| none | The whole project |
| `diff` | Changes on the current branch compared with main, including uncommitted changes (`git diff main...HEAD` and `git diff HEAD`) |
| a path or a feature name | That file, package or feature, such as `feature/search` or `search` |

## What to report

- Report only findings that can cause wrong behavior, a crash, data loss or a wrong UI state, that break a rule in AGENTS.md or docs/agents/, or that clearly make the code harder to change.
- Do not report naming, formatting or style that the rules do not cover.
- At most about 10 findings, sorted by severity. If there are more, say how many were left out and of what kind.
- Verify each finding by reading the code it concerns. Mark a finding as uncertain if you could not confirm it.

## Format

For each finding:

- **Severity**: High (wrong behavior, crash or data loss), Medium (breaks a rule, or will cause bugs when the code changes) or Low (harder to read or change).
- **Location**: `file:line` as a clickable link.
- **Problem**: what is wrong, in one or two sentences.
- **Scenario**: a concrete case where it goes wrong, or the rule it breaks.
- **Suggestion**: what to change.
- **Rule**: the rule file and section, if any.

End the report with what could not be checked and why, and, if there are no findings, say so plainly instead of inventing some.
