package com.poe.poegps.feature.data.repository

import android.util.Log
import com.poe.poegps.feature.data.local.dao.ObjectsDao
import com.poe.poegps.feature.data.mapper.toDbModel
import com.poe.poegps.feature.data.mapper.toDomainModel
import com.poe.poegps.feature.data.mapper.toLinePillarDbModel
import com.poe.poegps.feature.data.mapper.toPsDbModel
import com.poe.poegps.feature.data.remote.api.ObjectsApi
import com.poe.poegps.feature.data.remote.model.LoginRequest
import com.poe.poegps.feature.data.remote.utils.apiRequestFlow
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
    override fun auth(login: String, password: String) = apiRequestFlow {
        objectsApi.auth(LoginRequest(login, password))
    }

    override suspend fun downloadLines(filial: Int, token: String) {
        objectsApi.getLineObjects(filial, "Bearer $token")
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

    override fun searchLine(tplnr: String): Flow<List<Line>> {
        return objectsDao.getLines(tplnr)
            .map {
                it.map { line ->
                    line.toDomainModel()
                }
            }
    }

    override fun getLineList10(): Flow<List<Line>> {
        return objectsDao.getLineList10()
            .map {
                it.map { line ->
                    line.toDomainModel()
                }
            }
    }

    override fun getLineList04(): Flow<List<Line>> {
        return objectsDao.getLineList04()
            .map {
                it.map { line ->
                    line.toDomainModel()
                }
            }
    }

    override suspend fun downloadTPs(filial: Int, token: String) {
        objectsApi.getTpObjects(filial, "Bearer $token")
            .map { tp ->
                tp.toDomainModel()
            }
            .also { tps ->
                tps.map {
                    objectsDao.insertTp(it.toDbModel())
                }
            }
    }

    override fun getTpList(): Flow<List<Tp>> {
        return objectsDao.getTps()
            .map { tp ->
                tp.map {
                    it.toDomainModel()
                }
            }
    }

    override fun searchTP(filial: String, tplnr: String): Flow<List<Tp>> {
        return objectsDao.getTp(filial, tplnr)
            .map { tp ->
                tp.map {
                    it.toDomainModel()
                }
            }
    }

    override suspend fun downloadPss(token: String) {
        objectsApi.getPsObjects("Bearer $token")
            .map { ps ->
                ps.toDomainModel()
            }
            .also { pss ->
                pss.map {
                    objectsDao.insertPs(it.toPsDbModel())
                }
            }
    }

    override suspend fun getPsByTplnr(): Tp {
        TODO("Not yet implemented")
    }

    override suspend fun downloadPillars(filial: Int, token: String) {
        objectsApi.getPillarObjects(filial, "Bearer $token")
            .map { pillar ->
                pillar.toDomainModel()
            }
            .also { pillars ->
                pillars.map {
                    objectsDao.insertPillar(it.toDbModel())
                }
            }
    }


    override fun getPillarList(tplnr: String): Flow<List<Pillar>> {
        return objectsDao.getPillarsByTplnr(tplnr)
            .map { pillar ->
                pillar.map {
                    it.toDomainModel()
                }
            }
    }

    override fun getSavedPillars(pltxt: String): Flow<List<Pillar>> {
        return objectsDao.getPillarsOfLine(pltxt)
            .map { pillar ->
                pillar.map {
                    it.toDomainModel()
                }
            }
    }

    override suspend fun savePillar(pillar: Pillar): Long {
        return objectsDao.insertSavedPillar(pillar.toLinePillarDbModel())
        //Log.d("testim", "savePillar: $i")
    }

    override suspend fun deletePillar(pillar: Pillar) {
        objectsDao.deletePillar(pillar.toLinePillarDbModel())
    }

    override suspend fun getParentName(tplnr: String): String {
        return objectsDao.getParentName(tplnr)
    }

    override fun checkTokenValidity(token: String) = apiRequestFlow {
        objectsApi.checkTokenValidity("Bearer $token")
    }

    override suspend fun saveLine(line: Line) {
        objectsDao.insertOtpaika(line.toDbModel())
    }

}