package com.mrkazofficial.paginator.compose.ui.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.mrkazofficial.paginator.compose.Paginator
import com.mrkazofficial.paginator.compose.helpers.SingleLiveEvent
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * @Project Paginator Compose
 * @Class MainViewModel
 * @Author MRKaZ
 * @Since 12:29 AM, 8/1/2023
 * @Origin Taprobana (LK)
 * @Copyright (c) 2023 MRKaZ. All rights reserved.
 */
class MainViewModel : ViewModel() {

    private val dataRepository = DataRepository()

    // Data source class that you extended with [Paginator.DataSource]
    // To load your data remotely or locally.
    private val dataSource = ListItemDatasource(dataRepository = dataRepository)

    // Instance of paginator
    private val paginator = Paginator(dataSource = dataSource)

    // Current status for your data and page
    val currentState: LiveData<Paginator.State<LazyListItem>> = paginator.state

    // Call to load next page data
    fun loadNextPage() = paginator.loadNextPage()

    // Handle error event
    val errorEvent: SingleLiveEvent<Throwable> = paginator.errorEvent

}

class DataRepository {

    private val iconList = listOf(
        Icons.Filled.Home,
        Icons.Filled.Favorite,
        Icons.Filled.Settings,
        Icons.Filled.Add,
        Icons.Filled.Delete,
        Icons.Filled.Edit,
        Icons.Filled.Email,
        Icons.Filled.Phone,
        Icons.Filled.Person,
        Icons.Filled.ShoppingCart,
        Icons.Filled.Search,
        Icons.Filled.Star,
        Icons.Filled.Call,
        Icons.Filled.PlayArrow,
        Icons.Filled.Share,
        Icons.Filled.Warning,
    )

    private val randomIcon: ImageVector
        get() = run {
            val randomIndex = Random.nextInt(0, iconList.size)
            return iconList[randomIndex]
        }

    private val items: List<LazyListItem> = IntRange(1, 300).toList().map { item ->
        LazyListItem(
            title = " Dummy title $item",
            description = "Dummy description $item",
            icon = randomIcon
        )
    }

    suspend fun getItemList(page: Int, pageSize: Int): List<LazyListItem> {
        delay(1000)
        val startIndex = page * pageSize
        val endIndex = startIndex + pageSize

        return if (startIndex < items.size) {
            items.subList(startIndex, endIndex.coerceAtMost(items.size))
        } else {
            emptyList()
        }
    }
}

/**
 * [ListItemDatasource]
 * - Example implement data source class [Paginator.DataSource] for pagination.
 *
 * Example usage:
 * ```
 * // Create an instance of DataRepository
 * val dataRepository = /**/
 *
 * // Create an instance of ListItemDatasource using the DataRepository
 * val dataSource = ListItemDatasource(dataRepository)
 *
 * // Create an instance of Paginator
 * val paginator = Paginator(dataSource = dataSource)
 *
 * ```
 * @param dataRepository [DataRepository] The repository responsible for providing data.
 */
class ListItemDatasource(
    private val dataRepository: DataRepository,
) : Paginator.DataSource<LazyListItem> {
    /*
    * Fetches a list of items for the specified page and page size.
    * (You can do also exception handle in here.)
    */
    override suspend fun callToData(page: Int, pageSize: Int): List<LazyListItem> =
        dataRepository.getItemList(page = page, pageSize = pageSize)
}


data class LazyListItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
)