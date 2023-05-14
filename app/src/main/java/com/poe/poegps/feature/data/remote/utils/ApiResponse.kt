package com.poe.poegps.feature.data.remote.utils

sealed class ApiResponse<out T> {
    object Loading : ApiResponse<Nothing>()
    data class Success<out T>(val data: T) : ApiResponse<T>()
    data class Error(val status: Int, val message: String) : ApiResponse<Nothing>()
}