package com.poe.poegps.feature.data.remote.utils

import com.google.gson.Gson
import com.poe.poegps.feature.data.remote.model.ErrorResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Response

fun<T> apiRequestFlow(call: suspend () -> Response<T>): Flow<ApiResponse<T>> = flow {
    emit(ApiResponse.Loading)

    withTimeoutOrNull(20000L) {
        val response = call()

        try {
            if (response.isSuccessful) {
                response.body()?.let { data ->
                    emit(ApiResponse.Success(data))
                }
            } else {
                response.errorBody()?.let { error ->
                    error.close()
                    val parsedError: ErrorResponse = Gson().fromJson(error.charStream(), ErrorResponse::class.java)
                    emit(ApiResponse.Error(parsedError.status, parsedError.error))
                }
            }
        } catch (e: Exception) {
            emit(ApiResponse.Error(400, e.message ?: e.toString()))
        }
    } ?: emit(ApiResponse.Error(408, "Timeout! Please try again."))
}.flowOn(Dispatchers.IO)