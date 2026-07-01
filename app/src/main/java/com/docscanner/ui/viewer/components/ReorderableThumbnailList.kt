package com.docscanner.ui.viewer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.docscanner.R
import com.docscanner.domain.model.Page
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ReorderableThumbnailList(
    pages: List<Page>,
    currentPageIndex: Int,
    onReorder: (List<Page>) -> Unit,
    modifier: Modifier = Modifier
) {
    var localPages by remember(pages) { mutableStateOf(pages) }
    val lazyListState = rememberLazyListState()

    val reorderState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            localPages = localPages.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        }
    )

    LazyRow(
        state = lazyListState,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = modifier
    ) {
        items(localPages, key = { it.id }) { page ->
            ReorderableItem(reorderState, key = page.id) { isDragging ->
                AsyncImage(
                    model = page.imagePath,
                    contentDescription = stringResource(R.string.page_number, page.pageNumber),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .longPressDraggableHandle(
                            onDragStopped = { onReorder(localPages) }
                        )
                )
            }
        }
    }
}
