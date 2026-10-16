package com.poe.poegps.feature.presentation.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.poe.poegps.core.designsystem.PoeTheme

/**
 * Before an upload: these lines aren't in GIS, by name or through their poles, so the server will add
 * them as new drawings. A line GIS draws through other points (surveyed under other pole numbers) would
 * end up drawn twice, so the crew confirms.
 */
@Composable
fun NewLinesDialog(prompt: NewLinesPrompt, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val c = PoeTheme.colors
    val n = prompt.lines.size
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
            Column(
                Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    if (n == 1) "У ГІС немає цієї лінії. Її буде намальовано за опорами з телефону:"
                    else "У ГІС немає цих ліній. Їх буде намальовано за опорами з телефону:",
                    style = PoeTheme.type.body, color = c.muted,
                )
                Text(
                    prompt.lines.take(MAX_NAMES).joinToString("\n") { "• ${it.name}" } +
                        if (n > MAX_NAMES) "\n… і ще ${n - MAX_NAMES}" else "",
                    style = PoeTheme.type.body, color = c.ink,
                )
                Text(
                    "Якщо лінія в ГІС уже є, але опори на телефоні інші (знято з нуля, інша нумерація), скасуйте: " +
                        "завантажте її опори з ГІС, інакше лінію буде намальовано двічі.",
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
