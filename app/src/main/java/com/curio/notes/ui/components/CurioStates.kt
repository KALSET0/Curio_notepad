package com.curio.notes.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.curio.notes.R
import com.curio.notes.ui.theme.Spacing

// Calm centered empty state: short title, one helpful line. No decoration.
@Composable
fun EmptyState(
    title: String,
    body: String = "",
    modifier: Modifier = Modifier,
    showLogo: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xl)
    ) {
        if (showLogo) {
            Image(
                painter = painterResource(R.drawable.curio_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .padding(bottom = Spacing.sm)
            )
        }
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (body.isNotBlank()) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Single inline error style used everywhere a friendly code maps to text.
@Composable
fun ErrorText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier
    )
}
