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

/** A pole on the phone that a GIS tap may hang from: on the main line or on another tap of the object. */
data class PhonePole(val line: String, val name: String, val lat: Double?, val lng: Double?)

/**
 * Where the GIS tap [tapName] hangs among the poles on the phone [onPhone], with its poles after that
 * junction ([gisTapPoles]); null when none of them is its junction. A pole on an end of the tap's
 * drawing qualifies, or one named as its "від оп." pole; those of [mainLine] are tried first, the usual
 * place, then those of other taps (a tap off a tap).
 */
fun tapJunction(
    tapName: String, mainLine: String, onPhone: List<PhonePole>, poles: List<GisPoint>, shapes: List<GisShape>,
): Pair<PhonePole, List<GisPoint>>? {
    val ends = shapes.filter { it.name == tapName && it.vertices.isNotEmpty() }
        .flatMap { listOf(it.vertices.first(), it.vertices.last()) }
    val from = fromPole(tapName)
    fun named(p: PhonePole) = from != null && p.name.trim().equals(from, ignoreCase = true)
    fun onEnd(p: PhonePole) = p.lat != null && p.lng != null &&
        ends.any { (lat, lng) -> Geo.distanceM(p.lat, p.lng, lat, lng) <= TAP_END_M }
    return onPhone.asSequence()
        .filter { it.line != tapName && (onEnd(it) || named(it)) }
        .sortedWith(compareBy({ it.line != mainLine }, { !named(it) }))
        // by name only for its "від оп." pole: other poles' numbers repeat across the object's lines
        .firstNotNullOfOrNull { p ->
            gisTapPoles(tapName, if (named(p)) p.name else "", p.lat, p.lng, poles, shapes)?.let { p to it }
        }
}

/**
 * All the poles of the GIS tap [tapName] along its drawing, for a tap that hangs on no pole on the phone
 * (GIS draws the main line short, or the tap's end off its junction): turned to start at its "від оп."
 * pole when the far end is that pole. Null when the drawing passes through fewer than two GIS poles.
 */
fun gisTapAlone(tapName: String, poles: List<GisPoint>, shapes: List<GisShape>): List<GisPoint>? {
    val ordered = LineOrdering.order(tapName, poles, shapes).takeIf { it.fromShape }?.poles ?: return null
    val from = fromPole(tapName)
    fun named(p: GisPoint) = from != null && p.name.trim().equals(from, ignoreCase = true)
    return if (!named(ordered.first()) && named(ordered.last())) ordered.reversed() else ordered
}

/** A tap's end within this of the pole is on it: junctions are drawn on the pole, GPS adds a few metres. */
const val TAP_END_M = 5.0

private val FROM_POLE = Regex("""від\s*оп\.?\s*([^\s,]+)""", RegexOption.IGNORE_CASE)
private val TO_POLE = Regex("""до\s*оп\.?\s*([^\s,]+)""", RegexOption.IGNORE_CASE)

private fun fromPole(name: String) = FROM_POLE.find(name)?.groupValues?.get(1)
private fun toPole(name: String) = TO_POLE.find(name)?.groupValues?.get(1)
