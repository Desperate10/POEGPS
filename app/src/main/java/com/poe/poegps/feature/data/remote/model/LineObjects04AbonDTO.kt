package com.poe.poegps.feature.data.remote.model

data class LineObjects04AbonDTO(
    val tplnr: String,
    val pltxt: String,
    val category: String = "0,4 кВ",
    val isAbon: Boolean = true
)
