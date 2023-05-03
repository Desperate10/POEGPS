package com.poe.poegps.feature.data.repository

import com.poe.poegps.feature.domain.model.Line
import com.poe.poegps.feature.domain.model.Pillar
import com.poe.poegps.feature.domain.model.Tp
import com.poe.poegps.feature.domain.repository.ObjectsRepository
import kotlinx.coroutines.flow.Flow

class ObjectsRepositoryImpl : ObjectsRepository {

    override fun getLines(): Flow<List<Line>> {
        TODO("Not yet implemented")
    }

    override fun getTPs(): Flow<List<Tp>> {
        TODO("Not yet implemented")
    }

    override fun getPillars(tplnr: String): Flow<List<Pillar>> {
        TODO("Not yet implemented")
    }

    override suspend fun saveCoordinatesOfLine(line: Line) {
        TODO("Not yet implemented")
    }

    override suspend fun savePillars(pillars: List<Pillar>) {
        TODO("Not yet implemented")
    }

    override suspend fun saveCoordinatesOfTp(tp: Tp) {
        TODO("Not yet implemented")
    }
}