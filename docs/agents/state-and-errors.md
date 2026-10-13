Every screen in this project handles loading, empty and error states the same way. Follow the rules below for all ViewModels, UiState classes and screens.

## Two kinds of errors

- Blocking error: the screen's content could not be loaded, so there is nothing to show. The screen shows an error state with a retry action instead of the content.
- Transient error: an action failed (save, delete, refresh) while content is already shown. The content stays, and a short message is shown in a Snackbar.

Decide which kind an error is before writing code for it.

## UiState

- A screen whose content comes from loading data uses a sealed interface:

      sealed interface ItemListUiState {
          data object Loading : ItemListUiState
          data class Success(val items: List<Item>) : ItemListUiState
          data object Error : ItemListUiState
      }

- Empty is not a separate state. It is Success with an empty list, and the screen shows an empty state for it.
- Values derived from the data are getters on Success, not stored fields.
- Forms, and screens that keep showing content while working, use a data class with val properties and flags such as isSaving instead.
- Never put a Throwable, an exception message or user-facing text in UiState.

## Building UiState from a repository

- Build the state directly from the repository's Flow with stateIn. Do not collect in init and copy values into a MutableStateFlow.

      val uiState: StateFlow<ItemListUiState> = itemRepository.observeItems()
          .map<List<Item>, ItemListUiState> { items -> ItemListUiState.Success(items) }
          .catch { emit(ItemListUiState.Error) }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(5_000),
              initialValue = ItemListUiState.Loading,
          )

- initialValue is Loading. Loading is never a separate flag.
- Always use SharingStarted.WhileSubscribed(5_000).
- When the state also depends on screen-local input (a filter, a search query), keep that input in a private MutableStateFlow and merge it with combine before stateIn.
- Retry for a blocking error re-runs the load: keep a private retry trigger (MutableStateFlow<Int>), build the flow with `retryTrigger.flatMapLatest { itemRepository.observeItems() }`, and expose `fun retry()` that increments it. Add @OptIn(ExperimentalCoroutinesApi::class) if the compiler requires it.

## Transient errors and messages

- Write operations are suspend functions called from viewModelScope.launch, wrapped in try/catch.
- Catch specific exceptions (e.g. IOException, SQLiteException). Never catch Exception or Throwable broadly, and never use runCatching around suspend calls, since that also swallows CancellationException.
- On failure, set a one-time message in a private MutableStateFlow<UserMessage?> exposed as StateFlow. UserMessage is an enum (or sealed interface) describing what happened, e.g. DeleteFailed.
- The ViewModel exposes `fun onMessageShown()` that sets the message back to null.
- Repositories do not catch errors to hide them. They let exceptions propagate, or emit them through the Flow, so the ViewModel decides how to present them.

## Search screens

A screen where the user types a query and results load from the network follows this pattern:

- The UiState has an extra state for before any search: `Idle`, alongside Loading, Success and Error. Success with an empty list means no results, which is different from Idle.
- The query text lives in the ViewModel as `var query by mutableStateOf("")` with a private setter and an onQueryChange function, following the text field rule.
- Results are built from the query with snapshotFlow, debounce (300 ms), distinctUntilChanged and flatMapLatest, so typing waits briefly and a new query cancels the previous request:

      val uiState: StateFlow<SearchUiState> = snapshotFlow { query.trim() }
          .debounce(300)
          .distinctUntilChanged()
          .flatMapLatest { text ->
              if (text.isEmpty()) {
                  flowOf(SearchUiState.Idle)
              } else {
                  flow<SearchUiState> {
                      emit(SearchUiState.Loading)
                      emit(SearchUiState.Success(itemRepository.search(text)))
                  }.catch { exception -> emit(exception.toSearchError()) }
              }
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState.Idle)

- With paging, the repository returns SearchResults (see the network rules) and Success holds it. The page is screen-local input in a private MutableStateFlow, combined with the retry trigger inside flatMapLatest, so changing page or retrying re-runs only the current query. A new query starts at page 1, and changing page shows Loading and replaces the results:

      private val page = MutableStateFlow(1)
      private val retryTrigger = MutableStateFlow(0)

      val uiState: StateFlow<SearchUiState> = snapshotFlow { query.trim() }
          .debounce(300)
          .distinctUntilChanged()
          .onEach { page.value = 1 }
          .flatMapLatest { text ->
              if (text.isEmpty()) {
                  flowOf(SearchUiState.Idle)
              } else {
                  combine(page, retryTrigger) { number, _ -> number }
                      .flatMapLatest { number ->
                          flow<SearchUiState> {
                              emit(SearchUiState.Loading)
                              emit(SearchUiState.Success(itemRepository.search(text, number)))
                          }.catch { exception -> emit(exception.toSearchError()) }
                      }
              }
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState.Idle)

      fun onNextPage() = page.update { number -> number + 1 }
      fun onPreviousPage() = page.update { number -> (number - 1).coerceAtLeast(1) }
      fun retry() = retryTrigger.update { count -> count + 1 }

- page is reset in onEach, before flatMapLatest subscribes to it, so a new query never makes a request with the previous query's page. Whether the previous and next controls are enabled is a getter on Success (results.page > 1, results.page < results.pageCount), not a stored field.
- Errors are mapped by exception type to distinct error states or error kinds (no connection, server error, unexpected response), each with its own text in strings.xml. Rethrow anything that is not an expected network or parsing exception.
- Retry re-runs the current query through the retry trigger described above.
- Add @OptIn(FlowPreview::class) and @OptIn(ExperimentalCoroutinesApi::class) if the compiler requires them for debounce and flatMapLatest.

## Screens

- The stateless Screen renders the state with an exhaustive when, without else:

      when (uiState) {
          ItemListUiState.Loading -> LoadingContent()
          ItemListUiState.Error -> ErrorContent(onRetry = onRetry)
          is ItemListUiState.Success ->
              if (uiState.items.isEmpty()) EmptyContent() else ItemList(items = uiState.items)
      }

- Loading, error and empty states are visible inside the screen's Scaffold, so the top bar and navigation remain usable.
- The error state always offers a way forward: retry, or go back.
- The Route owns a SnackbarHostState and reacts to the message with LaunchedEffect(message): it maps the UserMessage to text with stringResource, shows the Snackbar, then calls onMessageShown(). The Screen receives the SnackbarHostState and passes it to Scaffold.
- All user-facing text for states and messages lives in strings.xml.
- While a form is saving, disable its submit button using the isSaving flag so the action cannot run twice.

## Previews

Every screen with a sealed UiState has a preview for each state: Loading, Error, Success with data, and Success with an empty list.

## Before you finish

Verify: no screen shows a blank area while loading, every failed load shows an error state with a way forward, failed actions show a message without losing the content, messages do not reappear after rotation, every when over a UiState is exhaustive without else, and no ViewModel catches broad exceptions. If a screen does not fit these rules, say so instead of inventing a new pattern.