package com.poe.poegps.feature.data.remote.api

import com.poe.poegps.feature.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

interface ObjectsApi {
    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST("authenticate")
    suspend fun auth(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST("tokencheck")
    suspend fun checkTokenValidity(@Body token: String): Response<TokenCheckResponse>

    @GET("tp")
    suspend fun getTpObjects(@Query("filial") filial: String, @Header("Authorization") token: String): List<TpObjectsDTO>

    @GET("abontp")
    suspend fun getAbonTpObjects(@Query("filial") filial: String, @Header("Authorization") token: String): List<TpObjectsAbonDTO>

    @GET("ps")
    suspend fun getPsObjects(@Header("Authorization") token: String): List<PsModelDTO>

    @GET("lines10")
    suspend fun getLine10Objects(@Query("filial") filial: String, @Header("Authorization") token: String): List<LineObjects10DTO?>

    @GET("lines04")
    suspend fun getLine04Objects(@Query("filial") filial: String, @Header("Authorization") token: String): List<LineObjects04DTO?>

    @GET("pillars04")
    suspend fun getPillarObjects04(@Query("filial") filial: String, @Header("Authorization") token: String): List<PillarObjects04DTO>

    @GET("pillars10")
    suspend fun getPillarObjects10(@Query("filial") filial: String, @Header("Authorization") token: String): List<PillarObjects10DTO>

    @GET("abonlines10")
    suspend fun getLine10AbonObjects(@Query("filial") filial: String, @Header("Authorization") token: String): List<LineObjects10AbonDTO?>

    @GET("abonlines04")
    suspend fun getLine04AbonObjects(@Query("filial") filial: String, @Header("Authorization") token: String): List<LineObjects04AbonDTO?>

    @GET("wire04")
    suspend fun getWire04(@Header("Authorization") token: String): List<WireDTO>

    @GET("wire10")
    suspend fun getWire10(@Header("Authorization") token: String): List<WireDTO>

    @POST("save")
    suspend fun uploadSavedPillars(@Body pillars: List<LinePillarDTO>, @Header("Authorization") token: String): Response<SavePillarsResponse>
}