package com.curio.notes.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.curio.notes.R
import com.curio.notes.ui.settings.KeyTestState
import com.curio.notes.ui.theme.Spacing

// One key row, shared by Settings and first-run Setup. The saved key is
// never shown in full (masked placeholder); entry has show/hide.
@Composable
fun ApiKeyField(
    savedKey: String,
    testState: KeyTestState?,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onTest: ((String) -> Unit)?,
    onTyped: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }
    val saved = savedKey.isNotBlank()
    val candidate = draft.ifBlank { savedKey }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.api_key_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(
                    if (saved) {
                        R.string.api_key_configured
                    } else {
                        R.string.api_key_not_configured
                    }
                ),
                style = MaterialTheme.typography.labelSmall,
                color = if (saved) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        TextField(
            value = draft,
            onValueChange = {
                draft = it
                onTyped()
            },
            placeholder = {
                Text(
                    if (saved) {
                        maskApiKey(savedKey)
                    } else {
                        stringResource(R.string.api_key_enter)
                    }
                )
            },
            singleLine = true,
            visualTransformation = if (visible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (draft.isNotBlank()) onSave(draft) }
            ),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = stringResource(
                            if (visible) {
                                R.string.api_key_hide
                            } else {
                                R.string.api_key_show
                            }
                        )
                    )
                }
            },
            colors = borderlessFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            TextButton(
                onClick = { onSave(draft) },
                enabled = draft.isNotBlank() && draft != savedKey
            ) {
                Text(stringResource(R.string.action_save))
            }
            if (onTest != null) {
                TextButton(
                    onClick = { onTest(candidate) },
                    enabled = candidate.isNotBlank() &&
                        testState != KeyTestState.Checking
                ) {
                    Text(stringResource(R.string.api_key_test))
                }
            }
            if (saved) {
                TextButton(onClick = {
                    onClear()
                    draft = ""
                }) {
                    Text(
                        text = stringResource(R.string.api_key_clear),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        when (testState) {
            KeyTestState.Valid -> KeyTestLine(
                R.string.api_key_valid,
                MaterialTheme.colorScheme.primary
            )
            KeyTestState.Invalid -> KeyTestLine(
                R.string.api_key_invalid,
                MaterialTheme.colorScheme.error
            )
            KeyTestState.Unreachable -> KeyTestLine(
                R.string.api_key_unreachable,
                MaterialTheme.colorScheme.error
            )
            KeyTestState.Checking -> KeyTestLine(
                R.string.api_key_testing,
                MaterialTheme.colorScheme.onSurfaceVariant
            )
            KeyTestState.Idle, null -> Unit
        }
    }
}

@Composable
private fun KeyTestLine(textRes: Int, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodySmall,
        color = color
    )
}

// Masked display: never show a saved key in full ("..........A8F2").
private fun maskApiKey(key: String): String =
    if (key.length <= 4) "...." else ".........." + key.takeLast(4)
