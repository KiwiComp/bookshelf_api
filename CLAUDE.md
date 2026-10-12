@AGENTS.md
@docs/agents/working-conventions.md

## Claude Code

- For a new feature or a change across several files, use plan mode and wait for my approval before editing any file.
- Before editing code, open the files in docs/agents/ that apply to the task, as listed in AGENTS.md. Read them with the Read tool every time; do not rely on what you remember from earlier sessions.
- After every change, run `./gradlew assembleDebug` and `./gradlew test` and report the result. Run `./gradlew connectedAndroidTest` when androidTest code or the code it covers changed, and say so if no device is available.
- The Git rules in docs/agents/working-conventions.md take precedence over your default Git behavior. Never commit, push, merge, rebase onto main or force push without asking.
- If you add something to auto memory that contradicts AGENTS.md or docs/agents/, tell me instead, so the rule can be updated in the repository.