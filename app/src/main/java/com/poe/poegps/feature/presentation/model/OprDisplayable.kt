package com.poe.poegps.feature.presentation.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OprDisplayable(
    val tplnr: String,
    val name: String,
    val lat: String= "0.0",
    val lng: String= "0.0",
    val wire: String?
) : Parcelable