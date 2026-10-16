package com.poe.poegps.feature.domain

import com.poe.poegps.core.location.Geo

/** One place in a merged line: a pole already on the phone (by id) or a GIS pole to add. */
sealed interface MergedPole {
    data class Local(val id: Int) : MergedPole
    data class Gis(val point: GisPoint) : MergedPole
}

/**
 * Which of [gis] is the GIS row of the pole [name] now at [lat]/[lng]: its namesake nearest to it, as the
 * server picks the row it updates (taps reuse numbers, a moved pole is metres away). Without coordinates
 * only a single namesake counts. Null when GIS has none.
 */
fun nearestNamesake(name: String, lat: Double?, lng: Double?, gis: List<GisPoint>): Int? {
    val key = name.trim().lowercase()
    val namesakes = gis.indices.filter { gis[it].name.trim().lowercase() == key }
    if (lat == null || lng == null) return namesakes.singleOrNull()
    return namesakes.minByOrNull { Geo.distanceM(lat, lng, gis[it].lat, gis[it].lng) }
}

/** A pole of the line on the phone, in its line order. */
data class LocalPole(val id: Int, val name: String)

/**
 * A line surveyed on the phone before its GIS poles were loaded, completed with them: the GIS order
 * ([gis], from its drawing) is the backbone, a phone pole with a GIS namesake takes that namesake's
 * place (keeping the crew's coordinates), a phone pole GIS doesn't have stays right after the phone
 * pole it followed, and the GIS poles the phone lacks are added. Null when there is nothing to add,
 * or when no phone pole shares a name with GIS: then the numbering differs and merging would double poles.
 */
fun mergeWithGis(local: List<LocalPole>, gis: List<GisPoint>): List<MergedPole>? {
    fun key(name: String) = name.trim().lowercase()
    val localKeys = local.map { key(it.name) }.toSet()
    val gisKeys = gis.map { key(it.name) }.toSet()
    if (gis.none { key(it.name) !in localKeys }) return null
    if (local.none { key(it.name) in gisKeys }) return null

    val localByKey = local.associateBy { key(it.name) }
    val out = gis.mapTo(mutableListOf()) { p -> localByKey[key(p.name)]?.let { MergedPole.Local(it.id) } ?: MergedPole.Gis(p) }
    // phone-only poles, in their phone order, after the phone pole before them
    var anchor = -1
    for (pole in local) {
        val at = out.indexOf(MergedPole.Local(pole.id))
        if (key(pole.name) in gisKeys && at >= 0) {
            anchor = at
        } else if (at < 0) {
            out.add(anchor + 1, MergedPole.Local(pole.id))
            anchor += 1
        }
    }
    return out
}
