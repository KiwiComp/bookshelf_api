You are working in an Android app built with Kotlin, Jetpack Compose, Navigation Compose (Navigation 2) with type-safe @Serializable routes, and Hilt for dependency injection. Follow the navigation pattern below exactly. Do not introduce other navigation libraries or patterns.

## Structure

- navigation/ (app level): the root composable (Scaffold, bottom bar, NavHost), the NavHost file, TopLevelDestination, and an app-level ViewModel if needed.
- feature/<name>/: the feature's screens, and feature/<name>/navigation/<Name>Navigation.kt.
- core/: code shared between features.
- Dependencies go one way only: navigation -> feature -> core. A feature never imports from another feature or from the app's navigation package.

## Feature navigation file

For every screen, the feature's navigation file contains exactly three things:

1. A destination: `@Serializable data object XDestination`, or `@Serializable data class XDestination(val id: String)` when it takes arguments. The id has the API's id type (see network.md).
2. `fun NavController.navigateToX(...)` that builds the destination, or two of them for a screen with two modes (see Arguments). Only top-level tabs take `navOptions: NavOptions? = null`.
3. `fun NavGraphBuilder.xScreen(...)` that registers `composable<XDestination> { entry -> XRoute(...) }`, reads arguments with `entry.toRoute<XDestination>()`, and passes them to the Route as plain parameters.

The destination class is referenced only in this file (and in TopLevelDestination for tabs). Several screens of the same feature share one file.

## Screens

- Each screen file is named XScreen.kt and contains XRoute (obtains the ViewModel and reacts to events) and XScreen (stateless UI).
- Screens receive data as parameters and events as lambdas. They never receive a NavController and never reference a destination.
- Lambdas describe what the user did, not where to go: onOpenItem, onSaved, onDeleted, onBack. Use past tense for completed actions.
- A lambda carries data only when the next screen needs it. Name the parameters when the type alone is unclear: `onEdit: (date: LocalDate, itemId: String) -> Unit`.
- Routes obtain their ViewModel with hiltViewModel(), never viewModel().
- When a ViewModel needs a navigation argument, use assisted injection: `viewModel: XViewModel = hiltViewModel<XViewModel, XViewModel.Factory> { it.create(id) }`. ViewModels never read arguments from SavedStateHandle and never reference a destination.
- Data work (saving, deleting) happens in the ViewModel, never in navigation code. Events that follow data work are exposed as flags in UiState (e.g. isSaved, isDeleted). The Route reacts with `LaunchedEffect` on those flags and calls the navigation lambda. Never call a navigation lambda directly from a click handler when data work comes first.

## Arguments

- Destinations hold only simple types: Long, Int, String, Boolean. Pass ids, never objects.
- Convert other types in the navigation file, in both directions (e.g. `date.toEpochDay()` in, `LocalDate.ofEpochDay(...)` out).
- One screen with two modes (create/edit): one destination with an optional argument (`val itemId: String? = null`), two navigateTo functions with descriptive names, and the navigation file picks the exit lambda based on the mode.
- When the mode also changes the data work, such as whether removing an item leaves the screen, the navigation file passes the mode to the Route as a plain parameter (e.g. `openedFromList: Boolean = false`), and the ViewModel receives it through assisted injection. The ViewModel decides whether to set the flag that the Route reacts to; the navigation file does not pick the lambda.

## NavHost

- One call per screen: `xScreen(...)`. It is the only place that connects one feature's lambda to another feature's navigateTo function.
- Use a function reference when the signature matches (`onOpenItem = navController::navigateToItemDetail`), otherwise a lambda (`{ navController.popBackStack() }`).

## Tabs

- `enum class TopLevelDestination(label, icon, route: KClass<*>)` lists the tabs in display order.
- Selected tab: `currentDestination?.hierarchy?.any { it.hasRoute(destination.route) } == true`.
- `private fun NavDestination.isTopLevel() = TopLevelDestination.entries.any { hasRoute(it.route) }`.
- Show the bottom bar only on tabs: `if (currentDestination?.isTopLevel() != false)`. Pushed screens are full-screen.
- Every tab switch, including from buttons on screens, goes through:

  internal fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
  popToTopLevel()
  val options = navOptions {
  popUpTo(graph.findStartDestination().id) { saveState = true }
  launchSingleTop = true
  restoreState = true
  }
  when (destination) { /* one line per tab: navigateToX(options) */ }
  }

  internal fun NavHostController.popToTopLevel() {
  while (currentDestination?.isTopLevel() == false) popBackStack()
  }

## Back stack rules

- One step back: `popBackStack()`.
- Task finished, return to the tab it started from: `popToTopLevel()`.
- Move forward and remove the screen being left (flows): `navigate(Next) { popUpTo<Current> { inclusive = true } }`. The popUpTo target is the screen being replaced, never something further down.
- Prevent duplicates on top: `launchSingleTop = true`.
- Put back stack rules inside the navigateTo function, not at the call site.

## App-initiated navigation

For navigation the app starts on its own, such as resuming an unfinished task at startup:

- Use an app-level @HiltViewModel obtained with hiltViewModel() in the root composable, outside NavHost.
- Check once in init with `first()` and expose the result as StateFlow (e.g. an id or null).
- The root reacts with `LaunchedEffect(value)`, navigates, then calls a ViewModel function that resets the value to null so the event is consumed.

## Before you finish

Verify: system back from every screen, rotation on every screen and mid-flow, switching tabs keeps their state and back goes to the start tab, tapping the same tab does not grow the back stack, every flow ends on the tab it started from, and the bottom bar appears only on tabs. If a change would break any rule above, say so instead of working around it.