@file:Suppress("UNUSED")

package com.mrkazofficial.paginator.compose

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.LiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.lifecycle.flowWithLifecycle
import com.mrkazofficial.paginator.compose.helpers.SingleLiveEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @Project Paginator Compose
 * @Class Paginator
 * @Author MRKaZ
 * @Since 10:01 PM, 7/31/2023
 * @Origin Taprobana (LK)
 * @Copyright (c) 2023 MRKaZ. All rights reserved.
 */
class Paginator<T>(
    private val dataSource: DataSource<T>,
) {
    // This function should observe the error event! that caught from the CoroutineExceptionHandler
    val errorEvent = SingleLiveEvent<Throwable>()

    // Coroutine scopes with Supervisor Job + Coroutine Dispatcher
    private val superIOScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val superUIScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state: MutableStateFlow<State<T>> = MutableStateFlow(State())
    val state: LiveData<State<T>> get() = _state.asLiveData()

    private val _currentPage = MutableStateFlow(DEFAULT_PAGE_NUMBER)

    init {
        observePaginatorData()
    }

    /**
     * [observePaginatorData]
     * - Observes and loads paginated data from a data source using Kotlin Coroutines.
     *
     * Function that observes changes in the [_currentPage] and performs the data
     * loading process accordingly. It launches a new coroutine job in the [superUIScope].
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observePaginatorData() =
        launchJob(superUIScope.coroutineContext) {
            // Observe changes in the page changes.
            _currentPage
                .asFlow()
                // Only emit when the page changes to avoid redundant data loading.
                .distinctUntilChanged()
                .flatMapLatest { page ->
                    // Checks maximum page
                    if (MAX_PAGE <= page) {
                        _state.value = _state.value.copy(isLoading = false, maximumReached = true)
                        return@flatMapLatest emptyFlow()
                    }
                    _state.value = _state.value.copy(isLoading = true)
                    // Make an API request using the `dataSource.callToData(page)` method.
                    // The data loading process is wrapped in a new flow using `flow`.
                    // Ensure only distinct data is emitted using `distinctUntilChanged()` on the API response.
                    flow {
                        emit(dataSource.callToData(page = page, pageSize = DEFAULT_PAGE_SIZE))
                    }.distinctUntilChanged()
                }
                .catch { t ->
                    // Catch any errors that occur during the data loading process.
                    // Handle the error by emitting an empty list. for safe UI performance.
                    emit(emptyList())
                    _state.value = _state.value.copy(error = t.message ?: t.localizedMessage)
                }
                .collect { newData ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        items = _state.value.items + newData,
                        currentPage = _currentPage.value
                    )
                }
        }

    /**
     * [loadNextPage]
     * - Loads the next page of data from the data source.
     *
     * The function is responsible for loading the next page of data from the data source
     * if the current state is not [State.isLoading]. It increments the [_currentPage]
     * by one and performs the data loading process in a new coroutine job within the [superIOScope].
     * Any errors encountered during the data loading process are caught and set as the state's error message.
     *
     * Note: The [_state] and [_currentPage] properties are assumed to be MutableStateFlow or similar constructs
     * representing the current state and current page number, respectively.
     *
     */
    fun loadNextPage() {
        // Check if the current state is not `State.Loading`.
        if (!_state.value.isLoading) {
            // Launch a new coroutine job within the `superIOScope`.
            launchJob(superIOScope.coroutineContext) {
                /* Update code snippets as you wish; this is only for demonstration purposes. */
                if (_state.value.currentPage <= MAX_PAGE) {
                    _currentPage.value = _currentPage.value + 1
                }
            }
        }
    }

    /**
     * [launchJob]
     * - Launches a loading job using the provided [block] as the coroutine body. This function
     * creates a new coroutine scope with the given [context] and launches the [block] as a
     * suspending function within this scope. The [createErrorHandler] function is used to
     * handle exceptions during the loading job and log any unhandled exceptions, excluding
     * CancellationExceptions. The `counter` variable is used to track the number of running
     * loading jobs.
     *
     * @param context The coroutine context in which the loading job will run. Default is [EmptyCoroutineContext].
     * @param start The coroutine start mode. Default is [CoroutineStart.DEFAULT].
     * @param block The suspending function representing the loading job to be executed.
     * @return A reference to the launched job.
     */
    private fun launchJob(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = CoroutineScope(context)
        .launch(context + createErrorHandler(), start) {
            try {
                block()
            } finally {
                /* NOTHING HERE YET, Because, the exception is already catching from the coroutine exception handler.*/
            }
        }

    /**
     * Creates a coroutine exception handler to handle exceptions that occur during the loading job.
     * This handler logs the exception message and posts the error event using the [errorEvent]
     * post-call function if the exception is not a [CancellationException].
     *
     * @return A coroutine exception handler. [CoroutineExceptionHandler]
     */
    private fun createErrorHandler(): CoroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Caught ${throwable.message}", throwable)
            if (throwable !is CancellationException) {
                errorEvent.postCall(throwable)
            }
        }

    /**
     * [State]
     * - Represents the state of a data loading operation with optional generic items.
     *
     * @param T The type of items in the state.
     * @property isLoading True if the data is currently being loaded; false otherwise.
     * @property items The list of items in the current state. Defaults to an empty list.
     * @property error An optional error message in case of an error during data loading. Defaults to null.
     * @property maximumReached True if the maximum number of items has been reached; false otherwise.
     * @property currentPage The current page number. Defaults to 0.
     */
    data class State<T>(
        val isLoading: Boolean = false,
        val items: List<T> = emptyList(),
        val error: String? = null,
        val maximumReached: Boolean = false,
        val currentPage: Int = 0, //DEFAULT_PAGE_NUMBER
    )


    /**
     * [DataSource]
     * - A data source interface to fetch data in a paginated manner.
     *
     * Usage;
     * ```
     * class YourDataSource(
     *      private val repository: YourRepository
     * ) : DataSource<YourItem> {
     *
     *     // You can do also exception handle in here.
     *     override suspend fun callToData(page: Int, pageSize: Int): List<YourItem> =
     *             repository.getItemList(page = page, pageSize = pageSize)
     *
     *     }
     *}
     *```
     * @param T The type of data items to be fetched.
     */
    interface DataSource<T> {

        /**
         * [callToData]
         * - Fetches a list of data items for the specified page and page size.
         *
         * @param page The page number to fetch data for (0-indexed).
         * @param pageSize The number of items to fetch per page.
         * @return A list of data items for the specified page.
         */
        suspend fun callToData(page: Int, pageSize: Int): List<T>
    }


    companion object {
        private const val TAG = "Paginator"

        /* Update code snippets as your wish this is only for demonstration purposes. */
        // Default page number to load while on start.
        private const val DEFAULT_PAGE_NUMBER = 0

        // The maximum number of items that should be displayed per page.
        private const val DEFAULT_PAGE_SIZE = 20

        // Maximum page count MAX_PAGE <= page ? maximumReached : false
        private const val MAX_PAGE = 10

        /**
         * [OnLastItemReach]
         * - Composable function to observe the last item reach in a LazyList.
         *
         * Usage:
         * ```
         * // Assume you have a LazyList of type T
         * val lazyState = rememberLazyListState() // initialize your LazyListState here...
         *
         * // Result items list
         * val list = listOf<T>()
         * // Define the lambda function to be invoked when the last item is reached, if you need!
         * val onLastItemReached: () -> Unit = {
         *     println("Last item has been reached!")
         * }
         *
         * // Now, you can call the OnLastItemReach composable function.
         * Pair<LazyListState, List<T>>(lazyState, list)
         *      .OnLastItemReach(onLastItemReached)
         * ```
         *
         * @param T The type of items in the list.
         * @param reached Lambda function to be invoked when the last item is reached.
         */
        @Composable
        fun <T> Pair<LazyListState, List<T>>.OnLastItemReach(reached: () -> Unit) {
            LaunchedEffect(this) {
                snapshotFlow { first.layoutInfo.visibleItemsInfo.any { it.index == second.size - 1 } }
                    .filter { it } // filter only true
                    .collect { reached.invoke() }
            }
        }
    }

    /**
     * [MutableStateFlow.asFlow]
     * - Converts a [MutableStateFlow] into a [Flow].
     *
     * @param T The type of data held by the [MutableStateFlow].
     * @return A [Flow] emitting values from the [MutableStateFlow] as long as the lifecycle is at least in the started state.
     */
    private fun <T> MutableStateFlow<T>.asFlow(): Flow<T> =
        flowWithLifecycle(ProcessLifecycleOwner.get().lifecycle)

}