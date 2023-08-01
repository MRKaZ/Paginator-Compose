package com.mrkazofficial.paginator.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mrkazofficial.paginator.compose.Paginator.Companion.OnLastItemReach
import com.mrkazofficial.paginator.compose.ui.theme.PaginatorComposeTheme
import com.mrkazofficial.paginator.compose.ui.viewmodels.LazyListItem
import com.mrkazofficial.paginator.compose.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PaginatorComposeTheme {

                val mainModel by viewModels<MainViewModel>()

                val state by remember { mainModel.currentState }.observeAsState(Paginator.State() /* TO AVOID NULL */)

                val scope = rememberCoroutineScope()
                val lazyState = rememberLazyListState()

                Pair(lazyState, state.items).OnLastItemReach {
                    scope.launch {
                        mainModel.loadNextPage()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LazyColumn(
                        state = lazyState,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Text(text = "Current page ${state.currentPage}")
                        }

                        items(state.items) { item ->
                            LazyListItem(item = item)
                        }

                        item {
                            if (state.maximumReached) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(text = "End reached")
                                }
                            }
                        }

                        item {
                            if (state.isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LazyListItem(
    modifier: Modifier = Modifier,
    item: LazyListItem,
) {
    ListItem(
        modifier = modifier,
        headlineText = {
            Text(item.title)
        },
        overlineText = {
            Text(item.description)
        },
        leadingContent = {
            Icon(
                item.icon,
                contentDescription = item.icon.name,
            )
        }
    )
    Divider(modifier = Modifier.fillMaxWidth(), color = Color.White)
}


@Preview(showBackground = true)
@Composable
fun LazyListItemPreview() {
    PaginatorComposeTheme {
        LazyListItem(
            item = LazyListItem(
                title = "Preview title",
                description = "Preview description",
                icon = Icons.Filled.Favorite
            )
        )
    }
}