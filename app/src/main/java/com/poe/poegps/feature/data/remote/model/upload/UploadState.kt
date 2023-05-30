package com.poe.poegps.feature.data.remote.model.upload

sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    data class Progress(val percentage: Int) : UploadState()
    object Complete : UploadState()
    data class Error(val message: String) : UploadState()
}