package com.poe.poegps.feature.presentation.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OprDisplayable(
    val id: Int = 0,
    val tplnr: String,
    val name: String,
    val parentName: String,
    val order: Int = 0,
    val lat: String= "0.0",
    val lng: String= "0.0",
    val wire: String=""
) : Parcelable