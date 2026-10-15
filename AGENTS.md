# AGENTS.md

Instructions for AI coding agents working in this repository. Read this file in full before every task.

Read docs/roadmap.md to see the current step. When a task completes a step, update its status in the same branch.

## About the app

**Name:** Bookshelf (package com.example.bookshelfapi)

**Purpose:** A reading list app. The user searches for books through the Open Library API, opens a book to see its details, and saves the books they want to keep track of. Saved books, including their cover images, are available offline, and the user can mark them as read.

**Languages:** English and Swedish. The app follows the device language; English is the default for every other language. English text lives in res/values/strings.xml and Swedish text in res/values-sv/strings.xml. Every string except app_name is translated. Counts use plurals resources in both languages ("1 result" / "21 results", "1 bok" / "8 böcker"). The texts in this file and in the design images are the English reference; the Swedish texts are the ones in values-sv. Swedish text uses the Swedish quotation marks ”…” around {query}, {text} and {title}.

**Screens:**

| Screen | Kind | Opened from |
| --- | --- | --- |
| Search | Tab (start destination) | Bottom bar |
| Bookshelf | Tab | Bottom bar |
| Book detail | Pushed screen (full screen, no bottom bar) | Search, Bookshelf |

Every screen uses a large title at the top instead of a top app bar. Book detail has only a back arrow at the top. The design images in docs/design/ are the reference for layout and text; the states and texts are also listed below.

### Search

- Title "Search books", label "Search the book database", search field with placeholder "Title or author" and a clear button when it has text.
- A count above the list: "21 results", "0 results".
- Rows: cover, title, author, year. Tapping a row opens Book detail. Rows do not show whether a book is saved, and books cannot be saved from this list.
- Page controls as the last item of the list (see Search paging).

| State | Content | Image |
| --- | --- | --- |
| Idle (no query) | Book icon, "Find a book", "Search the book database by title or author. Open a book to save it to your bookshelf." | search-idle.png |
| Loading | Skeleton rows matching the result rows. No count line. | search-loading.png |
| Results | Count, rows, page controls | search-results.png |
| No results | Count "0 results", search icon, "No books found", "Nothing matches “{query}”. Check the spelling, or try searching for the author instead." | search-no-results.png |
| No connection | Wifi-off icon, "Couldn’t load results", "Check your internet connection and try again.", button "Try again" | search-error-connection.png |
| Server error | Server icon, "Search isn’t working right now", "The book database isn’t responding. Try again in a moment.", button "Try again" | search-error-server.png |

### Bookshelf

- Title "My bookshelf", summary under the title: "8 books · 3 read".
- Label "Filter your bookshelf", filter field, and a segmented control choosing Title or Author. The placeholder follows the option: "Type a title" or "Type an author".
- Rows: cover, title, author, year, and a read toggle button ("Read" / "Not read"). Tapping the rest of the row opens Book detail.

| State | Content | Image |
| --- | --- | --- |
| Books | Summary, filter, rows in alphabetical order | bookshelf.png |
| Loading | Skeleton for the summary, the filter controls and the rows, shown immediately | bookshelf-loading.png |
| Empty (no saved books) | Summary "0 books · 0 read", no filter controls, bookshelf icon, "Your bookshelf is empty", "Search for a book, open it and save it to your bookshelf.", button "Search books" | bookshelf-empty.png |
| No matches | Summary and filter stay, filter icon, "No matches", "None of your books have a title matching “{text}”." or "…an author matching…" depending on the option, button "Clear filter" | bookshelf-no-matches.png |
| Error | Alert icon, "Something went wrong", "Your bookshelf couldn’t be loaded.", button "Try again" | bookshelf-error.png |

- "Search books" switches to the Search tab: the screen calls an onSearchBooks lambda, and the NavHost connects it to navigateToTopLevel(Search).
- "Clear filter" empties the filter text and keeps the selected option.
- The error state only happens if Room cannot be read. It never mentions the network.

### Book detail

- Back arrow, large cover, title, author, then year and page count on one line: "1891 · 412 pages".
- Then "About the book" with the summary.

| State | Content | Image |
| --- | --- | --- |
| Not saved | Button "Save to bookshelf" with the hint "The book will be saved as not read." | detail-unsaved.png |
| Saving | The same button, disabled, showing a progress indicator and "Saving…" | detail-saving.png |
| Saved | Label "Status on your bookshelf", segmented control "Not read" / "Read", and a text button "Remove from bookshelf" | detail-saved.png |
| Loading | Skeleton following the not-saved layout | detail-loading.png |
| Not found | Book icon, "Book not found", "This book is no longer available in the book database.", button "Back to search" | detail-not-found.png |
| No connection | Wifi-off icon, "Couldn’t load the book", "Check your internet connection and try again.", button "Try again" | detail-error-connection.png |
| Server error | Server icon, "Couldn’t get this book", "The book database isn’t responding. Try again in a moment.", button "Try again" | detail-error-server.png |
| Remove dialog | Trash icon, "Remove book?", "“{title}” will be removed from your bookshelf, along with its read status. You can save it again from search.", buttons "Cancel" and "Remove" | detail-remove-dialog.png |
| No cover | The cover placeholder | detail-no-cover.png |

