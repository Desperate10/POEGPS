package com.poe.poegps.feature.presentation.mapper

import com.poe.poegps.feature.domain.model.Line
import com.poe.poegps.feature.domain.model.Pillar
import com.poe.poegps.feature.domain.model.Tp
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.OprDisplayable

fun Line.toObjectDisplayable() = ObjectDisplayable(
    id = id,
    tplnr = tplnr,
    name = name
)

fun Tp.toObjectDisplayable() = ObjectDisplayable(
    id = id,
    tplnr = tplnr,
    name = name
)

fun Tp.toOprDisplayable() = OprDisplayable(
    id = id,
    name = name,
    parentName = "",
    lat = lat,
    lng = lng,
    tplnr = tplnr
)

fun Pillar.toOprDisplayable() = OprDisplayable(
    id = id,
    name = name,
    parentName = parentName,
    order = order,
    lat = lat,
    lng = lng,
    tplnr = tplnr,
    wire = wire
)

fun OprDisplayable.toDomainModel() = Pillar(
    id = id,
    tplnr = tplnr,
    name = name,
    parentName = parentName,
    order = order,
    wire = wire,
    lat = lat,
    lng = lng
)

fun ObjectDisplayable.toOprDisplayable() = OprDisplayable(
    name = name,
    parentName = "",
    order = 1,
    tplnr = tplnr
)

fun ObjectDisplayable.toLineDomainModel() = Line(
    id = id,
    tplnr = tplnr,
    name = name
)
