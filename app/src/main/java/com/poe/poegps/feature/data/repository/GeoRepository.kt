package com.poe.poegps.feature.data.repository

import android.content.Context
import com.poe.poegps.core.location.Area
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
import org.maplibre.geojson.FeatureCollection
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
     * [branchLines] for a map that shows object [tplnr]. An object kept from another branch (unsent work
     * survives a branch switch) isn't in this branch's drawings: its own come from that branch's copy
     * still on the phone, so its line is drawn and its GIS poles can be ordered along it.
     */
    fun branchLines(tplnr: String): Flow<String> = branchLines().map { json -> withObject(json, tplnr) }.flowOn(Dispatchers.Default)

    private fun withObject(json: String, tplnr: String): String {
        val quoted = "\"" + tplnr + "\""
        if (json.contains(quoted)) return json
        val own = File(context.filesDir, "geo").listFiles { f -> f.name.startsWith("lines_") }.orEmpty()
            .sortedByDescending { it.lastModified() }
            .firstNotNullOfOrNull { f ->
                runCatching { f.readText() }.getOrNull()?.takeIf { it.contains(quoted) }?.let { other ->
                    runCatching {
                        FeatureCollection.fromJson(other).features().orEmpty()
                            .filter { it.hasProperty("tplnr") && it.getStringProperty("tplnr") == tplnr }
                    }.getOrNull()
                }
            }
        if (own.isNullOrEmpty()) return json
        return runCatching {
            FeatureCollection.fromFeatures(FeatureCollection.fromJson(json).features().orEmpty() + own).toJson()
        }.getOrDefault(json)
    }

    /** The branch outline for the "outside the branch" check. */
    fun branchArea(): Flow<Area> = cachedThenFresh("branch") { filial, token -> api.getGeoBranch(filial, token) }
        .mapNotNull { runCatching { parseArea(it) }.getOrNull() }

    private fun cachedThenFresh(
        name: String,
        fetch: suspend (filial: String, bearer: String) -> Response<ResponseBody>,
    ): Flow<String> = flow {
        val filial = filialManager.getFilial().firstOrNull() ?: return@flow
        val cache = File(context.filesDir, "geo/${name}_$filial.geojson")
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
