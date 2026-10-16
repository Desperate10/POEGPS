package com.poe.poegps.feature.domain.model

/** Capture progress of one line (or tap, or TP) in the local line_pillar table. */
data class LineProgress(
    val tplnr: String,
    val parentName: String,
    val isAbon: Boolean,
    val total: Int,
    val captured: Int,
    val unsent: Int,
)

/** How many poles GIS has for a line; [name] is the line's first row (taps share its tplnr). */
data class GisPoleCount(
    val tplnr: String,
    val name: String?,
    val poles: Int,
)

/** Captured poles of one line that go into an upload. */
data class UploadGroup(
    val tplnr: String,
    val name: String,
    val poles: Int,
)

data class RecloserCount(
    val tplnr: String,
    val count: Int,
)
