package com.poe.poegps.feature.data.mapper

import com.poe.poegps.feature.data.local.entity.Line04DbModel
import com.poe.poegps.feature.data.remote.model.LineObjectsDTO
import com.poe.poegps.feature.domain.model.Line

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

fun Line.toDbModel() = Line04DbModel(
    id = 0,
    tplnr = tplnr,
    name = name
)