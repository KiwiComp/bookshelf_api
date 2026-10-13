---
name: review-docs
description: Check that the project's documentation and skills agree with each other and with the repository, and report where they have drifted apart. Use when the developer asks to review the docs, or after rules or skills have changed.
disable-model-invocation: true
---

# Review docs

Follow docs/agents/reviewing.md for how to review and the report format. Read it with the Read
tool first. This review covers documents, not code: the location of a finding is a document and
line, and the rule column is left out.

## Documents

Read all of these with the Read tool:

- AGENTS.md and CLAUDE.md
- every file in docs/agents/
- docs/roadmap.md
- docs/design/colors.md, and the file names in docs/design/ and docs/api/
- every `.claude/skills/*/SKILL.md`

## What to check

- **Contradictions**: two documents that give different rules for the same thing, such as a
  function signature, a naming pattern, a command or a Git rule. Say which document seems right,
  or that it is unclear.
- **Copies that drifted**: the same rule repeated in several places with different wording or
  content. Suggest keeping it in one place and referring to it from the others.
- **Broken references**: files, images, folders, functions, skills or commands that a document
  mentions but that do not exist in the repository. Check with ls or grep.
- **Roadmap status**: a step marked `Done` whose content is missing on main, or a step whose
  content is on main but is not marked `Done`.
- **Gaps**: something AGENTS.md requires that no rule file describes how to build, when it matters
  for an upcoming roadmap step.

Do not report wording, typos or formatting unless they change the meaning.
