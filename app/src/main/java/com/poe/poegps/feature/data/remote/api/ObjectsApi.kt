package com.poe.poegps.feature.data.remote.api

import com.poe.poegps.feature.data.remote.model.TpObjectsResponse
import retrofit2.http.GET

interface ObjectsApi {

    @GET("/tpobjects")
    suspend fun getTpObjects(): List<TpObjectsResponse>
}