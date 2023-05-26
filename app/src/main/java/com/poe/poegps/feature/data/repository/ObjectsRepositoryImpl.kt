package com.poe.poegps.feature.data.repository

import android.util.Log
import com.poe.poegps.feature.data.local.dao.ObjectsDao
import com.poe.poegps.feature.data.mapper.*
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

    override suspend fun downloadLines(filial: String, token: String) {
        objectsApi.getLine04Objects(filial, "Bearer $token")
            .map { line ->
                line.toDomainModel()
            }
            .also { lines ->
                lines.map {
                    objectsDao.insertLine04(it.toLine04DbModel())
                }
            }
        objectsApi.getLine10Objects(filial, "Bearer $token")
            .map { line ->
                line.toDomainModel()
            }
            .also { lines ->
                lines.map {
                    objectsDao.insertLine10(it.toLine10DbModel())
                }
            }
        //abon
        objectsApi.getLine04AbonObjects(filial, "Bearer $token")
            .map { line ->
                line.toDomainModel()
            }
            .also { lines ->
                lines.map {
                    objectsDao.insertAbonLine04(it.toAbonLine04DbModel())
                }
            }
        objectsApi.getLine10AbonObjects(filial, "Bearer $token")
            .map { line ->
                line.toDomainModel()
            }
            .also { lines ->
                lines.map {
                    objectsDao.insertAbonLine10(it.toAbonLine10DbModel())
                }
            }
    }

    override suspend fun downloadTPs(filial: String, token: String) {
        objectsApi.getTpObjects(filial, "Bearer $token")
            .map { tp ->
                tp.toDomainModel()
            }
            .also { tps ->
                tps.map {
                    objectsDao.insertTp(it.toDbModel())
                }
            }
        //abon
        objectsApi.getAbonTpObjects(filial, "Bearer $token")
            .map { tp ->
                tp.toDomainModel()
            }
            .also { tps ->
                tps.map {
                    objectsDao.insertAbonTp(it.toAbonDbModel())
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
                    Log.d("testim", "downloadPss: $it")
                    objectsDao.insertPs(it.toPsDbModel())
                }
            }
    }

    override suspend fun downloadPillars(filial: String, token: String) {
        objectsApi.getPillarObjects04(filial, "Bearer $token")
            .map { pillar ->
                pillar.toDomainModel()
            }
            .also { pillars ->
                pillars.map {
                    objectsDao.insertPillar04(it.to04DbModel())
                }
            }
        objectsApi.getPillarObjects10(filial, "Bearer $token")
            .map { pillar ->
                pillar.toDomainModel()
            }
            .also { pillars ->
                pillars.map {
                    objectsDao.insertPillar10(it.to10DbModel())
                }
            }
    }

    override fun searchLine04(tplnr: String): Flow<List<Line>> {
        return objectsDao.getLines04(tplnr)
            .map {
                it.map { line ->
                    line.toDomainModel()
                }
            }
    }

    override fun searchAbonLine10(tplnr: String): Flow<List<Line>> {
        return objectsDao.getAbonLines10(tplnr)
            .map {
                it.map { line ->
                    line.toDomainModel()
                }
            }
    }

    override fun searchAbonLine04(tplnr: String): Flow<List<Line>> {
        return objectsDao.getAbonLines04(tplnr)
            .map {
                it.map { line ->
                    line.toDomainModel()
                }
            }
    }

    override fun searchLine10(tplnr: String): Flow<List<Line>> {
        return objectsDao.getLines10(tplnr)
            .map {
                it.map { line ->
                    line.toDomainModel()
                }
            }
    }

    override fun searchTP(tplnr: String, abonState: Boolean): Flow<List<Tp>> {
        return objectsDao.getTp(tplnr)
            .map { tp ->
                tp.map {
                    it.toDomainModel()
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

    override fun getTpList(): Flow<List<Tp>> {
        return objectsDao.getTps()
            .map { tp ->
                tp.map {
                    it.toDomainModel()
                }
            }
    }

    override fun getPillarList(tplnr: String): Flow<List<Pillar>> {
        return objectsDao.getPillars04ByTplnr(tplnr)
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
    }

    override suspend fun copyPillarForOtp(pillar: Pillar) {
        return objectsDao.copyPillarForOtp(pillar.toLinePillarDbModel())
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

    override suspend fun saveLine04(line: Line) {
        objectsDao.insertOtpaika04(line.toLine04DbModel())
    }

    override suspend fun saveLine10(line: Line) {
        objectsDao.insertOtpaika10(line.toLine10DbModel())

    }

    override suspend fun saveAbonLine04(line: Line) {
        objectsDao.insertAbonOtpaika04(line.toAbonLine04DbModel())
    }

    override suspend fun saveAbonLine10(line: Line) {
        objectsDao.insertAbonOtpaika10(line.toAbonLine10DbModel())
    }

    override suspend fun deleteLine04(pltxt: String) {
        objectsDao.deleteLine04(pltxt)
        deleteSavedPillars(pltxt)
    }

    override suspend fun deleteLine10(pltxt: String) {
        objectsDao.deleteLine10(pltxt)
        deleteSavedPillars(pltxt)
    }

    override suspend fun deleteAbonLine04(pltxt: String) {
        objectsDao.deleteAbonLine04(pltxt)
        deleteSavedPillars(pltxt)
    }

    override suspend fun deleteAbonLine10(pltxt: String) {
        objectsDao.deleteAbonLine10(pltxt)
        deleteSavedPillars(pltxt)
    }

    private fun deleteSavedPillars(pltxt: String) {
        objectsDao.deleteSavedPillars(pltxt)
    }

}