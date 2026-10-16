package com.poe.poegps.feature.presentation.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poe.poegps.core.designsystem.PoeShapes
import com.poe.poegps.core.designsystem.PoeTheme

/**
 * Before an upload: these lines aren't in GIS under their name, so the server will add them as new
 * drawings. When GIS has a drawing of the object through the line's poles under another name, the
 * crew picks whether it is that line (it takes the GIS name, and the upload edits the drawing) or a new
 * one; such lines must be decided before uploading. The rest are added as new.
 */
@Composable
fun NewLinesDialog(prompt: NewLinesPrompt, onConfirm: (Map<NewLine, String>) -> Unit, onCancel: () -> Unit) {
    val c = PoeTheme.colors
    val n = prompt.lines.size
    // per line: the GIS name picked, "" for "a new line"; absent = not decided yet
    val picks = remember(prompt) { mutableStateMapOf<NewLine, String>() }
    val undecided = prompt.lines.count { it.candidates.isNotEmpty() && it !in picks }
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (n == 1) "У ГІС немає лінії з такою назвою. Її буде намальовано за опорами з телефону."
                    else "У ГІС немає ліній з такими назвами. Їх буде намальовано за опорами з телефону.",
                    style = PoeTheme.type.body, color = c.muted,
                )
                prompt.lines.forEach { line ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("• ${line.name}", style = PoeTheme.type.body, color = c.ink)
                        if (line.candidates.isNotEmpty()) {
                            Text(
                                "У ГІС через ці опори проходить лінія з іншою назвою. Це вона?",
                                style = PoeTheme.type.captionRegular, color = c.actionText,
                            )
                            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                line.candidates.forEach { cand ->
                                    Choice(
                                        "Так, це «${cand.name}»",
                                        "${cand.polesOnIt} ${plural(cand.polesOnIt, "опора", "опори", "опор")} на ній · візьме назву з ГІС",
                                        selected = picks[line] == cand.name,
                                    ) { picks[line] = cand.name }
                                }
                                Choice("Ні, це нова лінія", "буде намальована окремо", selected = picks[line] == "") {
                                    picks[line] = ""
                                }
                            }
                        }
                    }
                }
                if (prompt.lines.any { it.candidates.isEmpty() }) {
                    Text(
                        "Якщо лінія в ГІС уже є під іншою назвою, скасуйте й перевірте назву, інакше вона буде намальована двічі.",
                        style = PoeTheme.type.captionRegular, color = c.muted,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(picks.filterValues { it.isNotEmpty() }) },
                enabled = undecided == 0,
            ) {
                Text(if (undecided == 0) "Вивантажити" else "Оберіть вище", color = if (undecided == 0) c.actionText else c.muted)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Скасувати", color = c.ink) }
        },
    )
}

@Composable
private fun Choice(title: String, note: String, selected: Boolean, onSelect: () -> Unit) {
    val c = PoeTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(PoeShapes.row)
            .background(if (selected) c.surface else c.surfaceMuted)
            .border(if (selected) 2.5.dp else 1.dp, if (selected) c.action else c.outlineVariant, PoeShapes.row)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = PoeTheme.type.rowTitle, color = c.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Text(note, style = PoeTheme.type.captionRegular, color = if (selected) c.actionText else c.muted)
    }
}
