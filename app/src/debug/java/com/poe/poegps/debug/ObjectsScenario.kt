package com.poe.poegps.debug

import com.poe.poegps.feature.domain.UploadSummary
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import com.poe.poegps.R
import com.poe.poegps.feature.domain.model.LineClearSummary
import com.poe.poegps.feature.domain.model.LineProgress
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.screens.main.Filial
import com.poe.poegps.feature.presentation.screens.main.MainUiState
import com.poe.poegps.feature.presentation.screens.main.ObjectItem
import com.poe.poegps.feature.presentation.screens.main.ObjectsScreen

private fun line(tplnr: String, name: String, total: Int, captured: Int, unsent: Int, gis: Int = 0) = ObjectItem(
    obj = ObjectDisplayable(tplnr = tplnr, name = name, category = "ПЛ-0,4 кВ", pillarType = "PILLAR", isAbon = false),
    progress = LineProgress(tplnr, name, false, total, captured, unsent).takeIf { total > 0 },
    gisPoles = gis,
)

private val SAMPLE_LINES = listOf(
    line("PO-F-08-L1278", "ПЛ-0,4 КВ ЗТП-338 л-1 смт.Опішня", 5, 3, 3, gis = 5),
    line("PO-F-08-L1301", "ПЛ-0,4 КВ КТП-112 л-2", 12, 12, 0),
    line("PO-F-08-L1302", "ПЛ-0,4 КВ КТП-112 л-3", 0, 0, 0, gis = 18),
    line("PO-F-08-L1310", "ПЛ-0,4 КВ ЗТП-41 л-1", 0, 0, 0),
    line("PO-F-08-L1311", "ПЛ-0,4 КВ ЗТП-41 л-2", 7, 2, 2),
)

/** B2 with sample lines: `--es screen objects` (the emulator can't log in to load real ones). */
@Composable
fun ObjectsScenario() {
    ObjectsScreen(
        state = MainUiState(items = SAMPLE_LINES, total = SAMPLE_LINES.size, unsent = 5, filial = "08"),
        filials = stringArrayResource(R.array.filials).map(Filial::parse),
        uploading = false,
        onTypeChange = {},
        onQueryChange = {},
        onOpen = {},
        onUpload = {},
        uploadSummary = { UploadSummary(line = 3, others = 5, otherLines = 2) },
        onDeleteTap = {},
        onClearLine = {},
        clearSummary = { LineClearSummary(taps = 2, unsent = 3) },
        onDownload = {},
        onUploadAll = {},
        onFilialSelected = {},
        downloadedAt = System.currentTimeMillis() - 3_600_000,
    )
}
