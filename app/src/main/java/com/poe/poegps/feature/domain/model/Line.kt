package com.poe.poegps.feature.domain.model

data class Line(
    val id: Int,
    val pid: Int,
    val name: String,
    val tplnr: String,
    val invnr: String,
    val category: String,
    val lineString: String
)