- Loading, Not found and the error states only happen for books that are not saved, since saved books are read from Room.
- "Back to search" does the same as the back arrow: one step back.
- Removing the book leaves the screen only when it was opened from Bookshelf (see Removing a book).
- After saving succeeds, the screen shows the Saved state.

**Flows:** None.

**API (Open Library):**

- Base URL: `https://openlibrary.org/`. No API key. Send a User-Agent header that identifies the app.
- Search: `GET search.json?q={query}&fields=key,title,author_name,first_publish_year,cover_i&limit=20&page={page}`. The response has `num_found` and a `docs` list.
- Details are built from two requests made concurrently in the repository and combined into one Book:
  - `GET works/{bookId}.json` for title, description and covers. `description` is either a string or an object with a `value` field; handle both.
  - `GET search.json?q=key:/works/{bookId}&fields=key,author_name,first_publish_year,number_of_pages_median,cover_i&limit=1` for author, year and page count.
- Covers: `https://covers.openlibrary.org/b/id/{coverId}-M.jpg` in lists and `-L.jpg` on the detail screen. The large image is the one downloaded when a book is saved. The covers base URL comes from BuildConfig.COVERS_BASE_URL, the same way as the API base URL.
- Ids: the API returns keys like `/works/OL27448W`. The app's bookId is the part after `/works/`, as a String.
- Example responses: docs/api/search-response.json, docs/api/work-response.json and docs/api/work-search-response.json. Use them for network models and MockWebServer tests.

**Errors:**

| Cause | Shown as |
| --- | --- |
| IOException | No connection |
| HTTP 404 from the details requests | Not found (Book detail only) |
| Any other HttpException, or SerializationException | Server error |
| Room read failure | Bookshelf error state |
| Save, remove or read status change fails | Snackbar |

**Data models:**

| Model | Fields | Stored in |
| --- | --- | --- |
| BookSummary | id: String, title: String, author: String?, year: Int?, thumbnailUrl: String? | Not stored |
| Book | id: String, title: String, author: String?, year: Int?, coverImage: String?, summary: String?, pageCount: Int?, isRead: Boolean | Room (BookEntity), cover downloaded to filesDir/covers/ |

- author is the first name in author_name. year is first_publish_year. pageCount is number_of_pages_median.
- isRead is always false for books that are not saved. A newly saved book is not read.
- BookEntity also stores the local cover path.
- There are no genres or subjects anywhere in the app. Ignore any genre text in the design images.

**Search paging:**

- 20 results per page. The page controls are the last item of the result list: a left arrow for the previous page, "Page 1 of 2", and a right arrow for the next page.
- The left arrow is disabled on the first page and the right arrow on the last page. A new query starts at page 1.
- Changing page shows Loading and replaces the list; it does not append.

**Bookshelf filter and order:**

- The filter matches the text case-insensitively against the title or the author, depending on the selected option.
- The filter text and the selected option are screen state in the ViewModel; they are not saved in Room.
- The summary always counts all saved books, not only the filtered ones.
- Sort by title with a Collator for the device locale, in the repository or ViewModel, not with SQL ORDER BY, so that titles with accented letters, such as å, ä and ö, sort correctly.

**Removing a book:**

- Only from Book detail, through the confirmation dialog. Whether the dialog is shown is UI state held with rememberSaveable in the Route.
- What happens after confirming depends on where Book detail was opened from. BookDetailDestination has `openedFromBookshelf: Boolean = false`: Search opens it with navigateToBookDetail, Bookshelf with navigateToBookDetailFromBookshelf. The ViewModel receives the value through assisted injection.
- Opened from Bookshelf: the ViewModel deletes the book and its cover file, stops observing the book, and sets a flag in UiState. The Route reacts with LaunchedEffect and calls onRemoved, which the NavHost connects to popToTopLevel, so the user returns to Bookshelf.
- Opened from Search: the book still exists in the book database, so the user stays on Book detail. The ViewModel deletes the book and its cover file, and the screen follows the repository, which fetches the book again and shows the Not saved state. Offline, this shows the No connection state. If the book was fetched from the API earlier on the same visit (it was not saved when the screen opened, then saved and removed), the repository shows that fetched book again instead of making a new request.

**Snackbars for temporary errors:**

- Material 3 standard snackbar, shown at the bottom of the screen and above the bottom bar where there is one. Image: snackbars.png.
- Texts: "Couldn’t save the book", "Couldn’t remove the book", "Couldn’t update read status", each with the action "Try again".
- "Try again" re-runs the action that failed. The ViewModel keeps the failed action together with the message so it can be retried.
- The screen always shows what is stored. Saved and read status come from Room, so a failed change leaves the UI in its previous state without extra code: the save button returns to normal, the book stays saved, and the read toggle shows the previous status.

