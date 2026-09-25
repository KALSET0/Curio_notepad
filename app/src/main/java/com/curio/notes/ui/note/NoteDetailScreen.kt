package com.curio.notes.ui.note

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.notes.R
import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.ChatMessage
import com.curio.notes.ai.ChatRole
import com.curio.notes.ai.formatGenerationDuration
import com.curio.notes.domain.model.Note
import com.curio.notes.domain.model.NoteStatus
import com.curio.notes.ui.components.AiResponseCard
import com.curio.notes.ui.components.CopyIconButton
import com.curio.notes.ui.components.CurioPrimaryButton
import com.curio.notes.ui.components.CurioSecondaryButton
import com.curio.notes.ui.components.DeleteDialog
import com.curio.notes.ui.components.ErrorText
import com.curio.notes.ui.components.MarkdownText
import com.curio.notes.ui.components.ProcessingIndicator
import com.curio.notes.ui.components.SectionHeader
import com.curio.notes.ui.components.ShareButton
import com.curio.notes.ui.components.StatusChip
import com.curio.notes.ui.components.TypeChip
import com.curio.notes.ui.components.appAiLanguage
import com.curio.notes.ui.theme.Spacing
import com.curio.notes.ui.util.errorMessageFor
import com.curio.notes.ui.util.appLocale
import com.curio.notes.ui.util.formatNoteExport
import com.curio.notes.ui.util.formatTimestamp
import com.curio.notes.ui.util.rememberAppContainer
import com.curio.notes.ui.util.shareText

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
                noteId,
                container.generationTracker
            )
        )
    }
) {
    val note by viewModel.note.collectAsStateWithLifecycle()
    val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()
    val operationError by viewModel.operationError.collectAsStateWithLifecycle()
    val developerMode by viewModel.developerMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val language = appAiLanguage()

    val current = note
    var sawNote by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(current) {
        if (current != null) sawNote = true
    }
    if (current == null) {
        Scaffold(
            topBar = {
                NoteDetailTopBar(
                    title = stringResource(R.string.detail_title),
                    onBack = onBack
                )
            }
        ) { padding ->
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
                        CurioPrimaryButton(
                            text = stringResource(R.string.cd_back),
                            onClick = onBack
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.loading),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    } else {
        key(current.id) {
            var title by rememberSaveable { mutableStateOf(current.title) }
            var body by rememberSaveable { mutableStateOf(current.originalText) }
            var showMenu by rememberSaveable { mutableStateOf(false) }
            var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
            val changed = title != current.title || body != current.originalText
            Scaffold(
                topBar = {
                    NoteDetailTopBar(
                        title = stringResource(R.string.detail_title_with_name, current.title),
                        onBack = onBack,
                        actions = {
                            ShareButton(
                                onShare = {
                                    shareText(
                                        context,
                                        formatNoteExport(current, aiResponse, conversation, language),
                                        context.getString(R.string.share_chooser_title)
                                    )
                                }
                            )
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.cd_more)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_save_changes)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    },
                                    enabled = changed && body.isNotBlank(),
                                    onClick = {
                                        showMenu = false
                                        viewModel.saveChanges(title.trim(), body.trim())
                                    }
                                )
                                if (current.status == NoteStatus.ANSWERED) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_reanalyze)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Refresh, contentDescription = null)
                                        },
                                        onClick = {
                                            showMenu = false
                                            viewModel.reanalyze()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                if (current.isArchived) {
                                                    R.string.action_unarchive
                                                } else {
                                                    R.string.action_archive
                                                }
                                            )
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (current.isArchived) {
                                                Icons.Default.Unarchive
                                            } else {
                                                Icons.Default.Archive
                                            },
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.toggleArchive()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                if (current.isPinned) {
                                                    R.string.action_unpin
                                                } else {
                                                    R.string.action_pin
                                                }
                                            )
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.PushPin, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.togglePin()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_delete)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                NoteDetailContent(
                    note = current,
                    aiResponse = aiResponse,
                    conversation = conversation,
                    operationError = operationError,
                    developerMode = developerMode,
                    title = title,
                    onTitleChange = { title = it },
                    body = body,
                    onBodyChange = { body = it },
                    onRetry = viewModel::retryProcessing,
                    onContinueWithAI = onContinueWithAI,
                    modifier = Modifier.padding(padding)
                )
            }
            if (showDeleteDialog) {
                DeleteDialog(
                    count = 1,
                    onDismiss = { showDeleteDialog = false },
                    onConfirm = {
                        showDeleteDialog = false
                        viewModel.deleteNote(onBack)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteDetailTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
        },
        actions = actions,
        modifier = modifier
    )
}

@Composable
private fun NoteDetailContent(
    note: Note,
    aiResponse: AIResponse?,
    conversation: List<ChatMessage>,
    operationError: String?,
    developerMode: Boolean,
    title: String,
    onTitleChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    onRetry: () -> Unit,
    onContinueWithAI: () -> Unit,
    modifier: Modifier = Modifier
) {

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
                ProcessingIndicator(text = stringResource(R.string.detail_processing))
            }
            NoteStatus.ERROR -> {
                ErrorText(
                    text = note.errorMessage?.let { errorMessageFor(it) }
                        ?: stringResource(R.string.error_unknown)
                )
                CurioSecondaryButton(
                    text = stringResource(R.string.action_retry),
                    onClick = onRetry,
                    icon = Icons.Default.Refresh
                )
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
            text = stringResource(R.string.detail_created, formatTimestamp(note.createdAt, appLocale())),
            style = MaterialTheme.typography.labelSmall
        )
        SectionHeader(text = stringResource(R.string.thought_header))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(stringResource(R.string.detail_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    label = { Text(stringResource(R.string.detail_original_label)) },
                    minLines = 5,
                    trailingIcon = {
                        if (body.isNotBlank()) {
                            CopyIconButton(text = body)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        operationError?.let { code ->
            ErrorText(text = errorMessageFor(code))
        }
        if (note.status == NoteStatus.ANSWERED && aiResponse == null) {
            ErrorText(text = stringResource(R.string.ai_unavailable))
        }
        AnimatedVisibility(
            visible = note.status == NoteStatus.ANSWERED && aiResponse != null,
            enter = fadeIn(animationSpec = tween(300)) +
                expandVertically(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(200)) +
                shrinkVertically(animationSpec = tween(200))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                aiResponse?.let { response ->
                    AiResponseCard(response = response)
                    if (developerMode && note.generationLabel != null) {
                        Text(
                            text = "${note.generationLabel} · " +
                                formatGenerationDuration(note.generationMillis ?: 0L),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    CurioPrimaryButton(
                        text = stringResource(R.string.continue_ai),
                        onClick = onContinueWithAI
                    )
                }
            }
        }
        if (conversation.isNotEmpty()) {
            ConversationHistory(messages = conversation)
        }
    }
}

@Composable
private fun ConversationHistory(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeader(text = stringResource(R.string.conv_history_title))
        messages.forEach { message ->
            val isUser = message.role == ChatRole.USER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                Surface(
                    color = if (isUser) scheme.primaryContainer else scheme.surfaceVariant,
                    contentColor = if (isUser) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    MarkdownText(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
