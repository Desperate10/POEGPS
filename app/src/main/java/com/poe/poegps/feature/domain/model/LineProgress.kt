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

/** A line an upload sends, with what decides the GIS table the server writes it to. */
data class UploadLine(
    val tplnr: String,
    val name: String,
    val pillarType: String,
    val category: String,
    val isAbon: Boolean,
    val poles: Int,
)

/** Captured poles of one line that go into an upload. */
data class UploadGroup(
    val tplnr: String,
    val name: String,
    val poles: Int,
)
