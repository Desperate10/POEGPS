package com.poe.poegps.feature.presentation.mapper

import com.poe.poegps.feature.domain.model.Line
import com.poe.poegps.feature.domain.model.Pillar
import com.poe.poegps.feature.domain.model.Ps
import com.poe.poegps.feature.domain.model.Tp
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.OprDisplayable

fun Line.toObjectDisplayable() = ObjectDisplayable(
    id = id,
    tplnr = tplnr,
    name = name,
    category = category,
    pillarType = pillarType,
    isAbon = isAbon
)

fun Tp.toObjectDisplayable() = ObjectDisplayable(
    id = id,
    tplnr = tplnr,
    name = name,
    category = "10 кВ",//добавить категорию к запросу тп и пс
    pillarType = pillarType,
    isAbon = isAbon
)

fun Tp.toOprDisplayable() = OprDisplayable(
    id = id,
    name = name,
    parentName = "",
    category = "",
    isAbon = isAbon,
    pillarType = pillarType,
    lat = lat,
    lng = lng,
    tplnr = tplnr
)

fun Ps.toObjectDisplayable() = ObjectDisplayable(
    id = id,
    tplnr = tplnr,
    name = pltxt,
    category = "",//добавить категорию к запросу тп и пс
    pillarType = pillarType,
    isAbon = false
)

fun Pillar.toOprDisplayable() = OprDisplayable(
    id = id,
    name = name,
    parentName = parentName,
    category = category,
    isAbon = isAbon,
    pillarType = pillarType,
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
    category = category,
    isAbon = isAbon,
    pillarType = pillarType,
    wire = wire,
    lat = lat,
    lng = lng
)

/*fun ObjectDisplayable.toOprDisplayable() = OprDisplayable(
    name = name,
    parentName = "",
    order = 1,
    tplnr = tplnr
)*/

fun ObjectDisplayable.toLineDomainModel() = Line(
    id = id,
    tplnr = tplnr,
    name = name,
    pillarType = pillarType,
    category = category,
    isAbon = isAbon
)
