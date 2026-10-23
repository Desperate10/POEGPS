package com.poe.poegps.feature.presentation.screens.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poe.poegps.core.designsystem.BadgeTone
import com.poe.poegps.core.designsystem.PoeShapes
import com.poe.poegps.core.designsystem.PoeStatusBadge
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.feature.domain.Branch
import com.poe.poegps.feature.presentation.screens.main.plural

/**
 * The object's lines in one place: the main line, then its taps as a tree in line order, each with its
 * survey progress. A tap opens; a long press offers to clear it on the phone.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun BranchesSheet(
    branches: List<Branch>,
    current: String,
    onOpen: (String) -> Unit,
    onClear: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = PoeTheme.colors
    var clearing by remember { mutableStateOf<Branch?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = PoeShapes.sheet,
        dragHandle = { SheetHandle(c) },
    ) {
        val taps = branches.count { !it.isMain }
        Text(
            if (taps > 0) "Гілки лінії · $taps ${plural(taps, "відпайка", "відпайки", "відпайок")}" else "Гілки лінії",
            style = PoeTheme.type.title, color = c.ink,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        )
        LazyColumn(
            Modifier.heightIn(max = 560.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(branches, key = { it.line }) { b ->
                val selected = b.line == current
                Row(
                    Modifier
                        .padding(start = 16.dp + (b.depth.coerceAtMost(4) * 18).dp, end = 16.dp)
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .clip(PoeShapes.row)
                        .background(if (selected) c.actionContainer else c.surfaceMuted)
                        .combinedClickable(
                            role = Role.Button,
                            onClickLabel = "Відкрити",
                            onClick = { onOpen(b.line) },
                            onLongClick = if (b.isMain) null else ({ clearing = b }),
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (b.depth > 0) {
                        Box(Modifier.width(3.dp).heightIn(min = 36.dp).clip(PoeShapes.pill).background(c.outline))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(b.label, style = PoeTheme.type.rowTitle, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        // the GIS name for reference: its numbers are often not the poles'
                        Text(
                            (if (b.isMain) "" else "${b.line} · ") + "уточнено ${b.checked} з ${b.poles}",
                            style = PoeTheme.type.captionRegular, color = c.muted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (b.unsent > 0) PoeStatusBadge("${b.unsent} не надіслано", BadgeTone.Pending)
                }
            }
        }
    }
    clearing?.let { b ->
        AlertDialog(
            onDismissRequest = { clearing = null },
            containerColor = c.surface,
            title = { Text("Очистити відпайку?", style = PoeTheme.type.sheetTitle, color = c.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${b.label} · ${b.line}", style = PoeTheme.type.rowTitle, color = c.ink)
                    Text(
                        "З телефона буде видалено опори цієї відпайки" +
                            (if (b.unsent > 0) ", серед них ${b.unsent} ${plural(b.unsent, "не надіслана", "не надіслані", "не надісланих")}" else "") +
                            ". У ГІС нічого не зміниться: відпайка з ГІС завантажиться знову, коли відкриєте лінію, " +
                            "а створена на телефоні зникне.",
                        style = PoeTheme.type.body, color = c.muted,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { clearing = null; onClear(b.line) }) {
                    Text("Очистити", style = PoeTheme.type.button, color = c.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { clearing = null }) {
                    Text("Скасувати", style = PoeTheme.type.button, color = c.ink)
                }
            },
        )
    }
}
