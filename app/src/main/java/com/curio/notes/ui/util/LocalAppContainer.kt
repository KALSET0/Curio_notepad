package com.curio.notes.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.curio.notes.CurioApplication
import com.curio.notes.di.AppContainer

@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current
    return (context.applicationContext as CurioApplication).container
}
