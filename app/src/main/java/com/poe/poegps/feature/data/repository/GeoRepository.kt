package com.poe.poegps.feature.data.repository

import android.content.Context
import com.poe.poegps.feature.data.remote.api.ObjectsApi
import com.poe.poegps.feature.data.remote.utils.FilialManager
import com.poe.poegps.feature.data.remote.utils.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
    fun branchLines(): Flow<String> = flow {
        val filial = filialManager.getFilial().firstOrNull() ?: return@flow
        val cache = File(context.filesDir, "geo/lines_$filial.geojson")
        val cached = cache.takeIf { it.exists() }?.readText()
        if (cached != null) emit(cached)
        val fresh = try {
            val token = tokenManager.getToken().firstOrNull().orEmpty()
            val response = api.getGeoLines(filial, "Bearer $token")
            // 404 until the endpoint is deployed: keep the cache quietly
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
}
