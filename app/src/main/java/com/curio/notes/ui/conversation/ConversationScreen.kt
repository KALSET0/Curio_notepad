package com.curio.notes.ui.conversation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.notes.R
import com.curio.notes.ai.ChatMessage
import com.curio.notes.ai.ChatRole
import com.curio.notes.ai.formatGenerationDuration
import com.curio.notes.domain.model.Note
import com.curio.notes.ui.components.ErrorText
import com.curio.notes.ui.components.CurioThinkingIndicator
import com.curio.notes.ui.components.MarkdownText
import com.curio.notes.ui.components.ShareButton
import com.curio.notes.ui.components.appAiLanguage
import com.curio.notes.ui.components.borderlessFieldColors
import com.curio.notes.ui.components.copyToClipboard
import com.curio.notes.ui.theme.Spacing
import com.curio.notes.ui.util.errorMessageFor
import com.curio.notes.ui.util.formatNoteExport
import com.curio.notes.ui.util.rememberAppContainer
import com.curio.notes.ui.util.shareText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    noteId: Long,
    onBack: () -> Unit,
    viewModel: ConversationViewModel = run {
        val container = rememberAppContainer()
        viewModel(
            factory = ConversationViewModel.factory(
                container.noteRepository,
                container.aiProvider,
                noteId,
                container.generationTracker
            )
        )
    }
) {
    val note by viewModel.note.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isReady by viewModel.isReady.collectAsStateWithLifecycle()
    val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()
    val developerMode by viewModel.developerMode.collectAsStateWithLifecycle()
    var draft by rememberSaveable { mutableStateOf("") }
    var menuForIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var suggestionsOpen by rememberSaveable { mutableStateOf(false) }
    var editingIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var savedDraft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
                val context = LocalContext.current
                val view = LocalView.current
    val language = appAiLanguage()
    val suggestedQuestions by viewModel.suggestedQuestions.collectAsStateWithLifecycle()
    val isGeneratingSuggestions by viewModel.isGeneratingSuggestions.collectAsStateWithLifecycle()
    val suggestionsError by viewModel.suggestionsError.collectAsStateWithLifecycle()
    val storedQuestions = aiResponse?.followUpQuestions.orEmpty()
    val displayedQuestions = suggestedQuestions.ifEmpty { storedQuestions }

    // While editing, system back cancels the edit instead of leaving.
    BackHandler(enabled = editingIndex != null) {
        draft = savedDraft
        savedDraft = ""
        editingIndex = null
    }
    // Registered after the editing handler so an open panel wins: first
    // back closes the panel, the next one cancels the edit or leaves.
    BackHandler(enabled = suggestionsOpen) {
        suggestionsOpen = false
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = note?.let { stringResource(R.string.session_title, it.title) }
                            ?: stringResource(R.string.continue_ai),
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
                actions = {
                    val current = note
                    ShareButton(
                        onShare = {
                            current?.let {
                                shareText(
                                    context,
                                    formatNoteExport(it, aiResponse, messages, language),
                                    context.getString(R.string.share_chooser_title)
                                )
                            }
                        },
                        enabled = current != null
                    )
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                note?.let { ContextHeader(note = it) }
                val lastUserIndex = messages.indexOfLast { it.role == ChatRole.USER }
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(messages.size) { index ->
                        val message = messages[index]
                        Box {
                            MessageBubble(
                                message = message,
                                showGeneration = developerMode,
                                onLongPress = { menuForIndex = index }
                            )
                            DropdownMenu(
                                expanded = menuForIndex == index,
                                onDismissRequest = { menuForIndex = null },
                                shape = RoundedCornerShape(16.dp),
                                tonalElevation = 2.dp
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.cd_copy)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                                    },
                                    onClick = {
                                        menuForIndex = null
                                        copyToClipboard(context, message.text)
                                    }
                                )
                                if (index == lastUserIndex) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_edit)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                        },
                                        onClick = {
                                            menuForIndex = null
                                            savedDraft = draft
                                            draft = message.text
                                            editingIndex = index
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_retry)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Refresh, contentDescription = null)
                                        },
                                        onClick = {
                                            menuForIndex = null
                                            editingIndex = null
                                            savedDraft = ""
                                            viewModel.resend(index, message.text)
                                        }
                                    )
                                }
                            }
                        }
                    }
                if (isSending) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            CurioThinkingIndicator(
                                logoSize = 28.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = stringResource(R.string.conv_thinking),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }
                if (suggestionsOpen && !isSending &&
                    (displayedQuestions.isNotEmpty() || suggestionsError != null || isGeneratingSuggestions)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { suggestionsOpen = false }
                            )
                    )
                }
                }
                if (displayedQuestions.isEmpty() && messages.isEmpty() && !isSending) {
                    Text(
                        text = stringResource(R.string.conv_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg)
                    )
                }
                error?.let { code ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ErrorText(
                            text = errorMessageFor(code),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::retry) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
                AnimatedVisibility(
                    visible = suggestionsOpen && !isSending &&
                        (displayedQuestions.isNotEmpty() || suggestionsError != null || isGeneratingSuggestions),
                    enter = expandVertically(expandFrom = Alignment.Bottom),
                    exit = shrinkVertically(shrinkTowards = Alignment.Bottom),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.suggestions_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isGeneratingSuggestions) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.padding(8.dp).size(20.dp)
                                    )
                                } else {
                                    IconButton(onClick = { viewModel.regenerateSuggestions() }) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = stringResource(R.string.suggestions_regenerate)
                                        )
                                    }
                                }
                            }
                            val suggError = suggestionsError
                            if (suggError != null) {
                                ErrorText(
                                    text = errorMessageFor(suggError),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            } else {
                                for (question in displayedQuestions) {
                                    TextButton(
                                        onClick = {
                                            viewModel.send(question)
                                            editingIndex = null
                                            savedDraft = ""
                                            suggestionsOpen = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = question,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Surface(
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = draft,
                            onValueChange = { draft = it },
                            placeholder = {
                                Text(
                                    stringResource(
                                        if (editingIndex != null) {
                                            R.string.conv_editing
                                        } else {
                                            R.string.conv_hint
                                        }
                                    )
                                )
                            },
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (draft.isNotBlank() && isReady && !isSending) {
                                        val target = editingIndex
                                        if (target != null) {
                                            viewModel.resend(target, draft)
                                            editingIndex = null
                                            savedDraft = ""
                                            draft = ""
                                        } else {
                                            viewModel.send(draft)
                                            draft = ""
                                        }
                                        suggestionsOpen = false
                                    }
                                }
                            ),
                            colors = borderlessFieldColors(),
                            modifier = Modifier.weight(1f).onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    suggestionsOpen = false
                                    view.requestApplyInsets()
                                }
                            }
                        )
                        if (displayedQuestions.isNotEmpty() && !isSending) {
                            IconButton(onClick = { suggestionsOpen = !suggestionsOpen }) {
                                Icon(
                                    Icons.Default.QuestionMark,
                                    contentDescription = stringResource(R.string.suggestions_title),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        val sendEnabled = draft.isNotBlank() && isReady && !isSending
                        val sendContainer by animateColorAsState(
                            targetValue = if (sendEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            label = "sendContainer"
                        )
                        FilledIconButton(
                            onClick = {
                                val target = editingIndex
                                if (target != null) {
                                    viewModel.resend(target, draft)
                                    editingIndex = null
                                    savedDraft = ""
                                    draft = ""
                                } else {
                                    viewModel.send(draft)
                                    draft = ""
                                }
                                suggestionsOpen = false
                            },
                            enabled = sendEnabled,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = sendContainer)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.cd_send)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextHeader(note: Note, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        onClick = { expanded = !expanded },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.conv_continuing),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.Default.ExpandMore
                    },
                    contentDescription = stringResource(
                        if (expanded) {
                            R.string.cd_collapse
                        } else {
                            R.string.cd_expand
                        }
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = note.originalText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    showGeneration: Boolean = false,
    onLongPress: () -> Unit = {}
) {
    val isUser = message.role == ChatRole.USER
    val scheme = MaterialTheme.colorScheme
    if (isUser) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = onLongPress),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                )
            }
        }
    } else {
        // Model answers read as knowledge, not chat bubbles: accent bar
        // plus plain text.
        Column(
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(scheme.primary)
                )
                MarkdownText(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }
            if (showGeneration && message.generationLabel != null) {
                Text(
                    text = "${message.generationLabel} · " +
                        formatGenerationDuration(message.generationMillis ?: 0L),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }
        }
    }
}
