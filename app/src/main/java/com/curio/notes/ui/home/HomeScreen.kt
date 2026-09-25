package com.curio.notes.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.notes.R
import com.curio.notes.domain.model.Note
import com.curio.notes.domain.model.NoteStatus
import com.curio.notes.ui.components.NoteCard
import com.curio.notes.ui.util.errorMessageFor
import com.curio.notes.ui.util.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(rememberAppContainer().noteRepository)
    )
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
    val showArchived by viewModel.showArchived.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val selectionError by viewModel.selectionError.collectAsStateWithLifecycle()
    val selectionMode = selectedIds.isNotEmpty()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val visibleNotes = if (showArchived) archivedNotes else notes

    BackHandler(enabled = selectionMode) {
        viewModel.clearSelection()
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            pluralStringResource(
                                R.plurals.selection_count,
                                selectedIds.size,
                                selectedIds.size
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_clear_selection)
                            )
                        }
                    },
                    actions = {
                        if (showArchived) {
                            IconButton(onClick = { viewModel.unarchiveSelected() }) {
                                Icon(
                                    Icons.Default.Unarchive,
                                    contentDescription = stringResource(R.string.action_unarchive)
                                )
                            }
                        } else {
                            IconButton(onClick = { viewModel.archiveSelected() }) {
                                Icon(
                                    Icons.Default.Archive,
                                    contentDescription = stringResource(R.string.cd_archive_selected)
                                )
                            }
                            IconButton(onClick = { viewModel.pinSelected() }) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = stringResource(R.string.cd_pin_selected)
                                )
                            }
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete_selected)
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.inbox_title))
                            if (notes.isNotEmpty()) {
                                Text(
                                    text = inboxSubtitle(notes),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSearch) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.cd_search)
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.cd_settings)
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(onClick = onCreateNote) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_create_note)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = !showArchived,
                    onClick = { viewModel.setShowArchived(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text(stringResource(R.string.tab_inbox)) }
                )
                SegmentedButton(
                    selected = showArchived,
                    onClick = { viewModel.setShowArchived(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text(stringResource(R.string.tab_archived)) }
                )
            }
            selectionError?.let { code ->
                Text(
                    text = errorMessageFor(code),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (visibleNotes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.tagline),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(
                            if (showArchived) {
                                R.string.archived_empty_body
                            } else {
                                R.string.inbox_empty_body
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 8.dp,
                        end = 16.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            selected = note.id in selectedIds,
                            onClick = {
                                if (selectionMode) {
                                    viewModel.toggleSelection(note.id)
                                } else {
                                    onOpenNote(note.id)
                                }
                            },
                            onLongClick = { viewModel.enterSelection(note.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    pluralStringResource(
                        R.plurals.delete_notes_title,
                        selectedIds.size,
                        selectedIds.size
                    )
                )
            },
            text = { Text(stringResource(R.string.delete_notes_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteSelected {}
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_keep))
                }
            }
        )
    }
}

@Composable
private fun inboxSubtitle(notes: List<Note>): String {
    val total = pluralStringResource(R.plurals.inbox_thoughts, notes.size, notes.size)
    val waiting = notes.count {
        it.status == NoteStatus.PENDING || it.status == NoteStatus.PROCESSING
    }
    return if (waiting > 0) {
        "$total ${stringResource(R.string.inbox_waiting, waiting)}"
    } else {
        total
    }
}
