package com.poe.poegps.feature.presentation.model

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
            objectType = ObjectType.LINE04,
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
    LINE04("Лінія 04"),
    LINE10("Лінія 10"),
    LINEABON04("Лінія 04 абон"),
    LINEABON10("Лінія 10 абон"),
}

