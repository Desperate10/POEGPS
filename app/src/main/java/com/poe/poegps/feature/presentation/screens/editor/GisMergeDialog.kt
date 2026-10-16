package com.poe.poegps.feature.presentation.screens.editor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.feature.presentation.screens.main.plural

/**
 * A line surveyed on this phone before GIS poles were loaded: offer to add the ones GIS has. With them
 * the upload edits the GIS line; with only a few poles the server would leave the line alone.
 */
@Composable
fun GisMergeDialog(offer: GisMergeOffer, onAccept: () -> Unit, onDecline: () -> Unit) {
    val c = PoeTheme.colors
    AlertDialog(
        onDismissRequest = onDecline,
        containerColor = c.surface,
        title = { Text("Додати опори з ГІС?", style = PoeTheme.type.statValue, color = c.ink) },
        text = {
            Text(
                "На телефоні ${offer.onPhone} ${plural(offer.onPhone, "опора", "опори", "опор")} цієї лінії, " +
                    "у ГІС є ще ${offer.toAdd}. Додайте їх, щоб бачити всю лінію і щоб під час вивантаження " +
                    "її було уточнено в ГІС. Опори, які вже є на телефоні, не зміняться.",
                style = PoeTheme.type.body, color = c.muted,
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) { Text("Додати", style = PoeTheme.type.button, color = c.actionText) }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text("Не зараз", style = PoeTheme.type.button, color = c.ink) }
        },
    )
}
