Tests are part of every change in this project. Follow the rules below whenever you write or change code.

## Tests are written together with the code

- Every new class, function, composable, screen or feature is delivered together with its tests, in the same change. Never write code first and leave tests for later.
- Nothing with behavior is left untested. The only exceptions are code without behavior of its own: Hilt modules, theme color and typography definitions, data classes without logic, previews, and generated code. These are covered through the tests of the code that uses them.
- A change is not finished until its tests exist and pass.

## Changing existing code

- Before changing code, find its existing tests (search the test source sets for the class and function names).
- When behavior changes, update the existing tests in the same change so they describe the new behavior, and add tests for anything new.
- Remove a test only when the behavior it tests is removed.
- Never make a failing test pass by weakening an assertion, deleting the test, or adding @Ignore. Decide whether the code or the test is wrong. If the test describes the intended behavior, fix the code.
- When fixing a bug, first write a test that reproduces it and fails, then fix the code so it passes.
- If an intended change makes existing tests obsolete or contradictory, say which tests and why in your summary.

## What to test, per layer

| Code | Test type | Source set | What to verify |
| --- | --- | --- | --- |
| ViewModel | Unit | test | Initial state, every state (Loading, Success, empty Success, Error), every public function, messages and flags set and consumed |
| UiState getters, conversions, extension functions | Unit | test | Every branch and edge case (empty, null, boundaries) |
| Repository implementation (Room) | Instrumented, in-memory database | androidTest | Every function, entity-to-model conversion, Flow emits after writes, sorting, files deleted with their item |
| DAO | Instrumented, in-memory database | androidTest | Every query, including empty tables and edge cases |
| Stateless Screen | Compose UI | androidTest | Each UiState renders correctly, and each lambda is called by its control |
| Shared component in core/ui | Compose UI | androidTest | Rendering of its parameters, and each lambda |
| Retrofit API and network models | Unit, MockWebServer | test | Requests have the right path and query, responses parse into network models, error status codes surface as HttpException |
| Repository implementation using the network | Unit, fake API and fake DAO | test | Network models convert correctly, saved items come from Room and never from the network, unsaved items are fetched, exceptions propagate |
| File store (e.g. image files) | Unit, temporary folder | test | Files are written, read and deleted, and a failed download still saves the item without a file |
| Feature navigation file and app-level navigation | Instrumented, TestNavHostController | androidTest | navigateTo functions reach the right destination with the right arguments, and the back stack is correct after popUpTo, popToTopLevel and tab switches |

A repository that uses both Room and the network gets both tests: the unit test (FakeXApi and FakeXDao) covers its logic, conversions and exceptions, and the instrumented test (in-memory database) covers what depends on real Room behavior.

A new feature therefore normally arrives with: a ViewModel test per screen, a UI test per screen, a navigation test for its navigation file, and repository and DAO tests for any new data code.

## How to write tests

- Test classes mirror the package of the code under test: `feature/items/ItemListViewModel.kt` is tested by `feature/items/ItemListViewModelTest.kt`.
- Test names are sentences in backticks that state the behavior: `` fun `toggleRead flips isRead of the matching item`() ``.
- Exception: tests in androidTest use the same sentence with underscores instead of spaces: `fun toggleRead_flips_isRead_of_the_matching_item()`. D8 rejects spaces in method and class names when minSdk is below 30, so backtick names with spaces fail the androidTest build.
- One behavior per test, structured as given, when, then.
- Tests are deterministic: no real network, no Thread.sleep, no dependence on the current date or time unless it is passed in.

### ViewModels

- Construct the ViewModel directly with fakes. Do not use Hilt in unit tests. For assisted injection, pass the argument to the constructor.
- Fakes implement the repository interface with in-memory data (Fake<X>Repository), live in src/test under core/testing/, and can be told to throw so error paths can be tested. Never use the real repository or a mocking framework for repositories.
- Use kotlinx-coroutines-test: runTest, and a MainDispatcherRule that sets Dispatchers.Main to a test dispatcher.
- Use Turbine to assert StateFlow emissions in order, including the initial Loading.
- For debounced searches, control time with the test dispatcher (advanceTimeBy, advanceUntilIdle). Never wait in real time.

