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
