---
name: review-ui
description: Review the Compose UI for structure (composables to extract or share) and for how well it matches the design in docs/design/. Use when the developer asks for a UI or design review.
argument-hint: "[structure | design] [feature]"
disable-model-invocation: true
---

# Review UI

Follow docs/agents/reviewing.md for how to review and the report format. Read it with the Read
tool first, together with AGENTS.md, docs/agents/structure.md, docs/agents/code-conventions.md and
docs/agents/state-and-errors.md.

## Arguments

The arguments are: $ARGUMENTS

- No arguments: run both parts on all UI code.
- `structure`: run only the structure part.
- `design`: run only the design part.
- A feature name after the part, or alone, such as `design search` or `search`: limit the review
  to that feature (feature/<name>) and the core/ui code it uses.
- Anything else: stop and list the valid arguments.

If there is no UI code yet (no composables under feature/ or core/ui other than the theme), say so
and stop.

Report the findings of each part under its own heading.

## Part 1: structure

- Composables that are too large to read: a Screen or content function that mixes several states
  or sections should be split into private composables in the same file or package.
- The split between Route and Screen: the Route holds the ViewModel, snackbars and effects; the
  Screen is stateless and takes the UiState and lambdas.
- Hoisted state, `modifier` as the first optional parameter, keys in lazy lists, stable
  parameters, and side effects only in LaunchedEffect.
- Duplicated UI: the same composable or nearly the same code in several features belongs in
  core/ui/components, following "Where shared code goes" in structure.md. Suggest moving a
  composable up only when a second user exists, never in advance.
- Previews for every state of each Screen, light and dark.

## Part 2: design

- Read the design images in docs/design/ for the screens in scope with the Read tool, and
  docs/design/colors.md. Compare the layout, the texts and the states with the code and with the
  screen tables in AGENTS.md.
- Every state listed in AGENTS.md exists and shows the right icon, title, text and button.
- Colors only through MaterialTheme.colorScheme roles with their on* pair; error icons in a circle
  with errorContainer and onErrorContainer; no hardcoded colors.
- All text in strings.xml, counts with plurals resources.
- Cover sizes and the cover placeholder: 150 × 225 dp on Book detail, 52 × 78 dp in lists,
  surfaceVariant background with a 1 dp outlineVariant border.
- Skeletons use surfaceContainerHighest, follow the layout they replace and have a content
  description for screen readers.
- Dark mode works from the same color roles.
- Accessibility: content descriptions on interactive icons, touch targets of at least 48 dp.
- Ignore genre text in the images; genres are out of scope.
