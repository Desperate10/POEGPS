package com.poe.poegps.feature.data.remote.api

import com.poe.poegps.feature.data.remote.model.*
import com.poe.poegps.feature.data.remote.utils.ApiResponse
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface ObjectsApi {
    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST("authenticate")
    suspend fun auth(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST("tokencheck")
    suspend fun checkTokenValidity(@Body token: String): Response<Boolean>

    @GET("tp")
    suspend fun getTpObjects(@Query("filial") filial: String, @Header("Authorization") token: String): List<TpObjectsDTO>

    @GET("abontp")
    suspend fun getAbonTpObjects(@Query("filial") filial: String, @Header("Authorization") token: String): List<TpObjectsAbonDTO>

    @GET("ps")
    suspend fun getPsObjects(@Header("Authorization") token: String): List<PsModelDTO>

    @GET("lines10")
    suspend fun getLine10Objects(@Query("filial") filial: String, @Header("Authorization") token: String): List<LineObjects10DTO>

    @GET("lines04")
    suspend fun getLine04Objects(@Query("filial") filial: String, @Header("Authorization") token: String): List<LineObjects04DTO>

    @GET("pillars04")
    suspend fun getPillarObjects04(@Query("filial") filial: String, @Header("Authorization") token: String): List<PillarObjects04DTO>

    @GET("pillars10")
    suspend fun getPillarObjects10(@Query("filial") filial: String, @Header("Authorization") token: String): List<PillarObjects10DTO>

    @GET("abonlines10")
    suspend fun getLine10AbonObjects(@Query("filial") filial: String, @Header("Authorization") token: String): List<LineObjects10AbonDTO>

    @GET("abonlines04")
    suspend fun getLine04AbonObjects(@Query("filial") filial: String, @Header("Authorization") token: String): List<LineObjects04AbonDTO>
}