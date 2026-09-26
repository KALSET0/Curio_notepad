package com.curio.notes.ui.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.notes.R
import com.curio.notes.domain.model.AiProviderChoice
import com.curio.notes.domain.model.ApiKeyKind
import com.curio.notes.ui.components.ApiKeyField
import com.curio.notes.ui.components.CurioBackground
import com.curio.notes.ui.components.SettingsCard
import com.curio.notes.ui.settings.SettingsViewModel
import com.curio.notes.ui.theme.Spacing
import com.curio.notes.ui.util.rememberAppContainer

// Optional first-run AI setup. Nothing here is mandatory: Skip leaves the
// safe Mock default and marks setup complete; keys can be added later in
// Settings. Shown once (setup_completed flag), never blocks the app.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onDone: () -> Unit,
    viewModel: SettingsViewModel = run {
        val container = rememberAppContainer()
        viewModel(
            factory = SettingsViewModel.factory(
                container.settingsRepository,
                container.isGeminiConfigured(),
                container.isOpenRouterConfigured(),
                container.appVersion,
                container.testOllamaConnection,
                container.apiKeyStorage::observeKey,
                container.apiKeyStorage::setKey,
                container.apiKeyStorage::clearKey,
                container.validateGeminiKey,
                container.validateOpenRouterKey
            )
        )
    }
) {
    val provider by viewModel.aiProvider.collectAsStateWithLifecycle()
    val geminiKey by viewModel.geminiKey.collectAsStateWithLifecycle()
    val openRouterKey by viewModel.openRouterKey.collectAsStateWithLifecycle()
    val geminiTest by viewModel.geminiTest.collectAsStateWithLifecycle()
    val openRouterTest by viewModel.openRouterTest.collectAsStateWithLifecycle()

    CurioBackground {
        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    text = stringResource(R.string.setup_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.setup_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SettingsCard {
                    SetupOption(
                        selected = provider == AiProviderChoice.MOCK,
                        title = stringResource(R.string.provider_mock),
                        subtitle = stringResource(R.string.provider_mock_sub),
                        onSelect = { viewModel.setAiProvider(AiProviderChoice.MOCK) },
                        icon = Icons.Default.SmartToy
                    )
                    SetupOption(
                        selected = provider == AiProviderChoice.GEMINI,
                        title = stringResource(R.string.provider_gemini),
                        subtitle = stringResource(R.string.provider_gemini_missing),
                        onSelect = { viewModel.setAiProvider(AiProviderChoice.GEMINI) },
                        icon = Icons.Default.AutoAwesome
                    )
                    if (provider == AiProviderChoice.GEMINI) {
                        ApiKeyField(
                            savedKey = geminiKey,
                            testState = geminiTest,
                            onSave = { viewModel.saveApiKey(ApiKeyKind.GEMINI, it) },
                            onClear = { viewModel.clearApiKey(ApiKeyKind.GEMINI) },
                            onTest = viewModel::testGeminiKey,
                            onTyped = { viewModel.resetKeyTest(ApiKeyKind.GEMINI) }
                        )
                    }
                    SetupOption(
                        selected = provider == AiProviderChoice.OPENROUTER,
                        title = stringResource(R.string.provider_openrouter),
                        subtitle = stringResource(R.string.provider_openrouter_missing),
                        onSelect = { viewModel.setAiProvider(AiProviderChoice.OPENROUTER) },
                        icon = Icons.Default.Cloud
                    )
                    if (provider == AiProviderChoice.OPENROUTER) {
                        ApiKeyField(
                            savedKey = openRouterKey,
                            testState = openRouterTest,
                            onSave = { viewModel.saveApiKey(ApiKeyKind.OPENROUTER, it) },
                            onClear = { viewModel.clearApiKey(ApiKeyKind.OPENROUTER) },
                            onTest = viewModel::testOpenRouterKey,
                            onTyped = { viewModel.resetKeyTest(ApiKeyKind.OPENROUTER) }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.api_key_local_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        viewModel.completeSetup()
                        onDone()
                    }) {
                        Text(stringResource(R.string.setup_skip))
                    }
                    Button(onClick = {
                        viewModel.completeSetup()
                        onDone()
                    }) {
                        Text(stringResource(R.string.setup_continue))
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupOption(
    selected: Boolean,
    title: String,
    subtitle: String?,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        icon?.let {
            Icon(
                it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
