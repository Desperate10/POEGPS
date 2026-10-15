package com.poe.poegps.core.location

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A ring of (lat, lng) vertices; the first ring of a polygon is its outline, the rest are holes. */
typealias Ring = List<Pair<Double, Double>>

/** A polygon as its rings; a branch can be several polygons (MultiPolygon). */
typealias Area = List<List<Ring>>

object Geo {
    private const val EARTH_RADIUS_M = 6_371_000.0

    fun distanceM(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = (lat2 - lat1).rad
        val dLng = (lng2 - lng1).rad
        val a = sin(dLat / 2).let { it * it } + cos(lat1.rad) * cos(lat2.rad) * sin(dLng / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    fun contains(area: Area, lat: Double, lng: Double): Boolean = area.any { polygon ->
        polygon.isNotEmpty() && inRing(polygon[0], lat, lng) && polygon.drop(1).none { inRing(it, lat, lng) }
    }

    /** Metres from the point to the nearest edge of [area]; local flat projection, fine at oblast scale. */
    fun distanceToEdgeM(area: Area, lat: Double, lng: Double): Double {
        val kx = 111_320.0 * cos(lat.rad)
        val ky = 110_540.0
        var best = Double.MAX_VALUE
        for (ring in area.flatten()) {
            for (i in ring.indices) {
                val (aLat, aLng) = ring[i]
                val (bLat, bLng) = ring[(i + 1) % ring.size]
                val ax = (aLng - lng) * kx
                val ay = (aLat - lat) * ky
                val bx = (bLng - lng) * kx
                val by = (bLat - lat) * ky
                val dx = bx - ax
                val dy = by - ay
                val len2 = dx * dx + dy * dy
                val t = if (len2 == 0.0) 0.0 else (-(ax * dx + ay * dy) / len2).coerceIn(0.0, 1.0)
                val px = ax + t * dx
                val py = ay + t * dy
                best = minOf(best, sqrt(px * px + py * py))
            }
        }
        return best
    }

    // ray casting on lng/lat
    private fun inRing(ring: Ring, lat: Double, lng: Double): Boolean {
        var inside = false
        var j = ring.size - 1
        for (i in ring.indices) {
            val (yi, xi) = ring[i]
            val (yj, xj) = ring[j]
            if ((yi > lat) != (yj > lat) && lng < (xj - xi) * (lat - yi) / (yj - yi) + xi) inside = !inside
            j = i
        }
        return inside
    }

    private val Double.rad get() = this * PI / 180
}
