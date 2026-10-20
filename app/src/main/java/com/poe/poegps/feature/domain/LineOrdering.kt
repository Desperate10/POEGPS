package com.poe.poegps.feature.domain

import com.poe.poegps.core.location.Geo

/** A tap (відпайка) by its name: GIS draws them as "Відп. від оп. …", and the phone names new ones the same way. */
fun isTapName(name: String) = name.startsWith("Відп.", ignoreCase = true)

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
        // GIS can hold several drawings under the line's name, e.g. a 2-point stub left beside the real
        // one (PO-F-23-L1740), and sends them in no fixed order: the one through the most poles wins
        val named = shapes.filter { it.name == lineName }
            .ifEmpty { shapes.filter { it.name.isNotBlank() && it.name.equals(lineName, ignoreCase = true) } }
        val main = named.map { it to walk(it.vertices, poles).size }.maxByOrNull { it.second }
            ?.takeIf { it.second >= 2 }?.first
            // the busiest drawing is the main line: a tap without its own drawing must not take its poles
            ?: mainByPoles(poles, shapes).takeUnless { isTap(lineName) }
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

    /**
     * GIS doesn't keep drawing names (pltxt) in step with the line list, so when no drawing has the line's
     * name the main line is the drawing that isn't a tap and passes through the most of the object's
     * poles, at least two; the server picks the drawing to edit the same way. A tie means no main line.
     */
    private fun mainByPoles(poles: List<GisPoint>, shapes: List<GisShape>): GisShape? {
        val counted = shapes.filter { !isTap(it.name) }.map { it to walk(it.vertices, poles).size }
        val best = counted.maxOfOrNull { it.second } ?: return null
        if (best < 2) return null
        return counted.filter { it.second == best }.singleOrNull()?.first
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

    private fun isTap(name: String) = isTapName(name)

    /** "2", "13а", "13/1", "16": by the leading number, then the rest; names without one go last. */
    val BY_NUMBER: Comparator<GisPoint> = compareBy(
        { Regex("\\d+").find(it.name)?.value?.toLongOrNull() ?: Long.MAX_VALUE },
        { it.name },
    )
}
