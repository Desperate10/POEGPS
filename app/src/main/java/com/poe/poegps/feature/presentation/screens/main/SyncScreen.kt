package com.poe.poegps.feature.presentation.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.poe.poegps.core.designsystem.PoeIcons
import com.poe.poegps.core.designsystem.PoeOutlinedButton
import com.poe.poegps.core.designsystem.PoePrimaryButton
import com.poe.poegps.core.designsystem.PoeShapes
import com.poe.poegps.core.designsystem.PoeTheme

/** B5 · Синхронізація: progress, then what the GIS server accepted and what it rejected. */
@Composable
fun SyncScreen(
    state: SyncUiState,
    onDone: () -> Unit,
    onRetry: () -> Unit,
) {
    val c = PoeTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(c.ground)
            // drawn over the objects list: keep taps from reaching it
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = {}),
    ) {
        when (state) {
            SyncUiState.Uploading -> Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                CircularProgressIndicator(Modifier.size(48.dp), color = c.action, strokeWidth = 4.dp)
                Text("Вивантаження на сервер ГІС…", style = PoeTheme.type.rowTitle, color = c.ink)
            }
            is SyncUiState.Done -> SyncDone(state, onDone)
            is SyncUiState.Failed -> SyncFailed(state, onDone, onRetry)
        }
    }
}

@Composable
private fun SyncDone(state: SyncUiState.Done, onDone: () -> Unit) {
    val c = PoeTheme.colors
    val allAccepted = state.accepted == state.total
    Column(
        Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(if (allAccepted) c.gps else c.action),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (allAccepted) PoeIcons.Check else PoeIcons.Alert, null,
                    tint = if (allAccepted) c.onGpsStrongOrGround() else c.onAction,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text("Вивантаження завершено · ${state.time}", style = PoeTheme.type.tabSelected, color = c.ink)
        }

        Column(Modifier.semantics(mergeDescendants = true) { heading() }) {
            Text("${state.accepted}", style = PoeTheme.type.display, color = c.action)
            Text(
                "з ${state.total} ${plural(state.total, "запису", "записів", "записів")} прийнято сервером ГІС",
                style = PoeTheme.type.title, color = c.ink,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            items(state.rows) { row -> SyncRowCard(row, state.rejectReason) }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(PoeShapes.row)
                .background(c.inverse)
                .clickable(role = Role.Button, onClick = onDone),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Готово", style = PoeTheme.type.cardTitle, color = c.onInverse)
        }
    }
}

@Composable
private fun SyncRowCard(row: SyncRow, rejectReason: String) {
    val c = PoeTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(PoeShapes.row)
            .background(c.surface)
            .border(if (row.rejected) 2.dp else 1.dp, if (row.rejected) c.action else c.cardBorder, PoeShapes.row)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (row.rejected) PoeIcons.Alert else PoeIcons.Check,
            contentDescription = if (row.rejected) "Відхилено" else "Прийнято",
            tint = if (row.rejected) c.action else c.gps,
            modifier = Modifier.size(20.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.name, style = PoeTheme.type.rowTitle, color = c.ink)
            val detail = if (row.rejected) {
                rejectReason.ifBlank { "Сервер не прийняв цей об'єкт" }
            } else {
                listOfNotNull(
                    // the whole line is sent, but only these poles were new or changed
                    row.poles.takeIf { it > 0 }?.let { "змінено $it ${plural(it, "опору", "опори", "опор")}" },
                    row.reclosers.takeIf { it > 0 }?.let { "$it ${plural(it, "реклоузер", "реклоузери", "реклоузерів")}" },
                ).joinToString(" · ")
            }
            Text(detail, style = PoeTheme.type.captionRegular, color = if (row.rejected) c.actionText else c.muted)
        }
    }
}

@Composable
private fun SyncFailed(state: SyncUiState.Failed, onClose: () -> Unit, onRetry: () -> Unit) {
    val c = PoeTheme.colors
    Column(
        Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(c.actionContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(PoeIcons.Alert, null, tint = c.onActionContainer, modifier = Modifier.size(28.dp))
        }
        Text("Не вдалося вивантажити", style = PoeTheme.type.sheetTitle, color = c.ink, modifier = Modifier.semantics { heading() })
        Text(state.message, style = PoeTheme.type.body, color = c.muted)
        Text(
            "Зняті координати залишаються в телефоні — нічого не втрачено.",
            style = PoeTheme.type.body, color = c.ink,
        )
        Spacer(Modifier.weight(1f))
        PoePrimaryButton("Спробувати ще", onRetry, icon = PoeIcons.Retake)
        PoeOutlinedButton("Закрити", onClose, Modifier.fillMaxWidth())
    }
}

// the check on the blue circle is white in light; in dark the circle is cyan, so the check is the ground
private fun com.poe.poegps.core.designsystem.PoeColors.onGpsStrongOrGround() = if (isDark) mapGround else onGpsStrong

/** Ukrainian plural: 1 опора, 2 опори, 5 опор. */
fun plural(n: Int, one: String, few: String, many: String): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}
