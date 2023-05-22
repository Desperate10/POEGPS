package com.poe.poegps.feature.data.remote.api

import com.poe.poegps.feature.data.remote.model.LineObjectsDTO
import com.poe.poegps.feature.data.remote.model.LoginRequest
import com.poe.poegps.feature.data.remote.model.LoginResponse
import com.poe.poegps.feature.data.remote.model.PillarObjectsDTO
import com.poe.poegps.feature.data.remote.model.TpObjectsDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ObjectsApi {
    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST("authenticate")
    suspend fun auth(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST("tokencheck")
    suspend fun checkTokenValidity(@Body token: String): Response<Boolean>

    @GET("tp")
    suspend fun getTpObjects(@Query("filial") filial: Int, @Header("Authorization") token: String): List<TpObjectsDTO>

    @GET("ps")
    suspend fun getPsObjects(@Header("Authorization") token: String): List<TpObjectsDTO>

    @GET("lines")
    suspend fun getLineObjects(@Query("filial") filial: Int, @Header("Authorization") token: String): List<LineObjectsDTO>

    @GET("pillars")
    suspend fun getPillarObjects(@Query("filial") filial: Int, @Header("Authorization") token: String): List<PillarObjectsDTO>
}