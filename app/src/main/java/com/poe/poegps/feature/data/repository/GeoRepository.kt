package com.poe.poegps.feature.data.repository

import android.content.Context
import com.poe.poegps.core.location.Area
import com.poe.poegps.core.location.Geo
import com.poe.poegps.core.location.Ring
import com.poe.poegps.feature.data.remote.api.ObjectsApi
import com.poe.poegps.feature.data.remote.utils.FilialManager
import com.poe.poegps.feature.data.remote.utils.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiLineString
import retrofit2.Response
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Map geometry from the backend's read-only /geo endpoints. The last good answer is kept in a
 * file so the map still shows the lines without a network; nothing goes into the database.
 */
@Singleton
class GeoRepository @Inject constructor(
    private val api: ObjectsApi,
    private val tokenManager: TokenManager,
    private val filialManager: FilialManager,
    @ApplicationContext private val context: Context,
) {
    /**
     * The branch's lines as a GeoJSON FeatureCollection: the cached copy first, so the map is
     * filled at once (and offline), then the server's answer when it differs.
     */
    fun branchLines(): Flow<String> = cachedThenFresh("lines") { filial, token -> api.getGeoLines(filial, token) }

    /**
     * [branchLines] for a screen that shows [place]. An object kept from another branch (unsent work
     * survives a branch switch) isn't in this branch's drawings: they come from that branch's copy still
     * on the phone, so its line is drawn, its GIS poles can be ordered along it and an upload doesn't
     * take it for a line new to GIS.
     */
    fun branchLines(place: ObjectPlace): Flow<String> = branchLines()
        .map { json -> if (place.isEmpty) json else withPlace(json, place, filialManager.getFilial().firstOrNull()) }
        .flowOn(Dispatchers.Default)

    /**
     * [branchArea] for capturing [place]: an object kept from another branch also lets its own branch
     * count as "inside", so its poles aren't flagged as captured outside the branch.
     */
    fun branchArea(place: ObjectPlace): Flow<Area> = branchArea()
        .map { area ->
            if (place.isEmpty) return@map area
            val filial = filialManager.getFilial().firstOrNull() ?: return@map area
            val current = cacheFile("lines", filial).takeIf { it.exists() }?.readText() ?: return@map area
            area + otherBranches(current, place, filial).keys.flatMap { cachedArea(it) }
        }
        .flowOn(Dispatchers.Default)

    private fun withPlace(json: String, place: ObjectPlace, filial: String?): String {
        val own = otherBranches(json, place, filial).values.flatten()
        if (own.isEmpty()) return json
        return runCatching { FeatureCollection.fromFeatures(features(json) + own).toJson() }.getOrDefault(json)
    }

    /**
     * The cached branches (code → the features taken from them) holding what [place] needs and [json]
     * lacks: the drawings of its tplnrs, and the lines around its point when [json] has none there.
     */
    private fun otherBranches(json: String, place: ObjectPlace, filial: String?): Map<String, List<Feature>> {
        val missing = place.tplnrs.filterNot { json.contains("\"" + it + "\"") }.toMutableSet()
        var near = place.near?.takeIf { p -> features(json).none { it.passesWithin(p, NEAR_M) } }
        if (missing.isEmpty() && near == null) return emptyMap()
        val result = LinkedHashMap<String, List<Feature>>()
        for ((other, file) in otherCaches("lines", filial)) {
            if (missing.isEmpty() && near == null) break
            val text = runCatching { file.readText() }.getOrNull() ?: continue
            val found = missing.filterTo(HashSet()) { text.contains("\"" + it + "\"") }
            if (found.isEmpty() && near == null) continue
            val all = features(text)
            val taken = all.filter { f ->
                f.tplnrIn(found) || (near != null && f.passesWithin(near!!, AROUND_M))
            }
            if (near != null && all.any { it.passesWithin(near!!, NEAR_M) }) near = null
            missing -= found
            if (taken.isNotEmpty()) result[other] = taken
        }
        return result
    }

    /** Other branches' cached copies of [name] (code → file), newest first. */
    private fun otherCaches(name: String, current: String?): List<Pair<String, File>> =
        File(context.filesDir, "geo").listFiles { f -> f.name.startsWith(name + "_") && f.name.endsWith(".geojson") }.orEmpty()
            .sortedByDescending { it.lastModified() }
            .map { it.name.removePrefix(name + "_").removeSuffix(".geojson") to it }
            .filter { it.first != current }

    private fun cachedArea(filial: String): Area =
        cacheFile("branch", filial).takeIf { it.exists() }?.let { f -> runCatching { parseArea(f.readText()) }.getOrNull() }.orEmpty()

    private fun cacheFile(name: String, filial: String) = File(context.filesDir, "geo/${name}_$filial.geojson")

    private fun features(json: String): List<Feature> =
        runCatching { FeatureCollection.fromJson(json).features().orEmpty() }.getOrDefault(emptyList())

    private fun Feature.tplnrIn(tplnrs: Set<String>) = hasProperty("tplnr") && getStringProperty("tplnr") in tplnrs

    private fun Feature.passesWithin(p: Pair<Double, Double>, meters: Double): Boolean {
        val points = when (val g = geometry()) {
            is LineString -> g.coordinates()
            is MultiLineString -> g.coordinates().flatten()
            else -> emptyList()
        }
        return points.any { Geo.distanceM(p.first, p.second, it.latitude(), it.longitude()) <= meters }
    }

    /** The branch outline for the "outside the branch" check. */
    fun branchArea(): Flow<Area> = cachedThenFresh("branch") { filial, token -> api.getGeoBranch(filial, token) }
        .mapNotNull { runCatching { parseArea(it) }.getOrNull() }

    private fun cachedThenFresh(
        name: String,
        fetch: suspend (filial: String, bearer: String) -> Response<ResponseBody>,
    ): Flow<String> = flow {
        val filial = filialManager.getFilial().firstOrNull() ?: return@flow
        val cache = cacheFile(name, filial)
        val cached = cache.takeIf { it.exists() }?.readText()
        if (cached != null) emit(cached)
        val fresh = try {
            val token = tokenManager.getToken().firstOrNull().orEmpty()
            val response = fetch(filial, "Bearer $token")
            // 404 until the endpoint is deployed/configured: keep the cache quietly
            if (response.isSuccessful) response.body()?.string() else null
        } catch (e: IOException) {
            null
        }
        if (fresh != null && fresh != cached) {
            cache.parentFile?.mkdirs()
            cache.writeText(fresh)
            emit(fresh)
        }
    }.flowOn(Dispatchers.IO)

    /** Polygon or MultiPolygon Feature (or bare geometry) → rings of (lat, lng). */
    private fun parseArea(json: String): Area {
        val root = JSONObject(json)
        val geometry = root.optJSONObject("geometry") ?: root
        val coords = geometry.getJSONArray("coordinates")
        fun ring(a: JSONArray): Ring = List(a.length()) { i -> a.getJSONArray(i).let { it.getDouble(1) to it.getDouble(0) } }
        fun polygon(a: JSONArray): List<Ring> = List(a.length()) { i -> ring(a.getJSONArray(i)) }
        return when (geometry.getString("type")) {
            "Polygon" -> listOf(polygon(coords))
            "MultiPolygon" -> List(coords.length()) { i -> polygon(coords.getJSONArray(i)) }
            else -> emptyList()
        }
    }
}

/**
 * What a screen shows, to find it in another branch's cached drawings: its lines' tplnrs, and for a
 * point object (TP) its position, as (lat, lng).
 */
data class ObjectPlace(val tplnrs: Set<String> = emptySet(), val near: Pair<Double, Double>? = null) {
    val isEmpty get() = tplnrs.isEmpty() && near == null
}

// a drawing this close to a point object means the branch has its surroundings
private const val NEAR_M = 1_000.0
// how much of another branch is brought in around a point object
private const val AROUND_M = 3_000.0
