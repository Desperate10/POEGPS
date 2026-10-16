package com.poe.poegps.feature.domain

import com.poe.poegps.core.location.Geo

/** A GIS drawing that may be the same line under another name, and how many of the line's poles it passes. */
data class NameCandidate(val name: String, val polesOnIt: Int)

/** A drawing passes a pole when one of its vertices is this close: GIS draws lines through their poles. */
const val ON_DRAWING_M = 5.0

/**
 * GIS drawings of the line's object and [layer] that could be the line [lineName] under another name:
 * named, not used by another line on the phone ([usedNames]), and passing at least two of its [poles]
 * (lat to lng). Fewer would not help: the server edits a drawing only when two poles sit on it.
 * Pieces of one drawing count together; the best match comes first.
 */
fun gisNameCandidates(
    lineName: String,
    layer: String,
    poles: List<Pair<Double, Double>>,
    shapes: List<GisShape>,
    usedNames: Set<String>,
): List<NameCandidate> = shapes
    .filter { it.layer == layer && it.name.isNotBlank() && it.name != lineName && it.name !in usedNames }
    .groupBy { it.name }
    .map { (name, pieces) ->
        val vertices = pieces.flatMap { it.vertices }
        NameCandidate(name, poles.count { (lat, lng) -> vertices.any { (vLat, vLng) -> Geo.distanceM(lat, lng, vLat, vLng) <= ON_DRAWING_M } })
    }
    .filter { it.polesOnIt >= 2 }
    .sortedWith(compareByDescending<NameCandidate> { it.polesOnIt }.thenBy { it.name })
