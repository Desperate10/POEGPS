package com.poe.poegps.feature.data.remote.model

import com.poe.poegps.feature.presentation.model.ObjectDisplayable

data class ObjectState(
    val loadState: LoadState,
    val objects: List<ObjectDisplayable>,
    val objectType: ObjectType,
    val errorMessage: CharSequence,
) {
    companion object {
        val initial = ObjectState(
            loadState = LoadState.IDLE,
            objects = emptyList(),
            objectType = ObjectType.LINE,
            errorMessage = "",
        )
    }
}

enum class LoadState {
    IDLE,
    LOADING,
    LOADED,
    ERROR
}

enum class ObjectType(val type: String) {
    LINE("Лінія"),
    TP("ТП")
}

