package com.poe.poegps.feature.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.poe.poegps.feature.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectsDao {

    suspend fun insertLine04IfNotExist(line: Line04DbModel) {
        if (getLine04(line.tplnr) == null) {
            insertLine04(line)
        }
    }
    @Query("SELECT * FROM line04 WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getLine04(tplnr: String): Line04DbModel?

    suspend fun insertLine10IfNotExist(line: Line10DbModel) {
        if (getLine10(line.tplnr) == null) {
            insertLine10(line)
        }
    }

    @Query("SELECT * FROM line10 WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getLine10(tplnr: String): Line10DbModel?

    suspend fun insertAbonLine04IfNotExist(line: AbonLine04DbModel) {
        if (getAbonLine04(line.tplnr) == null) {
            insertAbonLine04(line)
        }
    }
    @Query("SELECT * FROM abon_line04 WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getAbonLine04(tplnr: String): AbonLine04DbModel?

    suspend fun insertAbonLine10IfNotExist(line: AbonLine10DbModel) {
        if (getAbonLine10(line.tplnr) == null) {
            insertAbonLine10(line)
        }
    }

    @Query("SELECT * FROM abon_line10 WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getAbonLine10(tplnr: String): AbonLine10DbModel?

    suspend fun insertTpIfNotExist(tp: TpDbModel) {
        if (getSingleTp(tp.tplnr) == null) {
            insertTp(tp)
        }
    }

    @Query("SELECT * FROM tp WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getSingleTp(tplnr: String): TpDbModel

    suspend fun insertAbonTpIfNotExist(tp: AbonTpDbModel) {
        if (getSingleAbonTp(tp.tplnr) == null) {
            insertAbonTp(tp)
        }
    }

    @Query("SELECT * FROM abon_tp WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getSingleAbonTp(tplnr: String): AbonTpDbModel

    suspend fun insertPsIfNotExist(ps: PsDbModel) {
        if (getSinglePs(ps.tplnr) == null) {
            insertPs(ps)
        }
    }

    @Query("SELECT * FROM ps WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getSinglePs(tplnr: String): PsDbModel?

    suspend fun insertPillar04IfNotExist(pillar: Pillar04DbModel) {
        if (getSinglePillar04(pillar.tplnr, pillar.name) == null) {
            insertPillar04(pillar)
        }
    }

    @Query("SELECT * FROM pillars04 WHERE tplnr = :tplnr AND name = :name LIMIT 1")
    suspend fun getSinglePillar04(tplnr: String, name: String): Pillar04DbModel?

    suspend fun insertPillar10IfNotExist(pillar: Pillar10DbModel) {
        if (getSinglePillar10(pillar.tplnr, pillar.name) == null) {
            insertPillar10(pillar)
        }
    }

    @Query("SELECT * FROM pillars10 WHERE tplnr = :tplnr AND name = :name LIMIT 1")
    suspend fun getSinglePillar10(tplnr: String, name: String): Pillar10DbModel?

    suspend fun insertWire04IfNotExist(wire: Wire04DbModel) {
        if (getSingleWire04(wire.name) == null) {
            insertWire04(wire)
        }
    }

    @Query("SELECT * FROM wire04 WHERE name = :name LIMIT 1")
    suspend fun getSingleWire04(name: String): Wire04DbModel?

    suspend fun insertWire10IfNotExist(wire: Wire10DbModel) {
        if (getSingleWire10(wire.name) == null) {
            insertWire10(wire)
        }
    }

    @Query("SELECT * FROM wire10 WHERE name = :name LIMIT 1")
    suspend fun getSingleWire10(name: String): Wire10DbModel?

    @Query("SELECT * FROM line_pillar WHERE tplnr = :tplnr LIMIT 1")
    suspend fun tpAlreadySaved(tplnr: String): LinePillarDbModel?

    suspend fun saveNewTpCoord(tp: LinePillarDbModel) {
        if (tpAlreadySaved(tp.tplnr) == null) {
            insertSavedPillar(tp)
        } else {
            updateSavedPillar(tp.lat, tp.lng, tp.tplnr)
        }
        if (tp.isAbon) {
            updateAbonTpCoords(tp.lat, tp.lng, tp.tplnr)
        } else {
            updateTpCoords(tp.lat, tp.lng, tp.tplnr)
        }
    }

    @Query("UPDATE tp SET lat = :lat, lng = :lng  WHERE tplnr = :tplnr")
    suspend fun updateTpCoords(lat: String, lng: String, tplnr: String)

    @Query("UPDATE abon_tp SET lat = :lat, lng = :lng  WHERE tplnr = :tplnr")
    suspend fun updateAbonTpCoords(lat: String, lng: String, tplnr: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWire04(wire: Wire04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWire10(wire: Wire10DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine04(line: Line04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine10(line: Line10DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbonLine04(line: AbonLine04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbonLine10(line: AbonLine10DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTp(tp: TpDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbonTp(tp: AbonTpDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPs(ps: PsDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPillar04(pillar: Pillar04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPillar10(pillar: Pillar10DbModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun copyPillarForOtp(pillar: LinePillarDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPillar(pillar: LinePillarDbModel): Long

    @Query("UPDATE line_pillar SET lat = :lat, lng = :lng WHERE tplnr = :tplnr")
    suspend fun updateSavedPillar(lat: String, lng: String, tplnr: String)

    @Delete
    suspend fun deletePillar(pillar: LinePillarDbModel)

    @Query("SELECT * FROM line_pillar WHERE lat <> '0.0' AND isSent = 0")
    suspend fun getSavedPillars(): List<LinePillarDbModel>

    @Query("SELECT * FROM line_pillar WHERE lat <> '0.0' AND tplnr LIKE :tplnr")
    suspend fun getSavedPillarsByTplnr(tplnr: String): List<LinePillarDbModel>

    @Query("SELECT * FROM tp WHERE lat <> '0.0' ORDER BY pltxt ASC")
    fun getTps(): Flow<List<TpDbModel>>

    @Query("SELECT * FROM abon_tp WHERE lat <> '0.0' ORDER BY pltxt ASC")
    fun getAbonTps(): Flow<List<TpDbModel>>

    @Query("SELECT * FROM line04 WHERE tplnr LIKE '%-L' || :tplnr || '%' GROUP BY name")
    fun getLines04(tplnr: String): Flow<List<Line04DbModel>>

    @Query("SELECT * FROM line10 WHERE tplnr LIKE '%-L' || :tplnr || '%' GROUP BY name")
    fun getLines10(tplnr: String): Flow<List<Line10DbModel>>

    @Query("SELECT * FROM abon_line04 WHERE tplnr LIKE '%-L' || :tplnr || '%' GROUP BY name")
    fun getAbonLines04(tplnr: String): Flow<List<AbonLine04DbModel>>

    @Query("SELECT * FROM abon_line10 WHERE tplnr LIKE '%-L' || :tplnr || '%' GROUP BY name")
    fun getAbonLines10(tplnr: String): Flow<List<AbonLine10DbModel>>

    @Query("SELECT * FROM line10 WHERE name LIKE 'ПЛ%'")
    fun getLineList10(): Flow<List<Line10DbModel>>

    @Query("SELECT * FROM line04 WHERE name LIKE 'ПЛ%'")
    fun getLineList04(): Flow<List<Line04DbModel>>

    @Query("SELECT * FROM line_pillar WHERE parentName LIKE :pltxt")
    fun getPillarsOfLine(pltxt: String): Flow<List<LinePillarDbModel>>

    @Query("SELECT * FROM pillars04 WHERE tplnr LIKE :tplnr")
    fun getPillars04ByTplnr(tplnr: String): Flow<List<Pillar04DbModel>>

    @Query("SELECT * FROM pillars10 WHERE tplnr LIKE :tplnr")
    fun getPillars10ByTplnr(tplnr: String): Flow<List<Pillar10DbModel>>

    @Query("SELECT * FROM tp WHERE tplnr LIKE '%-P'|| :tplnr || '%'")
    fun getTpFlow(tplnr: String): Flow<List<TpDbModel>>

    @Query("SELECT * FROM abon_tp WHERE tplnr LIKE '%-P'|| :tplnr || '%'")
    fun getAbonTpFlow(tplnr: String): Flow<List<AbonTpDbModel>>

    @Query("SELECT * FROM ps WHERE tplnr LIKE '%-P'|| :tplnr || '%'")
    fun getPs(tplnr: String): Flow<List<PsDbModel>>

    @Query("SELECT * FROM wire04")
    fun getWire04(): Flow<List<Wire04DbModel>>

    @Query("SELECT * FROM wire10")
    fun getWire10(): Flow<List<Wire10DbModel>>

    @Query("SELECT name FROM line04 WHERE tplnr LIKE :tplnr")
    suspend fun getParentName(tplnr: String): String

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtpaika04(otpaika: Line04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtpaika10(otpaika: Line10DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbonOtpaika04(otpaika: AbonLine04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbonOtpaika10(otpaika: AbonLine10DbModel)

    @Query("UPDATE tp SET lng = :lng, lat = :lat WHERE tplnr LIKE :tplnr")
    suspend fun updateTpCoordinates(tplnr: String, lng: String, lat: String)

    @Query("UPDATE abon_tp SET lng = :lng, lat = :lat WHERE tplnr LIKE :tplnr")
    suspend fun updateAbonTpCoordinates(tplnr: String, lng: String, lat: String)

    @Query("UPDATE ps SET lng = :lng, lat = :lat WHERE tplnr LIKE :tplnr")
    suspend fun updatePsCoordinates(tplnr: String, lng: String, lat: String)

    @Query("DELETE FROM line04 WHERE name LIKE :pltxt")
    suspend fun deleteLine04(pltxt: String)

    @Query("DELETE FROM line10 WHERE name LIKE :pltxt")
    suspend fun deleteLine10(pltxt: String)

    @Query("DELETE FROM abon_line04 WHERE name LIKE :pltxt")
    suspend fun deleteAbonLine04(pltxt: String)

    @Query("DELETE FROM abon_line10 WHERE name LIKE :pltxt")
    suspend fun deleteAbonLine10(pltxt: String)

    @Query("DELETE FROM line_pillar WHERE parentName LIKE :pltxt")
    suspend fun deleteSavedPillars(pltxt: String)

    @Query("UPDATE line_pillar SET isSent = 1 WHERE tplnr NOT IN (:tplnrList)")
    suspend fun updateStatus(tplnrList: List<String>)


}