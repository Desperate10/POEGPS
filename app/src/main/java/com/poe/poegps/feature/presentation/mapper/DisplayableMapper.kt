package com.poe.poegps.feature.presentation.mapper

import com.poe.poegps.feature.domain.model.Line
import com.poe.poegps.feature.domain.model.Tp
import com.poe.poegps.feature.presentation.model.ObjectDisplayable

fun Line.toObjectDisplayable() = ObjectDisplayable(
    tplnr = tplnr,
    name = name
)

fun Tp.toObjectDisplayable() = ObjectDisplayable(
    tplnr = tplnr,
    name = name
)