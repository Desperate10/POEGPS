package com.poe.poegps.feature.data.mapper

import com.poe.poegps.feature.data.local.entity.Line04DbModel
import com.poe.poegps.feature.data.local.entity.LinePillarDbModel
import com.poe.poegps.feature.data.local.entity.PillarDbModel
import com.poe.poegps.feature.data.local.entity.PsDbModel
import com.poe.poegps.feature.data.local.entity.TpDbModel
import com.poe.poegps.feature.data.remote.model.LineObjectsDTO
import com.poe.poegps.feature.data.remote.model.PillarObjectsDTO
import com.poe.poegps.feature.data.remote.model.TpObjectsDTO
import com.poe.poegps.feature.domain.model.Line
import com.poe.poegps.feature.domain.model.Pillar
import com.poe.poegps.feature.domain.model.Tp

fun LineObjectsDTO.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = pltxt
)

fun Line04DbModel.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = name
)

fun TpObjectsDTO.toDomainModel() = Tp(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    lat = lat,
    lng = lng
)

fun Tp.toDbModel() = TpDbModel(
    tplnr = tplnr,
    name = name,
    lat = lat,
    lng = lng
)

fun Tp.toPsDbModel() = PsDbModel(
    tplnr = tplnr,
    name = name,
    lat = lat,
    lng = lng
)

fun Line.toDbModel() = Line04DbModel(
    tplnr = tplnr,
    name = name
)

fun TpDbModel.toDomainModel() = Tp(
    id= id,
    tplnr = tplnr,
    name = name,
    lat = lat,
    lng = lng
)

fun PillarObjectsDTO.toDomainModel() = Pillar(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    parentName = "",
    order = 0,
    wire = wire,
    lat = lat,
    lng = lng
)

fun Pillar.toDbModel() = PillarDbModel(
    id = id,
    tplnr = tplnr,
    name = name,
    wire = wire,
    lat = lat,
    lng = lng
)

fun PillarDbModel.toDomainModel() = Pillar(
    id = id,
    tplnr = tplnr,
    name = name,
    parentName = "",
    order = 0,
    wire = wire,
    lat = lat,
    lng = lng
)

fun LinePillarDbModel.toDomainModel() = Pillar(
    id = id,
    tplnr = tplnr,
    name = name,
    parentName = parentName,
    order = order,
    wire = wire,
    lat = lat,
    lng = lng
)

fun Pillar.toLinePillarDbModel() = LinePillarDbModel(
    id = id,
    tplnr = tplnr,
    name = name,
    parentName = parentName,
    order = order,
    wire = wire,
    lat = lat,
    lng = lng
)