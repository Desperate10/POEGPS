package com.poe.poegps.feature.data.remote.model

data class KlObjects10DTO(
    val tplnr: String,
    val pltxt: String,
    val category: String = "10 кВ",
    val isAbon: Boolean = false
)
