package com.curio.notes.ui.note

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.curio.notes.ai.AIResponse
import com.curio.notes.domain.model.Note
import com.curio.notes.domain.model.NoteStatus
import com.curio.notes.ui.components.AiResponseCard
import com.curio.notes.ui.components.StatusChip
import com.curio.notes.ui.components.TypeChip
import com.curio.notes.ui.util.errorMessageFor
import com.curio.notes.ui.util.formatTimestamp
import com.curio.notes.ui.util.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long,
    onBack: () -> Unit,
    onContinueWithAI: () -> Unit,
    viewModel: NoteDetailViewModel = run {
        val container = rememberAppContainer()
        viewModel(
            factory = NoteDetailViewModel.factory(
                container.noteRepository,
                container.processNoteUseCase,
                noteId
            )
        )
    }
) {
    val note by viewModel.note.collectAsStateWithLifecycle()
    val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()
    val operationError by viewModel.operationError.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
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
        val current = note
        var sawNote by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(current) {
            if (current != null) sawNote = true
        }
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (sawNote) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.note_gone),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onBack) {
                            Text(stringResource(R.string.cd_back))
                        }
                    }
                } else {
                    Text(
                        stringResource(R.string.loading),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            key(current.id) {
                NoteDetailContent(
                    note = current,
                    aiResponse = aiResponse,
                    operationError = operationError,
                    onSave = { title, body -> viewModel.saveChanges(title, body) },
                    onRetry = viewModel::retryProcessing,
                    onContinueWithAI = onContinueWithAI,
                    onToggleArchive = viewModel::toggleArchive,
                    onTogglePin = viewModel::togglePin,
                    onDelete = { viewModel.deleteNote(onBack) },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun NoteDetailContent(
    note: Note,
    aiResponse: AIResponse?,
    onSave: (String, String) -> Unit,
    onRetry: () -> Unit,
    onContinueWithAI: () -> Unit,
    onToggleArchive: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    operationError: String?,
    modifier: Modifier = Modifier
) {
    var title by rememberSaveable { mutableStateOf(note.title) }
    var body by rememberSaveable { mutableStateOf(note.originalText) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val changed = title != note.title || body != note.originalText

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (note.status) {
            NoteStatus.PROCESSING -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.detail_processing),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            NoteStatus.ERROR -> {
                Text(
                    text = note.errorMessage?.let { errorMessageFor(it) }
                        ?: stringResource(R.string.error_unknown),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(
                        stringResource(R.string.action_retry),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            NoteStatus.PENDING, NoteStatus.ANSWERED -> {}
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(status = note.status)
            if (note.type != null) {
                TypeChip(type = note.type)
            } else {
                Text(
                    stringResource(R.string.not_classified),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Text(
            text = stringResource(R.string.detail_created, formatTimestamp(note.createdAt)),
            style = MaterialTheme.typography.labelSmall
        )
        if (note.status == NoteStatus.ANSWERED) {
            if (aiResponse != null) {
                AiResponseCard(response = aiResponse)
            } else {
                Text(
                    text = stringResource(R.string.ai_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (note.status == NoteStatus.ANSWERED && aiResponse != null) {
            OutlinedButton(
                onClick = onContinueWithAI,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                Text(
                    stringResource(R.string.continue_ai),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.detail_title_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text(stringResource(R.string.detail_original_label)) },
            minLines = 5,
            modifier = Modifier.fillMaxWidth()
        )
        operationError?.let { code ->
            Text(
                text = errorMessageFor(code),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Button(
            onClick = { onSave(title.trim(), body.trim()) },
            enabled = changed && body.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_save_changes))
        }
        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Text(
                stringResource(R.string.action_delete),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        OutlinedButton(
            onClick = onToggleArchive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                contentDescription = null
            )
            Text(
                stringResource(
                    if (note.isArchived) {
                        R.string.action_unarchive
                    } else {
                        R.string.action_archive
                    }
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        OutlinedButton(
            onClick = onTogglePin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PushPin, contentDescription = null)
            Text(
                stringResource(
                    if (note.isPinned) R.string.action_unpin else R.string.action_pin
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(pluralStringResource(R.plurals.delete_notes_title, 1))
            },
            text = { Text(stringResource(R.string.delete_notes_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
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
