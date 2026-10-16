package com.poe.poegps.feature.presentation.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.poe.poegps.core.designsystem.PoeTheme

/**
 * Before an upload: these lines aren't in GIS under the same name, so the server will add them as new
 * drawings. A line GIS has under a slightly different name would end up drawn twice.
 */
@Composable
fun NewLinesDialog(prompt: NewLinesPrompt, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val c = PoeTheme.colors
    val n = prompt.names.size
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = c.surface,
        title = {
            Text(
                if (n == 1) "Нова лінія в ГІС" else "Нові лінії в ГІС: $n",
                style = PoeTheme.type.statValue, color = c.ink,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (n == 1) "У ГІС немає лінії з такою назвою. Її буде намальовано за опорами з телефону:"
                    else "У ГІС немає ліній з такими назвами. Їх буде намальовано за опорами з телефону:",
                    style = PoeTheme.type.body, color = c.muted,
                )
                Text(
                    prompt.names.take(MAX_NAMES).joinToString("\n") { "• $it" } +
                        if (n > MAX_NAMES) "\n… і ще ${n - MAX_NAMES}" else "",
                    style = PoeTheme.type.body, color = c.ink,
                )
                Text(
                    "Якщо лінія в ГІС уже є під іншою назвою, скасуйте й виправте назву, інакше вона буде намальована двічі.",
                    style = PoeTheme.type.body, color = c.muted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Вивантажити", color = c.actionText) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Скасувати", color = c.ink) }
        },
    )
}

private const val MAX_NAMES = 8
