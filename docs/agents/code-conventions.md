Write all Kotlin and Compose code in this project according to the conventions below. They build on the official Kotlin coding conventions; where they are stricter, follow these.

## General

- Code, identifiers, comments and commit messages are in English.
- Indent with 4 spaces. Keep lines under 100 characters.
- Use trailing commas in every multi-line parameter or argument list.
- No unused imports, no commented-out code, no TODO() left in finished code.
- Comments explain why, not what. Use KDoc (/** */) on public declarations whose purpose is not obvious from the name.

## Naming

- Classes, objects and composables that emit UI: PascalCase. Functions, properties and composables that return a value (remember..., stringResource): camelCase. Constants: UPPER_SNAKE_CASE. Packages: lowercase, no underscores.
- A private mutable state holder is prefixed with an underscore and paired with a read-only public one: `_uiState` / `uiState`.
- Event lambdas start with `on` and describe what happened from the component's point of view, never where to go: onOpenItem, onSaved, onDeleted, onValueChange. Lambdas that lead to navigation follow the Screens rules in navigation.md.
- Name a parameter after its role. Use the short role (`repository`, `database`, `context`) when the type is right next to it and there is only one; use the full name (`itemRepository`) when several of the same kind exist.

## Values and types

- Prefer val over var. Use var only when the value is actually reassigned.
- Prefer immutable types: listOf over mutableListOf, data classes with val properties. Change data by creating a new value with copy(), map or filter.
- Omit types that are obvious from the right-hand side for local and private declarations. Write them out on public properties, public function return types, and when the right-hand side does not make the type obvious.
- Use a nullable type only where null is a real possible value.
- Never use !!. Use ?., ?:, let and smart casts.
- Use const val for strings that no tool checks for you (preference keys, file names, event names). Do not extract SQL table names in Room queries into constants; Room verifies them at build time.

## Functions and expressions

- Use an expression body (`= ...`) for functions that are a single expression.
- Use if and when as expressions. Never write `if (condition) true else false`; write `condition`.
- Use when without else on enums and sealed types so the compiler enforces exhaustiveness.
- Use extension functions for conversions between types (XEntity.toItem()), with the narrowest visibility that works.
- Visibility: private by default, internal when other files in the module need it, public only when required.

## Arguments and lambdas

- Name arguments when the value alone does not say what it means: bare literals (true, 0, ""), several arguments of the same type, or expressions such as `epochDay = date.toEpochDay()`. Do not name a single argument whose variable already says it: `navigate(ItemDestination(itemId))`.
- In Compose calls, always name arguments.
- Use `it` only in short, non-nested lambdas. Name the parameter in nested or multi-line lambdas, and never have two `it` in scope.
- Name the parameters of a function type when the type alone is unclear: `onEdit: (date: LocalDate, itemId: Long) -> Unit`.
- Pass a function reference when the signature matches exactly (`viewModel::save`); otherwise use a lambda.

## Coroutines and Flow

- One-shot work is a suspend function; streams of values are Flow.
- Launch work from a ViewModel with viewModelScope.launch. Never use GlobalScope.
- Read a Flow in Compose with collectAsStateWithLifecycle().
- Update StateFlow with update { } when the new value depends on the old one; assign value directly only when it replaces it entirely.

## Compose

- Every composable that emits UI takes `modifier: Modifier = Modifier` as its first optional parameter and applies it only to its outermost element. Inner elements start a new `Modifier` chain.
- Parameter order: required parameters, modifier, other optional parameters, and a trailing content lambda last.
- Hoist state: a component takes a value and an onValueChange-style lambda instead of owning state. Use remember only for UI-only state, rememberSaveable when it must survive recreation, and never store values that can be derived.
- Modifier order matters and is read outside-in: padding before background acts as a margin, clip comes before background and clickable.
- Use spacing through Arrangement.spacedBy and contentPadding rather than Spacer between every item.
- Use Material 3 (androidx.compose.material3) only, never androidx.compose.material.
- Never hardcode colors, text styles or user-facing text. Use MaterialTheme.colorScheme roles (always with their on* pair), MaterialTheme.typography, and stringResource.
- Interactive icons need a contentDescription; decorative icons, or icons next to a text label, use null.
- Give LazyColumn and LazyRow items a stable key.
- Never trigger side effects such as navigation directly in composition; use LaunchedEffect keyed on the state that drives it.
- Previews are private, wrapped in the app theme, preview the stateless Screen (never the Route), and cover light and dark mode plus relevant states such as empty, error and long text.
- Check imports: Modifier is androidx.compose.ui.Modifier, Color is androidx.compose.ui.graphics.Color.

## Before you finish

Verify the code against every rule above. Where a rule has no clear answer for a case, choose the more readable option and mention the choice. If following a convention would make the code worse in a specific case, say so instead of silently breaking it.