### Network

- Test Retrofit interfaces against MockWebServer with canned JSON responses stored in src/test/resources. Build the Retrofit instance in the test the same way NetworkModule does.
- Cover a successful response, a response with missing optional fields, an error status code, and a malformed body.
- Fake APIs (FakeXApi) implement the Retrofit interface in memory and can be told to throw IOException or HttpException. Use them in repository tests.
- Inject a test dispatcher wherever an @IoDispatcher is used.

### Room

- Use Room.inMemoryDatabaseBuilder in androidTest. Create the database before each test and close it after.

### Compose

- Test the stateless Screen with createComposeRule, passing a UiState and recording lambda calls. Do not test the Route.
- Use the v2 test rules from androidx.compose.ui.test.junit4.v2 (createComposeRule, createAndroidComposeRule), not the deprecated ones in androidx.compose.ui.test.junit4. The v2 rules queue coroutines instead of running them immediately, so synchronize with waitForIdle, waitUntil or mainClock, never by waiting in real time.
- Find nodes by visible text or contentDescription. Use testTag only when there is no other stable way.

### Navigation

- Use TestNavHostController with the app's NavHost and assert on currentBackStack and currentDestination.

### Hilt in instrumented tests

Unit tests never use Hilt. Instrumented tests that need Hilt ViewModels, such as app-level navigation tests, use this setup:

- HiltTestRunner (in androidTest, root package) is the testInstrumentationRunner and starts HiltTestApplication.
- HiltTestActivity is an empty @AndroidEntryPoint activity in src/debug, used with the v2 createAndroidComposeRule<HiltTestActivity>().
- The test class is annotated with @HiltAndroidTest and declares HiltAndroidRule with `order = 0` before the Compose rule with `order = 1`.
- TestXModule in androidTest under core/testing/ uses @TestInstallIn to replace the data layer's module and binds the repository interface to FakeXRepository.
- Fakes for instrumented tests live in src/androidTest under core/testing/, separate from the fakes in src/test.

## Dependencies

Add through the version catalog, using the latest stable versions: kotlinx-coroutines-test, Turbine and OkHttp MockWebServer (testImplementation), androidx.navigation:navigation-testing (androidTestImplementation). JUnit, Compose UI test and the Android test runner come from the template.

## Coverage

`./gradlew verifyUnitTestCoverage` checks the debug unit test coverage of each layer with JaCoCo. The layers and thresholds are defined in `coverageLayers` in app/build.gradle.kts:

| Layer | Classes | Lines | Branches |
| --- | --- | --- | --- |
| ViewModel | feature/*/*ViewModel* | 95 % | 80 % |
| UiState | feature/*/*UiState* | 95 % | 90 % |
| Data | core/data/repository, core/data/storage | 90 % | 85 % |
| Network | core/network | 80 % | – |

- Screens, core/ui, navigation and Room are tested in androidTest and have no unit coverage threshold. Code generated by Hilt and kotlinx.serialization is excluded.
- Never lower a threshold, or exclude code from a layer, to make the check pass. Add the missing tests instead.
- Raise a threshold in the same change that adds the tests reaching it, and update this table.

## Running tests

- After every change, run the checks in .claude/skills/verify/SKILL.md. It decides when the instrumented tests are needed and what to say when no device is available.
- Report which test tasks you ran and their result.
- Instrumented tests find nodes by English text, so run them on a device or emulator with English as the system language. Tests of translated resources load them for their locale explicitly with createConfigurationContext, so they do not depend on the device language.

## Before you finish

Verify: every new or changed piece of behavior has a test, existing tests for changed code were updated, no test was weakened, ignored or deleted to make it pass, and all tests you could run pass. List any code you left without tests and why; this list should normally be empty.