package com.poe.poegps.feature.presentation.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecloserDisplayable(
    val id: Int = 0,
    val tplnr: String,
    val name: String,
    val opr: String,
    val type: RecloserType,
    val lat: String,
    val lng: String
) : Parcelable {
    override fun toString(): String {
        return name
    }
}

enum class RecloserType {
    RECLOSER,
    DISCONNECTOR;

    override fun toString(): String {
        return when (this) {
            RECLOSER -> "Реклоузер"
            DISCONNECTOR -> "Роз'єднувач"
        }
    }
}