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
    LINE04("ПЛ 0,4"),
    LINE10("ПЛ 6-10"),
    /*LINE35("Лінія 35"),
    LINE110("Лінія 110"),
    LINE154("Лінія 154"),*/
    LINEABON04("ПЛ 0,4 абон"),
    LINEABON10("ПЛ 6-10 абон"),
    /*LINEABON35("Лінія 35 абон"),
    LINEABON110("Лінія 110 абон"),
    LINEABON154("Лінія 154 абон"),*/
    TP("ТП/РП"),
    TPABON("ТП/РП абон"),
    //PS("ПС"),
    KLKV04("КЛ/КВ 0,4"),
    KLKV10("КЛ/КВ 6-10"),
    KLKVABON04("КЛ/КВ 0,4 абон"),
    KLKVABON10("КЛ/КВ 6-10 абон")
}

enum class PillarType(val type: String) {
    KL("КЛ"), //заменить опоры на кл в кл/кв линиях
    PILLAR("Опора"),
    TP("ТП"),
    ABONTP("ТП"),
    PS("ПС")
}

