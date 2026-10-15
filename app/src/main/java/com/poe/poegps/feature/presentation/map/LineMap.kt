package com.poe.poegps.feature.presentation.map

import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.poe.poegps.core.designsystem.PoeColors
import org.maplibre.android.MapLibre
import org.maplibre.geojson.MultiLineString
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.eq
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.neq
import org.maplibre.android.style.expressions.Expression.switchCase
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineDasharray
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A pole with coordinates, as drawn on the map. */
data class MapPole(val id: Int, val label: String, val lat: Double, val lng: Double, val selected: Boolean)

data class MapGps(val lat: Double, val lng: Double, val accuracy: Float?)

/** Move the camera to a point; a new [seq] repeats the move. Null [zoom] keeps the current zoom. */
data class MapFocus(val lat: Double, val lng: Double, val zoom: Double?, val seq: Int, val animate: Boolean = true)

/**
 * The line editor's map: branch lines from GIS (this line emphasised), GIS pole positions,
 * captured poles joined in editor order, the GPS position with its accuracy circle and a dashed
 * link from the last captured pole.
 */
@Composable
fun LineMap(
    tplnr: String,
    colors: PoeColors,
    gisLines: String?,
    gisPoles: List<Pair<Double, Double>>,
    poles: List<MapPole>,
    gps: MapGps?,
    /** Bumped to fly back to the GPS position. */
    recenter: Int,
    bottomInset: Dp,
    topInset: Dp,
    onPoleClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    focus: MapFocus? = null,
    /** Reports the point under the centre of the visible area as the camera moves (B6). */
    onCenterChange: ((Double, Double) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var framed by remember { mutableStateOf(false) }
    val onPoleClickState by rememberUpdatedState(onPoleClick)
    val onCenterChangeState by rememberUpdatedState(onCenterChange)

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.getMapAsync { m ->
                m.uiSettings.apply {
                    isAttributionEnabled = false // shown by the screen above the sheet
                    isLogoEnabled = false
                    isRotateGesturesEnabled = false
                    isTiltGesturesEnabled = false
                    isCompassEnabled = false
                }
                m.setStyle(Style.Builder().fromUri(MapConfig.styleUrl(colors.isDark))) { s ->
                    addLayers(s, tplnr, colors)
                    style = s
                }
                m.addOnCameraMoveListener {
                    m.cameraPosition.target?.let { t -> onCenterChangeState?.invoke(t.latitude, t.longitude) }
                }
                m.addOnCameraIdleListener {
                    m.cameraPosition.target?.let { t -> onCenterChangeState?.invoke(t.latitude, t.longitude) }
                }
                m.addOnMapClickListener { latLng ->
                    val p = m.projection.toScreenLocation(latLng)
                    val r = 24 * density.density
                    val hit = m.queryRenderedFeatures(RectF(p.x - r, p.y - r, p.x + r, p.y + r), LAYER_POLES)
                        .firstOrNull()?.getNumberProperty("id")?.toInt()
                    hit?.let(onPoleClickState)
                    hit != null
                }
                map = m
            }
            mapView
        },
    )

    // keep what the map frames clear of the header and the bottom sheet; the camera target is
    // then the centre of the visible area, which is where B6 draws its pin
    val padding = with(density) { doubleArrayOf(0.0, topInset.toPx().toDouble(), 0.0, bottomInset.toPx().toDouble()) }
    LaunchedEffect(map, padding[1], padding[3]) {
        map?.moveCamera(CameraUpdateFactory.paddingTo(padding))
    }

    LaunchedEffect(style, gisLines) {
        val s = style ?: return@LaunchedEffect
        s.getSourceAs<GeoJsonSource>(SRC_GIS_LINES)?.setGeoJson(gisLines ?: EMPTY)
    }
    LaunchedEffect(style, gisPoles) {
        val s = style ?: return@LaunchedEffect
        s.getSourceAs<GeoJsonSource>(SRC_GIS_POLES)?.setGeoJson(
            FeatureCollection.fromFeatures(gisPoles.map { (lat, lng) -> Feature.fromGeometry(Point.fromLngLat(lng, lat)) })
        )
    }
    LaunchedEffect(style, poles) {
        val s = style ?: return@LaunchedEffect
        s.getSourceAs<GeoJsonSource>(SRC_POLES)?.setGeoJson(
            FeatureCollection.fromFeatures(poles.map { p ->
                Feature.fromGeometry(Point.fromLngLat(p.lng, p.lat)).apply {
                    addNumberProperty("id", p.id)
                    addStringProperty("label", p.label)
                    addBooleanProperty("selected", p.selected)
                }
            })
        )
        s.getSourceAs<GeoJsonSource>(SRC_ROUTE)?.setGeoJson(
            features(if (poles.size > 1) Feature.fromGeometry(LineString.fromLngLats(poles.map { Point.fromLngLat(it.lng, it.lat) })) else null)
        )
    }
    LaunchedEffect(style, gps, poles) {
        val s = style ?: return@LaunchedEffect
        val g = gps
        s.getSourceAs<GeoJsonSource>(SRC_GPS)?.setGeoJson(
            features(g?.let { Feature.fromGeometry(Point.fromLngLat(it.lng, it.lat)) })
        )
        s.getSourceAs<GeoJsonSource>(SRC_GPS_ACCURACY)?.setGeoJson(
            features(g?.accuracy?.let { Feature.fromGeometry(circle(g.lat, g.lng, it.toDouble())) })
        )
        val last = poles.lastOrNull()
        s.getSourceAs<GeoJsonSource>(SRC_GPS_LINK)?.setGeoJson(
            features(
                if (g == null || last == null) null
                else Feature.fromGeometry(LineString.fromLngLats(listOf(Point.fromLngLat(last.lng, last.lat), Point.fromLngLat(g.lng, g.lat))))
            )
        )
    }

    // this line's shape in the branch GeoJSON: frames a line that has no poles on the phone yet
    var lineShape by remember(tplnr) { mutableStateOf<List<LatLng>>(emptyList()) }
    LaunchedEffect(gisLines, tplnr) {
        val json = gisLines ?: return@LaunchedEffect
        lineShape = withContext(Dispatchers.Default) { lineVertices(json, tplnr) }
    }

    // frame the work once, as soon as there is something to frame
    LaunchedEffect(map, poles, gisPoles, lineShape, gps != null) {
        val m = map ?: return@LaunchedEffect
        if (framed) return@LaunchedEffect
        val points = poles.map { LatLng(it.lat, it.lng) } + gisPoles.map { LatLng(it.first, it.second) } +
            lineShape + listOfNotNull(gps?.let { LatLng(it.lat, it.lng) })
        when {
            points.size >= 2 -> m.moveCamera(
                CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(points).build(), (48 * density.density).toInt())
            )
            points.size == 1 -> m.moveCamera(CameraUpdateFactory.newLatLngZoom(points[0], 16.5))
            else -> {
                m.moveCamera(CameraUpdateFactory.newLatLngZoom(MapConfig.OBLAST_CENTER, MapConfig.OBLAST_ZOOM))
                return@LaunchedEffect
            }
        }
        framed = true
    }

    LaunchedEffect(map, focus) {
        val m = map ?: return@LaunchedEffect
        val f = focus ?: return@LaunchedEffect
        framed = true
        val position = CameraPosition.Builder()
            .target(LatLng(f.lat, f.lng))
            .zoom(f.zoom ?: m.cameraPosition.zoom)
            .padding(padding)
            .build()
        val update = CameraUpdateFactory.newCameraPosition(position)
        if (f.animate) m.animateCamera(update, 400) else m.moveCamera(update)
    }

    LaunchedEffect(recenter) {
        if (recenter == 0) return@LaunchedEffect
        val g = gps ?: return@LaunchedEffect
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(g.lat, g.lng), 17.0))
    }
}

