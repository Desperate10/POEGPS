package com.poe.poegps.feature.presentation.capture

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.poe.poegps.core.designsystem.PoeTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import java.util.Locale

/**
 * What a capture screen tells the crew: a snackbar for each [CaptureResult] (plus any [messages])
 * and the "position looks wrong" confirmation, whose approval goes to [onConfirmed].
 */
@Composable
fun BoxScope.CaptureFeedback(
    results: Flow<CaptureResult>,
    onConfirmed: (CaptureResult.NeedsConfirm) -> Unit,
    messages: Flow<String> = emptyFlow(),
) {
    val c = PoeTheme.colors
    val snackbar = remember { SnackbarHostState() }
    var confirm by remember { mutableStateOf<CaptureResult.NeedsConfirm?>(null) }

    LaunchedEffect(Unit) {
        merge(
            messages,
            results.mapNotNull {
                when (it) {
                    is CaptureResult.Captured ->
                        "${it.pole}: координати знято" + (it.accuracy?.let { a -> String.format(Locale.US, " · ±%.1f м", a) } ?: "")
                    is CaptureResult.Placed -> "${it.pole}: позицію поставлено вручну"
                    CaptureResult.NoFix -> "Немає позиції GPS — зачекайте на супутники"
                    is CaptureResult.Locked -> "Знімання вимкнено: ${it.trust.level.label.lowercase()} GPS"
                    is CaptureResult.NeedsAccuracy ->
                        "Точність ${it.accuracy?.let { a -> String.format(Locale.US, "±%.0f м", a) } ?: "невідома"} — " +
                            "чекаємо ≤ ${CapturePolicy.ACCURACY_GATE_M.toInt()} м"
                    is CaptureResult.NeedsConfirm -> {
                        confirm = it
                        null
                    }
                }
            },
        ).collect { snackbar.showSnackbar(it) }
    }

    confirm?.let { pending ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = c.surface,
            title = { Text("Позиція схожа на хибну", style = PoeTheme.type.statValue, color = c.ink) },
            text = {
                Text(
                    pending.warnings.joinToString("\n") { "• $it" } + "\n\nПеревірте, що ви біля «${pending.pole.name}».",
                    style = PoeTheme.type.body, color = c.ink,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirm = null; onConfirmed(pending) }) {
                    Text(if (pending.manual != null) "Зберегти все одно" else "Зняти все одно", style = PoeTheme.type.button, color = c.actionText)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) {
                    Text("Скасувати", style = PoeTheme.type.button, color = c.ink)
                }
            },
        )
    }
    SnackbarHost(
        snackbar,
        Modifier.align(Alignment.TopCenter).padding(top = 112.dp, start = 16.dp, end = 16.dp),
    ) { data ->
        Snackbar(data, containerColor = c.inverse, contentColor = c.onInverse)
    }
}
