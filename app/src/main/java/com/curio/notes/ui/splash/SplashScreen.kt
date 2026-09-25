package com.curio.notes.ui.splash

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.curio.notes.R
import com.curio.notes.ui.theme.SecondaryDark
import com.curio.notes.ui.util.rememberAppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

const val SPLASH_MIN_MILLIS = 1500L
const val SPLASH_MAX_WAIT_MILLIS = 5000L

/** Advances once the minimum time passed AND the inbox emitted (or gave up waiting). */
fun shouldAdvanceSplash(elapsedMillis: Long, notesReady: Boolean): Boolean =
    notesReady && elapsedMillis >= SPLASH_MIN_MILLIS

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val container = rememberAppContainer()
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600)
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = tween(600)
    )

    LaunchedEffect(Unit) {
        visible = true
        val start = SystemClock.elapsedRealtime()
        withTimeoutOrNull(SPLASH_MAX_WAIT_MILLIS) {
            val readyJob = launch { container.noteRepository.observeNotes().first() }
            while (
                !shouldAdvanceSplash(
                    SystemClock.elapsedRealtime() - start,
                    readyJob.isCompleted
                )
            ) {
                delay(50)
            }
            readyJob.cancel()
        }
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
        ) {
            Image(
                painter = painterResource(R.drawable.curio_logo),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryDark
            )
        }
    }
}
