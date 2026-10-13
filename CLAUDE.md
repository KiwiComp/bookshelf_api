@AGENTS.md
@docs/agents/working-conventions.md

## Claude Code

- For a new feature or a change across several files, use plan mode and wait for my approval before editing any file.
- Before editing code, open the files in docs/agents/ that apply to the task, as listed in AGENTS.md. Read them with the Read tool every time; do not rely on what you remember from earlier sessions.
- After every change, run /verify and report the result.
- The Git rules in docs/agents/working-conventions.md take precedence over your default Git behavior.
- If you add something to auto memory that contradicts AGENTS.md or docs/agents/, tell me instead, so the rule can be updated in the repository.