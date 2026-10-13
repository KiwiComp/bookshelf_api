# Roadmap

Where the project stands. Update the status of a step in the same branch that completes it. Requirements and rules live in AGENTS.md and docs/agents/, not here.

Status: Not started, In progress, Done.

| # | Step | Branch | Content | Status |
| --- | --- | --- | --- | --- |
| 0 | Project setup | refactor/project-setup | Build configuration, dependencies, entry points, theme | Done |
| 1 | Network | feature/network | core/model (BookSummary, Book), core/network (Open Library API, network models, NetworkModule with User-Agent), MockWebServer tests | Done |
| 2 | Database | feature/database | BookEntity, BookDao, AppDatabase, DatabaseModule, core/data/storage for cover files in filesDir/covers/, dispatcher module | Done |
| 3 | Repository | feature/repository | BookRepository: search, details from two concurrent requests, save, remove, read status, Collator sorting | Done |
| 4 | Navigation shell | feature/navigation-shell | TopLevelDestination, Scaffold with bottom bar, NavHost, navigateToTopLevel and popToTopLevel, shared components in core/ui/components | Done |
| 5 | Search | feature/search | Search screen with all states and paging | Not started |
| 6 | Book detail | feature/book-detail | Book detail screen with save, remove dialog and snackbars | Not started |
| 7 | Bookshelf | feature/bookshelf | Bookshelf screen with filter, read toggle and all states | Not started |

## Decisions

- The test dependencies required by docs/agents/testing.md (kotlinx-coroutines-test, Turbine, MockWebServer, navigation-testing) are added in the step that first needs them, not in a separate branch.
- The dispatcher module moved from step 3 to step 2, since CoverImageStore needs the @IoDispatcher.
