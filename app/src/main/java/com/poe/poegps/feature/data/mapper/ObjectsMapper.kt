package com.poe.poegps.feature.data.mapper

import com.poe.poegps.feature.data.local.entity.*
import com.poe.poegps.feature.data.remote.model.*
import com.poe.poegps.feature.domain.model.Line
import com.poe.poegps.feature.domain.model.Pillar
import com.poe.poegps.feature.domain.model.Tp

fun LineObjects04DTO.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    category = "0,4 кВ",
    isAbon = false
)

fun LineObjects10DTO.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    category = "10 кВ",
    isAbon = false
)

fun LineObjects04AbonDTO.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    category = "0,4 кВ",
    isAbon = true
)

fun LineObjects10AbonDTO.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    category = "10 кВ",
    isAbon = true
)

fun Line04DbModel.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = name,
    category = "0,4 кВ",
    isAbon = false
)

fun Line10DbModel.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = name,
    category = "10 кВ",
    isAbon = false
)

fun AbonLine04DbModel.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = name,
    category = "0,4 кВ",
    isAbon = true
)

fun AbonLine10DbModel.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = name,
    category = "10 кВ",
    isAbon = true
)

fun TpObjectsDTO.toDomainModel() = Tp(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    lat = lat,
    lng = lng,
    isAbon = false
)

fun PsModelDTO.toDomainModel() = Tp(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    lat = lat,
    lng = lng,
    isAbon = false
)

fun TpObjectsAbonDTO.toDomainModel() = Tp(
    id = 0,
    name = pltxt,
    tplnr = tplnr,
    lat = lat,
    lng = lng,
    isAbon = true
)

fun Tp.toDbModel() = TpDbModel(
    tplnr = tplnr,
    pltxt = name,
    lat = lat,
    lng = lng
)

fun Tp.toAbonDbModel() = AbonTpDbModel(
    tplnr = tplnr,
    pltxt = name,
    lat = lat,
    lng = lng
)

fun Tp.toPsDbModel() = PsDbModel(
    tplnr = tplnr,
    name = name,
    lat = lat,
    lng = lng
)

fun Line.toLine04DbModel() = Line04DbModel(
    tplnr = tplnr,
    name = name
)

fun Line.toLine10DbModel() = Line10DbModel(
    tplnr = tplnr,
    name = name
)

fun Line.toAbonLine04DbModel() = AbonLine04DbModel(
    tplnr = tplnr,
    name = name
)

fun Line.toAbonLine10DbModel() = AbonLine10DbModel(
    tplnr = tplnr,
    name = name
)

fun TpDbModel.toDomainModel() = Tp(
    id= id,
    tplnr = tplnr,
    name = pltxt,
    isAbon = false,
    lat = lat,
    lng = lng
)

fun PillarObjects04DTO.toDomainModel() = Pillar(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    parentName = "",
    category = category,
    isAbon = isAbon,
    wire = wire,
    lat = lat,
    lng = lng
)

fun PillarObjects10DTO.toDomainModel() = Pillar(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    parentName = "",
    category = category,
    isAbon = isAbon,
    wire = wire,
    lat = lat,
    lng = lng
)

fun Pillar.to04DbModel() = Pillar04DbModel(
    id = id,
    tplnr = tplnr,
    name = name,
    wire = wire,
    lng = lng,
    lat = lat

)

fun Pillar.to10DbModel() = Pillar10DbModel(
    id = id,
    tplnr = tplnr,
    name = name,
    wire = wire,
    lat = lat,
    lng = lng
)

fun Pillar04DbModel.toDomainModel() = Pillar(
    id = id,
    tplnr = tplnr,
    name = name,
    parentName = "",
    category = "0,4 кВ",
    isAbon = false,
    wire = wire,
    lat = lat,
    lng = lng
)

fun Pillar10DbModel.toDomainModel() = Pillar(
    id = id,
    tplnr = tplnr,
    name = name,
    parentName = "",
    category = "10 кВ",
    isAbon = false,
    wire = wire,
    lat = lat,
    lng = lng
)

fun LinePillarDbModel.toDomainModel() = Pillar(
    id = id,
    tplnr = tplnr,
    name = name,
    parentName = parentName,
    category = category,
    isAbon = isAbon,
    wire = wire,
    lat = lat,
    lng = lng
)

fun Pillar.toLinePillarDbModel() = LinePillarDbModel(
    id = id,
    tplnr = tplnr,
    name = name,
    parentName = parentName,
    category = category,
    isAbon = isAbon,
    wire = wire,
    lat = lat,
    lng = lng
)