package com.curio.notes.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.notes.R
import com.curio.notes.domain.model.NoteSortOrder
import com.curio.notes.domain.model.NoteType
import com.curio.notes.ui.components.EmptyState
import com.curio.notes.ui.components.NoteCard
import com.curio.notes.ui.components.SectionHeader
import com.curio.notes.ui.theme.Spacing
import com.curio.notes.ui.util.appLocale
import com.curio.notes.ui.util.label
import com.curio.notes.ui.util.rememberAppContainer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    val grouped = remember(results) { results.groupBy { dayOf(it.createdAt) } }

    // Fast capture of intent: focus the field the moment search opens.
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
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
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.search_label)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null
                    )
                },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            // One compact filter strip: sort, types, and clear share a
            // single horizontal scroll instead of two permanent rows.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                item(key = "sort-newest") {
                    FilterChip(
                        selected = sortOrder == NoteSortOrder.NEWEST_FIRST,
                        onClick = { viewModel.setSortOrder(NoteSortOrder.NEWEST_FIRST) },
                        label = { Text(stringResource(R.string.sort_newest)) }
                    )
                }
                item(key = "sort-oldest") {
                    FilterChip(
                        selected = sortOrder == NoteSortOrder.OLDEST_FIRST,
                        onClick = { viewModel.setSortOrder(NoteSortOrder.OLDEST_FIRST) },
                        label = { Text(stringResource(R.string.sort_oldest)) }
                    )
                }
                items(NoteType.entries, key = { it.name }) { type ->
                    FilterChip(
                        selected = type in selectedTypes,
                        onClick = { viewModel.toggleTypeFilter(type) },
                        label = { Text(type.label()) }
                    )
                }
                if (filtersActive) {
                    item(key = "filters-clear") {
                        TextButton(onClick = viewModel::clearFilters) {
                            Text(stringResource(R.string.filters_clear))
                        }
                    }
                }
            }
            when {
                query.isBlank() -> {
                    EmptyState(
                        title = stringResource(R.string.search_title),
                        body = stringResource(R.string.search_hint),
                        modifier = Modifier.weight(1f)
                    )
                }
                results.isEmpty() -> {
                    EmptyState(
                        title = stringResource(R.string.search_no_results, query),
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        grouped.forEach { (day, dayNotes) ->
                            item(key = "header-$day") {
                                SectionHeader(
                                    text = dayLabel(day),
                                    modifier = Modifier.animateItem()
                                )
                            }
                            items(dayNotes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    onClick = { onOpenNote(note.id) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
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
        else -> day.format(DateTimeFormatter.ofPattern("MMM d, yyyy", appLocale()))
    }
}
