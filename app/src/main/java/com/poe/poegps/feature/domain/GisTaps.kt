package com.poe.poegps.feature.domain

import com.poe.poegps.core.location.Geo

/** A tap GIS already has: its exact name (what the server matches) and its first pole, when the name says. */
data class GisTap(val name: String, val firstPole: String?)

/**
 * The taps GIS draws from the pole [poleName] at [lat]/[lng] (null when it has no coordinates), so a new
 * tap can take the exact GIS name instead of a generated one that the server would add as a second line.
 * A tap belongs to the pole when one end of its drawing is on the pole, or its name says "від оп. <pole>".
 * Lines in [exclude] (the line being edited) are left out; those whose name names the pole come first.
 */
fun gisTapsAt(poleName: String, lat: Double?, lng: Double?, shapes: List<GisShape>, exclude: Set<String>): List<GisTap> {
    val pole = poleName.trim()
    return shapes
        .asSequence()
        .filter { it.name.isNotBlank() && it.name !in exclude }
        .mapNotNull { shape ->
            val named = fromPole(shape.name)?.equals(pole, ignoreCase = true) == true
            val end = if (lat == null || lng == null) Double.MAX_VALUE
            else listOf(shape.vertices.first(), shape.vertices.last())
                .minOf { (vLat, vLng) -> Geo.distanceM(lat, lng, vLat, vLng) }
            if (!named && end > TAP_END_M) null else Triple(shape.name, named, end)
        }
        .sortedWith(compareBy({ !it.second }, { it.third }))
        .distinctBy { it.first }
        .map { GisTap(it.first, toPole(it.first)) }
        .toList()
}

/**
 * The poles of the GIS tap [tapName] after its junction pole [junction] (null coordinates: unknown), in
 * order away from it, walked along the tap's GIS drawing. Null when GIS has no usable drawing of the tap
 * or neither end of it is on the junction, so the tap is surveyed from scratch instead.
 */
fun gisTapPoles(tapName: String, junction: String, lat: Double?, lng: Double?, poles: List<GisPoint>, shapes: List<GisShape>): List<GisPoint>? {
    val ordered = LineOrdering.order(tapName, poles, shapes).takeIf { it.fromShape }?.poles ?: return null
    fun near(p: GisPoint) = lat != null && lng != null && Geo.distanceM(lat, lng, p.lat, p.lng) <= TAP_END_M
    fun named(p: GisPoint) = p.name.trim().equals(junction.trim(), ignoreCase = true)
    // drawn from either end; by position first, as tap poles often reuse the main line's numbers
    val fromJunction = when {
        near(ordered.first()) -> ordered
        near(ordered.last()) -> ordered.reversed()
        named(ordered.first()) -> ordered
        named(ordered.last()) -> ordered.reversed()
        else -> return null
    }
    return fromJunction.drop(1).ifEmpty { null }
}

/** A tap's end within this of the pole is on it: junctions are drawn on the pole, GPS adds a few metres. */
const val TAP_END_M = 5.0

private val FROM_POLE = Regex("""від\s*оп\.?\s*([^\s,]+)""", RegexOption.IGNORE_CASE)
private val TO_POLE = Regex("""до\s*оп\.?\s*([^\s,]+)""", RegexOption.IGNORE_CASE)

private fun fromPole(name: String) = FROM_POLE.find(name)?.groupValues?.get(1)
private fun toPole(name: String) = TO_POLE.find(name)?.groupValues?.get(1)
