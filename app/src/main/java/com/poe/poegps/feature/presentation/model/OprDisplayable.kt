package com.poe.poegps.feature.presentation.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OprDisplayable(
    val id: Int = 0,
    val tplnr: String,
    val name: String,
    var parentName: String,
    val order: Int = 0,
    var lat: String= "0.0",
    var lng: String= "0.0",
    val wire: String=""
) : Parcelable {
    override fun toString(): String {
        return name
    }
}