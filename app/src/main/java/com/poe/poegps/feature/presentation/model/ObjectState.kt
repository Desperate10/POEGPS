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
    /*LINE35("Лінія 35"),
    LINE110("Лінія 110"),
    LINE154("Лінія 154"),*/
    LINEABON04("Лінія 04 абон"),
    LINEABON10("Лінія 10 абон"),
    /*LINEABON35("Лінія 35 абон"),
    LINEABON110("Лінія 110 абон"),
    LINEABON154("Лінія 154 абон"),*/
    TP("ТП"),
    TPABON("ТП абон"),
    PS("ПС"),
}

enum class PillarType(val type: String) {
    PILLAR("Опора"),
    TP("ТП"),
    PS("ПС")
}