/** Every vertex of the features with this [tplnr] (LineString or MultiLineString); empty if none or unreadable. */
private fun lineVertices(geoJson: String, tplnr: String): List<LatLng> = runCatching {
    FeatureCollection.fromJson(geoJson).features().orEmpty()
        .filter { it.hasProperty("tplnr") && it.getStringProperty("tplnr") == tplnr }
        .flatMap { f ->
            when (val g = f.geometry()) {
                is LineString -> g.coordinates()
                is MultiLineString -> g.coordinates().flatten()
                else -> emptyList()
            }
        }
        .map { LatLng(it.latitude(), it.longitude()) }
}.getOrDefault(emptyList())

private fun addLayers(style: Style, tplnr: String, c: PoeColors) {
    val ink = c.ink.toArgb()
    val action = c.action.toArgb()
    val gps = c.gps.toArgb()
    val surface = c.surface.toArgb()
    val route = if (c.isDark) action else ink

    listOf(SRC_GIS_LINES, SRC_GIS_POLES, SRC_ROUTE, SRC_POLES, SRC_GPS, SRC_GPS_ACCURACY, SRC_GPS_LINK)
        .forEach { style.addSource(GeoJsonSource(it)) }

    style.addLayer(
        LineLayer("gis-lines-other", SRC_GIS_LINES)
            .withProperties(lineColor(c.muted.toArgb()), lineWidth(1.5f), lineOpacity(0.55f))
            .withFilter(neq(get("tplnr"), literal(tplnr)))
    )
    // this line as GIS has it today: the faint track the crew is re-surveying
    style.addLayer(
        LineLayer("gis-line-current", SRC_GIS_LINES)
            .withProperties(lineColor(route), lineWidth(5f), lineOpacity(0.35f), lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND))
            .withFilter(eq(get("tplnr"), literal(tplnr)))
    )
    style.addLayer(
        CircleLayer("gis-poles", SRC_GIS_POLES)
            .withProperties(circleRadius(4f), circleColor(surface), circleStrokeColor(c.outline.toArgb()), circleStrokeWidth(1.5f))
    )
    style.addLayer(FillLayer("gps-accuracy", SRC_GPS_ACCURACY).withProperties(fillColor(gps), fillOpacity(0.16f)))
    style.addLayer(
        LineLayer("gps-link", SRC_GPS_LINK)
            .withProperties(lineColor(gps), lineWidth(2.5f), lineDasharray(arrayOf(1.6f, 2.4f)), lineCap(Property.LINE_CAP_ROUND))
    )
    style.addLayer(
        LineLayer("route", SRC_ROUTE)
            .withProperties(lineColor(route), lineWidth(3.5f), lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND))
    )
    style.addLayer(
        CircleLayer(LAYER_POLES, SRC_POLES).withProperties(
            circleRadius(switchCase(eq(get("selected"), literal(true)), literal(9f), literal(6f))),
            circleColor(switchCase(eq(get("selected"), literal(true)), literal(action.hex()), literal(route.hex()))),
            circleStrokeColor(surface),
            circleStrokeWidth(switchCase(eq(get("selected"), literal(true)), literal(3f), literal(1.5f))),
        )
    )
    style.addLayer(
        SymbolLayer("pole-labels", SRC_POLES).withProperties(
            textField(get("label")),
            textFont(arrayOf("Noto Sans Bold")),
            textSize(12f),
            textColor(ink),
            textHaloColor(surface),
            textHaloWidth(1.5f),
            textAnchor(Property.TEXT_ANCHOR_LEFT),
            textOffset(arrayOf(0.9f, -0.6f)),
        )
    )
    style.addLayer(
        CircleLayer("gps-dot", SRC_GPS)
            .withProperties(circleRadius(8f), circleColor(gps), circleStrokeColor(surface), circleStrokeWidth(3f))
    )
}

private fun features(feature: Feature?): FeatureCollection = FeatureCollection.fromFeatures(listOfNotNull(feature))

private fun Int.hex() = String.format("#%06X", this and 0xFFFFFF)

/** A ring of [radiusM] metres around a point, close enough to a circle at survey scales. */
private fun circle(lat: Double, lng: Double, radiusM: Double): Polygon {
    val dLat = radiusM / 111_320.0
    val dLng = radiusM / (111_320.0 * cos(lat * PI / 180))
    val ring = (0..48).map { i ->
        val a = 2 * PI * i / 48
        Point.fromLngLat(lng + dLng * cos(a), lat + dLat * sin(a))
    }
    return Polygon.fromLngLats(listOf(ring))
}

private const val EMPTY = """{"type":"FeatureCollection","features":[]}"""
private const val SRC_GIS_LINES = "gis-lines"
private const val SRC_GIS_POLES = "gis-poles"
private const val SRC_ROUTE = "route"
private const val SRC_POLES = "poles"
private const val SRC_GPS = "gps"
private const val SRC_GPS_ACCURACY = "gps-accuracy"
private const val SRC_GPS_LINK = "gps-link"
private const val LAYER_POLES = "poles"
