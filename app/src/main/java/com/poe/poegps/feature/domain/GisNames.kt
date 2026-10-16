package com.poe.poegps.feature.domain

import com.poe.poegps.core.location.Geo

/** A drawing passes a pole when one of its vertices is this close: GIS draws lines through their poles. */
const val ON_DRAWING_M = 5.0

/**
 * Whether GIS already draws this line, whatever the drawing is called: a drawing of the line's object and
 * [layer] passes through at least two of its [poles] (lat to lng). The server then edits that drawing
 * instead of adding one (it matches by poles too, as GIS doesn't keep drawing names up to date), so the
 * line isn't new to GIS.
 */
fun drawnInGis(layer: String, poles: List<Pair<Double, Double>>, shapes: List<GisShape>): Boolean = shapes
    .filter { it.layer == layer }
    .any { shape ->
        poles.count { (lat, lng) -> shape.vertices.any { (vLat, vLng) -> Geo.distanceM(lat, lng, vLat, vLng) <= ON_DRAWING_M } } >= 2
    }
