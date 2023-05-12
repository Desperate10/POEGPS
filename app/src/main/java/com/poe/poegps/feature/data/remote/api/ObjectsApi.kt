package com.poe.poegps.feature.data.remote.api

import com.poe.poegps.feature.data.remote.model.LineObjectsDTO
import com.poe.poegps.feature.data.remote.model.PillarObjectsDTO
import com.poe.poegps.feature.data.remote.model.TpObjectsDTO
import retrofit2.http.GET
import retrofit2.http.Path

interface ObjectsApi {

    @GET("/tp?{filial}")
    suspend fun getTpObjects(@Path("filial") filial: Int): List<TpObjectsDTO>

    @GET("/ps")
    suspend fun getPsObjects(): List<TpObjectsDTO>

    @GET("/lines?{filial}")
    suspend fun getLineObjects(@Path("filial") filial: Int): List<LineObjectsDTO>

    @GET("/pillars?{filial}")
    suspend fun getPillarObjects(@Path("filial") filial: Int): List<PillarObjectsDTO>
}