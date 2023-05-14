package com.poe.poegps.feature.data.remote.model

data class ErrorResponse(
    val status: Int,
    val error: String,
    val timestamp: String
)
