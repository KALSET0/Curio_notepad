package com.curio.notes.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.notes.R
import com.curio.notes.domain.model.AiProviderChoice
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.AppTheme
import com.curio.notes.ui.components.ErrorText
import com.curio.notes.ui.components.SectionHeader
import com.curio.notes.ui.util.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = run {
        val container = rememberAppContainer()
        viewModel(
            factory = SettingsViewModel.factory(
                container.settingsRepository,
                container.isGeminiConfigured,
                container.isOpenRouterConfigured,
                container.appVersion
            )
        )
    }
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val provider by viewModel.aiProvider.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val webSearchEnabled by viewModel.webSearchEnabled.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cd_settings)) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionHeader(stringResource(R.string.section_provider))
            RadioOption(
                selected = provider == AiProviderChoice.MOCK,
                title = stringResource(R.string.provider_mock),
                subtitle = stringResource(R.string.provider_mock_sub),
                onSelect = { viewModel.setAiProvider(AiProviderChoice.MOCK) },
                icon = Icons.Default.SmartToy
            )
            RadioOption(
                selected = provider == AiProviderChoice.GEMINI,
                title = stringResource(R.string.provider_gemini),
                subtitle = if (viewModel.isGeminiConfigured) {
                    stringResource(R.string.provider_gemini_ok)
                } else {
                    stringResource(R.string.provider_gemini_missing)
                },
                onSelect = { viewModel.setAiProvider(AiProviderChoice.GEMINI) },
                icon = Icons.Default.AutoAwesome
            )
            RadioOption(
                selected = provider == AiProviderChoice.OPENROUTER,
                title = stringResource(R.string.provider_openrouter),
                subtitle = if (viewModel.isOpenRouterConfigured) {
                    stringResource(R.string.provider_openrouter_ok)
                } else {
                    stringResource(R.string.provider_openrouter_missing)
                },
                onSelect = { viewModel.setAiProvider(AiProviderChoice.OPENROUTER) },
                icon = Icons.Default.Cloud
            )
            if (provider == AiProviderChoice.GEMINI && !viewModel.isGeminiConfigured) {
                ErrorText(
                    text = stringResource(R.string.provider_gemini_hint),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            if (provider == AiProviderChoice.OPENROUTER && !viewModel.isOpenRouterConfigured) {
                ErrorText(
                    text = stringResource(R.string.provider_openrouter_hint),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            SectionHeader(stringResource(R.string.section_appearance))
            RadioOption(
                selected = theme == AppTheme.SYSTEM,
                title = stringResource(R.string.theme_system),
                subtitle = stringResource(R.string.theme_system_sub),
                onSelect = { viewModel.setTheme(AppTheme.SYSTEM) },
                icon = Icons.Default.Smartphone
            )
            RadioOption(
                selected = theme == AppTheme.LIGHT,
                title = stringResource(R.string.theme_light),
                subtitle = null,
                onSelect = { viewModel.setTheme(AppTheme.LIGHT) },
                icon = Icons.Default.LightMode
            )
            RadioOption(
                selected = theme == AppTheme.DARK,
                title = stringResource(R.string.theme_dark),
                subtitle = null,
                onSelect = { viewModel.setTheme(AppTheme.DARK) },
                icon = Icons.Default.DarkMode
            )

            SettingsDivider()

            SectionHeader(stringResource(R.string.section_websearch))
            SwitchOption(
                checked = webSearchEnabled,
                title = stringResource(R.string.websearch_title),
                subtitle = stringResource(R.string.websearch_sub),
                onCheckedChange = viewModel::setWebSearchEnabled,
                icon = Icons.Default.Public
            )

            SettingsDivider()

            SectionHeader(stringResource(R.string.section_language))
            RadioOption(
                selected = language == AppLanguage.SYSTEM,
                title = stringResource(R.string.lang_system),
                subtitle = stringResource(R.string.lang_system_sub),
                onSelect = { viewModel.setLanguage(AppLanguage.SYSTEM) },
                icon = Icons.Default.Translate
            )
            RadioOption(
                selected = language == AppLanguage.ENGLISH,
                title = stringResource(R.string.lang_english),
                subtitle = null,
                onSelect = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                icon = Icons.Default.Translate
            )
            RadioOption(
                selected = language == AppLanguage.SPANISH,
                title = stringResource(R.string.lang_spanish),
                subtitle = null,
                onSelect = { viewModel.setLanguage(AppLanguage.SPANISH) },
                icon = Icons.Default.Translate
            )

            SettingsDivider()

            SectionHeader(stringResource(R.string.section_about))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.curio_logo),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.about_version, viewModel.appVersion),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = stringResource(R.string.about_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SettingsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun RadioOption(
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
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

@Composable
private fun SwitchOption(
    checked: Boolean,
    title: String,
    subtitle: String?,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { onCheckedChange(!checked) })
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon?.let {
            Icon(
                it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
