package com.poe.poegps.feature.presentation.map

import com.poe.poegps.BuildConfig
import org.maplibre.android.geometry.LatLng

object MapConfig {
    // TODO: point at the company tile server (OSM vector extract of Poltava oblast) once it exists.
    // Placeholder: OpenFreeMap's free hosted styles, no key needed. Not for offline/bulk use.
    private const val LIGHT_STYLE = "https://tiles.openfreemap.org/styles/positron"
    private const val DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"

    const val ATTRIBUTION = "© OpenStreetMap · OpenFreeMap"

    fun styleUrl(dark: Boolean) = if (dark) DARK_STYLE else LIGHT_STYLE

    fun attribution(satellite: Boolean) = if (satellite) SATELLITE.attribution else ATTRIBUTION

    /** Where satellite imagery comes from; [SATELLITE] picks one. */
    enum class SatelliteSource(val tiles: String, val maxZoom: Float, val attribution: String) {
        /**
         * The Google Hybrid address the company's GIS uses in QGIS/Lizmap. Chosen by the company for its
         * village imagery (2026-10-20). It is Google's internal tile address, not its licensed Map Tiles
         * API: Google may throttle or block it at any time, and tiles must not be stored for offline use.
         */
        GOOGLE_HYBRID("https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}", 20f, "© Google"),

        /**
         * Esri World Imagery: licensed, free tier with an ArcGIS Location Platform key in local.properties
         * (esri.apiKey); Esri's public endpoint without one. Sharp in towns, ~zoom 17 in villages, and
         * grey "no data" tiles past zoom 18 there.
         */
        ESRI(
            if (BuildConfig.ESRI_API_KEY.isNotBlank())
                "https://ibasemaps-api.arcgis.com/arcgis/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}?token=${BuildConfig.ESRI_API_KEY}"
            else
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
            18f,
            "© Esri, Maxar, Earthstar Geographics",
        ),
    }

    // if Google stops serving the app, switch to ESRI here
    val SATELLITE = SatelliteSource.GOOGLE_HYBRID

    val OBLAST_CENTER = LatLng(49.59, 34.55)
    const val OBLAST_ZOOM = 8.0
}
