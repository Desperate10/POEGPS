package com.poe.poegps.feature.presentation.map

import com.poe.poegps.feature.domain.GisShape
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiLineString

/** The line geometries of object [tplnr] in the branch GeoJSON (/gis/geo/lines); empty if none or unreadable. */
fun lineShapes(geoJson: String, tplnr: String): List<GisShape> = runCatching {
    FeatureCollection.fromJson(geoJson).features().orEmpty()
        .filter { it.hasProperty("tplnr") && it.getStringProperty("tplnr") == tplnr }
        .flatMap { f ->
            val name = if (f.hasProperty("name")) f.getStringProperty("name").orEmpty() else ""
            when (val g = f.geometry()) {
                is LineString -> listOf(GisShape(name, g.coordinates().map { it.latitude() to it.longitude() }))
                is MultiLineString -> g.coordinates().map { part -> GisShape(name, part.map { it.latitude() to it.longitude() }) }
                else -> emptyList()
            }
        }
}.getOrDefault(emptyList())
