This app reads data from a remote API and lets the user save selected items locally. Follow the rules below for everything that touches the network, the models that come from it, and images.

## Libraries

- Retrofit with the kotlinx.serialization converter, on top of OkHttp. Do not add Ktor, Moshi or Gson.
- Coil for images (coil-compose with coil-network-okhttp).
- The base URL comes from BuildConfig.API_BASE_URL. Never hardcode it in Kotlin code, and never put secrets in source or in BuildConfig fields that are committed.
- The INTERNET permission is declared in AndroidManifest.xml.

## Package layout

    core/network/
    ├── api/        Retrofit interfaces (XApi.kt)
    ├── model/      network models (NetworkXSummary.kt, NetworkX.kt)
    └── di/         NetworkModule

- core.network knows nothing about core.model, core.data, core.database, core.ui or features. Only core.data uses it.
- NetworkModule is an object module with @Provides for a single Json instance (ignoreUnknownKeys = true), the OkHttpClient, Retrofit and each API interface, all @Singleton.

## API interfaces

- One suspend function per endpoint, returning the network model directly. Use Response<T> only when the status code itself is needed.
- Parameters use @Query and @Path. Never build URLs by string concatenation.
- Image downloads use a separate function: `@Streaming @GET suspend fun downloadFile(@Url url: String): ResponseBody`.

## Models

- Network models are @Serializable data classes named NetworkX that mirror the API response exactly. Use @SerialName for API field names. Fields the API may omit are nullable or have defaults.
- Network models never leave core.network and core.data. The data layer converts them to core.model types with private extension functions (NetworkX.toX()).
- When a list endpoint and a detail endpoint return different shapes, use two app models: XSummary with only the fields shown in lists, and X with everything shown on the detail screen. Do not merge them into one model with nullable fields.
- Only the full model X is stored in Room. XSummary is never stored.
- The API's id is the identity everywhere: in core.model, in navigation destinations, and as the Room primary key without autoGenerate. Use the API's id type (often String).

## Repository pattern: search remotely, open by id, save locally

The repository exposes:

- `suspend fun search(query: String, page: Int): SearchResults`: always from the network. Never cached. `page` starts at 1. SearchResults is a core.model class with the page's `List<XSummary>`, the total number of matches, the page and the number of pages. The repository computes the number of pages from the page size, so the page size never leaves the data layer. Without paging, `search(query: String): List<XSummary>` is enough.
- `fun observeX(id: String): Flow<X>`: from Room if the item is saved, otherwise fetched once from the network. Saved items are never refreshed from the network.
- `fun observeIsSaved(id: String): Flow<Boolean>`
- `fun observeSaved(): Flow<List<X>>`: from Room only.
- `suspend fun save(item: X)` and `suspend fun remove(id: String)`.

Screens:

- A list screen calls an onOpenX(id) lambda. It never fetches details itself.
- The detail destination carries only the id. The detail ViewModel receives it through assisted injection and observes observeX(id) and observeIsSaved(id).
- The same detail screen and destination are used for search results and saved items.
- Lists of saved items convert X to XSummary (X.toSummary() in core.model) so search results and saved items share one list component.

## Images

- Models hold the image as a String that is either a URL or a local file path. Composables pass it to Coil's AsyncImage and never check which it is. Always set a placeholder and an error image.
- When an item is saved, its image is downloaded and written to the app's internal storage: `filesDir/<folder>/<id>.<extension>`. Never use cacheDir or external storage.
- The file is handled by a store class in core/data/storage/ (e.g. XImageStore) with suspend functions to save and delete a file. The entity stores the local path.
- If the download fails, the item is saved anyway without an image, and the UI shows the placeholder. Saving never fails because of an image.
- When an item is removed, its image file is deleted in the same operation.
- File I/O runs on an IO dispatcher injected through Hilt with an @IoDispatcher qualifier, defined in core/data/di. Never reference Dispatchers.IO directly inside a class.

## Errors and offline behavior

- Repositories and the network layer let exceptions propagate. Do not hide retries or fallbacks in them.
- ViewModels catch specific exceptions and map each to its own error: IOException means no connection, HttpException means a server error, SerializationException means an unexpected response. Follow the state and error rules for how they are shown.
- Features that need the network (search, details of unsaved items) show an error with a retry action when offline. Never show stale or invented data instead.
- Everything about saved items, including their images, works without a network connection.

## Before you finish

Verify: no network model is used outside core.network and core.data, no URL is hardcoded, saved items and their images open with the device offline, removing an item deletes its image file, a failed image download still saves the item, and every network error path shows a message with a way forward. If the API does not fit these rules, say so instead of inventing a new pattern.