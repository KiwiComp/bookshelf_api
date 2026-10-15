You are working in a single-module Android app built with Kotlin, Jetpack Compose, Hilt, Room and Retrofit. Follow the package structure below exactly. Do not invent other top-level packages or layers.

## Repository layout

    <project root>/
    ├── app/                the Android app module
    ├── gradle/             version catalog (libs.versions.toml) and wrapper
    ├── docs/agents/        the rule files referenced from AGENTS.md
    ├── AGENTS.md           instructions for coding agents
    └── CLAUDE.md           imports AGENTS.md for Claude Code

- Do not move or rename AGENTS.md, CLAUDE.md or the files in docs/agents/.
- Do not edit them unless asked. If a rule seems outdated or wrong, say so.

## Package layout

    com.example.app/
    ├── core/
    │   ├── model/              app models shared by all layers except database and network (Item.kt)
    │   ├── data/
    │   │   ├── repository/     repository interfaces and their implementations
    │   │   ├── storage/        classes that read and write files in internal storage
    │   │   └── di/             Hilt modules binding repositories and providing dispatchers
    │   ├── database/
    │   │   ├── dao/            Room DAOs (ItemDao.kt)
    │   │   ├── entity/         Room entities (ItemEntity.kt)
    │   │   ├── di/             Hilt module providing the database and DAOs
    │   │   └── AppDatabase.kt
    │   ├── network/
    │   │   ├── api/            Retrofit interfaces (ItemApi.kt)
    │   │   ├── model/          network models (NetworkItem.kt)
    │   │   └── di/             Hilt module providing Json, OkHttpClient, Retrofit and the APIs
    │   └── ui/
    │       ├── theme/          Color.kt, Theme.kt, Type.kt
    │       └── components/     composables shared by several features
    ├── feature/
    │   └── <name>/
    │       ├── navigation/     the feature's navigation file
    │       ├── XScreen.kt      XRoute + XScreen
    │       ├── XViewModel.kt
    │       └── XUiState.kt
    ├── navigation/             app-level navigation (see the navigation prompt)
    ├── <App>Application.kt     @HiltAndroidApp
    └── MainActivity.kt         @AndroidEntryPoint

- A feature with several screens gets one subpackage per screen (feature/<name>/<screen>/), sharing feature/<name>/navigation/.
- Create a package when its first file is added. Never create empty packages. An app without a remote API has no core/network.

## Dependency rules

Dependencies go one way only:

    navigation -> feature -> core.data -> core.database
                                       -> core.network
    navigation, feature -> core.ui
    feature, core.data, core.ui -> core.model

- core.model knows nothing about any other package. It contains only data classes.
- core.database and core.network know nothing about core.model, core.data, core.ui, features, or each other.
- core.data may import core.model, core.database and core.network, never core.ui, feature or navigation.
- core.ui may import core.model only, never core.data, core.database, core.network, feature or navigation.
- A feature never imports from another feature, from navigation, from core.database or from core.network.
- ViewModels depend on repository interfaces only, never on implementations, DAOs, APIs, entities or network models.

## Layers

### core.model

- App models are plain data classes with val properties. They are the app's shared language: repositories return them, ViewModels and composables use them.
- No Room or serialization annotations, no Android framework types, no logic beyond simple derived getters and conversions between app models (e.g. Item.toSummary()).

### core.database

- One entity per table, named XEntity, with @Entity(tableName = "...").
- When data comes from an external source, the primary key is the source's id without autoGenerate. Use autoGenerate only for data created in the app.
- One DAO per table, named XDao. DAO functions use database language: insert, update, delete, observeAll, observeById.
- DAO functions are suspend, except those returning Flow.
- AppDatabase lists the entities and exposes one function per DAO.
- database/di contains an object module with @Provides: the database as @Singleton, and one function per DAO.

### core.network

- Retrofit interfaces, @Serializable network models named NetworkX, and the module that provides them. Details are in the network rules.

### core.data

- Each repository is an interface (XRepository) with one or more implementations named after their source (OfflineFirstXRepository, RoomXRepository, InMemoryXRepository).
- Repository functions use app language: addItem, markAllAsRead, observeItems.
- Reads that change over time are exposed as Flow of core.model types; one-shot reads and writes are suspend functions.
- Conversions between entities, network models and app models are private extension functions in the repository implementation (XEntity.toItem(), NetworkX.toItem(), Item.toEntity()). Entities and network models never leave the data layer.
- data/storage contains classes that read and write files in internal storage, injected into repositories.
- data/di contains an abstract class module with @Binds mapping each interface to its implementation, and an object module providing an @IoDispatcher CoroutineDispatcher.

### core.ui

- Theme and shared composables only. No state, no ViewModels, no data access.
- Shared components may take core.model types as parameters (e.g. `ItemCard(item: ItemSummary, ...)`).

### feature

- Organized by feature, not by type. Each screen keeps its Route, Screen, ViewModel and UiState together.
- ViewModels are @HiltViewModel, receive repositories through @Inject constructor (or @AssistedInject when they need a navigation argument), and expose state as StateFlow<XUiState>. Use mutableStateOf only for text field input.
- UiState is a data class with val properties and default values, or a sealed interface for screens that load data. Values that can be derived are getters, not stored fields.

## Where shared code goes

- Used by one composable: private in the same file.
- Used by several parts of one screen: the screen's package.
- Used by several features: core/ui/components.
- Move code up only when a second user appears.

## Hilt

- Each layer owns its Hilt module in its own di/ package. There is no top-level di/ package.
- All modules are @InstallIn(SingletonComponent::class).
- Bind interfaces with @Binds; construct third-party objects (Room, Retrofit, OkHttp) with @Provides.

## Build

- Dependencies are declared in gradle/libs.versions.toml and referenced through the version catalog.
- Code generators (Hilt, Room) use ksp(...), not implementation(...).
- User-facing text lives in res/values/strings.xml, with translations in res/values-<lang>/strings.xml. A new string is added to every language in the same change; lint's MissingTranslation check fails the build otherwise.

## Before you finish

Verify: every file is in the package described above, no import breaks the dependency rules, no entity or DAO is referenced outside core.database and the repository implementations, no network model or API is referenced outside core.network and the repository implementations, no ViewModel references a repository implementation, core.model has no imports from the app, and no empty packages exist. If a change would break any rule above, say so instead of working around it.