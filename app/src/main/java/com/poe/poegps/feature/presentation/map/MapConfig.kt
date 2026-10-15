package com.poe.poegps.feature.presentation.map

import org.maplibre.android.geometry.LatLng

object MapConfig {
    // TODO: point at the company tile server (OSM vector extract of Poltava oblast) once it exists.
    // Placeholder: OpenFreeMap's free hosted styles, no key needed. Not for offline/bulk use.
    private const val LIGHT_STYLE = "https://tiles.openfreemap.org/styles/positron"
    private const val DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"

    const val ATTRIBUTION = "© OpenStreetMap · OpenFreeMap"

    fun styleUrl(dark: Boolean) = if (dark) DARK_STYLE else LIGHT_STYLE

    val OBLAST_CENTER = LatLng(49.59, 34.55)
    const val OBLAST_ZOOM = 8.0
}
