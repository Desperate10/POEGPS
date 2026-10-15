package com.poe.poegps.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.poe.poegps.core.designsystem.BadgeTone
import com.poe.poegps.core.designsystem.PoeActionRow
import com.poe.poegps.core.designsystem.PoeCard
import com.poe.poegps.core.designsystem.PoeChip
import com.poe.poegps.core.designsystem.PoeGpsPill
import com.poe.poegps.core.designsystem.PoeIcons
import com.poe.poegps.core.designsystem.PoeOutlinedButton
import com.poe.poegps.core.designsystem.PoeOverlayIconButton
import com.poe.poegps.core.designsystem.PoeOverlayTitle
import com.poe.poegps.core.designsystem.PoeOverline
import com.poe.poegps.core.designsystem.PoePrimaryButton
import com.poe.poegps.core.designsystem.PoeSearchField
import com.poe.poegps.core.designsystem.PoeSegmentedTabs
import com.poe.poegps.core.designsystem.PoeSheet
import com.poe.poegps.core.designsystem.PoeStatTile
import com.poe.poegps.core.designsystem.PoeStatusBadge
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.feature.presentation.screens.main.SyncRow
import com.poe.poegps.feature.presentation.screens.main.SyncScreen
import com.poe.poegps.feature.presentation.screens.main.SyncUiState

private val SAMPLE_SYNC = SyncUiState.Done(
    accepted = 15,
    total = 16,
    rows = listOf(
        SyncRow("Л-12 · Кохнівка", poles = 6, reclosers = 1, rejected = false),
        SyncRow("Л-14 · Рокита", poles = 6, reclosers = 0, rejected = false),
        SyncRow("ТП-245", poles = 1, reclosers = 0, rejected = true),
    ),
    time = "09:14",
    rejectReason = "Опора з такою назвою вже існує",
)

/** Every design-system component on one page, in the current system theme. */
class ThemeGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // --es screen sync: the upload result screen with sample data (the emulator can't reach the GIS server)
        // --es screen gps-…: the line editor in GPS states, see EditorScenarios.kt
        val screen = intent.getStringExtra("screen")
        setContent {
            PoeTheme {
                when {
                    screen == "sync" -> SyncScreen(SAMPLE_SYNC, onDone = ::finish, onRetry = {})
                    screen?.startsWith("gps-") == true -> EditorScenario(screen, onExit = ::finish)
                    else -> Gallery()
                }
            }
        }
    }
}

@Composable
private fun Gallery() {
    val c = PoeTheme.colors
    var tab by remember { mutableIntStateOf(0) }
    var hv by remember { mutableStateOf(true) }
    var abon by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("Кохнівка") }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.ground)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Об'єкти", style = PoeTheme.type.screenTitle, color = c.ink)
            PoeSegmentedTabs(listOf("Лінії ПЛ", "ТП/РП", "КЛ/КВ"), tab, { tab = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PoeChip("6-10 кВ", hv, { hv = true })
                PoeChip("0,4 кВ", !hv, { hv = false })
                Spacer(Modifier.weight(1f))
                PoeChip("Абон.", abon, { abon = !abon }, icon = PoeIcons.Person, dashed = true)
            }
            PoeSearchField(query, { query = it }, trailing = "3 з 186")
            PoeCard(onClick = {}) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Л-12 · ПС «Кохнівка»", style = PoeTheme.type.cardTitle, color = c.ink)
                        Text("02-PS15-L12 · ПЛ-10 кВ", style = PoeTheme.type.monoSmall, color = c.muted)
                    }
                    PoeStatusBadge("6 не надіслано", BadgeTone.Pending)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PoeStatusBadge("Готово", BadgeTone.Done, icon = PoeIcons.Check)
                    PoeStatusBadge("без координат", BadgeTone.Neutral)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PoeOverlayIconButton(PoeIcons.Back, "Назад", {})
                PoeOverlayTitle("Л-12 · Кохнівка", "18 з 24 опор · ПЛ-10 кВ", Modifier.weight(1f))
                PoeOverlayIconButton(PoeIcons.Plus, "Додати опору", {}, filled = true)
            }
            PoeGpsPill("±2.8 м · 14 супутн.")
        }

        PoeSheet {
            Column {
                PoeOverline("Наступна опора")
                Text("Опора 13", style = PoeTheme.type.hero, color = c.ink)
                Text("49.58731, 34.55412", style = PoeTheme.type.mono, color = c.muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PoeStatTile("Точність", "2.8 м", Modifier.weight(1f), valueColor = c.gpsText)
                PoeStatTile("Супутники", "14", Modifier.weight(1f), suffix = "/22")
                PoeStatTile("Прогон", "86 м", Modifier.weight(1f))
            }
            PoePrimaryButton("Зняти координати", {}, icon = PoeIcons.Capture)
            PoePrimaryButton("Чекаємо точності ≤ 5 м", {}, icon = PoeIcons.Clock, enabled = false)
            PoeActionRow(PoeIcons.Tap, "Створити відпайку", "Відп. від оп. 12 — нова гілка", {},
                iconBackground = c.action, iconTint = c.onAction)
            PoeActionRow(PoeIcons.Recloser, "Реклоузер / роз'єднувач", "Комутаційний апарат на опорі", {})
            PoeActionRow(PoeIcons.Retake, "Перезняти координати", "Зараз точніше: ±2.8 м", {},
                iconBackground = if (c.isDark) c.gpsContainer else c.gps, iconTint = if (c.isDark) c.gps else c.onGpsStrong)
            PoeActionRow(PoeIcons.Move, "Уточнити на мапі", "Пересунути позначку вручну", {},
                iconBackground = c.surface, iconTint = c.action, iconBorder = c.action)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PoeOutlinedButton("Очистити", {}, Modifier.weight(1f))
                PoeOutlinedButton("Видалити", {}, Modifier.weight(1f), icon = PoeIcons.Trash, danger = true)
            }
        }
    }
}
