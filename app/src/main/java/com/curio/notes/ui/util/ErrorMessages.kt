package com.curio.notes.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.curio.notes.R

@Composable
fun errorMessageFor(code: String): String = when (code) {
    "missing_api_key" -> stringResource(R.string.error_missing_api_key)
    "invalid_api_key" -> stringResource(R.string.error_invalid_api_key)
    "network" -> stringResource(R.string.error_network)
    "timed_out" -> stringResource(R.string.error_timed_out)
    "rate_limited" -> stringResource(R.string.error_rate_limited)
    "invalid_response" -> stringResource(R.string.error_invalid_response)
    "service_error" -> stringResource(R.string.error_service)
    "save_failed" -> stringResource(R.string.error_save_failed)
    "save_changes_failed" -> stringResource(R.string.error_save_changes_failed)
    "delete_failed" -> stringResource(R.string.error_delete_failed)
    "conversation_failed" -> stringResource(R.string.error_conversation_failed)
    "unknown" -> stringResource(R.string.error_unknown)
    else -> code
}
