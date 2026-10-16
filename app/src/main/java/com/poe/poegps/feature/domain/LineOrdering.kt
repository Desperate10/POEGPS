package com.poe.poegps.feature.domain

import com.poe.poegps.core.location.Geo

/** A GIS pole of the object (pillars04/pillars10). */
data class GisPoint(val name: String, val lat: Double, val lng: Double)

/** One line geometry of the object from /gis/geo/lines: the main line, a tap ("Відп. від оп. …") or unnamed. */
data class GisShape(val name: String, val vertices: List<Pair<Double, Double>>, val layer: String = "")

/** The poles of one line in the order to load them; [fromShape] = taken from its GIS geometry, else by pole number. */
data class OrderedLine(val poles: List<GisPoint>, val fromShape: Boolean)

/**
 * Puts the GIS poles of [lineName] in line order. GIS keeps no order for poles, and taps share the line's
 * tplnr, but every line geometry was built through its poles: the main line's vertices, matched to the
 * nearest pole within [MATCH_M], give both which poles belong to it and their order.
 */
object LineOrdering {
    const val MATCH_M = 3.0

    fun order(lineName: String, poles: List<GisPoint>, shapes: List<GisShape>): OrderedLine {
        val main = shapes.firstOrNull { it.name == lineName }
            ?: shapes.firstOrNull { it.name.isNotBlank() && it.name.equals(lineName, ignoreCase = true) }
        if (main != null) {
            val ordered = walk(main.vertices, poles)
            if (ordered.size >= 2) return OrderedLine(ordered, fromShape = true)
        }
        // no usable geometry: everything not on another named line, by pole number. A tap's first vertex
        // is its junction pole on the main line, so that one stays.
        // by pole, not by name: tap poles often reuse the main line's numbers ("1", "2", …)
        val elsewhere = shapes
            .filter { it !== main && it.name.isNotBlank() }
            .flatMap { shape -> walk(if (isTap(shape.name)) shape.vertices.drop(1) else shape.vertices, poles) }
            .toSet()
        return OrderedLine(poles.filter { it !in elsewhere }.sortedWith(BY_NUMBER), fromShape = false)
    }

    /** The poles the vertices pass through, in vertex order, each pole at most once. */
    private fun walk(vertices: List<Pair<Double, Double>>, poles: List<GisPoint>): List<GisPoint> {
        val used = mutableSetOf<GisPoint>()
        return vertices.mapNotNull { (lat, lng) ->
            poles.asSequence()
                .filter { it !in used }
                .map { it to Geo.distanceM(lat, lng, it.lat, it.lng) }
                .filter { it.second <= MATCH_M }
                .minByOrNull { it.second }
                ?.first
                ?.also { used += it }
        }
    }

    private fun isTap(name: String) = name.startsWith("Відп.", ignoreCase = true)

    /** "2", "13а", "13/1", "16": by the leading number, then the rest; names without one go last. */
    val BY_NUMBER: Comparator<GisPoint> = compareBy(
        { Regex("\\d+").find(it.name)?.value?.toLongOrNull() ?: Long.MAX_VALUE },
        { it.name },
    )
}
