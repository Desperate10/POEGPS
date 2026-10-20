package com.poe.poegps.feature.presentation.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poe.poegps.core.designsystem.PoeIcons
import com.poe.poegps.core.designsystem.PoeOverline
import com.poe.poegps.core.designsystem.PoePrimaryButton
import com.poe.poegps.core.designsystem.PoeShapes
import com.poe.poegps.core.designsystem.PoeSheet
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.core.update.UpdatePhase
import com.poe.poegps.core.update.UpdateState
import java.util.Locale

/** A strip under the search field: a newer version exists. Tapping it opens [UpdateSheet]. */
@Composable
fun UpdateBanner(state: UpdateState, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val c = PoeTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(PoeShapes.tile)
            .background(c.actionContainer)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(PoeIcons.Download, contentDescription = null, tint = c.onActionContainer, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title(state), style = PoeTheme.type.rowTitle, color = c.onActionContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(bannerLine(state), style = PoeTheme.type.captionRegular, color = c.onActionContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (state.phase == UpdatePhase.Available) {
            Box(
                Modifier.size(40.dp).clip(PoeShapes.chip).clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(PoeIcons.Close, contentDescription = "Пізніше", tint = c.onActionContainer, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** What's new, the size, and the one button that moves the update on (download → install). */
@Composable
fun UpdateSheet(state: UpdateState, unsent: Int, onProceed: () -> Unit, onClose: () -> Unit) {
    val c = PoeTheme.colors
    BackHandler(onBack = onClose)
    Box(
        Modifier
            .fillMaxSize()
            .background(c.ink.copy(alpha = 0.4f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
    ) {
        PoeSheet(Modifier.align(Alignment.BottomCenter).clickable(remember { MutableInteractionSource() }, indication = null) {}) {
            Text(title(state), style = PoeTheme.type.title, color = c.ink)
            UpdateBody(state, unsent, onProceed)
        }
    }
}

/** Below the server's minimum version: uploads are refused, so nothing else is offered until the update. */
@Composable
fun RequiredUpdateScreen(state: UpdateState, unsent: Int, onProceed: () -> Unit) {
    val c = PoeTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.ground)
            // the screen under it must not get taps
            .clickable(remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        PoeOverline("Потрібне оновлення")
        Text(
            state.versionName?.let { "Версія $it" } ?: "Нова версія",
            style = PoeTheme.type.sheetTitle, color = c.ink,
        )
        Text(
            "Ця версія застосунку застаріла: сервер ГІС більше не приймає з неї зміни. Оновіть, щоб продовжити роботу.",
            style = PoeTheme.type.body, color = c.inkSecondary,
        )
        UpdateBody(state, unsent, onProceed)
    }
}

@Composable
private fun UpdateBody(state: UpdateState, unsent: Int, onProceed: () -> Unit = {}) {
    val c = PoeTheme.colors
    if (state.notes.isNotBlank()) {
        Text(
            state.notes,
            style = PoeTheme.type.body, color = c.ink,
            modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
        )
    }
    if (unsent > 0) {
        Text(
            "Не надіслано: $unsent. Зміни на телефоні збережуться під час оновлення.",
            style = PoeTheme.type.captionRegular, color = c.actionText,
        )
    }
    state.error?.let { Text(it, style = PoeTheme.type.captionStrong, color = c.danger) }
    if (state.phase == UpdatePhase.Downloading) {
        val p = state.progress
        if (p != null) {
            LinearProgressIndicator(
                progress = { p }, color = c.action, trackColor = c.tabTrack,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        } else {
            LinearProgressIndicator(color = c.action, trackColor = c.tabTrack, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        }
    }
    if (state.unavailable && !state.playStore) {
        Text(
            "Нової версії для встановлення ще немає на сервері. Повідомте адміністратора.",
            style = PoeTheme.type.body, color = c.muted,
        )
        return
    }
    Spacer(Modifier.size(2.dp))
    PoePrimaryButton(
        text = buttonText(state),
        onClick = onProceed,
        icon = if (state.phase == UpdatePhase.Ready) PoeIcons.Check else PoeIcons.Download,
        enabled = state.phase == UpdatePhase.Available || state.phase == UpdatePhase.Ready,
        disabledBackground = c.tabTrack,
    )
    if (state.phase == UpdatePhase.Ready && !state.playStore) {
        Text(
            "Android попросить підтвердити. Після встановлення відкрийте застосунок знову.",
            style = PoeTheme.type.caption, color = c.muted,
        )
    }
}

private fun title(state: UpdateState) =
    state.versionName?.let { "Нова версія $it" } ?: "Доступне оновлення"

private fun bannerLine(state: UpdateState) = when (state.phase) {
    UpdatePhase.Available -> if (state.playStore) "Оновити через Google Play" else "Натисніть, щоб оновити" + (state.sizeBytes?.let { " · ${megabytes(it)}" } ?: "")
    UpdatePhase.Downloading -> state.progress?.let { "Завантаження ${(it * 100).toInt()}%" } ?: "Завантаження…"
    UpdatePhase.Ready -> if (state.playStore) "Завантажено: перезапустіть, щоб оновити" else "Завантажено: можна встановлювати"
    UpdatePhase.Installing -> "Встановлення…"
}

private fun buttonText(state: UpdateState) = when (state.phase) {
    UpdatePhase.Available -> when {
        state.unavailable -> "Відкрити Google Play"
        state.playStore -> "Оновити"
        else -> "Завантажити" + (state.sizeBytes?.let { " · ${megabytes(it)}" } ?: "")
    }
    UpdatePhase.Downloading -> state.progress?.let { "Завантаження ${(it * 100).toInt()}%" } ?: "Завантаження…"
    UpdatePhase.Ready -> if (state.playStore) "Перезапустити й оновити" else "Встановити"
    UpdatePhase.Installing -> "Встановлення…"
}

private fun megabytes(bytes: Long) = String.format(Locale.US, "%.1f МБ", bytes / 1_048_576.0).replace('.', ',')
