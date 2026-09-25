package com.curio.notes.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.notes.R
import com.curio.notes.domain.model.Note
import com.curio.notes.domain.model.NoteSortOrder
import com.curio.notes.domain.model.NoteType
import com.curio.notes.ui.components.NoteCard
import com.curio.notes.ui.util.label
import com.curio.notes.ui.util.rememberAppContainer
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.factory(rememberAppContainer().noteRepository)
    )
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val selectedTypes by viewModel.selectedTypes.collectAsStateWithLifecycle()
    val filtersActive =
        selectedTypes.isNotEmpty() || sortOrder != NoteSortOrder.NEWEST_FIRST
    val listState = rememberLazyListState()
    val grouped = remember(results) { results.groupBy { dayOf(it.createdAt) } }
    val totalItems = grouped.values.sumOf { it.size } + grouped.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cd_search)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.search_label)) },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_clear_search)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = sortOrder == NoteSortOrder.NEWEST_FIRST,
                    onClick = { viewModel.setSortOrder(NoteSortOrder.NEWEST_FIRST) },
                    label = { Text(stringResource(R.string.sort_newest)) }
                )
                FilterChip(
                    selected = sortOrder == NoteSortOrder.OLDEST_FIRST,
                    onClick = { viewModel.setSortOrder(NoteSortOrder.OLDEST_FIRST) },
                    label = { Text(stringResource(R.string.sort_oldest)) }
                )
                if (filtersActive) {
                    TextButton(onClick = viewModel::clearFilters) {
                        Text(stringResource(R.string.filters_clear))
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NoteType.entries, key = { it.name }) { type ->
                    FilterChip(
                        selected = type in selectedTypes,
                        onClick = { viewModel.toggleTypeFilter(type) },
                        label = { Text(type.label()) }
                    )
                }
            }
            when {
                query.isBlank() -> {
                    Text(
                        text = stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                results.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.search_no_results, query),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            grouped.forEach { (day, dayNotes) ->
                                item(key = "header-$day") {
                                    DayHeader(label = dayLabel(day))
                                }
                                items(dayNotes, key = { it.id }) { note ->
                                    NoteCard(note = note, onClick = { onOpenNote(note.id) })
                                }
                            }
                        }
                        FastScrollbar(
                            listState = listState,
                            totalItems = totalItems,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }
    }
}

private fun dayOf(epochMillis: Long): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
private fun dayLabel(day: LocalDate): String {
    val today = LocalDate.now()
    return when (day) {
        today -> stringResource(R.string.search_today)
        today.minusDays(1) -> stringResource(R.string.search_yesterday)
        else -> day.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    }
}

@Composable
private fun DayHeader(label: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun FastScrollbar(
    listState: LazyListState,
    totalItems: Int,
    modifier: Modifier = Modifier
) {
    if (totalItems <= 1) return
    val scope = rememberCoroutineScope()
    val firstIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val fraction = if (totalItems <= 1) 0f else firstIndex / (totalItems - 1).toFloat()
    BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
        val trackHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val thumbHeightPx = with(LocalDensity.current) { 48.dp.toPx() }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(28.dp)
                .pointerInput(totalItems) {
                    detectVerticalDragGestures { _, dragAmount ->
                        val current = listState.firstVisibleItemIndex
                        val currentFraction =
                            if (totalItems <= 1) 0f else current / (totalItems - 1).toFloat()
                        val target = ((currentFraction + dragAmount / trackHeightPx) * (totalItems - 1))
                            .toInt()
                            .coerceIn(0, totalItems - 1)
                        scope.launch { listState.scrollToItem(target) }
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, ((trackHeightPx - thumbHeightPx) * fraction).roundToInt()) }
                    .width(4.dp)
                    .height(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}
