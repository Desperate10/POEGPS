package com.poe.poegps.feature.data.repository

import com.poe.poegps.app.database.AppDatabase
import com.poe.poegps.feature.data.local.dao.ObjectsDao
import com.poe.poegps.feature.data.mapper.toDbModel
import com.poe.poegps.feature.data.mapper.toDomainModel
import com.poe.poegps.feature.data.remote.api.ObjectsApi
import com.poe.poegps.feature.domain.model.Line
import com.poe.poegps.feature.domain.model.Pillar
import com.poe.poegps.feature.domain.model.Tp
import com.poe.poegps.feature.domain.repository.ObjectsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObjectsRepositoryImpl @Inject constructor(
    private val objectsDao: ObjectsDao,
    private val objectsApi: ObjectsApi
) : ObjectsRepository {

    override suspend fun downloadLines(filial: Int) {
        objectsApi.getLineObjects(filial)
            .map { line ->
                line.toDomainModel()
            }
            .also { lines ->
                lines.map {
                    objectsDao.insertLine(it.toDbModel())
                }
            }
    }

    /*override fun getLowLines(): Flow<List<Line>> {
        TODO("Not yet implemented")
    }*/

    override fun getLinesByTplnr(tplnr: String): Flow<List<Line>> {
        return objectsDao.getLines(tplnr)
            .map {
                it.map { line ->
                    line.toDomainModel()
                }
            }
    }

    override suspend fun downloadTPs(filial: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun getTpList(): Flow<List<Tp>> {
        TODO("Not yet implemented")
    }

    override suspend fun getTpByTplnr(tplnr: String): Tp {
        TODO("Not yet implemented")
    }

    override suspend fun downloadPss() {
        TODO("Not yet implemented")
    }

    override suspend fun getPsByTplnr(): Tp {
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