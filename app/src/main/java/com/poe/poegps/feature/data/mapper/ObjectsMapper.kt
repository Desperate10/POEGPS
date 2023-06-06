package com.poe.poegps.feature.data.mapper

import com.poe.poegps.feature.data.local.entity.*
import com.poe.poegps.feature.data.remote.model.*
import com.poe.poegps.feature.domain.model.*
import com.poe.poegps.feature.presentation.model.PillarType

fun LineObjects04DTO.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    category = "0,4 кВ",
    pillarType = PillarType.PILLAR.name,
    isAbon = false
)

fun LineObjects10DTO.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    category = "10 кВ",
    pillarType = PillarType.PILLAR.name,
    isAbon = false
)

fun LineObjects04AbonDTO.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    category = "0,4 кВ",
    pillarType = PillarType.PILLAR.name,
    isAbon = true
)

fun LineObjects10AbonDTO.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    category = "10 кВ",
    pillarType = PillarType.PILLAR.name,
    isAbon = true
)

fun Line04DbModel.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = name,
    category = "0,4 кВ",
    pillarType = PillarType.PILLAR.name,
    isAbon = false
)

fun Line10DbModel.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = name,
    category = "10 кВ",
    pillarType = PillarType.PILLAR.name,
    isAbon = false
)

fun AbonLine04DbModel.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = name,
    pillarType = PillarType.PILLAR.name,
    category = "0,4 кВ",
    isAbon = true
)

fun AbonLine10DbModel.toDomainModel() = Line(
    id = 0,
    tplnr = tplnr,
    name = name,
    category = "10 кВ",
    pillarType = PillarType.PILLAR.name,
    isAbon = true
)

fun TpObjectsDTO.toDomainModel() = Tp(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    lat = lat,
    lng = lng,
    pillarType = PillarType.TP.name,
    isAbon = false
)

fun PsModelDTO.toDomainModel() = Ps(
    id = 0,
    tplnr = tplnr,
    pltxt = pltxt,
    category = ucat,
    pillarType = PillarType.PS.name,
    lat = lat,
    lng = lng
)

fun TpObjectsAbonDTO.toDomainModel() = Tp(
    id = 0,
    name = pltxt,
    tplnr = tplnr,
    pillarType = PillarType.ABONTP.name,
    lat = lat ?: "0.0",
    lng = lng ?: "0.0",
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

fun Ps.toPsDbModel() = PsDbModel(
    tplnr = tplnr,
    pltxt = pltxt,
    category = category,
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
    pillarType = PillarType.TP.name,
    lat = lat,
    lng = lng
)

fun AbonTpDbModel.toDomainModel() = Tp(
    id= id,
    tplnr = tplnr,
    name = pltxt,
    isAbon = false,
    pillarType = PillarType.ABONTP.name,
    lat = lat,
    lng = lng
)

fun PsDbModel.toDomainModel() = Ps(
    id= id,
    tplnr = tplnr,
    pltxt = pltxt,
    category = category,
    pillarType = PillarType.PS.name,
    lat = lat?:"0.0",
    lng = lng?:"0.0"
)

fun PillarObjects04DTO.toDomainModel() = Pillar(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    parentName = "",
    category = "0,4 кВ",
    isAbon = false,
    pillarType = PillarType.PILLAR.name,
    wire = wire,
    lat = lat,
    lng = lng
)

fun PillarObjects10DTO.toDomainModel() = Pillar(
    id = 0,
    tplnr = tplnr,
    name = pltxt,
    parentName = "",
    category = "10 кВ",
    pillarType = PillarType.PILLAR.name,
    isAbon = false,
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
    pillarType = PillarType.PILLAR.name,
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
    pillarType = PillarType.PILLAR.name,
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
    pillarType = pillarType,
    wire = wire,
    lat = lat,
    lng = lng
)

fun LinePillarDbModel.toDTObject() = LinePillarDTO(
    tplnr = tplnr,
    opr = name,
    lineName = parentName,
    ucat = category,
    isAbon = isAbon,
    pillarType = pillarType,
    wire = wire,
    lat = lat,
    lng = lng,
    isSent = isSent
)

fun Pillar.toLinePillarDbModel() = LinePillarDbModel(
    id = id,
    tplnr = tplnr,
    name = name,
    parentName = parentName,
    category = category,
    pillarType = pillarType,
    isAbon = isAbon,
    wire = wire,
    lat = lat,
    lng = lng
)

fun WireDTO.toWire() = Wire(
    name = name
)

fun Wire04DbModel.toDomainModel() = Wire(
    name = name
)

fun Wire10DbModel.toDomainModel() = Wire(
    name = name
)

fun Wire.toWire04DbModel() = Wire04DbModel(
    id=0,
    name = name
)

fun Wire.toWire10DbModel() = Wire10DbModel(
    id= 0,
    name = name
)