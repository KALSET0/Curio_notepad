package com.curio.notes.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.curio.notes.R

// Single share entry point used on every screen.
@Composable
fun ShareButton(
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    IconButton(onClick = onShare, enabled = enabled, modifier = modifier) {
        Icon(
            Icons.Default.Share,
            contentDescription = stringResource(R.string.cd_share)
        )
    }
}
