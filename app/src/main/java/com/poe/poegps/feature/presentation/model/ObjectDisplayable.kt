package com.poe.poegps.feature.presentation.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ObjectDisplayable(
    val id: Int = 0,
    var tplnr: String,
    val name: String,
    val category: String,
    val pillarType: String,
    val isAbon: Boolean
) : Parcelable {
    override fun toString(): String {
        return "$tplnr $name"
    }
}