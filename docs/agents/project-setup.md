You are setting up a new Android project that will follow the structure prompt and the navigation prompt. Your job is the project setup only: build configuration, dependencies, app entry points, theme location and Git. Do not build features or screens.

## Check the current state first

- Inspect the project before changing anything, and skip every step that is already done.
- Never change the package name, applicationId or minimum SDK that are already set. If they break a rule below, mention it instead.
- If Git is already initialized or the first commit exists, continue from there. Never re-initialize or rewrite history.
- List what you found already in place and what you changed.

## Starting point

- The project is created in Android Studio from the "Empty Activity" template (Compose), with Kotlin DSL build files and a version catalog.
- If the minimum SDK has not been chosen yet, use 26.
- The package name is lowercase with no underscores.
- Android Gradle Plugin 9 or later has Kotlin built in. Do not add the org.jetbrains.kotlin.android plugin.

## Versions

- Use the latest stable version of every library and plugin. Never invent a version number; if you cannot verify one, say so and leave a clear placeholder.
- Keep the versions the template already generated. Do not downgrade anything.
- Libraries released together share one version entry: all androidx.lifecycle artifacts reuse the template's lifecycle version, and the Kotlin serialization plugin uses the same version.ref as the Kotlin Compose plugin.
- The KSP version must be compatible with the project's Kotlin version.

## Dependencies

Add only what the app needs. The full set for an app with navigation, Hilt, Room and a remote API is:

    [versions]
    navigation = "<latest>"
    hilt = "<latest>"
    androidxHilt = "<latest>"
    ksp = "<latest>"
    room = "<latest>"
    retrofit = "<latest>"
    okhttp = "<latest>"
    kotlinxSerializationJson = "<latest>"
    coil = "<latest>"

    [libraries]
    androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
    androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "<template lifecycle ref>" }
    androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "<template lifecycle ref>" }
    hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
    hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
    androidx-hilt-lifecycle-viewmodel-compose = { group = "androidx.hilt", name = "hilt-lifecycle-viewmodel-compose", version.ref = "androidxHilt" }
    androidx-room3-runtime = { group = "androidx.room3", name = "room3-runtime", version.ref = "room" }
    androidx-room3-compiler = { group = "androidx.room3", name = "room3-compiler", version.ref = "room" }
    retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
    retrofit-kotlinx-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
    okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
    kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
    coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
    coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }

    [plugins]
    kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "<kotlin ref>" }
    ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
    hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }

- Every plugin is declared in the root build.gradle.kts with `apply false` and applied in app/build.gradle.kts.
- Code generators use ksp(...), never kapt or implementation: `ksp(libs.hilt.compiler)`, `ksp(libs.androidx.room3.compiler)`.
- In the dependencies block, keep a library and its ksp line together as a group, after the template's implementation lines and before the test dependencies.
- Use Room 3 (androidx.room3) for new projects. Use Room 2 (androidx.room) only if told to.
- For an app with a remote API, also: declare the INTERNET permission in AndroidManifest.xml, enable `buildFeatures { buildConfig = true }` in app/build.gradle.kts, and add `buildConfigField("String", "API_BASE_URL", "\"<base url>\"")` in defaultConfig. Never put API keys or other secrets in committed files; if the API needs one, read it from local.properties and ask me first.
- Do not add the network and image libraries to an app without a remote API.
- Sync and make sure the project builds before continuing.

## Entry points

- Create `<App>Application : Application()` in the root package, annotated with @HiltAndroidApp, and register it with `android:name=".<App>Application"` on the <application> tag in AndroidManifest.xml.
- MainActivity is annotated with @AndroidEntryPoint, calls enableEdgeToEdge(), and its setContent contains only the app theme and the app's root composable.
- Remove the template's sample code (Greeting, its preview and any other placeholder content).

## Theme

- Move the template's theme files to core/ui/theme and update their package.
- Rename the theme function to <App>Theme.
- Set dynamicColor to false by default and define the app's own light and dark color schemes.
- The app name lives in res/values/strings.xml as app_name.

## Git

- Keep the template's .gitignore and replace the individual /.idea/ entries with a single `.idea/`.
- Only add ignore rules for files that actually appear in `git status`. Signing keys and their passwords are never committed.
- If the repository has no commits yet, make the first commit on main: `Initial project setup`. Otherwise commit the setup changes on a branch named refactor/project-setup.
- After that, every change is made on its own branch named after what it adds (feature/<name>, refactor/<name>, fix/<name>), with Conventional Commits messages (`feat: ...`, `refactor: ...`, `fix: ...`).

## Before you finish

Verify: Gradle sync succeeds, the app builds and starts on an emulator, no unused dependencies or plugins were added, `git status` shows no build/, .gradle/, .idea/ or local.properties files, and no template sample code remains. If a version could not be verified or a step failed, say so instead of guessing.