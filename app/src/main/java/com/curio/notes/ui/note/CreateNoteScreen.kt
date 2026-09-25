package com.curio.notes.ui.note

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.notes.R
import com.curio.notes.ui.components.CopyIconButton
import com.curio.notes.ui.components.CurioPrimaryButton
import com.curio.notes.ui.components.ErrorText
import com.curio.notes.ui.components.borderlessFieldColors
import com.curio.notes.ui.util.errorMessageFor
import com.curio.notes.ui.util.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoteScreen(
    onBack: () -> Unit,
    viewModel: CreateNoteViewModel = run {
        val container = rememberAppContainer()
        viewModel(
            factory = CreateNoteViewModel.factory(
                container.noteRepository,
                container.processNoteUseCase,
                container.generationTracker
            )
        )
    }
) {
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var saving by rememberSaveable { mutableStateOf(false) }
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()
    val developerMode by viewModel.developerMode.collectAsStateWithLifecycle()
    val providerLabel by viewModel.providerLabel.collectAsStateWithLifecycle()
    LaunchedEffect(saveError) {
        if (saveError != null) saving = false
    }

    // Fast capture: focus the thought field and raise the keyboard immediately.
    // Once the thought field is focused, the blank title folds away so the
    // screen becomes a focused writing environment (it returns if the title
    // has text or focus leaves).
    val bodyFocus = remember { FocusRequester() }
    var bodyFocused by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        bodyFocus.requestFocus()
        keyboard?.show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_title)) },
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
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            // The blank title folds away only once there is thought text to
            // focus on, so the title stays reachable until you start typing.
            AnimatedVisibility(
                visible = !bodyFocused || title.isNotBlank() || body.isBlank(),
                enter = fadeIn(animationSpec = tween(250)) +
                    expandVertically(animationSpec = tween(250)),
                exit = fadeOut(animationSpec = tween(200)) +
                    shrinkVertically(animationSpec = tween(200))
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(stringResource(R.string.create_title_label)) },
                    textStyle = MaterialTheme.typography.titleLarge,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { bodyFocus.requestFocus() }
                    ),
                    colors = borderlessFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            TextField(
                value = body,
                onValueChange = { body = it },
                placeholder = { Text(stringResource(R.string.create_body_label)) },
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = borderlessFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .focusRequester(bodyFocus)
                    .onFocusChanged { bodyFocused = it.isFocused },
                trailingIcon = {
                    if (body.isNotBlank()) {
                        CopyIconButton(text = body)
                    }
                }
            )
            saveError?.let { code ->
                ErrorText(
                    text = errorMessageFor(code),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (developerMode) {
                Text(
                    text = providerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            CurioPrimaryButton(
                text = if (saving) {
                    stringResource(R.string.action_saving)
                } else {
                    stringResource(R.string.action_save)
                },
                onClick = {
                    saving = true
                    viewModel.saveNote(title, body, onBack)
                },
                enabled = body.isNotBlank() && !saving
            )
        }
    }
}
