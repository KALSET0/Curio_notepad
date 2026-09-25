package com.curio.notes.ui.components

import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.curio.notes.R

@Composable
fun CopyIconButton(
    text: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    IconButton(
        onClick = { copyToClipboard(context, text) },
        modifier = modifier
    ) {
        Icon(
            Icons.Default.ContentCopy,
            contentDescription = stringResource(R.string.cd_copy),
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Curio", text))
    Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT)
        .show()
}