**Design:**

- Colors: use the light and dark color schemes in docs/design/colors.md exactly as written. Do not change, add or generate colors.
- BookshelfTheme follows the system setting: the light scheme in light mode, the dark scheme in dark mode. dynamicColor is off. Only light mode has been designed; build dark mode from the same color roles.
- Typography: Material 3 defaults until a typeface is decided.
- Error icons sit in a circle with errorContainer background and onErrorContainer icon. Empty and idle icons use neutral container roles as in the images.
- Skeletons use surfaceContainerHighest shapes with a subtle pulse, have the same layout as the content they replace, and expose a content description such as "Loading results" for screen readers.
- Cover placeholder (cover-placeholder.png), used wherever a book has no cover: surfaceVariant background, 1 dp outlineVariant border, book icon in onSurfaceVariant. Same size and corner radius as a real cover: 150 × 225 dp on Book detail, 52 × 78 dp in lists.
- The colored covers in the design images are placeholders for real cover images.

**Scope of the current version:**

- In scope: search with paging, book detail, save and remove, mark as read from the list and from detail, Bookshelf with filter by title or author, offline access to saved books and their covers, all states listed above, English and Swedish.
- Out of scope: user accounts, syncing between devices, editing book data, ratings or notes, genres or subjects, other sort orders, showing saved status in search results, saving from the search list, removing from the Bookshelf list, infinite scrolling, languages other than English and Swedish, choosing the app language separately from the device language.

**App-specific notes:**

- Saved books are never refreshed from the API.
- The read status can be changed from the Bookshelf list and from Book detail.

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Navigation Compose (Navigation 2) with type-safe @Serializable routes
- Hilt for dependency injection
- Room 3 (androidx.room3) for local storage
- Retrofit with kotlinx.serialization and OkHttp for the remote API, Coil for images
- Kotlin Coroutines and Flow
- Gradle Kotlin DSL with a version catalog (gradle/libs.versions.toml), KSP for code generation
- Minimum SDK 26, single module (app)

## Commands

    ./gradlew assembleDebug          # build
    ./gradlew test                   # unit tests
    ./gradlew verifyUnitTestCoverage # unit test coverage per layer (JaCoCo)
    ./gradlew connectedAndroidTest   # instrumented tests (needs a device or emulator)
    ./gradlew lint                   # Android lint

After every change, run the checks in .claude/skills/verify/SKILL.md: build, unit tests, coverage, lint, and instrumented tests when androidTest code or the code it covers changed. CI (.github/workflows/ci.yml) runs the same checks except the instrumented tests on every pull request and on pushes to main.

## Detailed rules

The rules for this project live in docs/agents/. Read the files that apply before starting a task. working-conventions.md always applies.

| File | Read when |
| --- | --- |
| docs/agents/working-conventions.md | Always: how to plan, scope, use Git, and report |
| docs/agents/project-setup.md | Setting up the project, or adding or changing dependencies and plugins |
| docs/agents/structure.md | Creating files or packages, or touching more than one layer |
| docs/agents/navigation.md | Adding a screen, or changing how screens are reached or left |
| docs/agents/code-conventions.md | Writing or changing any Kotlin or Compose code |
| docs/agents/state-and-errors.md | Writing a ViewModel or UiState, or handling loading, empty or error states |
| docs/agents/network.md | Calling the API, adding network models, saving items locally, or handling images |
| docs/agents/testing.md | Always: every change comes with tests |
| docs/agents/reviewing.md | Running a review skill (review-conventions, review-ui, review-docs) |

If these files and existing code disagree, point out the difference instead of following either one silently.

## Always

The most important rules, repeated here because they apply to every task:

- Code, comments, commit messages and branch names are in English.
- Package layout: core/model, core/data, core/database, core/network, core/ui, feature/<name>, navigation. Dependencies go one way: navigation -> feature -> core.data -> core.database and core.network, with core.model and core.ui shared. A feature never imports another feature.
- Screens never receive a NavController. They expose events as lambdas, and only the NavHost decides where they lead.
- Every change is delivered with its tests. Update existing tests when behavior changes; never weaken, ignore or delete a test to make it pass.
- For a new feature or a change across several files, propose a short plan and wait for approval.
- One task per branch: feature/<name>, fix/<name>, refactor/<name>, chore/<name>, build/<name>, ci/<name>, test/<name> or docs/<name>. Never commit, push, merge, rebase onto main or force push without asking; leave changes uncommitted for review. Commits, push and pull requests go through the /pull-request skill.
- Ask before adding dependencies, changing the database schema, deleting files, or deviating from the rules.
- Never guess a library version, an API or a behavior. Say what you could not verify.

## Definition of done

The project builds, all tests that could be run pass, the code follows the rules above and in docs/agents/, and you have written a summary: what changed, choices not given by the rules, what to test manually, what could not be verified, and anything outside the task that should be addressed.