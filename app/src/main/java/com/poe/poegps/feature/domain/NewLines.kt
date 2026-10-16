package com.poe.poegps.feature.domain

import com.poe.poegps.feature.domain.model.UploadLine

/** A line drawing GIS has: its object, its name and its layer as /gis/geo/lines names them. */
data class GisLineKey(val tplnr: String, val name: String, val layer: String)

/**
 * The /gis/geo/lines layer of the GIS table the server writes a line of this kind to
 * (mirrors RepositoryImpl.lineTarget on the server); null for kinds it doesn't draw.
 */
fun gisLayerOf(pillarType: String, category: String, isAbon: Boolean): String? {
    val voltage = when {
        category.contains("0,4 кВ") -> "04"
        category.contains("10 кВ") -> "10"
        else -> return null
    }
    val kind = when {
        pillarType.equals("PILLAR", ignoreCase = true) -> "len"
        pillarType.equals("KL", ignoreCase = true) -> "lek"
        else -> return null
    }
    return (if (isAbon) "abon_" else "") + kind + voltage
}

/**
 * Lines of an upload that GIS has no drawing for under the same object and exact name: the server
 * will add them as new lines. Lines it won't draw (fewer than 2 poles, unknown kind) aren't listed.
 */
fun linesNewToGis(lines: List<UploadLine>, gis: Set<GisLineKey>): List<UploadLine> = lines.filter { line ->
    val layer = gisLayerOf(line.pillarType, line.category, line.isAbon) ?: return@filter false
    line.poles >= 2 && GisLineKey(line.tplnr, line.name, layer) !in gis
}
