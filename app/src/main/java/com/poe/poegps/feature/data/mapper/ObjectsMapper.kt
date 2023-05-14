package com.poe.poegps.feature.data.mapper

import com.poe.poegps.feature.data.local.entity.Line04DbModel
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
    tplnr = tplnr,
    name = pltxt,
    category = ucat
)

fun Line04DbModel.toDomainModel() = Line(
    tplnr = tplnr,
    name = name,
    category = "0,4"
)

fun TpObjectsDTO.toDomainModel() = Tp(
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
    tplnr = tplnr,
    name = name,
    lat = lat,
    lng = lng
)

fun PillarObjectsDTO.toDomainModel() = Pillar(
    tplnr = tplnr,
    name = pltxt,
    wire = wire,
    lat = lat,
    lng = lng
)

fun Pillar.toDbModel() = PillarDbModel(
    tplnr = tplnr,
    name = name,
    wire = wire,
    lat = lat,
    lng = lng
)